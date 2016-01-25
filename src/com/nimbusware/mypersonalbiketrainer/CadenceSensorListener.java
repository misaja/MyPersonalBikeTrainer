package com.nimbusware.mypersonalbiketrainer;

public interface CadenceSensorListener extends SensorListener {

	public void updateCadence(double rpm);

	public void updateCrankRevsCount(int revs);
}
