package com.nimbusware.android.ble.sensors;

import android.bluetooth.BluetoothGattCharacteristic;

public interface DataParser<T> {

	public T parse(BluetoothGattCharacteristic data);
}
