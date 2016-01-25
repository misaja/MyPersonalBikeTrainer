package com.nimbusware.mypersonalbiketrainer;

public interface Sensor {
	
	public boolean isOpen();
	
	public boolean isBusy();
	
	public boolean open();
	
	public boolean refresh();
	
	public void close();
}
