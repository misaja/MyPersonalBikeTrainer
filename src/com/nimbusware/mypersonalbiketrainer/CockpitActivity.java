package com.nimbusware.mypersonalbiketrainer;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.nimbusware.mypersonalbiketrainer.svc.WorkSessionService;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class CockpitActivity extends Activity {

	private static final String TAG = CockpitActivity.class.getSimpleName();
	
	// number of milliseconds between each iteration of the timed check
	// that detects the existence of ACTIVE sensors
	private static final int CHECK_INTERVAL = 2000;

	// maximum number of milliseconds sensors are allowed to be silent
	// before we assume they are all (temporarily?) disconnected
	private static final int SENSOR_TIMEOUT = 5000;
	
	private static final String DISCONNECTED = " * ";
	private static final String VOID = "";
	
	private final ServiceConnection mServiceConnection = new ServiceConnection() {
		
		// this method will be called each time we Resume this activity
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
        	Log.i(TAG, "Connecting to service");
        	
    		// get a handle to the service register our listeners with it
    		mSensorService = ((WorkSessionService.LocalBinder) service).getService();
    		
    		// set the default appearance of all gauges
    		syncGauges();
    		
    		// register all sensor listeners
    		mSensorService.registerSensorListeners(mHeartListener, mWheelListener, mCrankListener);
    		
    		if (mSensorService.isSessionRunning()) {
    			// active session on the run: register the session listener
    			mSensorService.registerSessionListener(mSessionListener);

    			// cannot Exit, can End the current session
    			mBtnExit.setEnabled(false);
    			mBtnStartStop.setEnabled(true);
				mBtnStartStop.setText(R.string.end);
    		} else {
    			// no active session: can Exit, can Start new session 
    			// if we have at least one sensor available for use
    			mBtnExit.setEnabled(true);
				mBtnStartStop.setText(R.string.start);
    			mBtnStartStop.setEnabled(mSensorService.hasSensors());
    		}
    		
			// start iterative check for sensor activity
    		// (will call syncGauges every N seconds)
			mStatusCheckTimer.schedule(new ActiveSensorsCheckTask(), CHECK_INTERVAL);
        }

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// nothing to do: this will never be called unless in
			// extreme circumstances
		}
    };
	
	// listener for the wheel sensor, if any
	private final SpeedSensorListener mWheelListener = new SpeedSensorListener() {
		@Override
		public void updateSpeed(final double kmh) {
			// this is called in a system thread:
			// need to run in the UI thread instead
			CockpitActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					
					// store the timestamp of the last notification:
					// used to detect inactive/disconnected sensors
					mLastWheelSensorRead = new Date();

					// speed is rounded to the nearest 0.5 value, so that the value presented
					// to the user does not flutter too much but is still decently reliable
					mViewSpeed.setText(String.format("%.1f", Globals.roundZeroFive(kmh)));
					
					// exploit the speed update notification to also update
					// the current workout distance, if any
					WorkSessionInfo data = mSensorService.getSessionData();
					if (null != data) {
						mViewDistance.setText(String.format("%.1f", data.getDistanceCovered()));
					}
				}
			});
		}
		
		@Override
		public void updateDistance(double meters) {
			// nothing to do: we track the distance covered only when a workout session is active,
			// and we pull the cumulative measurements directly from the session itself (see above)
		}

		@Override
		public void updateWheelRevsCount(int revs) {
			// nothing to do: we don't have any use for this info here
		}
	};
	
	// listener for the crank sensor, if any
	private final CadenceSensorListener mCrankListener = new CadenceSensorListener() {
	
		@Override
		public void updateCadence(final double rpm) {
			// this is called in a system thread:
			// need to run in the UI thread instead
			CockpitActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					
					// store the timestamp of the last notification:
					// used to detect inactive/disconnected sensors
					mLastCrankSensorRead = new Date();

					// truncate the value into an integer
					mViewCadence.setText(String.format("%d", (int) rpm));
				}
			});
		}

		@Override
		public void updateCrankRevsCount(int revs) {
			// nothing to do: we don't have any use for this info here
		}
	};
	
	// listener for the heart sensor, if any
	private final BeatRateSensorListener mHeartListener = new BeatRateSensorListener() {
		@Override
		public void updateBeatRate(final double bpm) {
			// this is called in a system thread:
			// need to run in the UI thread instead
			CockpitActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					
					// store the timestamp of the last notification:
					// used to detect inactive/disconnected sensors
					mLastHeartSensorRead = new Date();
					
					// truncate the value into an integer
					mViewCardio.setText(String.format("%d", (int) bpm));
				}
			});
		}
	};
	
	// listener for the session time
	private final ElapsedTimeListener mSessionListener = new ElapsedTimeListener() {
		@Override
		public void updateElapsedTime(final double seconds) {
			CockpitActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// this check is just to prevent the last Timer task(s) in the queue
					// from updating the UI after the session has been closed and the
					// chronometer has been reset to 00:00
					if (mSensorService.isSessionRunning()) {
						mViewChronometer.setText(DateUtils.formatElapsedTime((long) seconds));
					}
				}
			});
		}
	};
	
	// handler of the exit button
	private final View.OnClickListener mExitListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if (null != mSensorService) {
				
				if (mSensorService.isSessionRunning()) {
					// this should never happen, but just in case...
					mSensorService.stopSession();
				}

				// signal to the onPause method to shutdown the service
				mExiting = true;
			}
			
			// launch main activity (the onPause method will be called)
	        Intent intent = new Intent(CockpitActivity.this, MainActivity.class);
	        startActivity(intent);
		}
	};
	
	// handler of the start/stop button
	private final View.OnClickListener mStartStopListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (null != mSensorService) {
				if (mSensorService.isSessionRunning()) {
					// processing STOP SESSION command
					
					// get session data _before_ stopping
					long sessionId = mSensorService.getSessionData().getLocalId();
					
					// stop running session
					mSensorService.unregisterSessionListener(mSessionListener);
					mSensorService.stopSession();
					
					// reset all UI items to default
			        mViewDistance.setText(VOID);
			        mViewChronometer.setText(VOID);
					
					// change function of button to START SESSION
					mBtnStartStop.setText(R.string.start);
					
					// enable EXIT button
					mBtnExit.setEnabled(true);
					
					// enable navigation
					if (null != mMenu) {
						enableNavigation(true);
					}
					
					// launch session viewer
			        Intent intent = new Intent(CockpitActivity.this, SessionActivity.class);
			        intent.putExtra(Globals.WORKOUT_ID, sessionId);
			        startActivity(intent);
				} else if (mSensorService.hasSensors()) {
					// processing START SESSION command

					// start new session
					mSensorService.registerSessionListener(mSessionListener);
					mSensorService.startSession();

					// change function of button to END SESSION
					mBtnStartStop.setText(R.string.end);

					// disable EXIT button
					mBtnExit.setEnabled(false);
					
					// disable navigation
					if (null != mMenu) {
						enableNavigation(false);
					}
				} else {
					// trying to START SESSION with an
					// uninitialized sensor service
					Toast.makeText(
							CockpitActivity.this, 
							R.string.msg_sensor_uninitialized, 
							Toast.LENGTH_SHORT).show();
				}
			}
		}
	};
    
	private final Timer mStatusCheckTimer = new Timer();
    private Menu mMenu;
	private TextView mViewCardio;
	private TextView mViewSpeed;
	private TextView mViewCadence;
	private TextView mViewCardioUnit;
	private TextView mViewSpeedUnit;
	private TextView mViewCadenceUnit;
	private TextView mViewDistance;
	private TextView mViewChronometer;
	private Button mBtnExit;
	private Button mBtnStartStop;
	private int mWheelSize;
	private String mHeartSensorAddr;
	private String mWheelSensorAddr;
	private String mCrankSensorAddr;
	private WorkSessionService mSensorService;
	private Date mLastWheelSensorRead;
	private Date mLastCrankSensorRead;
	private Date mLastHeartSensorRead;
	private boolean mExiting;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
    	Log.i(TAG, "Creating activity");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cockpit);
        
		mViewCardio = (TextView) findViewById(R.id.valCardio);
		mViewCardioUnit = (TextView) findViewById(R.id.lblBpm);
        mViewSpeed = (TextView) findViewById(R.id.valSpeed);
        mViewSpeedUnit = (TextView) findViewById(R.id.lblKmh);
        mViewCadence = (TextView) findViewById(R.id.valCadence);
        mViewCadenceUnit = (TextView) findViewById(R.id.lblRpm);
        mViewChronometer = (TextView) findViewById(R.id.chronometer);
        mViewDistance = (TextView) findViewById(R.id.valDistance);
        mBtnExit = (Button) findViewById(R.id.btnExit);
        mBtnExit.setOnClickListener(mExitListener);
        mBtnStartStop = (Button) findViewById(R.id.btnStartStop);
        mBtnStartStop.setOnClickListener(mStartStopListener);

        SharedPreferences prefs = getSharedPreferences(MainActivity.class.getSimpleName(), MODE_PRIVATE);
        mHeartSensorAddr = prefs.getString(Globals.HEART_SENSOR_ADDR, null);
        mWheelSensorAddr = prefs.getString(Globals.WHEEL_SENSOR_ADDR, null);
        mCrankSensorAddr = prefs.getString(Globals.CRANK_SENSOR_ADDR, null);
        mWheelSize = prefs.getInt(Globals.WHEEL_SIZE, Globals.WHEEL_SIZE_DEFAULT);
        
        if (null == mHeartSensorAddr && null == mWheelSensorAddr && null == mCrankSensorAddr) {
			// not much to do, unfortunately...
        	Log.e(TAG, "No saved preferences");
			Toast.makeText(CockpitActivity.this, "No saved preferences! Quitting...", Toast.LENGTH_SHORT).show();
			finish();
			return;
        }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.cockpit, menu);
	    mMenu = menu;
	    return true;
	}

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_diary:
		        startActivity(new Intent(this, DiaryActivity.class));
				break;
			case R.id.action_settings:
		        startActivity(new Intent(this, MainActivity.class));
				break;
			case R.id.action_refresh:
				if (null != mSensorService) {
					mSensorService.restartSensors();
					mSensorService.registerSensorListeners(mHeartListener, mWheelListener, mCrankListener);
				}
				break;
		}
		return true;
	}

	@Override
	protected void onResume() {
    	Log.i(TAG, "Resuming activity");
    	
    	// attach to service
    	// this, after some time, will trigger the onServiceConnected handler
    	attachToService();
		
		super.onResume();
	}

    @Override
	protected void onPause() {
		super.onPause();

		Log.i(TAG, "Pausing activity");
		
		detachFromService();

		if (mExiting) {
			mExiting = false;
			stopMyService();
		}
	}
	
    @Override
	protected void onDestroy() {
		super.onDestroy();
		
		Log.i(TAG, "Destroying activity");
	}

	// called each time we Resume this activity 
	private void attachToService() {
		// pass global settings along to service
        Intent serviceIntent = new Intent(this, WorkSessionService.class);
		serviceIntent.putExtra(Globals.HEART_SENSOR_ADDR, mHeartSensorAddr);
		serviceIntent.putExtra(Globals.WHEEL_SENSOR_ADDR, mWheelSensorAddr);
		serviceIntent.putExtra(Globals.CRANK_SENSOR_ADDR, mCrankSensorAddr);
		serviceIntent.putExtra(Globals.WHEEL_SIZE, mWheelSize);
		
		// we start our service explicitly and we'll need to stop it explicitly;
		// note that after the service is started the first time,
		// all subsequent start calls actually do nothing
    	Log.i(TAG, "Starting service");
		if (null == startService(serviceIntent)) {
			// not much to do, unfortunately...
	    	Log.e(TAG, "Unable to start service");
			Toast.makeText(CockpitActivity.this, "Unable to start service! Quitting...", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		// bind to our service and get all the references
		// that are needed for direct interaction
    	Log.i(TAG, "Binding service");
		if (!bindService(serviceIntent, mServiceConnection, BIND_AUTO_CREATE)) {
			// not much to do, unfortunately...
        	Log.e(TAG, "Unable to bind service");
        	Toast.makeText(this, "Unable to reach sensor service", Toast.LENGTH_LONG).show();
        	finish();
		}
	}
    
	// called each time we Pause this activity and the user presses the Exit button 
    private void detachFromService() {
		// stop the background thread that checks for sensor activity
		mStatusCheckTimer.purge();
		
		if (null != mSensorService) {
    		// unregister all listeners and discard the service handle,
			// so that we know we are not connected any more
    		mSensorService.unregisterSensorListeners(mHeartListener, mWheelListener, mCrankListener);
    		if (mSensorService.isSessionRunning()) {
    			mSensorService.unregisterSessionListener(mSessionListener);
    		}
    		mSensorService = null;
		}
		
		// set the default appearance of all gauges
		syncGauges();
		
		// note that this will NOT trigger a call to 
		// ServiceConnection.onServiceDisconnected
		Log.i(TAG, "Unbinding service");
		unbindService(mServiceConnection);
    }
	
    // called only when the user clicks the Exit button 
	private void stopMyService() {
		Log.i(TAG, "Stopping service");
        Intent serviceIntent = new Intent(this, WorkSessionService.class);
        stopService(serviceIntent);
	}
	
	private void syncGauges() {
		if (null != mSensorService && mSensorService.hasSensors()) {
			// we are connected to the service: gauges should
			// convey the impression that we are waiting for new data
			// to arrive from sensors
			if (!isSensorActive(mLastHeartSensorRead)) {
				mViewCardio.setText(DISCONNECTED);
			}
			if (!isSensorActive(mLastWheelSensorRead)) {
		        mViewSpeed.setText(DISCONNECTED);
			}
			if (!isSensorActive(mLastCrankSensorRead)) {
		        mViewCadence.setText(DISCONNECTED);
			}
			mViewCardioUnit.setText(R.string.bpm);
			mViewSpeedUnit.setText(R.string.kmh);
			mViewCadenceUnit.setText(R.string.rpm);
		} else {
			// we are NOT connected to the service: gauges should
			// convey the impression that they are dead
			mViewCardio.setText(VOID);
	        mViewSpeed.setText(VOID);
	        mViewCadence.setText(VOID);
			mViewCardioUnit.setText(VOID);
	        mViewSpeedUnit.setText(VOID);
	        mViewCadenceUnit.setText(VOID);
	    	mViewDistance.setText(VOID);
	    	mViewChronometer.setText(VOID);
		}
	}
	
	private boolean isSensorActive(Date lastRead) {
		// we assume a sensor is not active if we've never received a notification,
		// or if the last notification received is older than SENSOR_TIMEOUT
		if (null != lastRead) {
			return new Date().getTime() - lastRead.getTime() < SENSOR_TIMEOUT ?
					true : false;
		} else {
			return false;
		}
	}
	
	private void enableNavigation(boolean enabled) {
        mMenu.findItem(R.id.action_diary).setEnabled(enabled);
        mMenu.findItem(R.id.action_settings).setEnabled(enabled);
	}
	
	// this deferred task is responsible for resetting gauges to
	// a default state if and when their corresponding sensor is
	// disconnected
	private class ActiveSensorsCheckTask extends TimerTask {
		@Override
		public void run() {
			// this is called in a background thread:
			// need to run in the UI thread instead
			CockpitActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// need to obtain an exclusive lock even if running in the UI thread,
					// as we may still incur in some race condition against the service connection
					synchronized (CockpitActivity.this) {
						
						// is any sensor is inactive, reset the UI accordingly
						syncGauges();
						
						// if the sensor service is connected, the activity is currently
						// in the foreground: we need to reschedule this task
						// (but the task will be smart enough to detect, when run,
						// the actual activity status at that very moment)
						if (null != mSensorService) { 
							mStatusCheckTimer.schedule(new ActiveSensorsCheckTask(), CHECK_INTERVAL);
						}
					}
				}
			});
		}
	}
}
