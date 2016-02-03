package com.nimbusware.android.ble.sensors;

import java.util.HashMap;
import java.util.Map;

import android.bluetooth.BluetoothAdapter;

/**
 * Configuration info of a deployed sensor.
 * @author Mauro Isaja
 *
 */
public class SensorConfig {

	private final String _name;
	private final String _profileName;
	private final String _address;
	private final Map<String, String> _params;
	
	public SensorConfig(String profileName, String address) {
		this(null, profileName, address, null);
	}

	public SensorConfig(String name, String profileName, String address) {
		this(name, profileName, address, null);
	}

	public SensorConfig(String name, String profileName, String address, 
			Map<String, String> params) {
		if (null == profileName || null == address)
			throw new NullPointerException();
		
		try {
			if (!SensorProfile.class.isAssignableFrom(Class.forName(profileName))) {
				throw new IllegalArgumentException("Invalid sensor profile specification: class " + profileName + " is not a SensorProfile");
			}
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Invalid sensor profile specification: class " + profileName + " cannot be loaded");
		}

		if (!BluetoothAdapter.checkBluetoothAddress(address)) {
			throw new IllegalArgumentException("Invalid format of sensor address: " + address);
		}
		
		_name = name; // may be null or empty
		_profileName = profileName;
		_address = address;
		if (null != params) {
			// clone the argument, so we don't have surprises...
			_params = new HashMap<>(params);
		} else {
			_params = new HashMap<>(0);
		}
	}

	/**
	 * The human-readable name of this sensor configuration.
	 * This information is optional, and may be legally null or empty.
	 * @return
	 */
	public String getName() {
		return _name;
	}

	/**
	 * The fully-qualified name of the class supporting the BLE profile.
	 * In other words, the type of the sensor.
	 * @return
	 */
	public String getProfileName() {
		return _profileName;
	}

	/**
	 * The saved network address of the sensor.
	 * @return
	 */
	public String getAddress() {
		return _address;
	}

	/**
	 * A key-value dictionary of runtime configuration parameters, if any are required.
	 * Each key has a special meaning for the profile-supporting class.
	 * The retuned Map may be empty, but is never null.
	 * @return
	 */
	public Map<String, String> getParams() {
		return _params;
	}
}
