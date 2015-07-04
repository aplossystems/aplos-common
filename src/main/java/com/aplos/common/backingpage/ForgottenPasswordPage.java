package com.aplos.common.backingpage;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.GlobalAccess;
import com.aplos.common.appconstants.AplosAppConstants;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.enums.CommonEmailTemplateEnum;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@GlobalAccess
public class ForgottenPasswordPage extends BackingPage {

	private static final long serialVersionUID = 6104610643299961701L;

	private String username;
	private String emailAddressUsed;

	public ForgottenPasswordPage() {}

	@Override
	public boolean responsePageLoad() {
		super.responsePageLoad();
		if (username == null) {
			String usernamePassthrough = (String) JSFUtil.getFromFlashViewMap( AplosAppConstants.USERNAME_PASSTHROUGH );
			if ( usernamePassthrough != null ) {
				username = usernamePassthrough;
			}
		}
		return true;
	}

	public void goToLoginPage() {
		JSFUtil.addToFlashViewMap( AplosAppConstants.USERNAME_PASSTHROUGH, username );
		JSFUtil.redirect( LoginPage.class );
	}

	public void resetPassword() {
		//validate username (email)
		if (getUsername() != null && !getUsername().equals("")) {
			BeanDao systemUserDao = new BeanDao( SystemUser.class ).addWhereCriteria( "bean.username=:username").setIsReturningActiveBeans(true);
			systemUserDao.setNamedParameter( "username", getUsername());
			SystemUser systemUser = (SystemUser) systemUserDao.getFirstBeanResult();
			if (systemUser != null) {
				//create a passwordResetCode with 24 hours validity
				systemUser = systemUser.getSaveableBean();
				Random rand = new Random();
				String passwordResetCode = Long.toString( Math.abs( rand.nextLong() ), 36 );
				Calendar cal = Calendar.getInstance();
				cal.setTime( new Date() );
				cal.add(Calendar.DATE, 1);
				systemUser.setResetCode(passwordResetCode);
				systemUser.setResetDate( cal.getTime() );
				sendResetEmail(systemUser);
				systemUser.saveDetails();
				JSFUtil.getFacesContext().getViewRoot().getViewMap().put( "forgottenPasswordSent", true );
				JSFUtil.getFacesContext().getViewRoot().getViewMap().put( "usernameNotRecognised", null );
			} else {
				JSFUtil.addMessage( getUsername() + " is not registered with an account.", FacesMessage.SEVERITY_ERROR );
				JSFUtil.getFacesContext().getViewRoot().getViewMap().put( "usernameNotRecognised", true );
			}
		} else {
			JSFUtil.addMessage( "Username is empty. Please re-enter it.", FacesMessage.SEVERITY_ERROR );
		}
	}

	public void sendResetEmail( SystemUser systemUser ) {
		AplosEmail aplosEmail = new AplosEmail( CommonEmailTemplateEnum.FORGOTTEN_SYSTEM_PASSWORD, systemUser, systemUser );
		if( aplosEmail.sendAplosEmailToQueue() != null ) {
			emailAddressUsed = systemUser.getEmailAddress();
		} else {
			emailAddressUsed = null;
		}
	}

	public String resetUsernameNotRecognised() {
		JSFUtil.getFacesContext().getViewRoot().getViewMap().put( "usernameNotRecognised", null );
		return null;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getUsername() {
		return username;
	}

	public void setEmailAddressUsed(String emailAddressUsed) {
		this.emailAddressUsed = emailAddressUsed;
	}

	public String getEmailAddressUsed() {
		return emailAddressUsed;
	}
}