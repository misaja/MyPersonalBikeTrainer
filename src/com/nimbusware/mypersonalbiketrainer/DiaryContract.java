package com.nimbusware.mypersonalbiketrainer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

/**
 * Front-end to the DiaryContentProvider.
 * @author misaja
 *
 */
public class DiaryContract {

	public static final String AUTHORITY = DiaryContract.class.getPackage().getName() + ".diary";
	
	public static final String WORKOUTS = "workouts";
	public static final String WORKOUTS_URI_STR = "content://" + AUTHORITY + "/" + WORKOUTS;
	public static final Uri WORKOUTS_URI = Uri.parse(WORKOUTS_URI_STR);

	// sessions are an alias to workouts: we use a sessions content uri when
	// we are addressing a workout by its UUID instead of its _ID
	public static final String SESSIONS = "sessions";
	public static final String SESSIONS_URI_STR = "content://" + AUTHORITY + "/" + SESSIONS;
	public static final Uri SESSIONS_URI = Uri.parse(SESSIONS_URI_STR);

	// log entries are details records of a workout/session
	public static final String LOG = "log";

	public static String _ID = "_id";
	public static String COL_UUID = "_uuid";
	public static String COL_START = "start_time";
	public static String COL_END = "end_time";
	public static String COL_ELAPSED = "time_elapses"; // typo, too late to amend
	public static String COL_DISTANCE = "distance";
	public static String COL_CARDIO_MAX = "cardio_max";
	public static String COL_CARDIO_AVG = "cardio_avg";
	public static String COL_SPEED_MAX = "speed_max";
	public static String COL_SPEED_AVG = "speed_avg";
	public static String COL_CADENCE_MAX = "cadence_max";
	public static String COL_CADENCE_AVG = "cadence_avg";
	public static String COL_GEAR = "gear_ratio_avg";
	public static String COL_FITNESS = "fitness_factor";

	public static String COL_WORKOUT = "workout"; // fk, references workout._id
	public static String COL_CARDIO = "cardio";
	public static String COL_SPEED = "speed";
	public static String COL_CADENCE = "cadence";
	
	public static final String[] WORKOUT_PROJECTION = {
		_ID,
		COL_UUID,
		COL_START,
		COL_END,
		COL_ELAPSED,
		COL_DISTANCE,
		COL_CARDIO_MAX,
		COL_CARDIO_AVG,
		COL_SPEED_MAX,
		COL_SPEED_AVG,
		COL_CADENCE_MAX,
		COL_CADENCE_AVG,
		COL_GEAR,
		COL_FITNESS
	};
	
	public static final String[] LOG_PROJECTION = {
		_ID,
		COL_WORKOUT,
		COL_DISTANCE,
		COL_CARDIO,
		COL_SPEED,
		COL_CADENCE
	};
	
	public static Uri getWorkoutUri(long workoutId) {
		return ContentUris.withAppendedId(DiaryContract.WORKOUTS_URI, workoutId);
	}
	
	public static Uri getWorkoutLogContentUri(long workoutId) {
		return Uri.withAppendedPath(getWorkoutUri(workoutId), LOG);
	}
	
	public static Uri getSessionUri(String sessionId) {
		return Uri.withAppendedPath(SESSIONS_URI, sessionId);
	}
	
	public static WorkSessionInfo getWorkoutEntry(Context ctx, long workoutId) {
		ContentResolver cs = ctx.getContentResolver();
		Uri uri = getWorkoutUri(workoutId);
		Cursor cursor = cs.query(uri, WORKOUT_PROJECTION, null, null, null);
		return getWorkoutEntry(cursor);
	}
	
	public static WorkSessionInfo getWorkoutEntry(Cursor cursor) {
		WSData item = null;
	    if (cursor != null && cursor.moveToFirst()) {
	    	item = new WSData();
			item.mLocalId = cursor.getLong(cursor.getColumnIndex(_ID));
			item.mUniqueId = cursor.getString(cursor.getColumnIndex(COL_UUID));
			item.mStartTime = new Date(cursor.getLong(cursor.getColumnIndex(COL_START)));
			item.mEndTime = new Date(cursor.getLong(cursor.getColumnIndex(COL_END)));
			item.mElapsedTime = cursor.getDouble(cursor.getColumnIndex(COL_ELAPSED));
			// variable is expected to be in meters, saved data is in kilometers
			item.mDistanceCovered = cursor.getDouble(cursor.getColumnIndex(COL_DISTANCE)) * 1000;
			item.mMaxHeartCadence = cursor.getDouble(cursor.getColumnIndex(COL_CARDIO_MAX));
			item.mAverageHeartCadence = cursor.getDouble(cursor.getColumnIndex(COL_CARDIO_AVG));
			item.mMaxSpeed = cursor.getDouble(cursor.getColumnIndex(COL_SPEED_MAX));
			item.mAverageSpeed = cursor.getDouble(cursor.getColumnIndex(COL_SPEED_AVG));
			item.mMaxCrankCadence = cursor.getDouble(cursor.getColumnIndex(COL_CADENCE_MAX));
			item.mAverageCrankCadence = cursor.getDouble(cursor.getColumnIndex(COL_CADENCE_AVG));
			item.mAverageGearRatio = cursor.getDouble(cursor.getColumnIndex(COL_GEAR));
			item.mCardioFitnessFactor = cursor.getDouble(cursor.getColumnIndex(COL_FITNESS));
	    } 
	    return item;
	}
	
	public static boolean deleteWorkoutEntry(Context ctx, long workoutId) {
		ContentResolver cs = ctx.getContentResolver();
		Uri uri = getWorkoutUri(workoutId);
		return (1 == cs.delete(uri, null, null));
	}
	
	public static List<WorkSessionLogEntry> getWorkoutLogEntries(Context ctx, long workoutId) {
		ContentResolver cs = ctx.getContentResolver();
		Uri uri = getWorkoutLogContentUri(workoutId);
		Cursor cursor = cs.query(uri, LOG_PROJECTION, null, null, null);
		return getWorkoutLogEntries(cursor);
	}
	
	public static List<WorkSessionLogEntry> getWorkoutLogEntries(Cursor cursor) {
		ArrayList<WorkSessionLogEntry> items = 
				new ArrayList<WorkSessionLogEntry>();
	    if (cursor != null) {
	    	while (cursor.moveToNext()) {
		    	WSLogData item = new WSLogData();
		    	item.mTime = new Date(cursor.getLong(cursor.getColumnIndex(_ID)));
		    	item.mWorkoutLocalId = cursor.getLong(cursor.getColumnIndex(COL_WORKOUT));
		    	item.mPartialDistance = cursor.getDouble(cursor.getColumnIndex(COL_DISTANCE));
		    	item.mHeartCadence = cursor.getDouble(cursor.getColumnIndex(COL_CARDIO));
		    	item.mSpeed = cursor.getDouble(cursor.getColumnIndex(COL_SPEED));
		    	item.mCrankCadence = cursor.getDouble(cursor.getColumnIndex(COL_CADENCE));
		    	items.add(item);
	    	}
	    } 
	    return items;
	}

	
	private DiaryContract() {}
	
	
	private static class WSData implements WorkSessionInfo {
		
		private long mLocalId;
		private String mUniqueId;
		private Date mStartTime;
		private Date mEndTime;
		private double mElapsedTime;
		private double mDistanceCovered;
		private int mWheelRevs;
		private int mCrankRevs;
		private double mHeartBeats;
		private double mMaxSpeed;
		private double mMaxCrankCadence;
		private double mMaxHeartCadence;
		private double mAverageSpeed;
		private double mAverageCrankCadence;
		private double mAverageHeartCadence;
		private double mAverageGearRatio;
		private double mCardioFitnessFactor;

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
		public Date getEndTime() {
			return mEndTime;
		}
		
		@Override
		public double getDistanceCovered() {
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
		public double getMaxSpeed() {
			return mMaxSpeed;
		}

		@Override
		public double getMaxCrankCadence() {
			return mMaxCrankCadence;
		}

		@Override
		public double getMaxHeartCadence() {
			return mMaxHeartCadence;
		}
		
		@Override
		public double getElapsedTime() {
			return mElapsedTime;
		}
		
		@Override
		public double getAverageSpeed() {
			return mAverageSpeed;
		}
		
		@Override
		public double getAverageCrankCadence() {
			return mAverageCrankCadence;
		}
		
		@Override
		public double getAverageGearRatio() {
			return mAverageGearRatio;
		}
		
		@Override
		public double getAverageHeartCadence() {
			return mAverageHeartCadence;
		}
		
		@Override
		public double getCardioFitnessFactor() {
			return mCardioFitnessFactor;
		}
	}
	
	private static class WSLogData implements WorkSessionLogEntry {

		private long mWorkoutLocalId;
		private Date mTime;
		private double mPartialDistance;
		private double mSpeed;
		private double mCrankCadence;
		private double mHeartCadence;
		
		@Override
		public long getWorkoutLocalId() {
			return mWorkoutLocalId;
		}

		@Override
		public Date getTime() {
			return mTime;
		}

		@Override
		public double getPartialDistance() {
			return mPartialDistance;
		}

		@Override
		public double getSpeed() {
			return mSpeed;
		}

		@Override
		public double getCrankCadence() {
			return mCrankCadence;
		}

		@Override
		public double getHeartCadence() {
			return mHeartCadence;
		}
		
	}
}
