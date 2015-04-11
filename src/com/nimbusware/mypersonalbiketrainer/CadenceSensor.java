package com.nimbusware.mypersonalbiketrainer;

public interface CadenceSensor extends Sensor {

	public void registerListener(CadenceSensorListener listener);

	public void unregisterListener(CadenceSensorListener listener);
}
