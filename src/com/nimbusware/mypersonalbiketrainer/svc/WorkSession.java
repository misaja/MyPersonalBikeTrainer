package com.nimbusware.mypersonalbiketrainer.svc;

import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;
import android.util.Log;

import com.nimbusware.mypersonalbiketrainer.BeatRateSensorListener;
import com.nimbusware.mypersonalbiketrainer.CadenceSensorListener;
import com.nimbusware.mypersonalbiketrainer.DiaryContract;
import com.nimbusware.mypersonalbiketrainer.ElapsedTimeListener;
import com.nimbusware.mypersonalbiketrainer.Globals;
import com.nimbusware.mypersonalbiketrainer.SensorSet;
import com.nimbusware.mypersonalbiketrainer.SpeedSensorListener;
import com.nimbusware.mypersonalbiketrainer.WorkSessionInfo;

class WorkSession {

	private final static String TAG = WorkSession.class.getSimpleName();
	private final static int SESSION_TICK_TIMING = 1000;
	private final static int SESSION_LOG_TIMING = 10000;
	
	private SensorSet mSensors;
	private Data mData;
	private Timer mSessionTickTimer;
	private Timer mSessionLogTimer;
	
	WorkSessionInfo getData() {
		return mData;
	}
	
	boolean isActive() {
		return null != mData;
	}
	
	void start(SensorSet sensors, final ElapsedTimeListener listener) {
		if (null == sensors) // fail fast
			throw new NullPointerException(); 
		
		if (null != mData) // defensive
			throw new IllegalStateException("Work session is already running");
		
		Log.d(TAG, "Starting work session");
		
		// write placeholder record in DB and get back assigned _ID
		// (this is a _synchronous_ operation)
		String uniqueId = UUID.randomUUID().toString();
		Date startTime = new Date();
		long localId = createMasterRecord(uniqueId, startTime);
		if (localId > 0) {
			Log.d(TAG, "Session master record inserted with Id=" + localId);
		} else {
			// this is impossible, afaik: no reason not to
			// crash the entire app if I'm wrong...
			throw new RuntimeException("Could not create session's master record");
		}
		
		// we successfully wrote our master record down:
		// session can now "officially" start
		mData = new Data(localId, uniqueId, startTime);

		mSensors = sensors;
		mSensors.registerWheelListener(mData);
		mSensors.registerCrankListener(mData);
		mSensors.registerHeartListener(mData);

		if (null != listener) { // listener is optional
			mSessionTickTimer = new Timer();
			mSessionTickTimer.schedule(new TimerTask() {
				
				@Override
				public void run() {
					listener.updateElapsedTime(mData.getElapsedTime());
				}
				
			}, 0, SESSION_TICK_TIMING);
		}
		
		mSessionLogTimer = new Timer();
		mSessionLogTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				try {
					createLogRecord(mData);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
		}, SESSION_LOG_TIMING, SESSION_LOG_TIMING);
		
		Log.d(TAG, "Work session started: localId=" + localId + 
		", uniqueId=" + uniqueId + ", startTime=" + startTime);
	}
	
	public void stop() {
		if (null == mData)
			throw new IllegalStateException("No work session is running");
		
		Log.d(TAG, "Stopping work session");
		
		// stop saving log entries
		mSessionLogTimer.cancel();
		mSessionLogTimer.purge();
		mSessionLogTimer = null;
		
		// stop sending timed notifications
		mSessionTickTimer.cancel();
		mSessionTickTimer.purge();
		mSessionTickTimer = null;

		// set session's closing timestamp, and
		// disable session updates
		mData.end();

		// stop listening to sensors, and disable them
		mSensors.unregisterWheelListener(mData);
		mSensors.unregisterCrankListener(mData);
		mSensors.unregisterHeartListener(mData);
		mSensors.close();
		mSensors = null;
		
		// persist all session data, updating the opening record
		// (this is a _synchronous_ operation)
		if (!updateMasterRecord(mData)) {
			// this is impossible, afaik: no reason not to
			// crash the entire app if I'm wrong...
			throw new RuntimeException("Could not update session's master record");
		}
		
		// done
		mData = null;
		Log.d(TAG, "Work session stopped");
	}
    
    // stores all session data in DB, returns entity _ID
	private long createMasterRecord(String uniqueId, Date startTime) {
		Log.d(TAG, "Inserting session master record");
		Log.d(TAG, "UniqueId=" + uniqueId + ", StartTime=" + startTime);
		ContentValues values = new ContentValues();
		values.put(DiaryContract.COL_UUID, uniqueId);
		values.put(DiaryContract.COL_START, startTime.getTime());
		values.put(DiaryContract.COL_END, 0);
		values.put(DiaryContract.COL_ELAPSED, 0);
		values.put(DiaryContract.COL_DISTANCE, 0.0);
		values.put(DiaryContract.COL_CARDIO_MAX, 0.0);
		values.put(DiaryContract.COL_CARDIO_AVG, 0.0);
		values.put(DiaryContract.COL_SPEED_MAX, 0.0);
		values.put(DiaryContract.COL_SPEED_AVG, 0.0);
		values.put(DiaryContract.COL_CADENCE_MAX, 0.0);
		values.put(DiaryContract.COL_CADENCE_AVG, 0.0);
		values.put(DiaryContract.COL_GEAR, 0.0);
		values.put(DiaryContract.COL_FITNESS, 0.0);
		ContentResolver cs = Globals.getContext().getContentResolver();
		Uri uri = cs.insert(DiaryContract.WORKOUTS_URI, values);
		return ContentUris.parseId(uri);
	}
    
	private long createLogRecord(Data data) {
		Log.d(TAG, "Inserting session log record");
		Log.d(TAG, mData.toString());
		ContentValues values = new ContentValues();
		values.put(DiaryContract._ID, new Date().getTime());
		values.put(DiaryContract.COL_WORKOUT, data.getLocalId());
		values.put(DiaryContract.COL_DISTANCE, data.getDistanceCovered());
		values.put(DiaryContract.COL_CARDIO, data.getLastHeartCadence());
		values.put(DiaryContract.COL_SPEED, data.getLastSpeed());
		values.put(DiaryContract.COL_CADENCE, data.getLastCrankCadence());
		ContentResolver cs = Globals.getContext().getContentResolver();
		Uri uri = DiaryContract.getWorkoutLogContentUri(data.getLocalId());
		uri = cs.insert(uri, values);
		return ContentUris.parseId(uri);
	}

	private boolean updateMasterRecord(Data data) {
		Log.d(TAG, "Updating session master record");
		Log.d(TAG, mData.toString());
		ContentValues values = new ContentValues();
		values.put(DiaryContract.COL_END, data.getEndTime().getTime());
		values.put(DiaryContract.COL_ELAPSED, data.getElapsedTime());
		values.put(DiaryContract.COL_DISTANCE, data.getDistanceCovered());
		values.put(DiaryContract.COL_CARDIO_MAX, data.getMaxHeartCadence());
		values.put(DiaryContract.COL_CARDIO_AVG, data.getAverageHeartCadence());
		values.put(DiaryContract.COL_SPEED_MAX, data.getMaxSpeed());
		values.put(DiaryContract.COL_SPEED_AVG, data.getAverageSpeed());
		values.put(DiaryContract.COL_CADENCE_MAX, data.getMaxCrankCadence());
		values.put(DiaryContract.COL_CADENCE_AVG, data.getAverageCrankCadence());
		values.put(DiaryContract.COL_GEAR, data.getAverageGearRatio());
		values.put(DiaryContract.COL_FITNESS, data.getCardioFitnessFactor());
		Uri uri = DiaryContract.getWorkoutUri(data.getLocalId());
		ContentResolver cs = Globals.getContext().getContentResolver();
		
		// we should update exactly ONE record, if not...
		// we have some serious mess!
		return (1 == cs.update(uri, values, null, null)); 
	}
	
	static class Data implements WorkSessionInfo,
		BeatRateSensorListener, SpeedSensorListener, CadenceSensorListener {

		private static final String TO_STRING_FORMAT = Data.class.getName() +
				" - LocalId=%d" + 
				", UniqueId=%s" + 
				", StartTime=%d" + 
				", EndTime=%d" + 
				", DistanceCovered=%.3f" +
				", WheelRevs=%d" + 
				", CrankRevs=%d" + 
				", HeartBeats=%.0f" + 
				", LastSpeed=%.1f" + 
				", MaxSpeed=%.1f" + 
				", LastCrankCadence=%.1f" + 
				", MaxCrankCadence=%.1f" + 
				", LastHeartCadence=%.1f" + 
				", MaxHeartCadence=%.1f" +
				", LastHeartCadenceReding=%d";

		private final long mLocalId;
		private final String mUniqueId;
		private final Date mStartTime;
		private Date mEndTime;
		private double mDistanceCovered;
		private int mWheelRevs;
		private int mCrankRevs;
		private double mHeartBeats;
		private double mLastSpeed;
		private double mMaxSpeed;
		private double mLastCrankCadence;
		private double mMaxCrankCadence;
		private double mLastHeartCadence;
		private double mMaxHeartCadence;
		private Date mLastHeartCadenceReding = Globals.NEVER;
		
		// TODO support for "cardio zone" . i.e., target min/max BPM values
		// (significance of cardio fitness factor depends on this)
		
		private Data(long localId, String uniqueId, Date startTime) {
			mLocalId = localId;
			mUniqueId = uniqueId;
			mStartTime = startTime;
		}
		
		// WorkSessionData impl
		@Override
		public long getLocalId() {
			return mLocalId;
		}
		
		@Override
		public String getUniqueId() {
			return mUniqueId;
		}

		@Override
		public Date getStartTime() {
			return mStartTime;
		}
		
		@Override
		public synchronized Date getEndTime() {
			return mEndTime;
		}
		
		@Override
		public synchronized double getElapsedTime() {
			Date stopTime = null != mEndTime ? mEndTime : new Date();
			double elapsed = ((double) (stopTime.getTime() - mStartTime.getTime())) / 1000;
			return elapsed;
		}
		
		@Override
		public synchronized double getDistanceCovered() {
			return mDistanceCovered / 1000d;
		}
		
		@Override
		public int getWheelRevs() {
			return mWheelRevs;
		}
		
		@Override
		public int getCrankRevs() {
			return mCrankRevs;
		}

		@Override
		public double getHeartBeats() {
			return mHeartBeats;
		}

		@Override
		public synchronized double getMaxSpeed() {
			return mMaxSpeed;
		}
		
		@Override
		public synchronized double getAverageSpeed() {
			double elapsed = getElapsedTime();
			if (elapsed > 0) {
				return getDistanceCovered() / elapsed * 3600;
			} else {
				return 0;
			}
		}

		@Override
		public synchronized double getMaxCrankCadence() {
			return mMaxCrankCadence;
		}
		
		@Override
		public synchronized double getAverageCrankCadence() {
			double elapsed = getElapsedTime();
			if (elapsed > 0) {
				return mCrankRevs / elapsed * 60;
			} else {
				return 0;
			}
		}
		
		@Override
		public synchronized double getAverageGearRatio() {
			if (mCrankRevs > 0 && mWheelRevs > 0) {
				return ((double) mWheelRevs) / ((double) mCrankRevs);
			} else {
				return 0;
			}
		}

		@Override
		public synchronized double getMaxHeartCadence() {
			return mMaxHeartCadence;
		}
		
		@Override
		public synchronized double getAverageHeartCadence() {
			double elapsed = getElapsedTime();
			if (elapsed > 0) {
				return mHeartBeats / elapsed * 60;
			} else {
				return 0;
			}
		}
		
		@Override
		public synchronized double getCardioFitnessFactor() {
			// TODO also check if session was "in zone"
			if (mHeartBeats > 0) {
				return mDistanceCovered / mHeartBeats;
			} else {
				return 0;
			}
		}

		// BeatRateSensorListener impl
		@Override
		public synchronized void updateBeatRate(double bpm) {
			addHeartCadenceReading(bpm);
		}

		// SpeedRateSensorListener impl
		@Override
		public synchronized void updateSpeed(double kmh) {
			addSpeedReading(kmh);
		}
		
		@Override
		public synchronized void updateDistance(double meters) {
			addDistanceCovered(meters);
		}

		@Override
		public synchronized void updateWheelRevsCount(int revs) {
			addWheelRevolutions(revs);
		}

		// CadenceRateSensorListener impl
		@Override
		public synchronized void updateCadence(double rpm) {
			addCrankCadenceReading(rpm);
		}

		@Override
		public synchronized void updateCrankRevsCount(int revs) {
			addCrankRevolutions(revs);
		}
		
		@Override
		public synchronized String toString() {
			return String.format(
					Locale.getDefault(), 
					TO_STRING_FORMAT,
					mLocalId,
					mUniqueId,
					null != mStartTime ? mStartTime.getTime() : 0,
					null != mEndTime ? mEndTime.getTime() : 0,
					mDistanceCovered,
					mWheelRevs,
					mCrankRevs,
					mHeartBeats,
					mLastSpeed,
					mMaxSpeed,
					mLastCrankCadence,
					mMaxCrankCadence,
					mLastHeartCadence,
					mMaxHeartCadence,
					null != mLastHeartCadenceReding ? mLastHeartCadenceReding.getTime() : 0);
		}

		private synchronized void end() {
			mEndTime = new Date();
		}
		
		private double getLastHeartCadence() {
			return mLastHeartCadence;
		}
		
		private void addHeartCadenceReading(double bpm) {
			if (null != mEndTime)
				return; // no-op if session has been closed
			
			Date now = new Date();
			mLastHeartCadence = bpm;
			mMaxHeartCadence = bpm > mMaxHeartCadence ? bpm : mMaxHeartCadence;
			if (!mLastHeartCadenceReding.equals(Globals.NEVER)) {
				mHeartBeats += getUnitsInTimeInterval(mLastHeartCadenceReding, now, bpm);
			}
			mLastHeartCadenceReding = now;
		}
		
		private double getLastSpeed() {
			return mLastSpeed;
		}
		
		private void addSpeedReading(double kmh) {
			if (null != mEndTime)
				return; // no-op if session has been closed
			
			mLastSpeed = kmh;
			mMaxSpeed = kmh > mMaxSpeed ? kmh : mMaxSpeed;
		}
		
		private void addWheelRevolutions(int revs) {
			if (null != mEndTime)
				return; // no-op if session has been closed
			
			mWheelRevs += revs;
		}
		
		private void addDistanceCovered(double meters) {
			if (null != mEndTime)
				return; // no-op if session has been closed
			
			mDistanceCovered += meters;
		}
		
		private double getLastCrankCadence() {
			return mLastCrankCadence;
		}
		
		private void addCrankCadenceReading(double rpm) {
			if (null != mEndTime)
				return; // no-op if session has been closed
			
			mLastCrankCadence = rpm;
			mMaxCrankCadence = rpm > mMaxCrankCadence ? rpm : mMaxCrankCadence;
		}
		
		private void addCrankRevolutions(int revs) {
			if (null != mEndTime)
				return; // no-op if session has been closed
			
			mCrankRevs += revs;
		}

		private double getUnitsInTimeInterval(Date lastReading, Date now, double upm) {
			double millisecondsElapsed = now.getTime() - lastReading.getTime();
			double minutesElapsed = millisecondsElapsed / 60000;
			return upm * minutesElapsed;
		}
	}
}
