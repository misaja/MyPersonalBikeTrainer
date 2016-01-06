package com.nimbusware.mypersonalbiketrainer.ble;

import java.util.ArrayList;
import java.util.List;

import com.nimbusware.mypersonalbiketrainer.BeatRateSensor;
import com.nimbusware.mypersonalbiketrainer.BeatRateSensorListener;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.util.Log;

public class HrmSensor extends SingleValueSensor implements BeatRateSensor {
    
	private final static String TAG = HrmSensor.class.getSimpleName();

	private final List<BeatRateSensorListener> mListeners = 
			new ArrayList<BeatRateSensorListener>();
    
	public HrmSensor(Context context, BluetoothAdapter adapter, String address) {
		super(context, adapter, address, GattNames.HRM_SERVICE, GattNames.HRM_CHARACTERISTIC);
	}

	@Override
	public void registerListener(BeatRateSensorListener listener) {
		if (null == listener)
			throw new NullPointerException();
		
		if (!mListeners.contains(listener)) {
			mListeners.add(listener);
		}
	}

	@Override
	public void unregisterListener(BeatRateSensorListener listener) {
		mListeners.remove(listener);
	}

	@Override
	protected void notifyListeners(BluetoothGattCharacteristic characteristic) {
		int flag = characteristic.getProperties();
		int format = -1;
		if ((flag & 0x01) != 0) {
		    format = BluetoothGattCharacteristic.FORMAT_UINT16;
		} else {
		    format = BluetoothGattCharacteristic.FORMAT_UINT8;
		}
		
		final int heartRate = characteristic.getIntValue(format, 1);

	    Log.v(TAG, "HRM notification data: HEART_RATE=" + heartRate);

	    for (BeatRateSensorListener listener : mListeners) {
		    Log.v(TAG, "Sending HRM sensor notification");
	    	listener.updateBeatRate(heartRate);
	    }
	}

	@Override
	protected void doClose() {
		mListeners.clear();
	}
}
