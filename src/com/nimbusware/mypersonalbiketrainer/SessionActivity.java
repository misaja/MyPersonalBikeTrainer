package com.nimbusware.mypersonalbiketrainer;

import java.text.DateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class SessionActivity extends Activity {
	
	private static final String TAG = SessionActivity.class.getSimpleName();

	private long mSessionId;
	private TextView mStarted;
	private TextView mEnded;
	private TextView mDuration;
	private TextView mDistance;
	private TextView mCardioAvg;
	private TextView mCardioMax;
	private TextView mSpeedAvg;
	private TextView mSpeedMax;
	private TextView mCadenceAvg;
	private TextView mCadenceMax;
	private TextView mGear;
	private TextView mFitness;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_session);
		
		mSessionId = getIntent().getLongExtra(Globals.SESSION_ID, -1);
		if (mSessionId < 0) {
        	Log.e(TAG, "Bad session ID, exiting");
        	finish();
        	return;
		}
		
		Log.d(TAG, "Creating activity, SESSION_ID=" + mSessionId);
		
		mStarted = (TextView) findViewById(R.id.valStart);
		mEnded = (TextView) findViewById(R.id.valEnd);
		mDuration = (TextView) findViewById(R.id.valElapsed);
		mDistance = (TextView) findViewById(R.id.valDistance);
		mCardioAvg = (TextView) findViewById(R.id.valCardioAvg);
		mCardioMax = (TextView) findViewById(R.id.valCardioMax);
		mSpeedAvg = (TextView) findViewById(R.id.valSpeedAvg);
		mSpeedMax = (TextView) findViewById(R.id.valSpeedMax);
		mCadenceAvg = (TextView) findViewById(R.id.valCadenceAvg);
		mCadenceMax = (TextView) findViewById(R.id.valCadenceMax);
		mGear = (TextView) findViewById(R.id.valGear);
		mFitness = (TextView) findViewById(R.id.valFitness);
		
		Button delete = (Button) findViewById(R.id.btnDelete);
		delete.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				DeleteDialog dialog = new DeleteDialog();
				dialog.show(SessionActivity.this.getFragmentManager(), "dialog");
			}
		});
		
		Button ok = (Button) findViewById(R.id.btnOk);
		ok.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(SessionActivity.this, CockpitActivity.class);
				startActivity(intent);
			}
		});
        
    	Log.d(TAG, "Activity successfully created");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.session, menu);
	    return true;
	}

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_cockpit:
				Intent intent = new Intent(this, CockpitActivity.class);
				startActivity(intent);
				break;
			case R.id.action_diary:
		        startActivity(new Intent(this, DiaryActivity.class));
				break;
			case R.id.action_settings:
		        startActivity(new Intent(this, MainActivity.class));
				break;
		}
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		Log.d(TAG, "Resuming activity");
		Uri uri = ContentUris.withAppendedId(DiaryContract.CONTENT_URI, mSessionId);
		ContentValues values = DiaryContract.getValues(getContentResolver().query(uri, DiaryContract.PROJECTION, null, null, null));
		
		if (null != values) {
			DateFormat formatter = DateFormat.getDateTimeInstance();
			Date start = new Date(values.getAsLong(DiaryContract.COL_START));
			mStarted.setText(formatter.format(start));

			Date end = new Date(values.getAsLong(DiaryContract.COL_END));
			mEnded.setText(formatter.format(end));
			
			// clumsy hack to downcast from Double to int, truncating any decimals
			int elapsedSecs = (int) ((double) values.getAsDouble(DiaryContract.COL_ELAPSED));
			int hours = elapsedSecs / 3600;
			int extraSecs = elapsedSecs % 3600;
			int mins = extraSecs / 60;
			int secs = extraSecs - (mins * 60);
			StringBuffer buf = new StringBuffer();
			buf.append(String.format("%d", hours)).append(":");
			buf.append(String.format("%02d", mins)).append(":");
			buf.append(String.format("%02d", secs));
			mDuration.setText(buf.toString());
			
			mDistance.setText(String.format("%.1f", values.getAsDouble(DiaryContract.COL_DISTANCE)));
			mCardioAvg.setText(String.format("%d", Math.round(values.getAsDouble(DiaryContract.COL_CARDIO_AVG))));
			mCardioMax.setText(String.format("%d", Math.round(values.getAsDouble(DiaryContract.COL_CARDIO_MAX))));
			mSpeedAvg.setText(String.format("%.1f", values.getAsDouble(DiaryContract.COL_SPEED_AVG)));
			mSpeedMax.setText(String.format("%.1f", values.getAsDouble(DiaryContract.COL_SPEED_MAX)));
			mCadenceAvg.setText(String.format("%.1f", values.getAsDouble(DiaryContract.COL_CADENCE_AVG)));
			mCadenceMax.setText(String.format("%.1f", values.getAsDouble(DiaryContract.COL_CADENCE_MAX)));
			
			Double gf = values.getAsDouble(DiaryContract.COL_GEAR);
			if (gf.isInfinite() || gf.isNaN() || gf == 0) {
				mGear.setText("(na)");
			} else {
				mGear.setText(String.format("%.2f", gf));
			}
			Double ff = values.getAsDouble(DiaryContract.COL_FITNESS);
			if (ff.isInfinite() || ff.isNaN() || ff == 0) {
				mFitness.setText("(na)");
			} else {
				mFitness.setText(String.format("%.2f", ff));
			}
		} else {
        	Log.e(TAG, "No data found, exiting");
        	finish();
        	return;
		}
		
    	Log.d(TAG, "Activity successfully resumed");
	}
	
	public class DeleteDialog extends DialogFragment {
	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
	        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	        builder.setTitle(R.string.title_delete_dialog)
	        	.setMessage(R.string.msg_delete_dialog)
	            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
	            	public void onClick(DialogInterface dialog, int id) {
	            		Uri uri = ContentUris.withAppendedId(DiaryContract.CONTENT_URI, mSessionId); 
	    				getContentResolver().delete(uri, null, null);
	    				SessionActivity.this.finish();
	            	}
	            })
	            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
	            	public void onClick(DialogInterface dialog, int id) {
	            		dialog.dismiss();
	            	}
	            });
	        return builder.create();
	    }
	}
}
