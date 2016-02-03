package com.nimbusware.android.ble.sensors;

public enum SensorState {

	// it is MANDATORY that this order reflects the progression of initialization
	Closed,
	WaitingForConnection,
	DiscoveringServices,
	SettingUpService,
	SettingUpCharacteristic,
	SettingUpNotifications,
	Open
}
