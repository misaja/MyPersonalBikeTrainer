package com.nimbusware.mypersonalbiketrainer;

import java.util.Date;

/**
 * Represents the basic information of a Work Session.
 * @author misaja
 *
 */
public interface WorkSessionRawData {

	/**
	 * Work Session local prog. number (0 is still unsaved).
	 * @return
	 */
	public long getLocalId();

	/**
	 * Work Session universally unique ID, in String format.
	 * @return
	 */
	public String getUniqueId();

	/**
	 * Work Session start timestamp.
	 * @return
	 */
	public Date getStartTime();

	/**
	 * Work Session end timestamp (null if still active).
	 * @return
	 */
	public Date getEndTime();

	/**
	 * Distance covered expressed in Kilometers.
	 * @return
	 */
	public double getDistanceCovered();
	
	/**
	 * Total count of wheel revolutions.
	 * @return
	 */
	public int getWheelRevs();
	
	/**
	 * Total count of crank revolutions.
	 * @return
	 */
	public int getCrankRevs();

	/**
	 * Total count of heart beats.
	 * @return
	 */
	public double getHeartBeats();
	
	/**
	 * Top speed reached, expressed in Kmh.
	 * @return
	 */
	public double getMaxSpeed();

	/**
	 * Top cadence reached, expressed in Rpm.
	 * @return
	 */
	public double getMaxCrankCadence();

	/**
	 * Top heart beat reached, expressed in Bpm.
	 * @return
	 */
	public double getMaxHeartCadence();
}
