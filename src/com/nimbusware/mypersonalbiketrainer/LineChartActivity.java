package com.nimbusware.mypersonalbiketrainer;

import java.util.ArrayList;
import java.util.List;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class LineChartActivity extends Activity {

	private static final String TAG = LineChartActivity.class.getSimpleName();

	private long mWorkoutId;
    private LineChart mChart;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_line_chart);
		
		String uriSegment = getIntent().getData().getLastPathSegment();
		mWorkoutId = Long.valueOf(uriSegment);
		
		Log.d(TAG, "Getting chart data, Workout ID=" + mWorkoutId);
		
		LineData data = getChartData(mWorkoutId);
		
		if (null != data) {
			Log.d(TAG, "Displaying chart data, Workout ID=" + mWorkoutId);
			
			mChart = (LineChart) findViewById(R.id.line_chart);
			mChart.setDrawGridBackground(false);

	        // set an alternative background color
	        // mChart.setBackgroundColor(Color.GRAY);
			
			// no description
	        mChart.setDescription("");

	        // enable value highlighting
	        mChart.setHighlightEnabled(true);

	        // enable touch gestures
	        mChart.setTouchEnabled(true);

	        // enable scaling, dragging and pinch-zooming
	        mChart.setDragEnabled(true);
	        mChart.setScaleEnabled(true);
	        mChart.setPinchZoom(true);
	        
	        // set data
	        mChart.setData(data);
		} else {
			Log.d(TAG, "No chart data exists, Workout ID=" + mWorkoutId);
			Toast.makeText(this, R.string.msg_no_log, Toast.LENGTH_SHORT).show();
			finish();
        	return;
		}
	}
	
	

	@Override
	protected void onResume() {
		super.onResume();
		
        // refresh the drawing
        // mChart.invalidate();
	}



	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.line_chart, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

    private LineData getChartData(long workoutId) {
    	LineData data = null;
    	
    	List<WorkSessionLogEntry> logEntries = 
    			DiaryContract.getWorkoutLogEntries(this, workoutId);
    	int size = logEntries.size();
    	
    	if (size > 0) {
            ArrayList<String> xVals = new ArrayList<String>(size);
            ArrayList<Entry> yValsCardio = new ArrayList<Entry>(size);
            ArrayList<Entry> yValsSpeed = new ArrayList<Entry>(size);
        	for (int i = 0; i < size; i++) {
        		WorkSessionLogEntry item = logEntries.get(i);
                xVals.add((i) + "");
                yValsCardio.add(new Entry((float)item.getHeartCadence(), i));
                yValsSpeed.add(new Entry((float)item.getSpeed(), i));
        	}

            LineDataSet set1 = new LineDataSet(yValsCardio, "Cardio");
            set1.setColor(Color.RED);

            LineDataSet set2 = new LineDataSet(yValsSpeed, "Speed");
            set2.setColor(Color.BLACK);
        	
            ArrayList<LineDataSet> dataSets = new ArrayList<LineDataSet>();
            dataSets.add(set1);
            dataSets.add(set2);

            data = new LineData(xVals, dataSets);
    	}
    	
    	return data;
        
        /*

        // create a dataset and give it a type
        LineDataSet set1 = new LineDataSet(yVals, "DataSet 1");
        // set1.setFillAlpha(110);
        // set1.setFillColor(Color.RED);

        // set the line to be drawn like this "- - - - - -"
        set1.enableDashedLine(10f, 5f, 0f);
        set1.setColor(Color.BLACK);
        set1.setCircleColor(Color.BLACK);
        set1.setLineWidth(1f);
        set1.setCircleSize(3f);
        set1.setDrawCircleHole(false);
        set1.setValueTextSize(9f);
        set1.setFillAlpha(65);
        set1.setFillColor(Color.BLACK);
//        set1.setDrawFilled(true);
        // set1.setShader(new LinearGradient(0, 0, 0, mChart.getHeight(),
        // Color.BLACK, Color.WHITE, Shader.TileMode.MIRROR));


        // create a data object with the datasets
        LineData data = new LineData(xVals, dataSets);

        // set data
        mChart.setData(data);
        */
    }
    
}
