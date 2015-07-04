package com.aplos.common.beans.communication;

import java.io.IOException;
import java.util.List;

import cb.jdynamite.JDynamiTe;

import com.aplos.common.beans.FileDetails;
import com.aplos.common.interfaces.BulkEmailSource;

public abstract class SourceGeneratedEmailTemplate<T extends BulkEmailSource> extends EmailTemplate<T,T>{

	@Override
	public String postProcessSubject(T bulkEmailRecipient, T emailGenerator, String subject) {
		return postProcessSubject(bulkEmailRecipient,subject);
	}
	
	@Override
	public void addSubjectJDynamiTeValues(JDynamiTe jDynamiTe, T bulkEmailRecipient, T emailGenerator) {
		super.addSubjectJDynamiTeValues(jDynamiTe, bulkEmailRecipient, emailGenerator);
		addSubjectJDynamiTeValues(jDynamiTe, bulkEmailRecipient);
	}
	
	@Override
	public void addContentJDynamiTeValues(JDynamiTe jDynamiTe, T bulkEmailRecipient, T emailGenerator, SingleEmailRecord singleEmailRecord) {
		super.addContentJDynamiTeValues(jDynamiTe, bulkEmailRecipient, emailGenerator, singleEmailRecord);
		addContentJDynamiTeValues(jDynamiTe, bulkEmailRecipient);
	}

	public String postProcessSubject(T bulkEmailRecipient, String subject) {
		return subject;
	}
	
	public void addContentJDynamiTeValues(JDynamiTe jDynamiTe, T bulkEmailRecipient) {
		
	}
	
	public void addSubjectJDynamiTeValues(JDynamiTe jDynamiTe, T bulkEmailRecipient) {
		
	}
	
	public String compileSubject(T bulkEmailRecipient) throws ClassCastException,IOException {
		return compileSubject(bulkEmailRecipient, bulkEmailRecipient, determineSubject());
	}

	public String compileContent(T bulkEmailRecipient, SingleEmailRecord singleEmailRecord) throws ClassCastException,IOException {
		return compileContent(bulkEmailRecipient, bulkEmailRecipient, determineContent(), singleEmailRecord);
	}
}
