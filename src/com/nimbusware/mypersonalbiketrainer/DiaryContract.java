package com.nimbusware.mypersonalbiketrainer;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class DiaryContract {

	public static final String AUTHORITY = DiaryContract.class.getPackage().getName() + ".diary";
	public static final String WORKOUTS = "workouts";
	public static final String URL = "content://" + AUTHORITY + "/" + WORKOUTS;
	public static final Uri CONTENT_URI = Uri.parse(URL);

	public static String _ID = "_id";
	public static String COL_START = "start_time";
	public static String COL_END = "end_time";
	public static String COL_ELAPSED = "time_elapses";
	public static String COL_DISTANCE = "distance";
	public static String COL_CARDIO_MAX = "cardio_max";
	public static String COL_CARDIO_AVG = "cardio_avg";
	public static String COL_SPEED_MAX = "speed_max";
	public static String COL_SPEED_AVG = "speed_avg";
	public static String COL_CADENCE_MAX = "cadence_max";
	public static String COL_CADENCE_AVG = "cadence_avg";
	public static String COL_GEAR = "gear_ratio_avg";
	public static String COL_FITNESS = "fitness_factor";
	
	
	public static final String[] PROJECTION = {
		_ID,
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
	
	public static ContentValues getValues(Cursor cursor) {
		ContentValues values = null;
	    if (cursor != null) {
	    	cursor.moveToFirst();
	    	values = new ContentValues();
			values.put(_ID, cursor.getLong(cursor.getColumnIndex(_ID)));
			values.put(COL_START, cursor.getLong(cursor.getColumnIndex(COL_START)));
			values.put(COL_END, cursor.getLong(cursor.getColumnIndex(COL_END)));
			values.put(COL_ELAPSED, cursor.getDouble(cursor.getColumnIndex(COL_ELAPSED)));
			values.put(COL_DISTANCE, cursor.getDouble(cursor.getColumnIndex(COL_DISTANCE)));
			values.put(COL_CARDIO_MAX, cursor.getDouble(cursor.getColumnIndex(COL_CARDIO_MAX)));
			values.put(COL_CARDIO_AVG, cursor.getDouble(cursor.getColumnIndex(COL_CARDIO_AVG)));
			values.put(COL_SPEED_MAX, cursor.getDouble(cursor.getColumnIndex(COL_SPEED_MAX)));
			values.put(COL_SPEED_AVG, cursor.getDouble(cursor.getColumnIndex(COL_SPEED_AVG)));
			values.put(COL_CADENCE_MAX, cursor.getDouble(cursor.getColumnIndex(COL_CADENCE_MAX)));
			values.put(COL_CADENCE_AVG, cursor.getDouble(cursor.getColumnIndex(COL_CADENCE_AVG)));
			values.put(COL_GEAR, cursor.getDouble(cursor.getColumnIndex(COL_GEAR)));
			values.put(COL_FITNESS, cursor.getDouble(cursor.getColumnIndex(COL_FITNESS)));
	    } 
	    return values;
	}

	
	private DiaryContract() {}
}
