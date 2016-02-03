package com.nimbusware.android.ble.profiles;

import java.util.Map;
import java.util.UUID;

import com.nimbusware.android.ble.sensors.DataParser;
import com.nimbusware.android.ble.sensors.SensorProfile;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

public class HrmProfile implements SensorProfile<HrmData> {
	
	private static final HrmProfile INSTANCE = new HrmProfile();
	private static final String TAG = HrmProfile.class.getSimpleName();
	
	// constants from BT documentation
	// see https://developer.bluetooth.org/TechnologyOverview/Pages/Profiles.aspx
    public static final UUID SERVICE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    public static final UUID CHARACTERISTIC = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    
    public static HrmProfile getInstance() {
    	return INSTANCE;
    }
    
    private HrmProfile() {} // singleton: use getInstance() instead
	
	@Override
	public UUID getServiceId() {
		return SERVICE;
	}
	
	@Override
	public UUID getCharacteristicId() {
		return CHARACTERISTIC;
	}

	@Override
	public Class<HrmData> getDataType() {
		return HrmData.class;
	}

	@Override
	public DataParser<HrmData> getParser(Map<String, String> config) {
		return new Parser();
	}
	
	private static class Data implements HrmData {

		private final int _rate;

		private Data(int rate) {
			_rate = rate;
		}

		@Override
		public int getHeartRate() {
			return _rate;
		}
	}
	
	private static class Parser implements DataParser<HrmData> {

		@Override
		public HrmData parse(BluetoothGattCharacteristic btData) {
			int flag = btData.getProperties();
			int format = -1;
			if ((flag & 0x01) != 0) {
			    format = BluetoothGattCharacteristic.FORMAT_UINT16;
			} else {
			    format = BluetoothGattCharacteristic.FORMAT_UINT8;
			}
			
			int heartRate = btData.getIntValue(format, 1);

		    Log.v(TAG, "HRM notification data: HEART_RATE=" + heartRate);
		    
		    return new Data(heartRate);
		}
		
	}
}
