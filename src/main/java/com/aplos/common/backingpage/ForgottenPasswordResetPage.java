package com.aplos.common.backingpage;

import java.util.Date;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.AplosUrl;
import com.aplos.common.annotations.GlobalAccess;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.beans.Website;
import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.enums.CommonEmailTemplateEnum;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@GlobalAccess

public class ForgottenPasswordResetPage extends BackingPage {

	private static final long serialVersionUID = 2911804860284151926L;

	private String password;
	private String passwordConfirm;

	private String passwordResetCode;

	public ForgottenPasswordResetPage() {}

	@Override
	public boolean responsePageLoad() {

		super.responsePageLoad();

		if( JSFUtil.getFacesContext().getViewRoot().getViewMap().get( "passwordChanged" ) == null ) {
			passwordResetCode = JSFUtil.getRequestParameter("passwordResetCode");
			if (passwordResetCode==null || passwordResetCode.equals("")) {
				JSFUtil.addMessage("No reset code has been specified. This code is required to reset a password.", FacesMessage.SEVERITY_ERROR);
				JSFUtil.getFacesContext().getViewRoot().getViewMap().put( "resetCodeIncorrect", true );
			} else {
				SystemUser systemUser = getUserFromResetCode();
				if (systemUser==null) {
					JSFUtil.addMessage("The reset code provided was not recognised.", FacesMessage.SEVERITY_ERROR);
					JSFUtil.getFacesContext().getViewRoot().getViewMap().put( "resetCodeIncorrect", true );
				} else {
					//ensure the code is in-date
					Date now = new Date();
					if (systemUser.getResetDate().compareTo(now) <= 0) {
						//clear the reset code and date
						systemUser.setResetCode(null);
						systemUser.setResetDate(null);
						systemUser.saveDetails();
						JSFUtil.addMessage("The reset code provided has expired.", FacesMessage.SEVERITY_ERROR);
						JSFUtil.getFacesContext().getViewRoot().getViewMap().put( "resetCodeIncorrect", true );
					}
				}
			}
		}
		return true;
	}
	
	public SystemUser getUserFromResetCode() {
		BeanDao systemUserDao = new BeanDao( SystemUser.class );
		systemUserDao.addWhereCriteria( "bean.resetCode=:resetCode" );
		systemUserDao.setNamedParameter( "resetCode", passwordResetCode );
		return systemUserDao.getFirstBeanResult();
	}

	public String updateNewPassword() {
		if( !getPassword().equals( getPasswordConfirm() ) ) {
			JSFUtil.addMessage("The passwords entered do not match.", FacesMessage.SEVERITY_ERROR);
		} else {
			SystemUser systemUser = getUserFromResetCode();
			if (systemUser == null) {
				JSFUtil.addMessage("The reset code provided was not recognised.", FacesMessage.SEVERITY_ERROR);
			} else {
//				systemUser.hibernateInitialise( true );
				systemUser = systemUser.getSaveableBean();
				systemUser.updatePassword( getPassword() );
				systemUser.setResetCode(null);
				systemUser.setResetDate(null);
				systemUser.setLockedOutByAttempts(false);
				systemUser.saveDetails();
				systemUser.login();
				//redirect to change password screen
				JSFUtil.getFacesContext().getViewRoot().getViewMap().put( "passwordChanged", true );
			}
		}
		return null;
	}

	public void goToForgottenPassword() {
		//JSFUtil.addToFlashViewMap( AplosAppConstants.USERNAME_PASSTHROUGH, username );
		JSFUtil.redirect( ForgottenPasswordPage.class );
	}

	public void goToAfterLogin() {
		JSFUtil.redirect( ApplicationUtil.getAplosContextListener().getAfterLoginUrl(Website.getCurrentWebsiteFromTabSession()));
	}

	public String resetPassword() {
		//get the passwordResetCode from the url parameters
		String passwordResetCode = JSFUtil.getRequestParameter("passwordResetCode");
		if (passwordResetCode==null || passwordResetCode.equals("")) {
			JSFUtil.addMessage("No reset code has been specified. This code is required to reset a password.", FacesMessage.SEVERITY_ERROR);
		} else {
			//find the matching user
			SystemUser systemUser = getUserFromResetCode();
			if (systemUser==null) {
				JSFUtil.addMessage("The reset code provided was not recognised.", FacesMessage.SEVERITY_ERROR);
			} else {
				//ensure the code is in-date
				Date now = new Date();
				if (systemUser.getResetDate().compareTo(now) > 0) {
					//reset the password
//					systemUser.hibernateInitialise( true );
					systemUser.updatePassword( CommonUtil.generateRandomCode() );
					sendResetEmail( systemUser );
					//login
					systemUser.login();
					//clear the reset code and date
					systemUser.setResetCode(null);
					systemUser.setResetDate(null);
					systemUser.saveDetails();
					//redirect to change password screen
					AplosUrl aplosUrl = ApplicationUtil.getAplosContextListener().getAfterLoginUrl(Website.getCurrentWebsiteFromTabSession());
					JSFUtil.redirect( aplosUrl, true);
				} else {
					//clear the reset code and date
					systemUser.setResetCode(null);
					systemUser.setResetDate(null);
					systemUser.saveDetails();
					JSFUtil.addMessage("The reset code provided has expired. You will need to resubmit a forgotten password request.", FacesMessage.SEVERITY_ERROR);
				}

			}
		}
		return null;
	}

	public void sendResetEmail( SystemUser systemUser ) {
		AplosEmail aplosEmail = new AplosEmail( CommonEmailTemplateEnum.RESET_PASSWORD, systemUser, systemUser );
		aplosEmail.sendAplosEmailToQueue();
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPassword() {
		return password;
	}

	public void setPasswordConfirm(String passwordConfirm) {
		this.passwordConfirm = passwordConfirm;
	}

	public String getPasswordConfirm() {
		return passwordConfirm;
	}
}



















