package com.aplos.common.interfaces;

import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.beans.communication.SmsMessage;

public interface SmsMessageOwner extends EmailFolder {
	public void smsMessageSent( SmsMessage smsMessage );
	public boolean saveDetails( SystemUser systemUser );
	public <T extends AplosAbstractBean> T  getSaveableBean();
}
