package com.nimbusware.mypersonalbiketrainer.ble;

import java.util.ArrayList;
import java.util.List;

import com.nimbusware.mypersonalbiketrainer.CadenceSensor;
import com.nimbusware.mypersonalbiketrainer.CadenceSensorListener;
import com.nimbusware.mypersonalbiketrainer.SpeedSensor;
import com.nimbusware.mypersonalbiketrainer.SpeedSensorListener;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.util.Log;

public class CscSensor extends SingleValueSensor implements SpeedSensor, CadenceSensor {
    
	private final static String TAG = CscSensor.class.getSimpleName();
    private final static int WHEEL_DATA_MASK = 0x01;
    private final static int CRANK_DATA_MASK = 0x02;
    
    private final List<SpeedSensorListener> mSpeedListeners = new ArrayList<SpeedSensorListener>();
    private final List<CadenceSensorListener> mCadenceListeners = new ArrayList<CadenceSensorListener>();
    private final int mWheelSize;
    
    private CscReading mLastCSCReading;

    public CscSensor(Context context, BluetoothAdapter adapter, String address, int wheelSize) {
		super(context, adapter, address, GattNames.CSC_SERVICE, GattNames.CSC_CHARACTERISTIC);
		mWheelSize = wheelSize;
	}

	@Override
	public void registerListener(SpeedSensorListener listener) {
		if (null == listener)
			throw new NullPointerException();
		
		if (!mSpeedListeners.contains(listener)) {
			mSpeedListeners.add(listener);
		}
	}

	@Override
	public void unregisterListener(SpeedSensorListener listener) {
		if (null == listener)
			throw new NullPointerException();
		
		mSpeedListeners.remove(listener);
	}

	@Override
	public void registerListener(CadenceSensorListener listener) {
		if (null == listener)
			throw new NullPointerException();
		
		if (!mCadenceListeners.contains(listener)) {
			mCadenceListeners.add(listener);
		}
	}

	@Override
	public void unregisterListener(CadenceSensorListener listener) {
		if (null == listener)
			throw new NullPointerException();
		
		mCadenceListeners.remove(listener);
	}

	protected void notifyListeners(BluetoothGattCharacteristic characteristic) {
		final byte[] data = characteristic.getValue();
		if (data != null && data.length > 0) {
			int flag = data[0];
		    boolean hasWheel = false;
		    boolean hasCrank = false;
		    if ((flag & WHEEL_DATA_MASK) == WHEEL_DATA_MASK) {
		    	hasWheel = true;
		    }
		    if ((flag & CRANK_DATA_MASK) == CRANK_DATA_MASK) {
		    	hasCrank = true;
		    }
		    
		    int wheelRevs = 0;
		    int wheelElapsed = 0;
		    int crankRevs = 0;
		    int crankElapsed = 0;
		    if (hasWheel && hasCrank) {
		    	wheelRevs = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 1);
		    	wheelElapsed = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 5);
		    	crankRevs = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 7);
		    	crankElapsed = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 9);
		    } else if (hasWheel) {
		    	wheelRevs = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 1);
		    	wheelElapsed = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 5);
		    } else if (hasCrank) {
		    	crankRevs = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1);
		    	crankElapsed = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 3);
		    }
		    
	        StringBuffer buf = new StringBuffer();
	        buf.append("WHEEL_REV_COUNT=").append(wheelRevs);
	        buf.append(", WHEEL_TIME_COUNT=").append(wheelElapsed);
	        buf.append(", CRANK_REV_COUNT=").append(crankRevs);
	        buf.append(", CRANK_TIME_COUNT=").append(crankElapsed);
		    Log.d(TAG, "CSC raw data: " + buf.toString());

		    CscReading newReading = new CscReading(wheelRevs, wheelElapsed, crankRevs, crankElapsed, mLastCSCReading);
		    
	    	double speed = newReading.getSpeed(mWheelSize);
	    	double distance = newReading.getDistance(mWheelSize);
	    	double cadence = newReading.getCrankRPM();
	    	int actualWheelRevs = newReading.getWheelRevsSinceLastReading();
	    	int actualCrankRevs = newReading.getCrankRevsSinceLastReading();
	    	
	    	buf = new StringBuffer();
	        buf.append("SPEED=").append(speed);
	        buf.append(", DISTANCE=").append(distance);
	        buf.append(", CADENCE=").append(cadence);
	        buf.append(", WHEEL_REVS=").append(actualWheelRevs);
	        buf.append(", CRANK_REVS=").append(actualCrankRevs);
		    Log.d(TAG, "CSC notification data: " + buf.toString());
		    
		    for (SpeedSensorListener listener : mSpeedListeners) {
			    Log.d(TAG, "Sending CSC wheel sensor notification");
		    	listener.updateSpeed(speed);
		    	listener.updateDistance(distance);
		    	listener.updateRevolutions(actualWheelRevs);
		    }
		    
		    for (CadenceSensorListener listener : mCadenceListeners) {
			    Log.d(TAG, "Sending CSC crank sensor notification");
		    	listener.updateBeatRate(cadence);
		    	listener.updateRevolutions(actualCrankRevs);
		    }
		    
		    mLastCSCReading = newReading;
		    
		} else {
		    Log.w(TAG, "CSC notification contains no data.");
		}
	}

	@Override
	protected void doClose() {
		mSpeedListeners.clear();
		mCadenceListeners.clear();
		mLastCSCReading = null;
	}
}
