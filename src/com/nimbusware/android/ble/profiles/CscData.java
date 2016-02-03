package com.nimbusware.android.ble.profiles;

/**
 * Simple DTO for Cycling Speed and Cadence.
 * 
 * @author Mauro Isaja
 *
 */
public interface CscData {

	public double getSpeed();

	public double getDistance();

	public int getWheelRevsSinceLastRead();

	public double getCadence();

	public int getCrankRevsSinceLastRead();}
