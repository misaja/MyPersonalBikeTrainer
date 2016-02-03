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

        // this may be called a LONG time after BluetoothDevice.connectGatt() is started
        // (e.g., 50s when the BLE device was in standby); if no BLE device is reachable at
    	// the given address, however, it is called quite immediately (5") or never at all
    	@Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
    		notifyListeners(status, newState);
        	if (status == BluetoothGatt.GATT_SUCCESS) {
	        	synchronized (SingleValueSensor.this) {
	                if (newState == BluetoothProfile.STATE_CONNECTED) {
	                    Log.i(TAG, "Connected to GATT server at " + mAddress);
		                //Globals.waitBleServer();
	                    // this launches a background system thread, which will
	                    // eventually call the onServicesDiscovered callback (see below)
	                    // when done - timing is very tight if the BLE device is responding
	                    if (gatt.discoverServices()) { 
	                        Log.i(TAG, "Discoverig services of GATT server at " + mAddress);
	                    } else {
	                    	mOperationPending = false;
	                        Log.w(TAG, "Cannot start service discovery of GATT server at " + mAddress);
	                    }
	                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
	                	// apparently, this never happens when we call close()
	                    Log.i(TAG, "Disconnected from GATT server at " + mAddress);
	                }
				}
        	} else {
        		// open() call failed
                mServer.disconnect();
                
            	mOperationPending = false;
                Log.w(TAG, "Connection to GATT server at " + mAddress + " failed: STATUS=" + status + ", STATE=" + newState);
                
                
                // TODO
                // Here status and newState are typically not one of the officially endorsed values
                // (see BluetoothGatt.GATT_* and BluetoothProfile.STATE_* constants, respectively).
                // In particular, the standard failure status should be BluetoothGatt.GATT_FAILURE, that is 0x0101,
                // or 257 in decimal notation; however, it has been observed that when we try to connect to
                // a sensor which is simply not there, the systems behaves in one of these two ways: A) this
                // callback is never called, and the open() call simply gets lost somewhere in the native
                // BLE stack; B) this callback is called with STATUS=133 and STATE=-2.
                // Now the point is: is it reasonable to detect these situations and react by restarting the 
                // connection all over again? The objective is making the application resilient: it will try to reach
                // sensors that are STILL not there but might pop up online later AND will try to re-establish a
                // CRASHED connection
        	}
        }

        // this is typically called immediately after BluetoothGatt.discoverServices() is started,
        // provided a BLE device is reachable at the given address; if that is not the case,
    	// however, this might NEVER be called - so it is important to detect the "device unreachable"
    	// condition in the onConnectionStateChange callback (see above)
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        	synchronized (SingleValueSensor.this) {
	            if (status == BluetoothGatt.GATT_SUCCESS) {
	                Log.i(TAG, "Service discovery successfully completed on GATT server at " + mAddress);

	                // Attempts to get a service handle from the connected device,
                    // but waits for a short time before sending the new command
                    // in order to ease BLE operations
	                //Globals.waitBleServer();
	            	BluetoothGattService srv = gatt.getService(mServiceId); // timing ~0.1s
	            	if (null != srv) {
	                    Log.i(TAG, "Target service " + mServiceId.toString() + " successfully acquired on GATT server at " + mAddress);

		                // Attempts to get a characteristic handle from the service handle,
	                    // but waits for a short time before sending the new command
	                    // in order to ease BLE operations
		                // Globals.waitBleServer();
	            		mCharacteristic = srv.getCharacteristic(mCharacteristicId); // timing ~0.1s
	            		if (null != mCharacteristic) {
	                        Log.i(TAG, "Target service characteristic " + mCharacteristic.toString() + " successfully acquired on GATT server at " + mAddress);

			                // Attempts to set up characteristic notification,
		                    // but waits for a short time before sending the new command
		                    // in order to ease BLE operations
			                // Globals.waitBleServer();
	            			if (mServer.setCharacteristicNotification(mCharacteristic, true)) { // timing ~0.1s
		            			Log.i(TAG, "Writing configuration for service characteristic " + mCharacteristic.toString() + " on GATT server at " + mAddress);
		            			BluetoothGattDescriptor descriptor = mCharacteristic.getDescriptor(GattNames.CLIENT_CHARACTERISTIC_CONFIG);
		            			descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		            			mServer.writeDescriptor(descriptor); // timing ~0.1s
		                    	
		                    	// sensor is now open for business
		                    	mOperationPending = false;
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
            Log.v(TAG, "New characteristic data from GATT server at " + mAddress + ": " + bytesToHex(characteristic.getValue()) + " [Thread ID: " + Thread.currentThread().getId() + "]");
        	notifyListeners(characteristic);
        }

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt,
				BluetoothGattDescriptor descriptor, int status) {
			super.onDescriptorWrite(gatt, descriptor, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Configuration of service characteristic " + mCharacteristic.toString() + " completed successfully on GATT server at " + mAddress);
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
	
	public synchronized boolean isBusy() {
		return mOperationPending;
	}
	
	public synchronized boolean open() {
		if (!mOperationPending) {
	        Log.i(TAG, "Opening Bluetooth device at " + mAddress);
	        mAdapter.cancelDiscovery();
	        BluetoothDevice device = mAdapter.getRemoteDevice(mAddress);
	    	mOperationPending = true;
	        Log.i(TAG, "Connecting to GATT server at " + mAddress);
	    	mServer = device.connectGatt(mContext, true, mCallback);
	    	return true;
		} else {
			return false;
		}
	}
	
	public synchronized boolean reopen() {
		if (!mOperationPending) {
			Log.i(TAG, "Refreshing Bluetooth device at " + mAddress);

			// close
			if (null != mServer) {
		        Log.i(TAG, "Closing Bluetooth device at " + mAddress);
				mOperationPending = false;
				mOpen = false;
				mServer.close();
				mServer = null;
			}
			
			// wait a few milliseconds to give BLE some time to clean up
			// (don't know if this is actually needed, but let's play it safe)
			Globals.waitBleServer();
			
			doClose(true); // this will NOT unregister the current listeners

			// open
	        mAdapter.cancelDiscovery();
			BluetoothDevice device = mAdapter.getRemoteDevice(mAddress);
	    	mOperationPending = true;
	        Log.i(TAG, "Connecting to GATT server at " + mAddress);
	    	mServer = device.connectGatt(mContext, true, mCallback);
	    	return true;
		} else {
			return false;
		}
	}
	
	public void close() {
		if (null != mServer) {
	        Log.i(TAG, "Closing Bluetooth device at " + mAddress);
			mOperationPending = false;
			mOpen = false;
			mServer.close();
			mServer = null;
		}
		
		doClose(false); // this will unregister all the current listeners
	}
	
	protected abstract void notifyListeners(int status, int newState);
	
	protected abstract void notifyListeners(BluetoothGattCharacteristic characteristic);
	
	protected abstract void doClose(boolean isRefresh);
}
