package com.aplos.common.beans.cardsave;

import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.beans.AplosBean;

@Entity
public class CardSaveGateway extends AplosBean {

	private static final long serialVersionUID = 4327596552327388868L;
	
	private String subdomain;
	private int priority = 100; //1 is highest,
	
	public String getSubdomain() {
		return subdomain;
	}
	
	public void setSubdomain(String subdomain) {
		this.subdomain = subdomain;
	}
	
	public int getPriority() {
		return priority;
	}
	
	public void setPriority(int priority) {
		this.priority = priority;
	}

}
