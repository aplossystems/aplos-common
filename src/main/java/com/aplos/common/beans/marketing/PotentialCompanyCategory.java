package com.aplos.common.beans.marketing;

import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.beans.AplosBean;

@Entity
public class PotentialCompanyCategory extends AplosBean {	
	private static final long serialVersionUID = 1966960582784742706L;

	private String name;
	
	@Override
	public String getDisplayName() {
		return getName();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
