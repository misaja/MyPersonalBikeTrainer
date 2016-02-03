package com.nimbusware.android.ble.sensors;

public class ConnectionCoordinator implements StateListener {
	
	private final GenericSensor[] _sensors;
	private int _current;
	private State _state;
	
	public ConnectionCoordinator(GenericSensor... sensors) {
		if (sensors.length == 0)
			throw new IllegalArgumentException();
		
		_sensors = sensors;
	}
	
	public synchronized boolean isOperationInProgress() {
		return _state != State.Idle;
	}

	public synchronized boolean open() {
		if (_state == State.Idle) {
			_state = State.Opening;
			_current = 0;
			
			for (GenericSensor sensor : _sensors) {
				sensor.registerStateListener(this);
			}
			
			openNext();
			
			return true;
		} else {
			return false;
		}
	}
	
	/*
	 * TODO find some way of doing this!
	public synchronized boolean reopen() {
		if (_state == State.Idle) {
			_state = State.Reopening;
			_current = 0;
			
			for (Sensor<?> sensor : _sensors) {
				sensor.registerStateListener(this);
			}
			
			reopenNext();
			
			return true;
		} else {
			return false;
		}
	}
	*/

	public synchronized void close() {
		_state = State.Closing;
		
		for (GenericSensor sensor : _sensors) {
			sensor.close();
		}

		_state = State.Idle;
	}

	@Override
	public void notifyStateChange(GenericSensor source, SensorState state) {
		if (_state == State.Opening) {
			// we are opening the connection, attempt might succeed of fail:
			// whatever the outcome, we skip to the next sensor, if any
			if (state == SensorState.Open || state == SensorState.Closed) {
				// we don't want to be notified of anything any more
				source.unregisterStateListener(this);
				openNext();
			}
		} else if (_state == State.Reopening) {
			// TODO find some way of doing this!
		}

	}
	
	private synchronized void openNext() {
		if (_current < _sensors.length) {
			_sensors[_current++].open();
		} else {
			_state = State.Idle;
			_current = 0;
		}
	}
	
	/*
	private synchronized void reopenNext() {
		if (_current < _sensors.length) {
			_sensors[_current++].reopen();
		} else {
			_state = State.Idle;
			_current = 0;
		}
	}
	*/
	
	private enum State {
		Idle,
		Opening,
		Reopening,
		Closing
	}
}
