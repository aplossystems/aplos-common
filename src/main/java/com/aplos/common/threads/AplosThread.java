package com.aplos.common.threads;

public abstract class AplosThread extends Thread {
	private boolean isRunning;
	
	public abstract int getQueueSize();
	
	public void startThread() {
		setRunning( true );
		start();
	}

	public boolean isRunning() {
		return isRunning;
	}

	public void setRunning(boolean isRunning) {
		this.isRunning = isRunning;
	}
}
