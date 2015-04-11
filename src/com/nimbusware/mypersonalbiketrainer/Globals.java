package com.nimbusware.mypersonalbiketrainer;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Globals {

	public static final String SENSOR_ADDR = "SENSOR_ADDR";
	public static final String HEART_SENSOR_ADDR = "HEART_SENSOR_ADDR";
	public static final String WHEEL_SENSOR_ADDR = "WHEEL_SENSOR_ADDR";
	public static final String CRANK_SENSOR_ADDR = "CRANK_SENSOR_ADDR";
	public static final String WHEEL_SIZE = "WHEEL_SIZE";
	public static final String SESSION_ID = "SESSION_ID";
	public static final int WHEEL_SIZE_DEFAULT = 1800;
	public static final int WHEEL_SIZE_MIN = 1000;
	public static final int WHEEL_SIZE_MAX = 3000;
	public static final String REQ_CODE = "REQ_CODE";
	public static final int REQ_ENABLE_BT = 1;
	public static final int REQ_DISCOVER_CARDIO_SENSOR = 2;
	public static final int REQ_DISCOVER_SPEED_SENSOR = 3;
	public static final int REQ_DISCOVER_CADENCE_SENSOR = 4;
	
	private static final int BLE_WAIT_TIME = 100;
	private static final BigDecimal ZEROTWOFIVE = new BigDecimal("0.25");
	private static final BigDecimal ZEROSEVENFIVE = new BigDecimal("0.75");
	private static final BigDecimal ZEROFIVE = new BigDecimal("0.50");
	
	/**
	 * Returns the argument rounded to one decimal position
	 * @param value double to be rounded
	 * @return rounded number
	 */
	public static double roundOneDecimalPosition(double value) {
	    BigDecimal roundedValue = new BigDecimal(value);
	    roundedValue.setScale(1, RoundingMode.HALF_UP);
	    return roundedValue.doubleValue();
	}
	
	/**
	 * Returns the argument rounded to the nearest half integer. E.g., 1.2 becomes 1.0,
	 * 1.3 becomes 1.5, 1.7 becomes 1.5, 1.8 becomes 2.
	 * @param value double to be rounded
	 * @return rounded number
	 */
	public static double roundZeroFive(double value) {
		
		// round to one decimal position
	    BigDecimal origVal = new BigDecimal(value);
	    
	    // get integer part (can be zero)
	    BigDecimal intPart = new BigDecimal(origVal.intValue());
	    
	    // get decimal part (can be zero)
	    BigDecimal decPart = null;
	    if (intPart.equals(BigDecimal.ZERO))  {
	    	// integer part is zero: decimal part is the original value
	    	// e.g., original: 0,3 integer: 0 decimal 0,3
	    	decPart = origVal;
	    } else {
	    	// integer part is non zero: decimal part is original % integer part
	    	// e.g., original: 1,3 integer: 1 decimal 0,3
	    	decPart = origVal.remainder(intPart);
	    }
	    
	    // only if decimal part > 0 we need to apply rounding
	    if (!decPart.equals(BigDecimal.ZERO)) {
	    	if (decPart.compareTo(ZEROTWOFIVE) <= 0) {
	    		// decimal part <= 0,25: truncate to integer value
	    		origVal = intPart;
	    	} else if (decPart.compareTo(ZEROSEVENFIVE) < 0) {
	    		// decimal part >= 0,26 and < 0,75: integer value + 0.5
	    		origVal = intPart.add(ZEROFIVE);
	    	} else {
	    		// decimal part >= 0,76: integer value + 1
	    		origVal = intPart.add(BigDecimal.ONE);
	    	}
	    }
	    
	    return origVal.doubleValue();
	}
	
	public static void waitBleServer() {
        try {
			Thread.sleep(BLE_WAIT_TIME);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private Globals() {}
}
