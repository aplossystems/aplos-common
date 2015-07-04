package com.aplos.common.backingpage;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.GlobalAccess;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@GlobalAccess

public class SessionTimeoutPage extends BackingPage {
	private static final long serialVersionUID = -8230803442277408116L;

	public SessionTimeoutPage() {

	}
	
	public void login() {
		JSFUtil.redirect( LoginPage.class );
	}
	
	public void navigateBack() {
		JSFUtil.getNavigationStack().navigateBack();
	}

	public String getMaxSessionInterval() {
		return String.valueOf( JSFUtil.getSessionTemp().getMaxInactiveInterval() / 60 );
	}

	public Boolean getIncludeForm() {
		return true;
	}
}
