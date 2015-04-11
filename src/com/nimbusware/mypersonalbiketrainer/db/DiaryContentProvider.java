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
import android.text.TextUtils;
import android.util.Log;

public class DiaryContentProvider extends ContentProvider {
	
	private static final int ONE = 1;
	private static final int ALL = 2;

	private static final UriMatcher uriMatcher;
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(DiaryContract.AUTHORITY, DiaryContract.WORKOUTS + "/#", ONE);
		uriMatcher.addURI(DiaryContract.AUTHORITY, DiaryContract.WORKOUTS, ALL);
	}
	
	private static final String DB_NAME = "mpbt";
	private static final Integer DB_VERSION = 1;
	private static final String TBL_NAME = "workout";

	private static final String CREATE_CMD = "CREATE TABLE " +
			TBL_NAME + " (" + DiaryContract._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
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

	private static String DEFAULT_ORDER = DiaryContract.COL_START + " DESC";
	
	/**
	 * Helper class that actually creates and manages the provider's underlying data repository.
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {
		
		DatabaseHelper(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_CMD);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(DatabaseHelper.class.getName(),
					"Upgrading database from version " + oldVersion + " to " + newVersion + ". Old data will be destroyed");

			db.execSQL("DROP TABLE IF EXISTS " + TBL_NAME);
			onCreate(db);
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
		SQLiteDatabase db = mHelper.getWritableDatabase();
		long rowID = db.insert(TBL_NAME, null, values);
		if (rowID > 0) {
			Uri _uri = ContentUris.withAppendedId(DiaryContract.CONTENT_URI, rowID);
			getContext().getContentResolver().notifyChange(_uri, null);
			return _uri;
		}
		throw new SQLException("Failed to add a record into " + uri);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(TBL_NAME);
		
		switch (uriMatcher.match(uri)) {
		case ALL:
			break;
		case ONE:
			qb.appendWhere(DiaryContract._ID + "=" + uri.getPathSegments().get(1));
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		
		if (sortOrder == null || sortOrder == "") {
			sortOrder = DEFAULT_ORDER;
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
		case ALL:
			count = db.delete(TBL_NAME, selection, selectionArgs);
			break;
		case ONE:
			long id = ContentUris.parseId(uri);
			count = db.delete(TBL_NAME, DiaryContract._ID
					+ " = "
					+ id
					+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
							+ ')' : ""), selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
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
		case ALL:
			count = db.update(TBL_NAME, values, selection,
					selectionArgs);
			break;
		case ONE:
			count = db.update(
					TBL_NAME,
					values,
					DiaryContract._ID
							+ " = "
							+ uri.getPathSegments().get(1)
							+ (!TextUtils.isEmpty(selection) ? " AND ("
									+ selection + ')' : ""), selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		
		getContext().getContentResolver().notifyChange(uri, null);
		
		return count;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case ALL: // all records
			return "vnd.android.cursor.dir/workout";
		case ONE: // a specific record
			return "vnd.android.cursor.item/workout";
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}
}
