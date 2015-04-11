/*
 * Copyright (C) 2014 Mauro Isaja mauro.isaja@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nimbusware.mypersonalbiketrainer.ble;

import android.util.Log;

public class CscReading {
	
	private static final String TAG = CscReading.class.getSimpleName();
	private static final double TIME_RESOLUTION = 1024d;
	private static final double SECS_IN_MINUTE = 60d;
	private static final double SPEED_CONSTANT = 60d / 1000000d;

    private final int mWheelCR;
    private final int mWheelCT;
    private final int mCrankCR;
    private final int mCrankCT;
	private final CscReading mPreviousReading;
    
	public CscReading(int wheelRevs, int wheelTime, int crankRevs, int crankTime, CscReading previousReading) {
		mWheelCR = wheelRevs;
		mWheelCT = wheelTime;
		mCrankCR = crankRevs;
		mCrankCT = crankTime;
		mPreviousReading = previousReading;
	}
	
	/**
	 * Returns the RPMs of the wheel measured starting from last reading.
	 * If no previous reading is available, returns zero.
	 * 
	 * @return
	 */
	public double getWheelRPM() {
		double rpm = 0;
		int revCount = getWheelRevsSinceLastReading();
		Log.d(TAG, "Wheel: rev count is " + revCount);
		double secCount = getWheelSecondsSinceLastReading();
		Log.d(TAG, "Wheel: sec count is " + secCount);
		if (secCount > 0) {
			rpm = revCount / secCount * 60;
			Log.d(TAG, "Wheel: rpm is " + rpm);
		}
		return rpm;
	}
	
	/**
	 * Returns the RPMs of the crank measured starting from last reading.
	 * If no previous reading is available, returns zero.
	 * 
	 * @return
	 */
	public double getCrankRPM() {
		double rpm = 0;
		int revCount = getCrankRevsSinceLastReading();
		Log.d(TAG, "Crank: rev count is " + revCount);
		double secCount = getCrankSecondsSinceLastReading();
		Log.d(TAG, "Crank: sec count is " + secCount);
		if (secCount > 0) {
			rpm = revCount / secCount * SECS_IN_MINUTE;
			Log.d(TAG, "Crank: rpm is " + rpm);
		}
		return rpm;
	}
	
	/**
	 * Given the wheel diameter in millimeters, returns the virtual speed measured
	 * starting from last reading, in kilometers per hour.
	 * If no previous reading is available, returns zero.
	 * 
	 * @param wheelSize
	 * @return
	 */
	public double getSpeed(int wheelSize) {
		double speed = 0;
		double rpms = getWheelRPM();
		if (rpms > 0) {
			speed = rpms * wheelSize * SPEED_CONSTANT;
			Log.d(TAG, "Wheel: speed is " + speed);
		} else {
			Log.d(TAG, "Wheel: speed is 0");
		}
		return speed;
	}

	/**
	 * Given the wheel diameter in millimeters, returns the virtual distance
	 * covered since last reading, expressed in meters.
	 * If no previous reading is available, returns zero.
	 * 
	 * @param wheelSize
	 * @return
	 */
	public double getDistance(int wheelSize) {
		return getWheelRevsSinceLastReading() * wheelSize / 1000d;
	}

	public int getWheelCumulativeRevs() {
		return mWheelCR;
	}

	public int getWheelCumulativeTime() {
		return mWheelCT;
	}

	public int getCrankCumulativeRevs() {
		return mCrankCR;
	}

	public int getCrankCumulativeTime() {
		return mCrankCT;
	}
	
	public int getWheelRevsSinceLastReading() {
		int revs = 0;
		if (null != mPreviousReading) {
			int prevReading = mPreviousReading.getWheelCumulativeRevs();
			int currReading = getWheelCumulativeRevs();
			if (currReading < prevReading) {
				// Integer overflow (wow... lots of miles!)
				revs = (Integer.MAX_VALUE - prevReading + currReading);
			} else {
				revs = currReading - prevReading;
			}
		}
		return revs;
	}
	
	public int getCrankRevsSinceLastReading() {
		int revs = 0;
		if (null != mPreviousReading) {
			int prevReading = mPreviousReading.getCrankCumulativeRevs();
			int currReading = getCrankCumulativeRevs();
			if (currReading < prevReading) {
				// Short overflow
				revs = (Short.MAX_VALUE - prevReading + currReading);
			} else {
				revs = currReading - prevReading;
			}
		}
		return revs;
	}
	
	private double getWheelSecondsSinceLastReading() {
		if (null != mPreviousReading) {
			int prevTime = mPreviousReading.getWheelCumulativeTime();
			int currTime = getWheelCumulativeTime();
			return getElapsedTimeSinceLastReading(prevTime, currTime) / TIME_RESOLUTION;
		} else {
			return 0d;
		}
	}
	
	private double getCrankSecondsSinceLastReading() {
		if (null != mPreviousReading) {
			int prevTime = mPreviousReading.getCrankCumulativeTime();
			int currTime = getCrankCumulativeTime();
			return getElapsedTimeSinceLastReading(prevTime, currTime) / TIME_RESOLUTION;
		} else {
			return 0d;
		}
	}
	
	private int getElapsedTimeSinceLastReading(int prevTime, int currTime) {
		if (currTime > prevTime) {
			return (currTime - prevTime);
		} else {
			// Short overflow occurred
			return (Short.MAX_VALUE - prevTime + currTime);
		}
	}
}
