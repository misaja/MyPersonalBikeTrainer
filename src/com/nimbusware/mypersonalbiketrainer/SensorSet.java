package com.nimbusware.mypersonalbiketrainer;

import android.os.AsyncTask;

public class SensorSet implements Sensor {
	
	private final BeatRateSensor mHeartSensor;
	private final SpeedSensor mWheelSensor;
	private final CadenceSensor mCrankSensor;
	
	private boolean mOperationPending;
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
	
	public synchronized boolean isOpen() {
		return mIsOpen;
	}
	
	public synchronized boolean isBusy() {
		return mOperationPending;
	}

	@Override
	public synchronized boolean open() {
		if (!mOperationPending && !mIsOpen) {
			mOperationPending = true;
			Opener opener = new Opener();
			opener.execute(false);
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public synchronized boolean reopen() {
		mOperationPending = true;
		Opener opener = new Opener();
		opener.execute(true);
		return true;
	}

	@Override
	public synchronized void close() {
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
		mOperationPending = false;
	}
	
	private class Opener extends AsyncTask<Boolean, Void, Void> {

		@Override
		protected Void doInBackground(Boolean... params) {
			synchronized (SensorSet.this) {
				boolean open = false;
				if (null != mHeartSensor && !mHeartSensor.isOpen()) {
					open = open || doSynchronously(mHeartSensor, params[0]);
				}
				if (null != mWheelSensor && !mWheelSensor.isOpen()) {
					open = open || doSynchronously(mWheelSensor, params[0]);
				}
				// wheel and crank may be (and usually are) monitored by the same sensor
				if (null != mCrankSensor && mCrankSensor != mWheelSensor && !mCrankSensor.isOpen()) {
					open = open || doSynchronously(mCrankSensor, params[0]);
				}
				
				// for us to be in "open" state, at least one sensor should be so
				mIsOpen = open;
				mOperationPending = false;
			}
			return null;
		}
		
		private boolean doSynchronously(Sensor sensor, boolean isRefresh) {
			boolean open = false;
			
			if (isRefresh) {
				sensor.reopen();
			} else {
				sensor.open();
			}
			
			// spend max 10 seconds waiting for a working device
			// before trying to open/refresh the next one
			for (int i = 100; i > 0; i--) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (!sensor.isBusy()) {
					open = sensor.isOpen();
					break;
				}
			}
			
			// TODO how can we manage a situation where we reached
			// the timeout and still we have a connection is progress?
			// there should be some way of aborting the system BLE
			// threads
			
			return open;
		}
	}
}
