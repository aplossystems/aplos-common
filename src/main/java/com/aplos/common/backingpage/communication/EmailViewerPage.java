package com.aplos.common.backingpage.communication;

import java.io.IOException;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.FrontendBackingPage;
import com.aplos.common.annotations.GlobalAccess;
import com.aplos.common.backingpage.BackingPage;
import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.enums.MessageGenerationType;
import com.aplos.common.interfaces.BulkEmailSource;
import com.aplos.common.interfaces.EmailGenerator;
import com.aplos.common.interfaces.EmailValidatorPage;
import com.aplos.common.utils.ApplicationUtil;

@ManagedBean
@ViewScoped
@FrontendBackingPage
@GlobalAccess
public class EmailViewerPage extends BackingPage implements EmailValidatorPage {
	private static final long serialVersionUID = 2591544171298363068L;
	private AplosEmail aplosEmail;
	private BulkEmailSource emailSource; 
	
	@Override
	public boolean responsePageLoad() {
		boolean continueLoad = super.responsePageLoad();
		if( getAplosEmail() == null ) {
			AplosEmail.verifyParameters( this );
		}
		return continueLoad;
	}
	
	public String getFullEmailBody() {
		String htmlBody = "";
		try {
			EmailGenerator determinedEmailGenerator = getAplosEmail().getEmailGenerator();
			if( determinedEmailGenerator == null ) {
				determinedEmailGenerator = getEmailSource();
			}
			String processedSubject;
			if( MessageGenerationType.SINGLE_SOURCE.equals( getAplosEmail().getEmailGenerationType() ) ) {
				processedSubject = getAplosEmail().getSubject();
			} else {
				processedSubject = getAplosEmail().processSubjectDynamicValues( getEmailSource(), determinedEmailGenerator );
			}
			
			htmlBody = getAplosEmail().generateHtmlBody( getAplosEmail().getSingleEmailRecord(getEmailSource(), true), determinedEmailGenerator, processedSubject );
		} catch( IOException ioex ) {
			ApplicationUtil.handleError( ioex );
		}
		return htmlBody;
	}

	public AplosEmail getAplosEmail() {
		return aplosEmail;
	}

	public void setAplosEmail(AplosEmail aplosEmail) {
		this.aplosEmail = aplosEmail;
	}

	public BulkEmailSource getEmailSource() {
		return emailSource;
	}

	public void setEmailSource(BulkEmailSource emailSource) {
		this.emailSource = emailSource;
	}

}
