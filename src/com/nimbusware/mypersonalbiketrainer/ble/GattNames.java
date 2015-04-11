/*
 * Copyright (C) 2013 The Android Open Source Project
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

import java.util.UUID;

import android.bluetooth.BluetoothGatt;
import android.util.SparseArray;

public class GattNames {
	
    public static final UUID HRM_SERVICE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    public static final UUID HRM_CHARACTERISTIC = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    public static final UUID CSC_SERVICE = UUID.fromString("00001816-0000-1000-8000-00805f9b34fb");
    public static final UUID CSC_CHARACTERISTIC = UUID.fromString("00002a5b-0000-1000-8000-00805f9b34fb");
    public static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    
    private static final SparseArray<String> GATT_ERROR_CODES = new SparseArray<String>(10);
    
    static {
    	GATT_ERROR_CODES.put(BluetoothGatt.GATT_SUCCESS, "SUCCESS");
    	GATT_ERROR_CODES.put(BluetoothGatt.GATT_READ_NOT_PERMITTED, "READ NOT PERMITTED");
    	GATT_ERROR_CODES.put(BluetoothGatt.GATT_WRITE_NOT_PERMITTED, "WRITE NOT PERMITTED");
    	GATT_ERROR_CODES.put(BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION, "INSUFFICIENT AUTHENTICATION");
    	GATT_ERROR_CODES.put(BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, "REQUEST NOT SUPPORTED");
    	GATT_ERROR_CODES.put(BluetoothGatt.GATT_INVALID_OFFSET, "INVALID OFFSET");
    	GATT_ERROR_CODES.put(BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH, "INVALID ATTRIBUTE LENGTH");
    	GATT_ERROR_CODES.put(BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION, "INSUFFICIENT ENCRYPTION");
    	GATT_ERROR_CODES.put(BluetoothGatt.GATT_FAILURE, "GENERIC FAILURE");
    	// from API level 21:
    	//GATT_ERROR_CODES.put(BluetoothGatt.GATT_CONNECTION_CONGESTED, "GATT_CONNECTION_CONGESTED");
    }
    
    public static String lookupErrorDescriiption(int code) {
    	String descr = GATT_ERROR_CODES.get(code);
    	if (null == descr)
    		descr = "UNKNOWN";
    	return descr;
    }
}
