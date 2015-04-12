package com.nimbusware.mypersonalbiketrainer;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class DiaryActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor> {
	
	private static String[] PROJECTION = { DiaryContract.COL_START, DiaryContract.COL_DISTANCE, DiaryContract._ID };

	private SimpleCursorAdapter mAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_diary);
		
		mAdapter = new SimpleCursorAdapter(
				this, R.layout.listitem_workout, null,
				PROJECTION,	new int[] { R.id.start, R.id.distance }, 0);
		
		mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
		    @Override
		    public boolean setViewValue(View view, Cursor cursor, int column) {
		        if (column == 0) { // COL_START
		            TextView tv = (TextView) view;
					DateFormat formatter = DateFormat.getDateTimeInstance();
		            Date start = new Date(cursor.getLong(cursor.getColumnIndex(DiaryContract.COL_START)));
		            tv.setText(formatter.format(start));
		            return true;
		        } else if (column == 1) { // COL_DISTANCE
		            TextView tv = (TextView) view;
		            DecimalFormat formatter = new DecimalFormat("#0.0");   
		            double distance = cursor.getDouble(cursor.getColumnIndex(DiaryContract.COL_DISTANCE));
		            tv.setText(formatter.format(distance));
		            return true;
		        } else {
		        	return false;
		        }
		    }
		});

		final ListView listview = (ListView) findViewById(R.id.listview);
		listview.setAdapter(mAdapter);
		
		listview.setOnItemClickListener(new ListView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> av, View v, int position, long id) {
		        Intent intent = new Intent(DiaryActivity.this, SessionActivity.class);
		        intent.putExtra(Globals.SESSION_ID, id);
		        startActivity(intent);
			}
		});
		
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.diary, menu);
	    return true;
	}

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_cockpit:
				Intent intent = new Intent(this, CockpitActivity.class);
				startActivity(intent);
				break;
			case R.id.action_settings:
		        startActivity(new Intent(this, MainActivity.class));
				break;
		}
		return true;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(this, DiaryContract.CONTENT_URI, PROJECTION, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}
}
