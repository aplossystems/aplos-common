package com.aplos.common.enums;

import java.util.Calendar;

import com.aplos.common.LabeledEnumInter;

public enum DayOfWeek implements LabeledEnumInter {
	MONDAY ("Monday", Calendar.MONDAY),
	TUESDAY ("Tuesday", Calendar.TUESDAY),
	WEDNESDAY ("Wednesday", Calendar.WEDNESDAY),
	THURSDAY ("Thursday", Calendar.THURSDAY),
	FRIDAY ("Friday", Calendar.FRIDAY),
	SATURDAY ("Saturday", Calendar.SATURDAY),
	SUNDAY ("Sunday", Calendar.SUNDAY);
	
	private String label;
	private int calendarIdx;
	
	private DayOfWeek( String label, int calendarIdx ) {
		this.label = label;
		this.calendarIdx = calendarIdx;
	}
	
	public String getLabel() {
		return label;
	}
	
	public int getCalendarIdx() {
		return calendarIdx;
	}
}
