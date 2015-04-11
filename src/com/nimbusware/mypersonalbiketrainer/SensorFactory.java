package com.nimbusware.mypersonalbiketrainer;

import com.nimbusware.mypersonalbiketrainer.ble.CscSensor;
import com.nimbusware.mypersonalbiketrainer.ble.HrmSensor;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

// TODO use dependency injection to abstract sensor technology
public class SensorFactory {

	public static SensorSet getSensorSet(Context context, BluetoothAdapter adapter, SensorInfo sensorInfo, int wheelSize) {
		HrmSensor heartSensor = null;
		CscSensor wheelSensor = null;
		CscSensor crankSensor = null;
		String heartSensorAddr = sensorInfo.getHeartSensorAddress();
		if (null != heartSensorAddr) {
			heartSensor = new HrmSensor(context, adapter, heartSensorAddr);
		}
		
		String wheelSensorAddr = sensorInfo.getWheelSensorAddress();
		if (null != wheelSensorAddr) {
			wheelSensor = new CscSensor(context, adapter, wheelSensorAddr, wheelSize);
		}
		
		String crankSensorAddr = sensorInfo.getCrankSensorAddress();
		if (null != crankSensorAddr && !crankSensorAddr.equals(wheelSensorAddr)) {
			CscSensor sensor = new CscSensor(context, adapter, crankSensorAddr, wheelSize);
			if (null == wheelSensor) {
				// one single CSC sensor for both wheel and crank,
				// saved in configuration as the "crank sensor"
				wheelSensor = crankSensor = sensor;
			} else {
				// two distinct CSC sensors for wheel and crank
				crankSensor = sensor;
			}
		} else {
			// one single CSC sensor for both wheel and crank,
			// saved in configuration as the "wheel sensor" OR as a duplicate
			// address for both wheel and crank
			crankSensor = wheelSensor;
		}
		
		return new SensorSet(heartSensor, wheelSensor, crankSensor);
	}
}
