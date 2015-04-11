package com.nimbusware.mypersonalbiketrainer;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;

public class CockpitActivity extends Activity {

	private static final String TAG = CockpitActivity.class.getSimpleName();
    private static final long MAX_SCAN_DURATION = 5000;
	
	public static Intent getLauncher(Context context) {
		Intent intent = new Intent(context, CockpitActivity.class);
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.class.getSimpleName(), MODE_PRIVATE);
        String heartSensorAddr = prefs.getString(Globals.HEART_SENSOR_ADDR, null);
        String wheelSensorAddr = prefs.getString(Globals.WHEEL_SENSOR_ADDR, null);
        String crankSensorAddr = prefs.getString(Globals.CRANK_SENSOR_ADDR, null);
        int wheelSize = prefs.getInt(Globals.WHEEL_SIZE, Globals.WHEEL_SIZE_DEFAULT);
		intent.putExtra(Globals.HEART_SENSOR_ADDR, heartSensorAddr);
		intent.putExtra(Globals.WHEEL_SENSOR_ADDR, wheelSensorAddr);
		intent.putExtra(Globals.CRANK_SENSOR_ADDR, crankSensorAddr);
		intent.putExtra(Globals.WHEEL_SIZE, wheelSize);
		return intent;
	}

	private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
        	Log.d(TAG, "Connecting to service");
            mSensorService = ((WorkSessionService.LocalBinder) service).getService();
    		Log.d(TAG, "Initializing service");
    		mSensorService.initialize(mSensors);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        	Log.d(TAG, "Disconnecting from service");
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
					if (null != mSensorService && mSensorService.isSessionRunning()) {
						mViewDistance.setText(String.format("%.1f km", mSensorService.getSessionData().getDistanceCoveredKms()));
					}
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
					mChronometer.stop();
					
					long sessionId = saveSession();
					
					setDefaults();
					setMenuItems(true);
			        Intent intent = new Intent(CockpitActivity.this, SessionActivity.class);
			        intent.putExtra(Globals.SESSION_ID, sessionId);
			        startActivity(intent);
				} else {
					setMenuItems(false);
					mBtnStartStop.setText(R.string.end);
					mSensorService.startSession();
					mChronometer.setBase(SystemClock.elapsedRealtime());
					mChronometer.start();
				}
			}
		}
	};


    private final BluetoothAdapter.LeScanCallback mScanCallback =
            new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice sensor, int rssi, byte[] scanRecord) {
        	Log.d(TAG, "Wakeup scan result: DEVICE=" + sensor.getName());
        }
    };
    
    private Handler mHandler;
    private Menu mMenu;
	private TextView mViewCardio;
	private TextView mViewSpeed;
	private TextView mViewCadence;
	private TextView mViewCardioUnit;
	private TextView mViewSpeedUnit;
	private TextView mViewCadenceUnit;
	private TextView mViewDistance;
	private Button mBtnStartStop;
	private Chronometer mChronometer;
	private String mHeartSensorAddr;
	private String mWheelSensorAddr;
	private String mCrankSensorAddr;
	private int mWheelSize;
	private BluetoothAdapter mAdapter;
	private SensorSet mSensors;
	private WorkSessionService mSensorService;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cockpit);

    	Log.d(TAG, "Creating activity");
        
        Intent intent = getIntent();
        mHeartSensorAddr = intent.getStringExtra(Globals.HEART_SENSOR_ADDR);
        mWheelSensorAddr = intent.getStringExtra(Globals.WHEEL_SENSOR_ADDR);
        mCrankSensorAddr = intent.getStringExtra(Globals.CRANK_SENSOR_ADDR);
        mWheelSize = intent.getIntExtra(Globals.WHEEL_SIZE, 0);
        
        if ((null == mHeartSensorAddr &&
        		null == mWheelSensorAddr &&
        		null == mCrankSensorAddr) ||
        		mWheelSize == 0) {
        	Log.e(TAG, "Required arguments missing: existing");
        	finish();
        	return;
        }

        mHandler = new Handler();
		mViewCardio = (TextView) findViewById(R.id.valCardio);
		mViewCardioUnit = (TextView) findViewById(R.id.lblBpm);
        mViewSpeed = (TextView) findViewById(R.id.valSpeed);
        mViewSpeedUnit = (TextView) findViewById(R.id.lblKmh);
        mViewCadence = (TextView) findViewById(R.id.valCadence);
        mViewCadenceUnit = (TextView) findViewById(R.id.lblRpm);
        mViewDistance = (TextView) findViewById(R.id.valDistance);
        mBtnStartStop = (Button) findViewById(R.id.btnStartStop);
        mBtnStartStop.setOnClickListener(mButtonListener);
        mChronometer = (Chronometer) findViewById(R.id.chronometer);
    	
        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mAdapter = manager.getAdapter();
		
		if (mAdapter.isDiscovering()) {
	        Log.d(TAG, "Cancelling discovery");
			mAdapter.cancelDiscovery();
		}

		scanSensors();
		
		Log.d(TAG, "Creating sensor set");
		SensorInfo sensorInfo = new SensorInfo(mHeartSensorAddr, mWheelSensorAddr, mCrankSensorAddr);
		mSensors = SensorFactory.getSensorSet(this, mAdapter, sensorInfo, mWheelSize);
		
		Log.d(TAG, "Creating service binding");
		Intent serviceIntent = new Intent(this, WorkSessionService.class);
		if (!bindService(serviceIntent, mServiceConnection, BIND_AUTO_CREATE)) {
        	Log.e(TAG, "Unable to bind to service: exiting");
            finish();
            return;
		}
        
    	Log.d(TAG, "Activity successfully created");
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
				// TODO
				break;
		}
		return true;
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		
    	Log.d(TAG, "Restarting activity");
        
    	// this will reconnect all sensors IF they where previously disconnected
    	// (if not, the call has no effect)
    	//mSensors.open();
		
    	Log.d(TAG, "Activity successfully restarted");
	}

	@Override
	protected void onResume() {
		super.onResume();

    	Log.d(TAG, "Resuming activity");
        
		mSensors.registerWheelListener(mWheelListener);
		mSensors.registerCrankListener(mCrankListener);
		mSensors.registerHeartListener(mHeartListener);
		
    	Log.d(TAG, "Activity successfully resumed");
	}

    @Override
	protected void onPause() {
		Log.d(TAG, "Pausing activity");
    	
		mSensors.unregisterWheelListener(mWheelListener);
		mSensors.unregisterCrankListener(mCrankListener);
		mSensors.unregisterHeartListener(mHeartListener);

		super.onPause();

    	Log.d(TAG, "Activity successfully paused");
	}

	@Override
    protected void onDestroy() {
    	Log.d(TAG, "Destroying activity");

        unbindService(mServiceConnection);
        mSensorService = null;
        mSensors = null;

    	super.onDestroy();
    	
    	Log.d(TAG, "Activity successfully destroyed");
    }

    private void scanSensors() {
        // Stops scanning after a pre-defined scan period.
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
            	Log.d(TAG, "Aborting wakeup scan");
                mAdapter.stopLeScan(mScanCallback);
            }
        }, MAX_SCAN_DURATION);
    	Log.d(TAG, "Performing wakeup scan");
        mAdapter.startLeScan(mScanCallback);
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
	
	private void setDefaults() {
		mViewCardio.setText("");
        mViewSpeed.setText("");
        mViewCadence.setText("");
		mViewCardioUnit.setText("");
        mViewSpeedUnit.setText("");
        mViewCadenceUnit.setText("");
        mViewDistance.setText("");
		mBtnStartStop.setText(R.string.start);
        mChronometer.setText("00:00");
	}
	
	private void setMenuItems(boolean enabled) {
        mMenu.findItem(R.id.action_diary).setEnabled(enabled);
        mMenu.findItem(R.id.action_settings).setEnabled(enabled);
        mMenu.findItem(R.id.action_refresh).setEnabled(enabled);
	}
}
