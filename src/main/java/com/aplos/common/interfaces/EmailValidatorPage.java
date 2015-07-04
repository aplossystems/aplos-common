package com.aplos.common.interfaces;

import com.aplos.common.beans.communication.AplosEmail;

public interface EmailValidatorPage {
	public void setAplosEmail( AplosEmail aplosEmail );
	public void setEmailSource( BulkEmailSource emailSource );
}
