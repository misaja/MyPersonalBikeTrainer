package com.nimbusware.mypersonalbiketrainer;

import java.util.Date;

public interface WorkSession {

	public Date getStartTime();

	public Date getEndTime();

	public double getElapsedTimeSeconds();

	public double getDistanceCoveredKms();

	public double getMaxSpeed();

	public double getAverageSpeed();

	public double getMaxCrankCadence();

	public double getAverageCrankCadence();

	public double getAverageGearRatio();

	public double getMaxHeartCadence();

	public double getAverageHeartCadence();

	public double getCardioFitnessFactor();

}