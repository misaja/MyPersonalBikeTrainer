package com.nimbusware.android.ble.sensors;

public interface GenericSensor {
	
	public String getAddress();

	public SensorState getState();

	public void registerStateListener(StateListener listener);
	
	public void unregisterStateListener(StateListener listener);

	public boolean open();
	
	public boolean reopen();
	
	public void close();
}
