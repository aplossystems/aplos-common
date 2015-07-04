package com.aplos.common.beans;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import com.aplos.common.annotations.persistence.Entity;

@Entity
@ManagedBean
@SessionScoped
public class SubscriberReferrer extends AplosBean {
	private static final long serialVersionUID = -5657882529896677053L;
	private String name;

	public SubscriberReferrer() {}

	public SubscriberReferrer( String name ) {
		this.name = name;
	}

	@Override
	public String getDisplayName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
