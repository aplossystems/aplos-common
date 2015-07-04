package com.aplos.common.interfaces;

import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.beans.communication.SmsMessage;
import com.aplos.common.enums.UnsubscribeType;

public interface SubscriberInter extends BulkEmailSource, BulkSubscriberSource {
	public String getFirstName();
	public String getSurname();
	public String getEmailAddress();
	public boolean isSubscribed();
	public void setSubscribed( boolean isSubscribed );
	public boolean saveDetails();
	public String getUnsubscribeReason();
	public void setUnsubscribeReason(String unsubscribeReason);
	public boolean determineIsSubscribed( AplosEmail aplosEmail );
	public boolean determineIsSmsSubscribed( SmsMessage smsMessage );
	public <T extends AplosAbstractBean> T getSaveableBean();
	public void setUnsubscribeType( UnsubscribeType unsubscribeType );
	public UnsubscribeType getUnsubscribeType();
}
