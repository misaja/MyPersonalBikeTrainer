package com.nimbusware.mypersonalbiketrainer;

import android.os.AsyncTask;

public class SensorSet implements Sensor {
	
	private final BeatRateSensor mHeartSensor;
	private final SpeedSensor mWheelSensor;
	private final CadenceSensor mCrankSensor;
	
	private boolean mIsOpen;
	
	public SensorSet(BeatRateSensor heartSensor, SpeedSensor wheelSensor, CadenceSensor crankSensor) {
		mHeartSensor = heartSensor;
		mWheelSensor = wheelSensor;
		mCrankSensor = crankSensor;
	}

	public void registerHeartListener(BeatRateSensorListener listener) {
		if (null != mHeartSensor) {
			mHeartSensor.registerListener(listener);
		}
	}

	public void unregisterHeartListener(BeatRateSensorListener listener) {
		if (null != mHeartSensor) {
			mHeartSensor.unregisterListener(listener);
		}
	}
	
	public void registerWheelListener(SpeedSensorListener listener) {
		if (null != mWheelSensor) {
			mWheelSensor.registerListener(listener);
		}
	}

	public void unregisterWheelListener(SpeedSensorListener listener) {
		if (null != mWheelSensor) {
			mWheelSensor.unregisterListener(listener);
		}
	}

	public void registerCrankListener(CadenceSensorListener listener) {
		if (null != mCrankSensor) {
			mCrankSensor.registerListener(listener);
		}
	}

	public void unregisterCrankListener(CadenceSensorListener listener) {
		if (null != mCrankSensor) {
			mCrankSensor.unregisterListener(listener);
		}
	}
	
	public boolean isOpen() {
		return mIsOpen;
	}

	@Override
	public boolean open() {
		if (!mIsOpen) {
			Opener opener = new Opener();
			opener.execute((Void)null);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void close() {
		if (mIsOpen) {
			if (null != mHeartSensor) {
				mHeartSensor.close();
			}
			if (null != mWheelSensor) {
				mWheelSensor.close();
			}
			if (null != mCrankSensor && mCrankSensor != mWheelSensor) {
				// wheel and crank may be served by the same sensor
				mCrankSensor.close();
			}
			mIsOpen = false;
		}
	}
	
	private class Opener extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			synchronized (SensorSet.this) {
				if (null != mHeartSensor && !mHeartSensor.isOpen()) {
					openSynchronously(mHeartSensor);
				}
				if (null != mWheelSensor && !mWheelSensor.isOpen()) {
					openSynchronously(mWheelSensor);
				}
				// wheel and crank may be (and usually are) monitored by the same sensor
				if (null != mCrankSensor && mCrankSensor != mWheelSensor && !mCrankSensor.isOpen()) {
					openSynchronously(mCrankSensor);
				}
				mIsOpen = true;
			}
			return null;
		}
		
		private boolean openSynchronously(Sensor sensor) {
			boolean open = false;
			// max 10 seconds waiting for a working device
			// BEWARE: as this code works now it will wait until
			// timeout even if NO sensor is actually there!
			// a possible improvement might be to start waiting
			// only after a connection is established, but this
			// would require Sensor to expose more of its inner state
			// (e.g., add isConnected and isConnecting calls)
			for (int i = 100; i > 0; i--) {
				sensor.open();
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (sensor.isOpen()) {
					open = true;
					break;
				}
			}
			return open;
		}
	}
}
