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
			opener.execute((Void)null);
			return true;
		} else {
			return false;
		}
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
	
	private class Opener extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			synchronized (SensorSet.this) {
				boolean open = false;
				if (null != mHeartSensor && !mHeartSensor.isOpen()) {
					boolean result = openSynchronously(mHeartSensor);
					open = open || result;
				}
				if (null != mWheelSensor && !mWheelSensor.isOpen()) {
					boolean result = openSynchronously(mWheelSensor);
					open = open || result;
				}
				// wheel and crank may be (and usually are) monitored by the same sensor
				if (null != mCrankSensor && mCrankSensor != mWheelSensor && !mCrankSensor.isOpen()) {
					boolean result = openSynchronously(mCrankSensor);
					open = open || result;
				}
				
				// for us to be in "open" state, at least one sensor should be so
				mIsOpen = open;
				mOperationPending = false;
			}
			return null;
		}
		
		private boolean openSynchronously(Sensor sensor) {
			boolean open = false;
			
			// spend max 100 seconds waiting for a working device
			// before trying to open the next one
			sensor.open();
			for (int i = 1000; i > 0; i--) {
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
