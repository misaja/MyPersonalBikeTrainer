package com.nimbusware.mypersonalbiketrainer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
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

public class CockpitActivity extends Activity {

	private static final String TAG = CockpitActivity.class.getSimpleName();

	private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
        	Log.d(TAG, "Connecting to service");
            mSensorService = ((WorkSessionService.LocalBinder) service).getService();
    		mSensorService.registerListeners(mHeartListener, mWheelListener, mCrankListener);
    		syncUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        	Log.d(TAG, "Disconnecting from service");
    		mSensorService.unregisterListeners(mHeartListener, mWheelListener, mCrankListener);
    		mSensorService = null;
        }
    };
	
	private final SpeedSensorListener mWheelListener = new SpeedSensorListener() {
		@Override
		public void updateSpeed(final double kmh) {
			CockpitActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Double oldValue = (Double) mViewSpeed.getTag();
					if (kmh > 0 || null == oldValue || oldValue.equals(0)) {
						mViewSpeed.setText(String.format("%.1f", Globals.roundZeroFive(kmh)));
						// clumsy and inefficient as this is fixed text, but needs to show up
						// only when an actual value is printed
						mViewSpeedUnit.setText(R.string.kmh);
					}
					mViewSpeed.setTag(Double.valueOf(kmh));
					syncUI();
				}
			});
		}
		
		@Override
		public void updateDistance(double meters) {
			// nothing to do: we use session for this
		}

		@Override
		public void updateRevolutions(int revs) {
			// nothing to do: we use session for this
		}
	};
	
	private final CadenceSensorListener mCrankListener = new CadenceSensorListener() {
		@Override
		public void updateBeatRate(final double rpm) {
			final int val = (int) rpm;
			CockpitActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Integer oldValue = (Integer) mViewCadence.getTag();
					if (rpm > 0 || null == oldValue || oldValue.equals(0)) {
						mViewCadence.setText(String.format("%d", val));
						// clumsy and inefficient as this is fixed text, but needs to show up
						// only when an actual value is printed
						mViewCadenceUnit.setText(R.string.rpm);
					}
					mViewCadence.setTag(Integer.valueOf(val));
					syncUI();
				}
			});
		}
		
		@Override
		public void updateRevolutions(int revs) {
			// nothing to do: we use session for this
		}
	};
	
	private final BeatRateSensorListener mHeartListener = new BeatRateSensorListener() {
		@Override
		public void updateBeatRate(final double bpm) {
			final int val = (int) bpm;
			CockpitActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Integer oldValue = (Integer) mViewCardio.getTag();
					if (val > 0 || null == oldValue || oldValue.equals(0)) {
						mViewCardio.setText(String.format("%d", val));
						// clumsy and inefficient as this is fixed text, but needs to show up
						// only when an actual value is printed
						mViewCardioUnit.setText(R.string.bpm);
					}
					mViewCardio.setTag(Integer.valueOf(val));
					syncUI();
				}
			});
		}
	};
	
	private final View.OnClickListener mButtonListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (null != mSensorService) {
				if (mSensorService.isSessionRunning()) {
					mSensorService.stopSession();
					setUIDefaults();
					
					long sessionId = saveSession();
					
			        Intent intent = new Intent(CockpitActivity.this, SessionActivity.class);
			        intent.putExtra(Globals.SESSION_ID, sessionId);
			        startActivity(intent);
				} else {
					mSensorService.startSession();
					syncUI();
				}
			}
		}
	};
    
    private Menu mMenu;
	private TextView mViewCardio;
	private TextView mViewSpeed;
	private TextView mViewCadence;
	private TextView mViewCardioUnit;
	private TextView mViewSpeedUnit;
	private TextView mViewCadenceUnit;
	private TextView mViewChronometer;
	private TextView mViewDistance;
	private Button mBtnStartStop;
	private WorkSessionService mSensorService;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
    	Log.d(TAG, "Creating activity");
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
        mBtnStartStop = (Button) findViewById(R.id.btnStartStop);
        mBtnStartStop.setOnClickListener(mButtonListener);
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.cockpit, menu);
	    mMenu = menu;
		syncUI();
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
					mSensorService.initSensors();
					mSensorService.registerListeners(mHeartListener, mWheelListener, mCrankListener);
					syncUI();
				}
				break;
		}
		return true;
	}

	@Override
	protected void onResume() {
    	Log.d(TAG, "Resuming activity");
		Intent serviceIntent = new Intent(this, WorkSessionService.class);
		if (bindService(serviceIntent, mServiceConnection, BIND_AUTO_CREATE)) {
	    	Log.d(TAG, "Service binding successful");
		} else {
        	Log.e(TAG, "Unable to bind to service");
		}
		super.onResume();
	}

    @Override
	protected void onPause() {
		Log.d(TAG, "Pausing activity");
		unbindService(mServiceConnection);
		super.onPause();
	}

	private long saveSession() {
		WorkSession session = mSensorService.getSessionData();
		ContentValues values = new ContentValues();
		values.put(DiaryContract.COL_START, session.getStartTime().getTime());
		values.put(DiaryContract.COL_END, session.getEndTime().getTime());
		values.put(DiaryContract.COL_ELAPSED, session.getElapsedTimeSeconds());
		values.put(DiaryContract.COL_DISTANCE, session.getDistanceCoveredKms());
		values.put(DiaryContract.COL_CARDIO_MAX, session.getMaxHeartCadence());
		values.put(DiaryContract.COL_CARDIO_AVG, session.getAverageHeartCadence());
		values.put(DiaryContract.COL_SPEED_MAX, session.getMaxSpeed());
		values.put(DiaryContract.COL_SPEED_AVG, session.getAverageSpeed());
		values.put(DiaryContract.COL_CADENCE_MAX, session.getMaxCrankCadence());
		values.put(DiaryContract.COL_CADENCE_AVG, session.getAverageCrankCadence());
		values.put(DiaryContract.COL_GEAR, session.getAverageGearRatio());
		values.put(DiaryContract.COL_FITNESS, session.getCardioFitnessFactor());
		Uri uri = getContentResolver().insert(DiaryContract.CONTENT_URI, values);
		return ContentUris.parseId(uri);
	}
	
	private void syncUI() {
		if (null != mSensorService && mSensorService.isSessionRunning()) {
			WorkSession session = mSensorService.getSessionData();
			mViewDistance.setText(String.format("%.1f km", session.getDistanceCoveredKms()));
			mViewChronometer.setText(DateUtils.formatElapsedTime((long) session.getElapsedTimeSeconds()));
			mBtnStartStop.setText(R.string.end);
			if (null != mMenu) {
				setMenuItems(false);
			}
		} else {
	        mViewDistance.setText("");
	        mViewChronometer.setText("00:00");
			mBtnStartStop.setText(R.string.start);
			if (null != mMenu) {
				setMenuItems(true);
			}
		}
	}
	
	private void setUIDefaults() {
		mViewCardio.setText("");
        mViewSpeed.setText("");
        mViewCadence.setText("");
		mViewCardioUnit.setText("");
        mViewSpeedUnit.setText("");
        mViewCadenceUnit.setText("");
        mViewDistance.setText("");
        mViewChronometer.setText("00:00");
		mBtnStartStop.setText(R.string.start);
		if (null != mMenu) {
			setMenuItems(true);
		}
	}
	
	private void setMenuItems(boolean enabled) {
        mMenu.findItem(R.id.action_diary).setEnabled(enabled);
        mMenu.findItem(R.id.action_settings).setEnabled(enabled);
        mMenu.findItem(R.id.action_refresh).setEnabled(enabled);
	}
}
