package com.nimbusware.mypersonalbiketrainer;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class DiscoveryActivity extends ListActivity {

    // max scan duration: 20 secs
    private static final long MAX_SCAN_DURATION = 20000;
    private static final String TAG = DiscoveryActivity.class.getSimpleName();

    private int mAction;
    private Handler mHandler;
    private BluetoothAdapter mBtAdapter;
	private SensorListAdapter mListAdapter;
    private boolean mScanning;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
        mAction = getIntent().getIntExtra(Globals.REQ_CODE, 0);
        if (Globals.REQ_DISCOVER_CARDIO_SENSOR != mAction) {
        	setTitle(R.string.title_set_cardio);
        } else if (Globals.REQ_DISCOVER_SPEED_SENSOR != mAction) {
        	setTitle(R.string.title_set_speed);
        } else if (Globals.REQ_DISCOVER_CADENCE_SENSOR != mAction) {
        	setTitle(R.string.title_set_crank);
        } else {
        	Log.e(TAG, "Bad request code " + mAction + ": exiting");
        	finish();
        	return;
        }

        setContentView(R.layout.activity_discovery);
        getActionBar().setTitle(R.string.title_devices);
        
        mHandler = new Handler();
        
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = bluetoothManager.getAdapter();
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.discovery, menu);
        if (!mScanning) {
            menu.findItem(R.id.action_stop).setVisible(false);
            menu.findItem(R.id.action_scan).setVisible(true);
            menu.findItem(R.id.action_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.action_stop).setVisible(true);
            menu.findItem(R.id.action_scan).setVisible(false);
            menu.findItem(R.id.action_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_scan:
	            mListAdapter.clear();
				scanSensors(true);
				break;
			case R.id.action_stop:
				scanSensors(false);
				break;
		}
		return true;
	}

    @Override
    protected void onResume() {
        super.onResume();
        mListAdapter = new SensorListAdapter();
        setListAdapter(mListAdapter);
        scanSensors(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanSensors(false);
        mListAdapter.clear();
    }

	@Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	Log.i(TAG, "Item selected");
        final BluetoothDevice device = mListAdapter.getSensor(position);
        if (device == null)
        	return;
        if (mScanning) {
            mBtAdapter.stopLeScan(mScanCallback);
            mScanning = false;
        }
    	Log.i(TAG, "Selected item address: " + device.getAddress());
    	Intent resultData = new Intent();
    	resultData.putExtra(Globals.SENSOR_ADDR, device.getAddress());
        setResult(RESULT_OK, resultData);
        finish();
    }

	private void scanSensors(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBtAdapter.stopLeScan(mScanCallback);
                    invalidateOptionsMenu();
                }
            }, MAX_SCAN_DURATION);

            mScanning = true;
            mBtAdapter.startLeScan(mScanCallback);
        } else {
            mScanning = false;
            mBtAdapter.stopLeScan(mScanCallback);
        }
        invalidateOptionsMenu();
    }

    private class SensorListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mSensors;
        private LayoutInflater mInflater;

        public SensorListAdapter() {
            super();
            mSensors = new ArrayList<BluetoothDevice>();
            mInflater = DiscoveryActivity.this.getLayoutInflater();
        }

        public void addSensor(BluetoothDevice sensor) {
            if(!mSensors.contains(sensor)) {
                mSensors.add(sensor);
            }
        }

        public BluetoothDevice getSensor(int position) {
            return mSensors.get(position);
        }

        public void clear() {
            mSensors.clear();
        }

        @Override
        public int getCount() {
            return mSensors.size();
        }

        @Override
        public Object getItem(int i) {
            return mSensors.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @SuppressLint("InflateParams")
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = mInflater.inflate(R.layout.listitem_sensor, null);
            }

            String sensorName = mSensors.get(i).getName();
            if (sensorName == null || sensorName.length() == 0)
            	sensorName = getString(R.string.unknown_device);
            
            TextView title = (TextView) view.findViewById(R.id.sensor_name);
            title.setText(sensorName);

            return view;
        }
    }

    private BluetoothAdapter.LeScanCallback mScanCallback =
            new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice sensor, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mListAdapter.addSensor(sensor);
                    mListAdapter.notifyDataSetChanged();
                }
            });
        }
    };
}
