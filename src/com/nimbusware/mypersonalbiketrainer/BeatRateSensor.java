package com.nimbusware.mypersonalbiketrainer;

public interface BeatRateSensor extends Sensor {

	public void registerListener(BeatRateSensorListener listener);

	public void unregisterListener(BeatRateSensorListener listener);
}
