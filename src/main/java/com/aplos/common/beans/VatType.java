package com.aplos.common.beans;

import java.math.BigDecimal;

import com.aplos.common.annotations.persistence.Entity;


@Entity
public class VatType extends AplosBean {
	private static final long serialVersionUID = -4081145336390284124L;

	private String code;
	private BigDecimal percentage;
	private boolean zeroRateAllowed;

	@Override
	public String getDisplayName() {
		return getCode();
	}

	public void setPercentage(BigDecimal percentage) {
		this.percentage = percentage;
	}
	public BigDecimal getPercentage() {
		return percentage;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}

	public void setZeroRateAllowed(boolean zeroRateAllowed) {
		this.zeroRateAllowed = zeroRateAllowed;
	}

	public boolean isZeroRateAllowed() {
		return zeroRateAllowed;
	}
}
