package com.nimbusware.android.trainer.service;

import com.nimbusware.mypersonalbiketrainer.BeatRateSensorListener;
import com.nimbusware.mypersonalbiketrainer.CadenceSensorListener;
import com.nimbusware.mypersonalbiketrainer.CockpitActivity;
import com.nimbusware.mypersonalbiketrainer.ElapsedTimeListener;
import com.nimbusware.mypersonalbiketrainer.Globals;
import com.nimbusware.mypersonalbiketrainer.R;
import com.nimbusware.mypersonalbiketrainer.SensorFactory;
import com.nimbusware.mypersonalbiketrainer.SensorInfo;
import com.nimbusware.mypersonalbiketrainer.SensorSet;
import com.nimbusware.mypersonalbiketrainer.SpeedSensorListener;
import com.nimbusware.mypersonalbiketrainer.WorkSessionInfo;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class WorkSessionService extends Service {

	private final static String TAG = WorkSessionService.class.getSimpleName();

	private final IBinder mBinder = new LocalBinder();
	private final WorkSession mSession = new WorkSession();
	private SensorSet mSensors;
	private int mWheelSize;
	private String mHeartSensorAddr;
	private String mWheelSensorAddr;
	private String mCrankSensorAddr;
	
	public void refreshSensors() {
		// this will span connection attempts in the background
		Log.i(TAG, "Refreshing sensors");
		mSensors.reopen();
		Log.i(TAG, "Sensor opening requested, waiting for reply");
	}

	public void registerSensorListeners(BeatRateSensorListener bsl,
			SpeedSensorListener ssl, CadenceSensorListener csl) {
		if (null != mSensors) {
			if (null != bsl) {
				mSensors.registerHeartListener(bsl);
			}
			if (null != ssl) {
				mSensors.registerWheelListener(ssl);
			}
			if (null != csl) {
				mSensors.registerCrankListener(csl);
			}
		}
	}

	public void unregisterSensorListeners(BeatRateSensorListener bsl,
			SpeedSensorListener ssl, CadenceSensorListener csl) {
		if (null != mSensors) {
			if (null != bsl) {
				mSensors.unregisterHeartListener(bsl);
			}
			if (null != ssl) {
				mSensors.unregisterWheelListener(ssl);
			}
			if (null != csl) {
				mSensors.unregisterCrankListener(csl);
			}
		}
	}
	
	public void registerSessionListener(ElapsedTimeListener listener) {
		mSession.registerListener(listener);
	}
	
	public void unregisterSessionListener(ElapsedTimeListener listener) {
		mSession.unregisterListener(listener);
	}

	public boolean hasSensors() {
		return null != mSensors;
	}

	public boolean isSessionRunning() {
		return mSession.isActive();
	}

	public void startSession() {
		Log.i(TAG, "Starting work session");

		// will throw exception if already active
		mSession.start(mSensors);

		Log.i(TAG, "Work session started");
	}

	public void stopSession() {
		Log.i(TAG, "Stopping work session");

		// will throw exception if not active
		mSession.stop();

		Log.i(TAG, "Work session stopped");
	}

	public WorkSessionInfo getSessionData() {
		return mSession.getData(); // returns null id not active
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "Processing start command");
		
		if (null != intent) {
			retrieveSettings(intent);
		}

		if(null == mSensors) { 
			initSensors();
		}

		Intent activityIntent = new Intent(getApplicationContext(),
				CockpitActivity.class);
		activityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				activityIntent, 0);
		
		Notification notification = new Notification.Builder(this)
				.setSmallIcon(R.drawable.ic_launcher)
				.setTicker("Bike Trainer started")
				.setContentTitle("Bike Trainer")
				.setContentText("Bike Trainer is running")
				.setContentIntent(pendingIntent)
				.setAutoCancel(false)
				.setOngoing(true) // user cannot dismiss this notification
				.build();
		
		startForeground(101, notification);
		
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.i(TAG, "Binding service");
		return mBinder;
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "Destroying service");
		super.onDestroy();
		stopForeground(true);
		if (mSession.isActive()) {
			mSession.stop();
		}
		if (null != mSensors) {
			mSensors.close();
			mSensors = null;
		}
	}

	public class LocalBinder extends Binder {
		public WorkSessionService getService() {
			return WorkSessionService.this;
		}
	}
	
	private void retrieveSettings(Intent intent) {
		Log.i(TAG, "Retrieving saved settings");
		mHeartSensorAddr = intent.getStringExtra(Globals.HEART_SENSOR_ADDR);
		mWheelSensorAddr = intent.getStringExtra(Globals.WHEEL_SENSOR_ADDR);
		mCrankSensorAddr = intent.getStringExtra(Globals.CRANK_SENSOR_ADDR);
		mWheelSize = intent.getIntExtra(Globals.WHEEL_SIZE, 0);
	}

	private void initSensors() {
		Log.i(TAG, "Initializing sensors");
		
		BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		final BluetoothAdapter adapter = manager.getAdapter();

		// this is strongly advised by BLE doc before attempting connection
		if (adapter.isDiscovering()) {
			Log.i(TAG, "Cancelling pre-existing BLE discovery");
			adapter.cancelDiscovery();
		}

		Log.i(TAG, "Creating sensor set");
		SensorInfo sensorInfo = new SensorInfo(mHeartSensorAddr,
				mWheelSensorAddr, mCrankSensorAddr);
		mSensors = SensorFactory.getSensorSet(this, adapter, sensorInfo,
				mWheelSize);

		// this will span connection attempts in the background
		Log.i(TAG, "Opening sensors");
		mSensors.open();
		Log.i(TAG, "Sensor opening requested, waiting for reply");
	}
}
