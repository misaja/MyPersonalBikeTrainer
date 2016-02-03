package com.nimbusware.android.ble.sensors;

public interface StateListener {

	public void notifyStateChange(GenericSensor source, SensorState state);
}
