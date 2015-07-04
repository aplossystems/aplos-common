package com.aplos.common.beans.communication;

import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.beans.AplosBean;

@Entity
public class DeletableEmailAddress extends AplosBean {
	private static final long serialVersionUID = -767216109002095968L;

	private String emailAddress;
	private int daysToDeletion;
	
	public String getEmailAddress() {
		return emailAddress;
	}
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}
	public int getDaysToDeletion() {
		return daysToDeletion;
	}
	public void setDaysToDeletion(int daysToDeletion) {
		this.daysToDeletion = daysToDeletion;
	}
}
