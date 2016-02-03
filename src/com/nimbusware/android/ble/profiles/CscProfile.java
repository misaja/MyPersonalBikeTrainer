package com.nimbusware.android.ble.profiles;

import java.util.Map;
import java.util.UUID;

import com.nimbusware.android.ble.sensors.DataParser;
import com.nimbusware.android.ble.sensors.SensorProfile;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

public class CscProfile implements SensorProfile<CscData> {
	
	public static final String WHEEL_SIZE = "WHEEL_SIZE";
	
	private static final CscProfile INSTANCE = new CscProfile();
	private static final String TAG = CscProfile.class.getSimpleName();
	
	// constants from BT documentation
	// see https://developer.bluetooth.org/TechnologyOverview/Pages/Profiles.aspx
	private static final UUID SERVICE = UUID.fromString("00001816-0000-1000-8000-00805f9b34fb");
    private static final UUID CHARACTERISTIC = UUID.fromString("00002a5b-0000-1000-8000-00805f9b34fb");
    
    public static CscProfile getInstance() {
    	return INSTANCE;
    }
    
    private CscProfile() {} // singleton: use getInstance() instead
	
	@Override
	public UUID getServiceId() {
		return SERVICE;
	}
	
	@Override
	public UUID getCharacteristicId() {
		return CHARACTERISTIC;
	}

	@Override
	public Class<CscData> getDataType() {
		return CscData.class;
	}

	@Override
	public DataParser<CscData> getParser(Map<String, String> config) {
		// we have a runtime exception if the wheel size is not properly configured
		return new Parser(Integer.parseInt(config.get(WHEEL_SIZE)));
	}
	
	private static class Data implements CscData {

		private final double _speed;
		private final double _distance;
		private final int _wheelRevs;
		private final double _cadence;
		private final int _crankRevs;
		
		private Data(double speed, double distance, int wheelRevs, double cadence, int crankRevs) {
			_speed = speed;
			_distance = distance;
			_wheelRevs = wheelRevs;
			_cadence = cadence;
			_crankRevs = crankRevs;
		}

		@Override
		public double getSpeed() {
			return _speed;
		}

		@Override
		public double getDistance() {
			return _distance;
		}

		@Override
		public int getWheelRevsSinceLastRead() {
			return _wheelRevs;
		}

		@Override
		public double getCadence() {
			return _cadence;
		}

		@Override
		public int getCrankRevsSinceLastRead() {
			return _crankRevs;
		}
	}
	
	private static class Parser implements DataParser<CscData> {

	    private final static int WHEEL_DATA_MASK = 0x01;
	    private final static int CRANK_DATA_MASK = 0x02;
	    
	    private final int _wheelSize;
    
	    private CscReading _lastReading;
		private double _lastSpeed;
		private double _lastCadence;
		
		private Parser(int wheelSize) {
			if (wheelSize <= 0)
				throw new IllegalArgumentException();
			
			_wheelSize = wheelSize;
		}
		
		@Override
		public CscData parse(BluetoothGattCharacteristic btData) {
			final byte[] data = btData.getValue();
			if (data != null && data.length > 0) {
				int flag = data[0];
			    boolean hasWheel = false;
			    boolean hasCrank = false;
			    if ((flag & WHEEL_DATA_MASK) == WHEEL_DATA_MASK) {
			    	hasWheel = true;
			    }
			    if ((flag & CRANK_DATA_MASK) == CRANK_DATA_MASK) {
			    	hasCrank = true;
			    }
			    
			    int wheelRevs = 0;
			    int wheelElapsed = 0;
			    int crankRevs = 0;
			    int crankElapsed = 0;
			    if (hasWheel && hasCrank) {
			    	wheelRevs = btData.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 1);
			    	wheelElapsed = btData.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 5);
			    	crankRevs = btData.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 7);
			    	crankElapsed = btData.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 9);
			    } else if (hasWheel) {
			    	wheelRevs = btData.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 1);
			    	wheelElapsed = btData.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 5);
			    } else if (hasCrank) {
			    	crankRevs = btData.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1);
			    	crankElapsed = btData.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 3);
			    }
			    
		        StringBuffer buf = new StringBuffer();
		        buf.append("WHEEL_REV_COUNT=").append(wheelRevs);
		        buf.append(", WHEEL_TIME_COUNT=").append(wheelElapsed);
		        buf.append(", CRANK_REV_COUNT=").append(crankRevs);
		        buf.append(", CRANK_TIME_COUNT=").append(crankElapsed);
			    Log.v(TAG, "CSC raw data: " + buf.toString());

			    CscReading newReading = new CscReading(wheelRevs, wheelElapsed, crankRevs, crankElapsed, _lastReading);
			    
		    	double speed = newReading.getSpeed(_wheelSize);
		    	double cadence = newReading.getCrankRPM();
		    	
		    	buf = new StringBuffer();
		        buf.append("SPEED=").append(speed);
		        buf.append(", CADENCE=").append(cadence);
			    Log.v(TAG, "CSC notification data: " + buf.toString());
			    
			    double distance = _lastReading != null ? _lastReading.getDistance(_wheelSize) : 0;
			    int wheelRevsSlr = _lastReading != null ? _lastReading.getWheelRevsSinceLastRead() : 0;
			    if (speed > 0 || _lastSpeed == 0) { // filter out spurious negative values (of unknown origin)
		    		// current speed is either a positive value or zero: in the latter case,
		    		// we skip notification unless the previous value was also zero, to the
		    		// effect that spurious zero values (i.e., single zero readings in the
		    		// midst of a regular session) are filtered out; not that a zero speed
		    		// reading also means that the wheel rev count is not changed since last
		    		// reading, so total distance and total rev count are unaffected
			    	distance = newReading.getDistance(_wheelSize);
			    	wheelRevsSlr = newReading.getWheelRevsSinceLastRead();
			    }
			    
			    int crankRevsSlr = _lastReading.getCrankRevsSinceLastRead();
			    if (cadence > 0 || _lastCadence == 0) { // filter out spurious negative values (of unknown origin)
		    		// current cadence is either a positive value or zero: in the latter case,
		    		// we skip notification unless the previous value was also zero, to the
		    		// effect that spurious zero values (i.e., single zero readings in the
		    		// midst of a regular session) are filtered out; not that a zero cadence
		    		// reading also means that the crank rev count is not changed since last
		    		// reading, so total total crank count is unaffected
			    	crankRevsSlr = newReading.getCrankRevsSinceLastRead();
			    }
			    
			    _lastReading = newReading;
			    _lastSpeed = speed;
			    _lastCadence = cadence;
			    
			    return new Data(speed, distance, wheelRevsSlr, cadence, crankRevsSlr);
			} else {
			    Log.w(TAG, "CSC notification contains no data.");
			    return null;
			}
		}
		
	}
	
	private static class CscReading {
		
		private static final double TIME_RESOLUTION = 1024d;
		private static final double SECS_IN_MINUTE = 60d;
		private static final double SPEED_CONSTANT = 60d / 1000000d;

	    private final int _wheelCR;
	    private final int _wheelCT;
	    private final int _crankCR;
	    private final int _crankCT;
		private final CscReading _previous;
	    
		private CscReading(int wheelRevs, int wheelTime, int crankRevs, int crankTime, CscReading previousReading) {
			_wheelCR = wheelRevs;
			_wheelCT = wheelTime;
			_crankCR = crankRevs;
			_crankCT = crankTime;
			_previous = previousReading;
		}
		
		/**
		 * Returns the RPMs of the wheel measured starting from last reading.
		 * If no previous reading is available, returns zero.
		 * 
		 * @return
		 */
		public double getWheelRPM() {
			double rpm = 0;
			int revCount = getWheelRevsSinceLastRead();
			Log.v(TAG, "Wheel: rev count is " + revCount);
			double secCount = getWheelSecondsSinceLastRead();
			Log.v(TAG, "Wheel: sec count is " + secCount);
			if (secCount > 0) {
				rpm = revCount / secCount * 60;
				Log.v(TAG, "Wheel: rpm is " + rpm);
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
			int revCount = getCrankRevsSinceLastRead();
			Log.v(TAG, "Crank: rev count is " + revCount);
			double secCount = getCrankSecondsSinceLastRead();
			Log.v(TAG, "Crank: sec count is " + secCount);
			if (secCount > 0) {
				rpm = revCount / secCount * SECS_IN_MINUTE;
				Log.v(TAG, "Crank: rpm is " + rpm);
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
				Log.v(TAG, "Wheel: speed is " + speed);
			} else {
				Log.v(TAG, "Wheel: speed is 0");
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
			return getWheelRevsSinceLastRead() * wheelSize / 1000d;
		}

		public int getWheelCumulativeRevs() {
			return _wheelCR;
		}

		public int getWheelCumulativeTime() {
			return _wheelCT;
		}

		public int getCrankCumulativeRevs() {
			return _crankCR;
		}

		public int getCrankCumulativeTime() {
			return _crankCT;
		}
		
		public int getWheelRevsSinceLastRead() {
			int revs = 0;
			if (null != _previous) {
				int prevReading = _previous.getWheelCumulativeRevs();
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
		
		public int getCrankRevsSinceLastRead() {
			int revs = 0;
			if (null != _previous) {
				int prevReading = _previous.getCrankCumulativeRevs();
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
		
		private double getWheelSecondsSinceLastRead() {
			if (null != _previous) {
				int prevTime = _previous.getWheelCumulativeTime();
				int currTime = getWheelCumulativeTime();
				return getElapsedTimeSinceLastRead(prevTime, currTime) / TIME_RESOLUTION;
			} else {
				return 0d;
			}
		}
		
		private double getCrankSecondsSinceLastRead() {
			if (null != _previous) {
				int prevTime = _previous.getCrankCumulativeTime();
				int currTime = getCrankCumulativeTime();
				return getElapsedTimeSinceLastRead(prevTime, currTime) / TIME_RESOLUTION;
			} else {
				return 0d;
			}
		}
		
		private int getElapsedTimeSinceLastRead(int prevTime, int currTime) {
			if (currTime > prevTime) {
				return (currTime - prevTime);
			} else {
				// Short overflow occurred
				return (Short.MAX_VALUE - prevTime + currTime);
			}
		}
	}
}
