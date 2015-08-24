package com.nimbusware.mypersonalbiketrainer.db;

import com.nimbusware.mypersonalbiketrainer.DiaryContract;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class DiaryContentProvider extends ContentProvider {
	
	private static final int ALL_WORKOUTS = 1;
	private static final int ONE_WORKOUT_BY_ID = 2;
	private static final int ONE_WORKOUT_BY_UUID = 3;
	private static final int WORKOUT_LOG = 4;

	private static final UriMatcher uriMatcher;
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		
		uriMatcher.addURI(
				DiaryContract.AUTHORITY, 
				DiaryContract.WORKOUTS, 
				ALL_WORKOUTS);
		
		// workout identified by _ID (autoincrement integer)
		uriMatcher.addURI(
				DiaryContract.AUTHORITY, 
				DiaryContract.WORKOUTS + "/#", 
				ONE_WORKOUT_BY_ID);
		
		// workout identified by UUID
		uriMatcher.addURI(
				DiaryContract.AUTHORITY, 
				DiaryContract.SESSIONS + "/*", 
				ONE_WORKOUT_BY_UUID);
		
		uriMatcher.addURI(
				DiaryContract.AUTHORITY, 
				DiaryContract.WORKOUTS + "/#/" + DiaryContract.LOG, 
				WORKOUT_LOG);
	}
	
	private static final String DB_NAME = "mpbt";
	private static final Integer DB_VERSION = 2;
	private static final String TBL_NAME_WORKOUT = "workout";
	private static final String TBL_NAME_WORKOUT_LOG = "workout_log";

	private static final String CREATE_CMD_1 = "CREATE TABLE " +
			TBL_NAME_WORKOUT + " (" + 
			DiaryContract._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
			DiaryContract.COL_UUID + " TEXT NULL, " +
			DiaryContract.COL_START + " INTEGER NOT NULL, " +
			DiaryContract.COL_END + " INTEGER NOT NULL, " +
			DiaryContract.COL_ELAPSED + " REAL NOT NULL, " +
			DiaryContract.COL_DISTANCE + " REAL NOT NULL, " +
			DiaryContract.COL_CARDIO_MAX + " REAL NOT NULL, " +
			DiaryContract.COL_CARDIO_AVG + " REAL NOT NULL, " +
			DiaryContract.COL_SPEED_MAX + " REAL NOT NULL, " +
			DiaryContract.COL_SPEED_AVG + " REAL NOT NULL, " +
			DiaryContract.COL_CADENCE_MAX + " REAL NOT NULL, " +
			DiaryContract.COL_CADENCE_AVG + " REAL NOT NULL, " +
			DiaryContract.COL_GEAR + " REAL NOT NULL, " +
			DiaryContract.COL_FITNESS + " REAL NOT NULL) ";

	private static final String CREATE_CMD_2 = "CREATE TABLE " +
			TBL_NAME_WORKOUT_LOG + " (" + 
			DiaryContract._ID + " INTEGER PRIMARY KEY, " +
			DiaryContract.COL_WORKOUT + " INTEGER NOT NULL " +
			"REFERENCES " + TBL_NAME_WORKOUT + " ON DELETE CASCADE, " +
			DiaryContract.COL_DISTANCE + " REAL NOT NULL, " +
			DiaryContract.COL_CARDIO + " REAL NOT NULL, " +
			DiaryContract.COL_SPEED + " REAL NOT NULL, " +
			DiaryContract.COL_CADENCE + " REAL NOT NULL) ";

	private static String DEFAULT_ORDER_WORKOUT = DiaryContract.COL_START + " DESC";

	private static String DEFAULT_ORDER_LOG = DiaryContract._ID; // ID is a timestamp
	
	/**
	 * Helper class that actually creates and manages the provider's underlying data repository.
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {
		
		DatabaseHelper(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onConfigure(SQLiteDatabase db) {
			super.onConfigure(db);
			db.setForeignKeyConstraintsEnabled(true);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_CMD_1);
			db.execSQL(CREATE_CMD_2);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(DatabaseHelper.class.getName(),
					"Upgrading database from version " + oldVersion + " to " + newVersion);

			if (oldVersion == 1 && newVersion == 2) {
				// special case: we simply add a new table
				db.execSQL(CREATE_CMD_2);
				db.execSQL("ALTER TABLE " + TBL_NAME_WORKOUT +
						" ADD COLUMN " + DiaryContract.COL_UUID + " TEXT NULL");
			} else {
				// any other case: let's drop and recreate everything 
				db.execSQL("DROP TABLE IF EXISTS " + TBL_NAME_WORKOUT_LOG);
				db.execSQL("DROP TABLE IF EXISTS " + TBL_NAME_WORKOUT);
				onCreate(db);
			}
		}
	}
	

	private DatabaseHelper mHelper;

	@Override
	public boolean onCreate() {
		// Create a writable database which will trigger its creation if it doesn't already exist
		mHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		long rowID = 0;
		switch (uriMatcher.match(uri)) {
		case ALL_WORKOUTS:
			rowID = mHelper.getWritableDatabase().insert(TBL_NAME_WORKOUT, null, values);
			break;
		case WORKOUT_LOG:
			rowID = mHelper.getWritableDatabase().insert(TBL_NAME_WORKOUT_LOG, null, values);
			break;
		default:
			// can only insert new workouts or new workout log entries
			throw new IllegalArgumentException("Unsupported URI for INSERT operation: " + uri);
		}

		if (rowID > 0) {
			Uri _uri = ContentUris.withAppendedId(DiaryContract.WORKOUTS_URI, rowID);
			getContext().getContentResolver().notifyChange(_uri, null);
			return _uri;
		} else {
			throw new SQLException("INSERT operation failed: " + uri);
		}
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		
		switch (uriMatcher.match(uri)) {
		case ALL_WORKOUTS:
			qb.setTables(TBL_NAME_WORKOUT);
			if (sortOrder == null || sortOrder == "") {
				sortOrder = DEFAULT_ORDER_WORKOUT;
			}
			break;
			
		case ONE_WORKOUT_BY_ID:
			qb.setTables(TBL_NAME_WORKOUT);
			qb.appendWhere(DiaryContract._ID + "=" + uri.getPathSegments().get(1));
			break;
			
		case ONE_WORKOUT_BY_UUID:
			qb.setTables(TBL_NAME_WORKOUT);
			qb.appendWhere(DiaryContract.COL_UUID + "=" + uri.getPathSegments().get(1));
			break;
			
		case WORKOUT_LOG:
			qb.setTables(TBL_NAME_WORKOUT_LOG);
			qb.appendWhere(DiaryContract.COL_WORKOUT + "=" + uri.getPathSegments().get(1));
			if (sortOrder == null || sortOrder == "") {
				sortOrder = DEFAULT_ORDER_LOG;
			}
			break;
			
		default:
			throw new IllegalArgumentException("Unsupported URI for SELECT operation: " + uri);
		}
		
		
		SQLiteDatabase db = mHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
		
		/**
		 * register to watch a content URI for changes
		 */
		c.setNotificationUri(getContext().getContentResolver(), uri);

		return c;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int count = 0;
		SQLiteDatabase db = mHelper.getWritableDatabase();

		switch (uriMatcher.match(uri)) {
		case ALL_WORKOUTS:
			count = db.delete(TBL_NAME_WORKOUT, selection, selectionArgs);
			break;
			
		case ONE_WORKOUT_BY_ID:
			// selection arguments, if any, are ignored
			long id = ContentUris.parseId(uri);
			count = db.delete(TBL_NAME_WORKOUT, 
					DiaryContract._ID + " = " + id, null);
			break;
			
		case ONE_WORKOUT_BY_UUID:
			// selection arguments, if any, are ignored
			String uuid = uri.getPathSegments().get(1);
			count = db.delete(TBL_NAME_WORKOUT, 
					DiaryContract.COL_UUID + " = " + uuid, null);
			break;
			
		default:
			// log delete is only supported by cascading from workout
			throw new IllegalArgumentException("Unsupported URI for DELETE operation: " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int count = 0;
		SQLiteDatabase db = mHelper.getWritableDatabase();

		switch (uriMatcher.match(uri)) {
		case ALL_WORKOUTS:
			count = db.update(
					TBL_NAME_WORKOUT, 
					values, 
					selection,
					selectionArgs);
			break;
			
		case ONE_WORKOUT_BY_ID:
			// selection arguments, if any, are ignored
			long id = ContentUris.parseId(uri);
			count = db.update(TBL_NAME_WORKOUT, values,
					DiaryContract._ID + " = " + id, null);
			break;
			
		case ONE_WORKOUT_BY_UUID:
			// selection arguments, if any, are ignored
			String uuid = uri.getPathSegments().get(1);
			count = db.update(TBL_NAME_WORKOUT, values,
					DiaryContract.COL_UUID + " = " + uuid, null);
			break;
			
		default:
			// log update is not supported
			throw new IllegalArgumentException("Unsupported URI for UPDATE operation: " + uri);
		}
		
		getContext().getContentResolver().notifyChange(uri, null);
		
		return count;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case ALL_WORKOUTS: // all records
			return "vnd.android.cursor.dir/workout";
			
		case ONE_WORKOUT_BY_ID: // a specific record
			return "vnd.android.cursor.item/workout";
			
		case ONE_WORKOUT_BY_UUID: // a specific record
			return "vnd.android.cursor.item/workout";
			
		case WORKOUT_LOG: // all detail records
			return "vnd.android.cursor.dir/log";
			
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}
}
