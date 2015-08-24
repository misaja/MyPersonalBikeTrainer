package com.nimbusware.mypersonalbiketrainer.svc;

import com.nimbusware.mypersonalbiketrainer.BeatRateSensorListener;
import com.nimbusware.mypersonalbiketrainer.CadenceSensorListener;
import com.nimbusware.mypersonalbiketrainer.ElapsedTimeListener;
import com.nimbusware.mypersonalbiketrainer.Globals;
import com.nimbusware.mypersonalbiketrainer.SensorFactory;
import com.nimbusware.mypersonalbiketrainer.SensorInfo;
import com.nimbusware.mypersonalbiketrainer.SensorSet;
import com.nimbusware.mypersonalbiketrainer.SpeedSensorListener;
import com.nimbusware.mypersonalbiketrainer.WorkSessionInfo;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class WorkSessionService extends Service {
	
	private final static String TAG = WorkSessionService.class.getSimpleName();
    private static final long MAX_SCAN_DURATION = 5000;
	
	private final IBinder mBinder = new LocalBinder();

    private final BluetoothAdapter.LeScanCallback mScanCallback =
            new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice sensor, int rssi, byte[] scanRecord) {
        	Log.d(TAG, "Wakeup scan result: DEVICE=" + sensor.getName());
        }
    };
	
	private final WorkSession mSession = new WorkSession();
    private final Handler mHandler = new Handler();

    private int mWheelSize;
	private String mHeartSensorAddr;
	private String mWheelSensorAddr;
	private String mCrankSensorAddr;
	private SensorSet mSensors;
	
	public boolean initSensors() {
        Log.d(TAG, "Initialize sensors request");
        
		if (mSession.isActive()) {
			Log.d(TAG, "Cannot initialize sensors: session is active");
			return false;
		}
		
		if (null != mSensors) {
			Log.d(TAG, "Closing sensors");
			// if we already have sensors, it's a refresh request
			mSensors.close(); // this also removes all previous listeners
			mSensors = null;
		}
		
        BluetoothManager manager = (BluetoothManager)
        		getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter adapter = manager.getAdapter();
        
        // this is strongly advised by BLE doc before attempting connection
		if (adapter.isDiscovering()) {
	        Log.d(TAG, "Cancelling pre-existing BLE discovery");
	        adapter.cancelDiscovery();
		}

		// sensor wakeup stuff: runs in background, stops scanning after some seconds
		// note that this should not be necessary, but it seems it actually is
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
            	Log.d(TAG, "BLE wakeup scan timeout");
            	adapter.stopLeScan(mScanCallback);
            }
        }, MAX_SCAN_DURATION);
        
		Log.d(TAG, "Performing BLE wakeup scan");
    	adapter.startLeScan(mScanCallback);

		Log.d(TAG, "Creating sensor set");
        SensorInfo sensorInfo = new SensorInfo(mHeartSensorAddr,
				mWheelSensorAddr, mCrankSensorAddr);
		mSensors = SensorFactory.getSensorSet(this, adapter,
				sensorInfo, mWheelSize);
		
		// this will span connection attempts in the background
		Log.d(TAG, "Opening sensors");
		mSensors.open();
		
		return true;
	}
	
	public void registerSensorListeners(BeatRateSensorListener bsl,
			SpeedSensorListener ssl, CadenceSensorListener csl) {
		Log.d("MYDEBUG", "registerListeners called");
		if (null != mSensors) {
			Log.d("MYDEBUG", "Registering external listeners");
			if (null != bsl) {
				Log.d("MYDEBUG", "Registering external HRM listener");
				mSensors.registerHeartListener(bsl);
			}
			if (null != ssl) {
				Log.d("MYDEBUG", "Registering external Speed listener");
				mSensors.registerWheelListener(ssl);
			}
			if (null != csl) {
				Log.d("MYDEBUG", "Registering external Cadence listener");
				mSensors.registerCrankListener(csl);
			}
		}
	}
	
	public void unregisterSensorListeners(BeatRateSensorListener bsl,
			SpeedSensorListener ssl, CadenceSensorListener csl) {
		if (null != mSensors) {
			if (null != bsl) {
				Log.d("MYDEBUG", "Unregistering external HRM listener");
				mSensors.unregisterHeartListener(bsl);
			}
			if (null != ssl) {
				Log.d("MYDEBUG", "Unregistering external Speed listener");
				mSensors.unregisterWheelListener(ssl);
			}
			if (null != csl) {
				Log.d("MYDEBUG", "Unregistering external Cadence listener");
				mSensors.unregisterCrankListener(csl);
			}
		}
	}
	
	public boolean hasSensors() {
		return  null != mSensors;
	}
	
	public boolean isSessionRunning() {
		return mSession.isActive();
	}
	
	public void startSession(final ElapsedTimeListener listener) {
		Log.d(TAG, "Starting work session");
		
		// will throw exception if already active 
		mSession.start(mSensors, listener);

		Log.d(TAG, "Work session started");
	}
	
	public void stopSession() {
		Log.d(TAG, "Stopping work session");

		// will throw exception if not active 
		mSession.stop();
		mSensors.close();
		
		Log.d(TAG, "Work session stopped");
	}
	
	public WorkSessionInfo getSessionData() {
		return mSession.getData(); // returns null id not active
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (null != intent) {
	        mHeartSensorAddr = intent.getStringExtra(Globals.HEART_SENSOR_ADDR);
	        mWheelSensorAddr = intent.getStringExtra(Globals.WHEEL_SENSOR_ADDR);
	        mCrankSensorAddr = intent.getStringExtra(Globals.CRANK_SENSOR_ADDR);
	        mWheelSize = intent.getIntExtra(Globals.WHEEL_SIZE, 0);
		}
		
		// TODO call startForeground(int id, Notification notification)
		// in order to keep the service as a top priority process, and
		// register a notification entry for interactive management
		
		initSensors();
		
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "Binding service");
		return mBinder;
	}
	
	@Override
	public void onDestroy() {
		Log.d(TAG, "Destroying service");
		if (mSession.isActive()) {
			mSession.stop();
		}
		if (null != mSensors) {
			// if we already have sensors, it's a refresh request
			mSensors.close(); // this also removes all previous listeners
			mSensors = null;
		}
		super.onDestroy();
	}

	public class LocalBinder extends Binder {
		public WorkSessionService getService() {
			return WorkSessionService.this;
		}
	}
}
