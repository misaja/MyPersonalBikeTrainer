package com.nimbusware.android.ble.sensors;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

/**
 * Wrapper for a simple sensor having one single target service/characteristic.
 * 
 * @author Mauro Isaja
 *
 * @param <T>
 */
public class SimpleSensor<T> implements Sensor<T> {
	
	private final static String TAG = SimpleSensor.class.getSimpleName();
	private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
	private final static int CONNECTION_TIMEOUT = 10000; // 10s 
	private final static int DISCOVERY_TIMEOUT = 3000; // 3s 
	private final static int WRITE_TIMEOUT = 1000; // 1s 

	private final Context _context;
	private final BluetoothAdapter _adapter;
	private final SensorProfile<T> _profile;
	private final SensorConfig _config;
	private final List<StateListener> _stateListeners = new ArrayList<StateListener>();
	private final List<DataListener<T>> _dataListeners = new ArrayList<DataListener<T>>();
	private final Handler _handler = new Handler();

	private BluetoothGatt _gatt;
	private DataParser<T> _parser;
	private SensorState _state;
	
    // Implements callback methods for GATT events that we care about
    private final BluetoothGattCallback mCallback = new BluetoothGattCallback() {

        // this may be called a LONG time after BluetoothDevice.connectGatt() is started
        // (e.g., 50s when the BLE device was in standby); if no BLE device is reachable at
    	// the given address, however, it is called quite immediately (5") or never at all
    	@Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        	if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                	synchronized (SimpleSensor.this) {
                		if (_state == SensorState.WaitingForConnection) {
                			// connection attempt successful:
                            // discoverServices() launches a background system thread, which will
                            // eventually call the onServicesDiscovered callback (see below)
                            // when done - timing is very tight if the BLE device is responding
                            Log.i(TAG, "Connected to GATT server at " + getAddress());
                            _state = SensorState.DiscoveringServices;
                            if (_gatt.discoverServices()) { 
                                Log.i(TAG, "Discoverig services of GATT server at " + getAddress());

                            	// launch a delayed task for checking connection timeout:
                            	// if we get no reply from the remote server, we must close our client and move forward
                            	_handler.postDelayed(new TimeoutCheckTask(SensorState.DiscoveringServices), DISCOVERY_TIMEOUT);
                            } else {
                        		doClose();
                                Log.w(TAG, "Cannot start service discovery of GATT server " + getAddress());
                            }
                		} else {
                			// connection was successful AFTER timeout expired:
                			// don't know if this might actually happen as we call
                			// BluetoothGatt.close() on timeout, but better be prepared
                    		doClose();
                            Log.w(TAG, "Connection to GATT server " + getAddress() + 
                            		" was successful, but was dropped due to timeout");
                		}
                	}
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                	// apparently, this never happens when we call close()
            		doClose();
                    Log.i(TAG, "Disconnected from GATT server " + getAddress());
                }
        	} else {
        		doClose();
                Log.w(TAG, "Connection to GATT server " + getAddress() + 
                		" failed: STATUS=" + status + ", STATE=" + newState);
                
                
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
	            if (status == BluetoothGatt.GATT_SUCCESS) {
	            	synchronized (SimpleSensor.this) {
	            		if (_state == SensorState.DiscoveringServices) {
			                Log.i(TAG, "Service discovery successfully completed on GATT server " + getAddress());
		
			                // attempts to get a service handle from the connected device (synchronous call)
			                _state = SensorState.SettingUpService;
			            	BluetoothGattService srv = gatt.getService(_profile.getServiceId());
			            	if (null != srv) {
			                    Log.i(TAG, "Target service " + getServiceId() + 
			                    		" successfully acquired on GATT server " + getAddress());
		
				                // attempts to get a characteristic handle from the service handle (synchronous call)
			                    _state = SensorState.SettingUpCharacteristic;
			                    BluetoothGattCharacteristic btChar = srv.getCharacteristic(_profile.getCharacteristicId());
			            		if (null != btChar) {
			                        Log.i(TAG, "Target characteristic " + btChar.toString() + 
			                        		" successfully acquired on GATT server " + getAddress());
		
					                // attempts to set up characteristic notification (synchronous call)
			                        _state = SensorState.SettingUpNotifications;
			            			if (_gatt.setCharacteristicNotification(btChar, true)) {
				            			Log.i(TAG, "Writing configuration for characteristic " + btChar.toString() + 
				            					" on GATT server " + getAddress());
				            			BluetoothGattDescriptor descriptor = 
				            					btChar.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
				            			descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
				            			
				            			// asynchronous call: wait for onDescriptorWrite() callback
				            			_gatt.writeDescriptor(descriptor); 

		                            	// launch a delayed task for checking connection timeout:
		                            	// if we get no reply from the remote server, we must close our client and move forward
		                            	_handler.postDelayed(new TimeoutCheckTask(SensorState.SettingUpNotifications), WRITE_TIMEOUT);
			            			} else {
			                            doClose();
			                            Log.w(TAG, "Cannot enable notifications for characteristic " + 
			                            		getCharId() + " on GATT server " + getAddress());
			            			}
			            		} else {
		                            doClose();
			                        Log.w(TAG, "Cannot find target characteristic " + 
			                        		getCharId() + " on GATT server " + getAddress());
			            		}
			            	} else {
		                        doClose();
			                    Log.w(TAG, "Cannot find target service " + getServiceId() + 
			                    		" on GATT server " + getAddress());
			            	}
                		} else {
                			// service discovery was successful AFTER timeout expired:
                			// don't know if this might actually happen as we call
                			// BluetoothGatt.close() on timeout, but better be prepared
                            doClose();
                            Log.w(TAG, "Service discovery on GATT server " + getAddress() + 
                            		" was successful, but connection was dropped due to timeout");
                		}
	            	}
            } else {
        		doClose();
                Log.w(TAG, "Service discovery failed on GATT server " + getAddress());
            }
        }

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt,
				BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	synchronized (SimpleSensor.this) {
            		if (_state == SensorState.SettingUpNotifications) {
		            	// sensor is now open for business
		                Log.i(TAG, "Connection to GATT server " + getAddress() + " is now open");
		                
		            	_state = SensorState.Open;
		            	_parser = (DataParser<T>) _profile.getParser(_config.getParams());
		            	
		            	// notify that we are done
		            	notifyStateListeners();
	            	} else {
            			// notification setting was successful AFTER timeout expired:
            			// don't know if this might actually happen as we call
            			// BluetoothGatt.close() on timeout, but better be prepared
                        doClose();
                        Log.w(TAG, "Characteristic configuration on GATT server " + getAddress() + 
                        		" was successful, but connection was dropped due to timeout");
	            	}
            	}
            } else {
                doClose();
                Log.w(TAG, "Configuration failed on GATT server " + getAddress());
            }
            
			// superclass doesn't do anything here, but we'll be strict...
			super.onDescriptorWrite(gatt, descriptor, status);
		}

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
        	notifyDataListeners(characteristic);
        }
    };
    
	public SimpleSensor(Context context, BluetoothAdapter adapter,
			SensorProfile<T> profile, SensorConfig config) {
		if (null == context || null == adapter || null == profile || null == config)
			throw new NullPointerException();
		
		_context = context;
		_adapter = adapter;
		_profile = profile;
		_config = config;
	}

	@Override
	public String getAddress() {
		return _config.getAddress();
	}
	
	private String getServiceId() {
		return _profile.getServiceId().toString();
	}
	
	private String getCharId() {
		return _profile.getCharacteristicId().toString();
	}
	
	public synchronized SensorState getState() {
		return _state;
	}

	@Override
	public synchronized boolean open() {
		if (_state == SensorState.Closed) {
			doOpen();
	    	return true;
		} else {
			return false;
		}
	}
	
	private synchronized void doOpen() {
        // to be safe, cancel any BT discovery process that some other app might have launched
        _adapter.cancelDiscovery();
        
        Log.i(TAG, "Connecting to GATT server at " + getAddress());
        _state = SensorState.WaitingForConnection;
    	_gatt = _adapter.getRemoteDevice(getAddress()).connectGatt(_context, false, mCallback);

    	// launch a delayed task for checking connection timeout:
    	// if we get no reply from the remote server, we must close our client and move forward
    	_handler.postDelayed(new TimeoutCheckTask(SensorState.WaitingForConnection), CONNECTION_TIMEOUT);
	}

	@Override
	public synchronized boolean reopen() {
		if (_state == SensorState.Open || _state == SensorState.Closed) {
			Log.i(TAG, "Reopening BLE device at " + getAddress());

			doClose(); // listeners are not affected
			
			// wait a few milliseconds to give BLE some time to clean up
			// (don't know if this is actually needed, but let's play it safe)
			Globals.waitBleServer();
			
			doOpen();
			
	    	return true;
		} else {
			return false;
		}
	}

	@Override
	public synchronized void close() {
		_dataListeners.clear();

		doClose();
		
		// remove listeners after closure, otherwise
		// they will not get notified of the change in state
		_stateListeners.clear();
	}
	
	private synchronized void doClose() {
		if (null != _gatt) {
	        Log.i(TAG, "Closing BLE device at " + getAddress());
			_gatt.close();
		}
		
		boolean notify = _state != SensorState.Closed;
		_state = SensorState.Closed;
		_gatt = null;
		_parser = null;
		
		if (notify) {
        	// notify that we are closed
        	notifyStateListeners();
		}
	}

	@Override
	public void registerStateListener(StateListener listener) {
		if (null == listener)
			throw new NullPointerException();
		
		if (!_stateListeners.contains(listener)) {
			_stateListeners.add(listener);
		}
	}

	@Override
	public void unregisterStateListener(StateListener listener) {
		if (null == listener)
			throw new NullPointerException();
		
		_stateListeners.remove(listener);
	}
	
	private void notifyStateListeners() {
		for (StateListener listener : _stateListeners) {
			listener.notifyStateChange(this, _state);
		}
	}

	@Override
	public SensorProfile<T> getProfile() {
		return _profile;
	}

	@Override
	public void registerDataListener(DataListener<T> listener) {
		if (null == listener)
			throw new NullPointerException();
		
		if (!_dataListeners.contains(listener)) {
			_dataListeners.add(listener);
		}
	}

	@Override
	public void unregisterDataListener(DataListener<T> listener) {
		if (null == listener)
			throw new NullPointerException();
		
		_dataListeners.remove(listener);
	}
	
	private void notifyDataListeners(BluetoothGattCharacteristic btChar) {
		if (null != _parser) {
			T data = _parser.parse(btChar);
			if (null != data) { // null if data is unreadable (parser should log the error)
				for (DataListener<T> listener : _dataListeners) {
						listener.notifyNewData(data);
				}
			}
		}
	}
	
	private class TimeoutCheckTask implements Runnable {
		
		private final SensorState _pendingState;
		
		TimeoutCheckTask(SensorState pendingState) {
			_pendingState = pendingState;
		}

		@Override
		public void run() {
			synchronized (SimpleSensor.this) {
				// will close the sensor if the current state is anything less or equal
				// (with the exception of Closed, as there would be nothing to do) to the
				// "pending state" - i.e., the was no progress since this task was scheduled
				if (getState() != SensorState.Closed && getState().ordinal() <= _pendingState.ordinal()) {
					doClose();
			        Log.i(TAG, "Timeout reached for operation on GATT server " + getAddress());
				}
			}
		}
		
	}
}
