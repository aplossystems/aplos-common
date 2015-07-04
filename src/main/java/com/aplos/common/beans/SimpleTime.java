package com.aplos.common.beans;

import java.text.DecimalFormat;

public class SimpleTime {
	private int hour;
	private int minute;
	
	public SimpleTime() {
		// TODO Auto-generated constructor stub
	}
	
	public SimpleTime( int hour, int minute ) {
		setHour( hour );
		setMinute(minute);
	}
	
	public int compareTo( SimpleTime simpleTime ) {
		return (getHour() - simpleTime.getHour()) * 60 + (getMinute() - simpleTime.getMinute());
	}
	
	public int getHour() {
		return hour;
	}
	public void setHour(int hour) {
		this.hour = hour;
	}
	public int getMinute() {
		return minute;
	}
	public void setMinute(int minute) {
		this.minute = minute;
	}
	
	@Override
	public String toString() {
		return getHour() + ":" + new DecimalFormat( "00" ).format( getMinute() );
	}
}
