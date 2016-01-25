package com.nimbusware.mypersonalbiketrainer;

public interface SpeedSensorListener extends SensorListener {

	public void updateSpeed(double kmh);

	public void updateDistance(double meters);

	public void updateWheelRevsCount(int revs);
}
