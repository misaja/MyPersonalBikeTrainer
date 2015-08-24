package com.nimbusware.mypersonalbiketrainer;

import java.util.Date;

/**
 * Represents the information saved as a log entry of a Work Session.
 * @author misaja
 *
 */
public interface WorkSessionLogEntry {
	
	/**
	 *  Work Session local prog. number.
	 * @return
	 */
	public long getWorkoutLocalId();

	/**
	 * Timestamp of the log entry.
	 * @return
	 */
	public Date getTime();

	/**
	 * Distance covered so far, expressed in Kilometers.
	 * @return
	 */
	public double getPartialDistance();

	/**
	 * Current speed, expressed in Km/h.
	 * @return
	 */
	public double getSpeed();

	/**
	 * Current crank cadence, expressed in Rpm.
	 * @return
	 */
	public double getCrankCadence();

	/**
	 * Current heart beat, expressed in Bpm.
	 * @return
	 */
	public double getHeartCadence();
	
}
