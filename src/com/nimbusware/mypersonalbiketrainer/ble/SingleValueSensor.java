package com.nimbusware.mypersonalbiketrainer.ble;

import java.util.UUID;

import com.nimbusware.mypersonalbiketrainer.Globals;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

public abstract class SingleValueSensor {

	private final static String TAG = SingleValueSensor.class.getSimpleName();
	private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
	
	protected static String bytesToHex(byte[] bytes) {
		if (null != bytes && bytes.length > 0) {
		    char[] hexChars = new char[bytes.length * 2];
		    for ( int j = 0; j < bytes.length; j++ ) {
		        int v = bytes[j] & 0xFF;
		        hexChars[j * 2] = hexArray[v >>> 4];
		        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		    }
		    return new String(hexChars);
		} else {
			return null;
		}
	}	

	private final Context mContext;
	private final BluetoothAdapter mAdapter;
	private final String mAddress;
	private final UUID mServiceId;
	private final UUID mCharacteristicId;

	private BluetoothGatt mServer;
	private BluetoothGattCharacteristic mCharacteristic;
	
	private boolean mOperationPending;
	private boolean mOpen;
	
    // Implements callback methods for GATT events that we care about
    private final BluetoothGattCallback mCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        	synchronized (SingleValueSensor.this) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "Connected to GATT server at " + mAddress);
                    // Attempts to discover services after successful connection,
                    // but waits for a short time before sending the new command
                    // in order to ease BLE operations
	                Globals.waitBleServer();
                    if (gatt.discoverServices()) {
                        Log.d(TAG, "Discoverig services of GATT server at " + mAddress);
                    } else {
                    	mOperationPending = false;
                        Log.w(TAG, "Cannot start service discovery of GATT server at " + mAddress);
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                	mOperationPending = false;
                	mOpen = false;
                    Log.d(TAG, "Disconnected from GATT server at " + mAddress);
                }
			}
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        	synchronized (SingleValueSensor.this) {
	            if (status == BluetoothGatt.GATT_SUCCESS) {
	                Log.d(TAG, "Service discovery successfully completed on GATT server at " + mAddress);

	                // Attempts to get a service handle from the connected device,
                    // but waits for a short time before sending the new command
                    // in order to ease BLE operations
	                Globals.waitBleServer();
	            	BluetoothGattService srv = gatt.getService(mServiceId);
	            	if (null != srv) {
	                    Log.d(TAG, "Target service " + mServiceId.toString() + " successfully acquired on GATT server at " + mAddress);

		                // Attempts to get a characteristic handle from the service handle,
	                    // but waits for a short time before sending the new command
	                    // in order to ease BLE operations
		                Globals.waitBleServer();
	            		mCharacteristic = srv.getCharacteristic(mCharacteristicId);
	            		if (null != mCharacteristic) {
	                        Log.d(TAG, "Target service characteristic " + mCharacteristic.toString() + " successfully acquired on GATT server at " + mAddress);

			                // Attempts to set up characteristic notification,
		                    // but waits for a short time before sending the new command
		                    // in order to ease BLE operations
			                Globals.waitBleServer();
	            			if (mServer.setCharacteristicNotification(mCharacteristic, true)) {
		            			BluetoothGattDescriptor descriptor = mCharacteristic.getDescriptor(GattNames.CLIENT_CHARACTERISTIC_CONFIG);
		            			descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		                        Log.d(TAG, "Writing configuration for service characteristic " + mCharacteristic.toString() + " on GATT server at " + mAddress);
		            			mServer.writeDescriptor(descriptor);
		                    	mOperationPending = false;
		                    	
		                    	// sensor is now open for business
		                    	mOpen = true;
	            			} else {
	                        	mOperationPending = false;
	                            Log.w(TAG, "Cannot enable notifications for service characteristic " + mCharacteristic.toString() + " on GATT server at " + mAddress);
	            			}
	            		} else {
	                    	mOperationPending = false;
	                        Log.w(TAG, "Cannot find target service characteristic " + mCharacteristic.toString() + " on GATT server at " + mAddress);
	            		}
	            	} else {
	                	mOperationPending = false;
	                    Log.w(TAG, "Cannot find target service " + mServiceId.toString() + " on GATT server at " + mAddress);
	            	}
	            } else {
                	mOperationPending = false;
	                Log.w(TAG, "Service discovery failed on GATT server at " + mAddress);
	            }
        	}
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "New characteristic data from GATT server at " + mAddress + ": " + bytesToHex(characteristic.getValue()) + " [Thread ID: " + Thread.currentThread().getId() + "]");
        	notifyListeners(characteristic);
        }

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt,
				BluetoothGattDescriptor descriptor, int status) {
			super.onDescriptorWrite(gatt, descriptor, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Configuration of service characteristic " + mCharacteristic.toString() + " completed successfully on GATT server at " + mAddress);
            } else {
                Log.w(TAG, "Configuration of service characteristic " + mCharacteristic.toString() + " failed on GATT server at " + mAddress);
            }
		}
    };

    public SingleValueSensor(Context context, BluetoothAdapter adapter,
    		String address, UUID serviceId, UUID characteristicId) {
		if (null == context || null == adapter ||
				null == address || null == serviceId ||null == characteristicId)
			throw new NullPointerException();
		
		if (!BluetoothAdapter.checkBluetoothAddress(address)) {
			throw new IllegalArgumentException("Invalid BLE sensorAddress " + address);
		}
		
		mContext = context;
		mAdapter = adapter;
		mAddress = address;
		mServiceId = serviceId;
		mCharacteristicId = characteristicId;
	}

	public String getAddress() {
		return mAddress;
	}
	
	public synchronized boolean isOpen() {
		return mOpen;
	}
	
	public synchronized boolean isWaiting() {
		return mOperationPending;
	}
	
	public synchronized boolean open() {
		if (!mOperationPending) {
	        Log.d(TAG, "Opening Bluetooth device at " + mAddress);
	        BluetoothDevice device = mAdapter.getRemoteDevice(mAddress);
	    	mOperationPending = true;
	        Log.d(TAG, "Connecting to GATT server at " + mAddress);
	    	mServer = device.connectGatt(mContext, true, mCallback);
	    	return true;
		} else {
			return false;
		}
	}
	
	public void close() {
        Log.d(TAG, "Closing Bluetooth device at " + mAddress);
		if (null != mServer) {
			mOperationPending = false;
			mOpen = false;
			mServer.close();
			mServer = null;
		}
		doClose();
	}
	
	protected abstract void notifyListeners(BluetoothGattCharacteristic characteristic);
	
	protected abstract void doClose();
}
