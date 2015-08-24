package com.nimbusware.mypersonalbiketrainer;

public interface SpeedSensorListener {

	public void updateSpeed(double kmh);

	public void updateDistance(double meters);

	public void updateWheelRevsCount(int revs);
}
