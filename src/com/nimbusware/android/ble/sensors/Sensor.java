package com.nimbusware.android.ble.sensors;

public interface Sensor<T> extends GenericSensor {
	
	public SensorProfile<T> getProfile();

	public void registerDataListener(DataListener<T> listener);
	
	public void unregisterDataListener(DataListener<T> listener);
}
