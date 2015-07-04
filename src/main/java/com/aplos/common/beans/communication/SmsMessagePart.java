package com.aplos.common.beans.communication;

import java.math.BigDecimal;

import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.beans.AplosAbstractBean;

@Entity
public class SmsMessagePart extends AplosAbstractBean {
	private static final long serialVersionUID = -486277629655580851L;
	private int statusCode;
	private String messageId;
	@Column(columnDefinition="LONGTEXT")
	private String errorText;
	private String destinationNumber;
	private BigDecimal messagePrice;
	private boolean isUnicode = false;
	
	public String getMessageId() {
		return messageId;
	}
	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}
	public String getErrorText() {
		return errorText;
	}
	public void setErrorText(String errorText) {
		this.errorText = errorText;
	}
	public int getStatusCode() {
		return statusCode;
	}
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
	public void setMessagePrice(BigDecimal messagePrice) {
		this.messagePrice = messagePrice;
	}
	public BigDecimal getMessagePrice() {
		return messagePrice;
	}
	public String getDestinationNumber() {
		return destinationNumber;
	}
	public void setDestinationNumber(String destinationNumber) {
		this.destinationNumber = destinationNumber;
	}
	public boolean isUnicode() {
		return isUnicode;
	}
	public void setUnicode(boolean isUnicode) {
		this.isUnicode = isUnicode;
	}
}
