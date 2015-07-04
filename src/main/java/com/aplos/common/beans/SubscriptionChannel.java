package com.aplos.common.beans;


import javax.faces.bean.SessionScoped;

import com.aplos.common.annotations.PluralDisplayName;
import com.aplos.common.annotations.persistence.Cache;
import com.aplos.common.annotations.persistence.Entity;

@SessionScoped
@Entity
@Cache
@PluralDisplayName(name="subscription channels")
public class SubscriptionChannel extends AplosBean {

	private static final long serialVersionUID = -8039330606961554706L;

	private String name;

	public SubscriptionChannel() {}

	@Override
	public String getDisplayName() {
		return getName();
	}

	public void setName(String name) {
		this.name = name;

	}

	public String getName() {
		return name;
	}


}
