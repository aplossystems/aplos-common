package com.aplos.common.backingpage;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.GlobalAccess;
import com.aplos.common.appconstants.AplosScopedBindings;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@GlobalAccess
public class PasswordExpiryPage extends BackingPage {
	private static final long serialVersionUID = -7675969412109905500L;
	private SystemUser systemUser;
	private String password;
	private String passwordConfirm;
	private boolean rememberMe;

	@Override
	public boolean requestPageLoad() {
		super.requestPageLoad();
		return true;
	}

	@Override
	public boolean responsePageLoad() {
		super.responsePageLoad();
		if( getSystemUser() == null ) {
			setSystemUser( (SystemUser) JSFUtil.getBeanFromFlash( SystemUser.class ) );
		}
		if( getSystemUser() == null ) {
			if( JSFUtil.getRequestParameter( AplosScopedBindings.SYSTEM_USER_ID ) != null ) {
				String passwordExpiryCode = JSFUtil.getRequestParameter( AplosScopedBindings.PASSWORD_EXPIRY_CODE );
				try {
					Long systemUserId = Long.parseLong( JSFUtil.getRequestParameter( AplosScopedBindings.SYSTEM_USER_ID ) );
					SystemUser localSystemUser = new BeanDao( SystemUser.class ).get( systemUserId );
					if( CommonUtil.isNullOrEmpty( localSystemUser.getPassword() ) ) {
						if( CommonUtil.getStringOrEmpty( passwordExpiryCode ).equals( localSystemUser.getPasswordExpiryCode() ) ) {
							setSystemUser(localSystemUser);
						} else {
							JSFUtil.addMessage( "The access code you have been given doesn't match the one stored in the database.  Please contact a system administrator." );
						}
					} else {
						JSFUtil.addMessage( "This user has a password set already please use the log in page" );
						JSFUtil.redirect( LoginPage.class );
						return false;
					}
				} catch( NumberFormatException nfex ) {
					JSFUtil.addMessage( "The URL is not in the format expected, please contact a system administrator" );
					JSFUtil.redirect( LoginPage.class );
					return false;
				}
			}
		}
		if( getSystemUser() == null ) {
			JSFUtil.addMessage( "You do not have access to the password expiry page, please contact a system administrator." );
			JSFUtil.redirect( LoginPage.class );
			return false;
		}
		return true;
	}

	public void changePassword() {
		if( getPassword().equals( getPasswordConfirm() ) ) {
			getSystemUser().updatePassword(getPassword());
			getSystemUser().setPasswordExpiryDate(null);
			getSystemUser().setPasswordExpiryCode(null);
			getSystemUser().saveDetails();
			JSFUtil.redirect( LoginPage.handleSuccessfulLogin(getSystemUser(),false) );
		} else {
			JSFUtil.addMessage( "The passwords do not match, please try again" );
		}
	}

	public String getPassword() {
		return password;
	}

	public void setPassword( String password ) {
		this.password = password;
	}

	public String getPasswordConfirm() {
		return passwordConfirm;
	}

	public void setPasswordConfirm(String passwordConfirm) {
		this.passwordConfirm = passwordConfirm;
	}

	public void setSystemUser(SystemUser systemUser) {
		this.systemUser = systemUser;
	}

	public void setRememberMe(boolean rememberMe) {
		this.rememberMe = rememberMe;
	}

	public boolean isRememberMe() {
		return rememberMe;
	}

	public SystemUser getSystemUser() {
		return systemUser;
	}

}