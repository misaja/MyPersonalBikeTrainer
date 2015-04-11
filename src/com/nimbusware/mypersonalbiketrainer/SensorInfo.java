package com.nimbusware.mypersonalbiketrainer;

public class SensorInfo {

	private final String mHeartSensorAddr;
	private final String mWheelSensorAddr;
	private final String mCrankSensorAddr;
	
	public SensorInfo(String heartSensorAddr, String wheelSensorAddr, String crankSensorAddr) {
		mHeartSensorAddr = heartSensorAddr;
		mWheelSensorAddr = wheelSensorAddr;
		mCrankSensorAddr = crankSensorAddr;
	}

	public String getHeartSensorAddress() {
		return mHeartSensorAddr;
	}

	public String getWheelSensorAddress() {
		return mWheelSensorAddr;
	}

	public String getCrankSensorAddress() {
		return mCrankSensorAddr;
	}
}
