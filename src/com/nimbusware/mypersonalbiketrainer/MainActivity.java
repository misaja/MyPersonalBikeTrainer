/*
 * Copyright (C) 2014 Mauro Isaja mauro.isaja@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nimbusware.mypersonalbiketrainer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private static final String TAG = MainActivity.class.getSimpleName();

    
	private BluetoothAdapter mAdapter;

	private ImageView mCardioIcon;
	private ImageView mSpeedIcon;
	private ImageView mCadenceIcon;
	private TextView mWheelSizeText;
	private Button  mGoButton;
	
	private int mWheelSize;
	private String mHeartSensorAddr;
	private String mWheelSensorAddr;
	private String mCrankSensorAddr;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
    	Log.d(TAG, "Creating activity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // override manifest, as this is the launcher and as such it has
        // the app name as the label
        setTitle(R.string.title_activity_settings);
    	
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
        	Log.w(TAG, "No Bluetooth LE support detected on device: exiting");
            Toast.makeText(this, R.string.error_ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mAdapter = manager.getAdapter();
        if (mAdapter == null) {
        	Log.w(TAG, "Bluetooth is not enabled: exiting");
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        mCardioIcon = (ImageView) findViewById(R.id.imgCardio);
        mSpeedIcon = (ImageView) findViewById(R.id.imgSpeed);
        mCadenceIcon = (ImageView) findViewById(R.id.imgCadence);
        mWheelSizeText = (TextView) findViewById(R.id.txtWheelSize);
        mGoButton = (Button) findViewById(R.id.btnGo);
        
    	Log.d(TAG, "Loading saved settings");
        SharedPreferences prefs = getSharedPreferences(MainActivity.class.getSimpleName(), MODE_PRIVATE);
        mHeartSensorAddr = prefs.getString(Globals.HEART_SENSOR_ADDR, null);
        mWheelSensorAddr = prefs.getString(Globals.WHEEL_SENSOR_ADDR, null);
        mCrankSensorAddr = prefs.getString(Globals.CRANK_SENSOR_ADDR, null);
        mWheelSize = prefs.getInt(Globals.WHEEL_SIZE, Globals.WHEEL_SIZE_DEFAULT);
    	
        StringBuffer buf = new StringBuffer();
        buf.append("HEART_SENSOR_ADDR=").append(mHeartSensorAddr);
        buf.append(", WHEEL_SENSOR_ADDR=").append(mWheelSensorAddr);
        buf.append(", CRANK_SENSOR_ADDR=").append(mCrankSensorAddr);
        buf.append(", WHEEL_SIZE=").append(mWheelSize);
    	Log.i(TAG, "Saved settings: " + buf.toString());

    	findViewById(R.id.btnSetCardio).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
		        Intent intent = new Intent(MainActivity.this, DiscoveryActivity.class);
		        intent.putExtra(Globals.REQ_CODE, Globals.REQ_DISCOVER_CARDIO_SENSOR);
		        startActivityForResult(intent, Globals.REQ_DISCOVER_CARDIO_SENSOR);
			}
		});
        
        findViewById(R.id.btnSetSpeed).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
		        Intent intent = new Intent(MainActivity.this, DiscoveryActivity.class);
		        intent.putExtra(Globals.REQ_CODE, Globals.REQ_DISCOVER_SPEED_SENSOR);
		        startActivityForResult(intent, Globals.REQ_DISCOVER_SPEED_SENSOR);
			}
		});
        
        findViewById(R.id.btnSetCadence).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
		        Intent intent = new Intent(MainActivity.this, DiscoveryActivity.class);
		        intent.putExtra(Globals.REQ_CODE, Globals.REQ_DISCOVER_CADENCE_SENSOR);
		        startActivityForResult(intent, Globals.REQ_DISCOVER_CADENCE_SENSOR);
			}
		});
        
        findViewById(R.id.btnSetWheel).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				WheelDialog dialog = new WheelDialog();
				dialog.show(MainActivity.this.getFragmentManager(), "dialog");
			}
		});
        
        mGoButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, CockpitActivity.class);
				startActivity(intent);
			}
		});
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.settings, menu);
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
		}
		return true;
	}

	@Override
	protected void onResume() {
    	Log.d(TAG, "Resuming activity");
		super.onResume();
    	
        if (!mAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, Globals.REQ_ENABLE_BT);
            return;
        }

        refreshUI();
	}

	private void refreshUI() {
    	mGoButton.setEnabled(false);
        
    	if (null != mHeartSensorAddr) {
        	mCardioIcon.setImageResource(R.drawable.ic_action_accept);
        	mGoButton.setEnabled(true);
        } else {
        	mCardioIcon.setImageResource(R.drawable.ic_action_cancel);
        }
    	
        if (null != mWheelSensorAddr) {
        	mSpeedIcon.setImageResource(R.drawable.ic_action_accept);
        	mGoButton.setEnabled(true);
        } else {
        	mSpeedIcon.setImageResource(R.drawable.ic_action_cancel);
        }
    	
        if (null != mCrankSensorAddr) {
        	mCadenceIcon.setImageResource(R.drawable.ic_action_accept);
        	mGoButton.setEnabled(true);
        } else {
        	mCadenceIcon.setImageResource(R.drawable.ic_action_cancel);
        }

        mWheelSizeText.setText(String.valueOf(mWheelSize));
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    	Log.d(TAG, "Handling activity results");
        
        if (requestCode == Globals.REQ_ENABLE_BT) {
        	if (resultCode == Activity.RESULT_OK) {
		    	Log.i(TAG, "Bluetooth is now enabled on this device");
        	} else {
	        	Log.w(TAG, "User choose not to enable Bluetooth: exiting");
	            finish();
        	}
        } else if (requestCode == Globals.REQ_DISCOVER_CARDIO_SENSOR) {
    		if (resultCode == Activity.RESULT_OK) {
		        mHeartSensorAddr = (String) data.getStringExtra(Globals.SENSOR_ADDR);
		        storeString(Globals.HEART_SENSOR_ADDR, mHeartSensorAddr);
		    	Log.i(TAG, "New heart sensor address: " + mHeartSensorAddr);
    		}
        } else if (requestCode == Globals.REQ_DISCOVER_SPEED_SENSOR) {
    		if (resultCode == Activity.RESULT_OK) {
    			mWheelSensorAddr = (String) data.getStringExtra(Globals.SENSOR_ADDR);
		        storeString(Globals.WHEEL_SENSOR_ADDR, mWheelSensorAddr);
		    	Log.i(TAG, "New wheel sensor address: " + mWheelSensorAddr);
    		}
        } else if (requestCode == Globals.REQ_DISCOVER_CADENCE_SENSOR) {
    		if (resultCode == Activity.RESULT_OK) {
    			mCrankSensorAddr = (String) data.getStringExtra(Globals.SENSOR_ADDR);
		        storeString(Globals.CRANK_SENSOR_ADDR, mCrankSensorAddr);
		    	Log.i(TAG, "New crank sensor address: " + mCrankSensorAddr);
    		}
        }

    	Log.d(TAG, "Activity results successfully handled");
    }
    
    private void storeString(String key, String value) {
        SharedPreferences prefs = getSharedPreferences(MainActivity.class.getSimpleName(), MODE_PRIVATE);
        prefs.edit().putString(key, value).apply();
    }
    
    private void storeInt(String key, int value) {
        SharedPreferences prefs = getSharedPreferences(MainActivity.class.getSimpleName(), MODE_PRIVATE);
        prefs.edit().putInt(key, value).apply();
    }

	public class WheelDialog extends DialogFragment {
	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
	        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	        final EditText input = new EditText(MainActivity.this);
	        input.setInputType(InputType.TYPE_CLASS_NUMBER);
	        input.setText(String.valueOf(mWheelSize));
	        builder.setTitle(R.string.title_wheel_dialog)
	        	.setView(input)
	            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
	            	public void onClick(DialogInterface dialog, int id) {
	            		String wheelSizeStr = input.getText().toString();
	            		int wheelSize = mWheelSize;
	            		try {
		            		wheelSize = Integer.valueOf(wheelSizeStr);
		            		storeInt(Globals.WHEEL_SIZE, wheelSize);
		            		mWheelSize = wheelSize;
		            		refreshUI();
	            		} catch (NumberFormatException ex) {
	            			Log.e(TAG, "Bad integer format: " + wheelSizeStr);
	            		}
	            	}
	            })
	            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
	            	public void onClick(DialogInterface dialog, int id) {
	            		// nothing to do
	            	}
	            });
	        return builder.create();
	    }
	}
}

