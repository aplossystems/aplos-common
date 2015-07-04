package com.aplos.common.backingpage;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import com.aplos.common.appconstants.AplosAppConstants;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.beans.Website;
import com.aplos.common.enums.WebServiceCallTypes;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@SessionScoped

public class NeaseGatewayPage extends BackingPage {

	private static final long serialVersionUID = 8077591935336468550L;
	private boolean incompleteNeaseDetails = false;
	private String responseError = null;
	//private String responseUrl = null;

	@Override
	public boolean responsePageLoad() {

		/**
		 * This page is correctly placed in common - do not remove it to networking ease
		 *
		 * Its purpose is to securely post login details to networking ease and also so we
		 * can dynamically load the details we want (as if we have a tab and no backing page,
		 * we have to hard code the details to pass in the url)
		 */
		super.responsePageLoad();
		if (!incompleteNeaseDetails && JSFUtil.getLoggedInUser()!=null) {
			//Load the user
			String nePassword = JSFUtil.getLoggedInUser().getNeasePassword();
			String neUsername = JSFUtil.getLoggedInUser().getNeaseUsername();
			
			Long neaseProjectId = Website.getCurrentWebsiteFromTabSession().getNeaseProjectId();
			if (neaseProjectId == null) {
				neaseProjectId = -1l;
			}
			
			//Check for valid nease details
			if (nePassword != null && neUsername != null &&  !nePassword.equals("") && !neUsername.equals("")) {
				incompleteNeaseDetails=false;
			} else {
				incompleteNeaseDetails=true;
			}
		}
		return true;
	}

	public String saveNeaseDetails() {
		SystemUser sysUsr = JSFUtil.getLoggedInUser();
		sysUsr.saveDetails();
		sysUsr.login();
		incompleteNeaseDetails=false;
		return null;
	}

	public String cancelAndChangeDetails() {
		//TODO: do any cancelling actions
		incompleteNeaseDetails=true;
		setResponseError(null);
		return null;
	}

	public void setIncompleteNeaseDetails(boolean incompleteNeaseDetails) {
		this.incompleteNeaseDetails = incompleteNeaseDetails;
	}

	public boolean isIncompleteNeaseDetails() {
		return incompleteNeaseDetails;
	}

	public String getResponseUrl() {
		String urlStart;

		if( JSFUtil.isLocalHost() ) {
			urlStart = "http://localhost:8080/networkingease";
		} else {
			urlStart = "https://app.networkingease.co.uk";
		}

		return urlStart + "/nease/common/login.jsf?" + AplosAppConstants.ALLOW_COOKIE_LOGIN + "=false";
	}

	public String getToken() {
		return "JKHDAIU23756UHV423";
	}

	public String getCallType() {
		return WebServiceCallTypes.AUTHENTICATE_LOGIN.toString();
	}

	public String getNeaseUsername() {
		return JSFUtil.getLoggedInUser().getNeaseUsername();
	}

	public String getNeasePassword() {
		return JSFUtil.getLoggedInUser().getNeasePassword();
	}

	public void setResponseError(String responseError) {
		this.responseError = responseError;
	}

	public String getResponseError() {
		return responseError;
	}

}