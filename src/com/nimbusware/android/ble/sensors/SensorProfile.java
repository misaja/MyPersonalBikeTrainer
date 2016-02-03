package com.nimbusware.android.ble.sensors;

import java.util.Map;
import java.util.UUID;

public interface SensorProfile<T> {

	public UUID getServiceId();
	
	public UUID getCharacteristicId();
	
	public Class<T> getDataType();

	public DataParser<T> getParser(Map<String, String> config);
}
