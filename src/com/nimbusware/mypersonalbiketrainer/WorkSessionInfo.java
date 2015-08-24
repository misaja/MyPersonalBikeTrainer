package com.nimbusware.mypersonalbiketrainer;

/**
 * Represents the full information set of a Work Session - i.e., basic
 * data from WorkSessionRawData plus computed data.
 * @author misaja
 *
 */
public interface WorkSessionInfo extends WorkSessionRawData {

	/**
	 * Time elapsed from the start, expressed in seconds.
	 * @return
	 */
	public double getElapsedTime();

	/**
	 * Average speed, expressed in Km/h.
	 * @return
	 */
	public double getAverageSpeed();

	/**
	 * Average crank cadence, expressed in Rpm.
	 * @return
	 */
	public double getAverageCrankCadence();

	/**
	 * Average gear ratio, computed from wheel revolutions and crank revolutions
	 * (the higher, the harder).
	 * @return
	 */
	public double getAverageGearRatio();

	/**
	 * Average heart beats, expressed in Bpm.
	 * @return
	 */
	public double getAverageHeartCadence();

	/**
	 * Overall fitness factor, computed from heart beats and distance covered
	 * (the higher, the better).
	 * @return
	 */
	public double getCardioFitnessFactor();

}