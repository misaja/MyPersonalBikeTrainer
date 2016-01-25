package com.nimbusware.mypersonalbiketrainer;

public interface BeatRateSensorListener extends SensorListener {

	public void updateBeatRate(double bpm);
}
