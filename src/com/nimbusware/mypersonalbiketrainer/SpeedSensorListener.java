package com.nimbusware.mypersonalbiketrainer;

public interface SpeedSensorListener extends RevolutionSensorListener {

	public void updateSpeed(double kmh);

	public void updateDistance(double meters);
}
