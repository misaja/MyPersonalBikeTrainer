package com.nimbusware.mypersonalbiketrainer;

public interface SpeedSensor extends Sensor {

	public void registerListener(SpeedSensorListener listener);

	public void unregisterListener(SpeedSensorListener listener);
}
