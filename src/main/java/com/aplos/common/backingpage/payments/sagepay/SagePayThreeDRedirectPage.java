package com.aplos.common.backingpage.payments.sagepay;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.GlobalAccess;
import com.aplos.common.annotations.WindowId;
import com.aplos.common.backingpage.BackingPage;

@ManagedBean
@ViewScoped
@WindowId(required=false)
@GlobalAccess
public class SagePayThreeDRedirectPage extends BackingPage {
	private static final long serialVersionUID = -3984945071235925919L;

	@Override
	public boolean responsePageLoad() {
		// TODO Auto-generated method stub
		return super.responsePageLoad();
	}
}












