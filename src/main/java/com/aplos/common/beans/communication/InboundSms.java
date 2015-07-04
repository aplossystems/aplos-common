package com.aplos.common.beans.communication;

import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.beans.AplosBean;

@Entity
public class InboundSms extends AplosBean {
	private static final long serialVersionUID = 6901878890973384076L;
	
	private String type;
	private String to;
	private String msisdn;
	private String networkCode;
	private String messageId;
	private String messageTimestamp;
	private String text;
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getTo() {
		return to;
	}
	public void setTo(String to) {
		this.to = to;
	}
	public String getMsisdn() {
		return msisdn;
	}
	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}
	public String getNetworkCode() {
		return networkCode;
	}
	public void setNetworkCode(String networkCode) {
		this.networkCode = networkCode;
	}
	public String getMessageId() {
		return messageId;
	}
	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}
	public String getMessageTimestamp() {
		return messageTimestamp;
	}
	public void setMessageTimestamp(String messageTimestamp) {
		this.messageTimestamp = messageTimestamp;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}

}
