package com.nimbusware.android.trainer.config;

import com.nimbusware.android.ble.sensors.SensorConfig;

/**
 * Defines the profile of an activity that is monitored by sensors.
 * @author Mauro Isaja
 *
 */
public class ActivityProfile {

	private final String _name;
	private final SensorConfig[] _configs;
	
	public ActivityProfile(String name, SensorConfig... configs) {
		if (null == name || configs.length == 0)
			throw new NullPointerException();
		_name = name;
		_configs = configs;
	}

	/**
	 * The human-readable name of this activity profile.
	 * @return
	 */
	public String getName() {
		return _name;
	}

	/**
	 * The configurations of the sensors that are used to monitor activities with this profile.
	 * @return
	 */
	public SensorConfig[] getConfigs() {
		return _configs;
	}
}
