package com.nimbusware.android.ble.sensors;

public interface DataListener<T> {
	
	public SensorProfile<T> getProfile();
	
	public void notifyNewData(T data);
}
