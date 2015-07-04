package com.aplos.common.beans.communication;

import java.io.IOException;

import com.aplos.common.interfaces.BulkSmsSource;

public abstract class SourceGeneratedSmsTemplate<T extends BulkSmsSource> extends SmsTemplate<T,T>{
	private static final long serialVersionUID = -8734411136784287654L;

	@Override
	public final String compileContent(T smsRecipient, T emailGenerator, String content) throws ClassCastException ,java.io.IOException {
		return compileContent( smsRecipient, content );
	}
	
	public abstract String compileContent(T smsRecipient, String content) throws ClassCastException ,java.io.IOException;

	public String compileContent(T smsRecipient) throws ClassCastException,IOException {
		return compileContent(smsRecipient, smsRecipient, determineContent());
	}
}
