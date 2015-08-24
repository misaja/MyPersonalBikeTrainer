package com.nimbusware.mypersonalbiketrainer;

import java.text.DateFormat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class SessionActivity extends Activity {
	
	private static final String TAG = SessionActivity.class.getSimpleName();
	private static final String PLACEHOLDER_LINK = "!!!";
	private static final String PLACEHOLDER_TEXT = "???";
	private static final String TEMPLATE = "<a href='" + 
			PLACEHOLDER_LINK +  "'>" + PLACEHOLDER_TEXT + "</a>";

	private long mWorkoutId;
	private TextView mStarted;
	private TextView mEnded;
	private TextView mDuration;
	private TextView mDistance;
	private TextView mCardio;
	private TextView mCardioAvg;
	private TextView mCardioMax;
	private TextView mSpeed;
	private TextView mSpeedAvg;
	private TextView mSpeedMax;
	private TextView mCadence;
	private TextView mCadenceAvg;
	private TextView mCadenceMax;
	private TextView mGear;
	private TextView mFitness;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_session);
		
		mWorkoutId = getIntent().getLongExtra(Globals.WORKOUT_ID, -1);
		if (mWorkoutId < 0) {
        	Log.e(TAG, "Bad Workout ID, exiting");
        	finish();
        	return;
		}
		
		Log.d(TAG, "Creating activity, Workout ID=" + mWorkoutId);
		
		String link = Globals.getWorkoutContentUri(mWorkoutId) ;
		
		mStarted = (TextView) findViewById(R.id.valStart);
		mEnded = (TextView) findViewById(R.id.valEnd);
		mDuration = (TextView) findViewById(R.id.valElapsed);
		mDistance = (TextView) findViewById(R.id.valDistance);
		mCardio = (TextView) findViewById(R.id.lblCardio);
		mCardio.setMovementMethod(LinkMovementMethod.getInstance());
		String htmlText = TEMPLATE.replace(PLACEHOLDER_TEXT, mCardio.getText())
				.replace(PLACEHOLDER_LINK, link);
		mCardio.setText(Html.fromHtml(htmlText));
		mCardioAvg = (TextView) findViewById(R.id.valCardioAvg);
		mCardioMax = (TextView) findViewById(R.id.valCardioMax);
		mSpeed = (TextView) findViewById(R.id.lblSpeed);
		mSpeed.setMovementMethod(LinkMovementMethod.getInstance());
		htmlText = TEMPLATE.replace(PLACEHOLDER_TEXT, mSpeed.getText())
				.replace(PLACEHOLDER_LINK, link);
		mSpeed.setText(Html.fromHtml(htmlText));
		mSpeedAvg = (TextView) findViewById(R.id.valSpeedAvg);
		mSpeedMax = (TextView) findViewById(R.id.valSpeedMax);
		mCadence = (TextView) findViewById(R.id.lblCadence);
		mCadence.setMovementMethod(LinkMovementMethod.getInstance());
		htmlText = TEMPLATE.replace(PLACEHOLDER_TEXT, mCadence.getText())
				.replace(PLACEHOLDER_LINK, link);
		mCadence.setText(Html.fromHtml(htmlText));
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
		
		loadData();
        
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

	private void loadData() {
		
		WorkSessionInfo values = DiaryContract.getWorkoutEntry(this, mWorkoutId);
		
		if (null != values) {
			DateFormat formatter = DateFormat.getDateTimeInstance();
			mStarted.setText(formatter.format(values.getStartTime()));
			mEnded.setText(formatter.format(values.getEndTime()));
			
			// clumsy hack to downcast from Double to int, truncating any decimals
			int elapsedSecs = (int) values.getElapsedTime();
			int hours = elapsedSecs / 3600;
			int extraSecs = elapsedSecs % 3600;
			int mins = extraSecs / 60;
			int secs = extraSecs - (mins * 60);
			StringBuffer buf = new StringBuffer();
			buf.append(String.format("%d", hours)).append(":");
			buf.append(String.format("%02d", mins)).append(":");
			buf.append(String.format("%02d", secs));
			mDuration.setText(buf.toString());
			
			mDistance.setText(String.format("%.1f", values.getDistanceCovered()));
			mCardioAvg.setText(String.format("%d", Math.round(values.getAverageHeartCadence())));
			mCardioMax.setText(String.format("%d", Math.round(values.getMaxHeartCadence())));
			mSpeedAvg.setText(String.format("%.1f", values.getAverageSpeed()));
			mSpeedMax.setText(String.format("%.1f", values.getMaxSpeed()));
			mCadenceAvg.setText(String.format("%.1f", values.getAverageCrankCadence()));
			mCadenceMax.setText(String.format("%.1f", values.getMaxCrankCadence()));
			
			Double gf = values.getAverageGearRatio();
			if (gf.isInfinite() || gf.isNaN() || gf == 0) {
				mGear.setText("(na)");
			} else {
				mGear.setText(String.format("%.2f", gf));
			}
			Double ff = values.getCardioFitnessFactor();
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
	            		DiaryContract.deleteWorkoutEntry(SessionActivity.this, mWorkoutId);
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
