package com.aplos.common.interfaces;

import com.aplos.common.beans.InternationalNumber;
import com.aplos.common.beans.communication.SmsMessage;

public interface BulkSmsSource extends BulkMessageSource {
	public InternationalNumber getInternationalNumber();
	public String getFirstName(); 
	public String getSurname(); 
	public boolean determineIsSmsSubscribed( SmsMessage smsMessage );
	public void setSmsSubscribed( boolean isSmsSubscribed );
}
