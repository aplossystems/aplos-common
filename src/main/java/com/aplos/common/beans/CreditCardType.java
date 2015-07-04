package com.aplos.common.beans;

import com.aplos.common.annotations.persistence.Entity;

@Entity
public class CreditCardType extends AplosBean {
	private static final long serialVersionUID = -3277851989571857627L;

	private String name;
	private String sagePayTag;
	private String payPalTag;

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

	public void setSagePayTag(String sagePayTag) {
		this.sagePayTag = sagePayTag;
	}

	public String getSagePayTag() {
		return sagePayTag;
	}

	public void setPayPalTag(String payPalTag) {
		this.payPalTag = payPalTag;
	}

	public String getPayPalTag() {
		return payPalTag;
	}
}
