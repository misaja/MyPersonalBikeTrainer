package com.nimbusware.mypersonalbiketrainer;

public interface CadenceSensorListener {

	public void updateCadence(double rpm);

	public void updateCrankRevsCount(int revs);
}
