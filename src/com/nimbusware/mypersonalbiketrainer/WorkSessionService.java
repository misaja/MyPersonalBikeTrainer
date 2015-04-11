package com.nimbusware.mypersonalbiketrainer;

import java.util.Date;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class WorkSessionService extends Service {
	
	private final static String TAG = WorkSessionService.class.getSimpleName();
	
	private final IBinder mBinder = new LocalBinder();
	
	private final SpeedSensorListener mWheelListener = new SpeedSensorListener() {
		
		@Override
		public void updateSpeed(double kmh) {
			mSession.addSpeedReading(kmh);
		}
		
		@Override
		public void updateDistance(double meters) {
			mSession.addDistanceCovered(meters);
		}

		@Override
		public void updateRevolutions(int revs) {
			mSession.addWheelRevolutions(revs);
		}
	};
	
	private final CadenceSensorListener mCrankListener = new CadenceSensorListener() {
		
		@Override
		public void updateBeatRate(double rpm) {
			mSession.addCrankCadenceReading(rpm);
		}

		@Override
		public void updateRevolutions(int revs) {
			mSession.addCrankRevolutions(revs);
		}
	};
	
	private final BeatRateSensorListener mHeartListener = new BeatRateSensorListener() {
		
		@Override
		public void updateBeatRate(double bpm) {
			mSession.addHeartCadenceReading(bpm);
		}
	};
	
	private SensorSet mSensors;
	private WorkSessionData mSession = new WorkSessionData();
	
	public boolean isInitialized() {
		return null != mSensors;
	}
	
	public void initialize(SensorSet sensors) {
		Log.d(TAG, "Service is being initialized");
		mSensors = sensors;
		mSensors.open();
	}
	
	public boolean isSessionRunning() {
		return mSession.isRunning();
	}
	
	public void startSession() {
		if (mSession.isRunning())
			throw new IllegalStateException("Work session is already running");
		Log.d(TAG, "Starting work session");
		mSensors.registerWheelListener(mWheelListener);
		mSensors.registerCrankListener(mCrankListener);
		mSensors.registerHeartListener(mHeartListener);
		mSession.start();
		Log.d(TAG, "Work session started");
	}
	
	public void stopSession() {
		if (!mSession.isRunning())
			throw new IllegalStateException("Work session is not running");
		
		Log.d(TAG, "Stopping work session");
		mSession.stop();
		mSensors.unregisterWheelListener(mWheelListener);
		mSensors.unregisterCrankListener(mCrankListener);
		mSensors.unregisterHeartListener(mHeartListener);
		mSensors.close();
		Log.d(TAG, "Work session stopped");
	}
	
	public WorkSession getSessionData() {
		return mSession;
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "Binding service");
		return mBinder;
	}

    @Override
    public boolean onUnbind(Intent intent) {
		Log.d(TAG, "Unbinding service");
    	mSession.stop();
    	if (null != mSensors) {
    		Log.d(TAG, "Closing sensors");
    		mSensors.close();
    		mSensors = null;
    	}
        return super.onUnbind(intent);
    }
	
	public class LocalBinder extends Binder {
		WorkSessionService getService() {
			return WorkSessionService.this;
		}
	}
	

	static class WorkSessionData implements WorkSession {
		
		private static final Date NEVER = new Date(0);
		
		private Date mStartTime;
		private Date mEndTime;
		private double mDistanceCovered;
		private int mWheelRevs;
		private int mCrankRevs;
		private double mHeartBeats;
		private double mMaxSpeed;
		private double mMaxCrankCadence;
		private double mMaxHeartCadence;
		private Date mLastHeartCadenceRedingTs = NEVER;

		// TODO support for "cardio zone" . i.e., target min/max BPM values
		// (significance of cardio fitness factor depends on this)
		
		private void start() {
			mStartTime = new Date();
			mEndTime = null;
			mDistanceCovered = 0;
			mWheelRevs = 0;
			mCrankRevs = 0;
			mHeartBeats = 0;
			mMaxSpeed = 0;
			mMaxCrankCadence = 0;
			mMaxHeartCadence = 0;
			mLastHeartCadenceRedingTs = NEVER;
		}
		
		private void stop() {
			mEndTime = new Date();
		}
		
		private boolean isRunning() {
			return null != mStartTime && null == mEndTime;
		}
		
		private void addDistanceCovered(double meters) {
			if (!isRunning())
				return;
			mDistanceCovered += meters;
		}
		
		private void addSpeedReading(double kmh) {
			if (!isRunning())
				return;
			mMaxSpeed = kmh > mMaxSpeed ? kmh : mMaxSpeed;
		}
		
		private void addCrankCadenceReading(double rpm) {
			if (!isRunning())
				return;
			mMaxCrankCadence = rpm > mMaxCrankCadence ? rpm : mMaxCrankCadence;
		}
		
		private void addHeartCadenceReading(double bpm) {
			if (!isRunning())
				return;
			Date now = new Date();
			mMaxHeartCadence = bpm > mMaxHeartCadence ? bpm : mMaxHeartCadence;
			if (!mLastHeartCadenceRedingTs.equals(NEVER)) {
				mHeartBeats += getUnitsInTimeInterval(mLastHeartCadenceRedingTs, now, bpm);
			}
			mLastHeartCadenceRedingTs = now;
		}
		
		private void addWheelRevolutions(int revs) {
			mWheelRevs += revs;
		}
		
		private void addCrankRevolutions(int revs) {
			mCrankRevs += revs;
		}
		
		@Override
		public Date getStartTime() {
			return mStartTime;
		}
		
		@Override
		public Date getEndTime() {
			return mEndTime;
		}
		
		@Override
		public double getElapsedTimeSeconds() {
			if (null != mStartTime) {
				Date stopTime = null != mEndTime ? mEndTime : new Date();
				return ((double) (stopTime.getTime() - mStartTime.getTime())) / 1000;
			} else {
				return 0;
			}
		}
		
		@Override
		public double getDistanceCoveredKms() {
			return mDistanceCovered / 1000d;
		}

		@Override
		public double getMaxSpeed() {
			return mMaxSpeed;
		}
		
		@Override
		public double getAverageSpeed() {
			double elapsed = getElapsedTimeSeconds();
			if (elapsed > 0) {
				return getDistanceCoveredKms() / elapsed * 3600;
			} else {
				return 0;
			}
		}

		@Override
		public double getMaxCrankCadence() {
			return mMaxCrankCadence;
		}
		
		@Override
		public double getAverageCrankCadence() {
			double elapsed = getElapsedTimeSeconds();
			if (elapsed > 0) {
				return mCrankRevs / elapsed * 60;
			} else {
				return 0;
			}
		}
		
		@Override
		public double getAverageGearRatio() {
			if (mCrankRevs > 0) {
				return ((double) mWheelRevs) / ((double) mCrankRevs);
			} else {
				return 0;
			}
		}

		@Override
		public double getMaxHeartCadence() {
			return mMaxHeartCadence;
		}
		
		@Override
		public double getAverageHeartCadence() {
			double elapsed = getElapsedTimeSeconds();
			if (elapsed > 0) {
				return mHeartBeats / elapsed * 60;
			} else {
				return 0;
			}
		}
		
		@Override
		public double getCardioFitnessFactor() {
			// TODO also check if session was "in zone"
			if (mHeartBeats > 0) {
				return mDistanceCovered / mHeartBeats;
			} else {
				return 0;
			}
		}

		private double getUnitsInTimeInterval(Date lastReading, Date now, double upm) {
			double millisecondsElapsed = now.getTime() - lastReading.getTime();
			double minutesElapsed = millisecondsElapsed / 60000;
			return upm * minutesElapsed;
		}
	}
}
