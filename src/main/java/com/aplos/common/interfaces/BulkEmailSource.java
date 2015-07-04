package com.aplos.common.interfaces;

import com.aplos.common.beans.communication.AplosEmail;


public interface BulkEmailSource extends BulkMessageSource { 
	public String getEmailAddress();
	public String getFirstName(); 
	public String getSurname(); 
	public String getJDynamiTeValue( String variableKey, AplosEmail aplosEmail );
}
