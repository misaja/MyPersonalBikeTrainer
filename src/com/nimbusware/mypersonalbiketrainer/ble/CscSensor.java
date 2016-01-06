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
    
    private final List<SpeedSensorListener> mSpeedListeners = 
    		new ArrayList<SpeedSensorListener>();
    private final List<CadenceSensorListener> mCadenceListeners = 
    		new ArrayList<CadenceSensorListener>();
    private final int mWheelSize;
    
    private CscReading mLastCSCReading;
	private double mLastSpeed;
	private double mLastCadence;

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
		    Log.v(TAG, "CSC raw data: " + buf.toString());

		    CscReading newReading = new CscReading(wheelRevs, wheelElapsed, crankRevs, crankElapsed, mLastCSCReading);
		    
	    	double speed = newReading.getSpeed(mWheelSize);
	    	double cadence = newReading.getCrankRPM();
	    	
	    	buf = new StringBuffer();
	        buf.append("SPEED=").append(speed);
	        buf.append(", CADENCE=").append(cadence);
		    Log.v(TAG, "CSC notification data: " + buf.toString());
		    
		    if (speed >= 0) { // filter out spurious negative values (of unknown origin)
		    	if (speed > 0 || mLastSpeed == 0) {
		    		// current speed is either a positive value or zero: in the latter case,
		    		// we skip notification unless the previous value was also zero, to the
		    		// effect that spurious zero values (i.e., single zero readings in the
		    		// midst of a regular session) are filtered out; not that a zero speed
		    		// reading also means that the wheel rev count is not changed since last
		    		// reading, so total distance and total rev count are unaffected
				    for (SpeedSensorListener listener : mSpeedListeners) {
					    Log.v(TAG, "Sending CSC wheel sensor notification");
				    	listener.updateSpeed(speed);
				    	listener.updateDistance(newReading.getDistance(mWheelSize));
				    	listener.updateWheelRevsCount(newReading.getWheelRevsSinceLastReading());
				    }
		    	}
		    }
		    
		    if (cadence >= 0) { // filter out spurious negative values (of unknown origin)
		    	if (cadence > 0 || mLastCadence == 0) {
		    		// current cadence is either a positive value or zero: in the latter case,
		    		// we skip notification unless the previous value was also zero, to the
		    		// effect that spurious zero values (i.e., single zero readings in the
		    		// midst of a regular session) are filtered out; not that a zero cadence
		    		// reading also means that the crank rev count is not changed since last
		    		// reading, so total total crank count is unaffected
				    for (CadenceSensorListener listener : mCadenceListeners) {
					    Log.v(TAG, "Sending CSC crank sensor notification");
				    	listener.updateCadence(cadence);
				    	listener.updateCrankRevsCount(newReading.getCrankRevsSinceLastReading());
				    }
		    	}
		    }
		    
		    mLastCSCReading = newReading;
		    mLastSpeed = speed;
		    mLastCadence = cadence;
		    
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
