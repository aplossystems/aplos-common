package com.aplos.common.backingpage.communication;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.GlobalAccess;
import com.aplos.common.backingpage.BackingPage;

@ManagedBean
@ViewScoped
@GlobalAccess
public class AplosEmailPrintContentPage extends BackingPage {
	private static final long serialVersionUID = -3613654151066096307L;

	public AplosEmailPrintContentPage() {
	}
	
	@Override
	public boolean shouldStackOnNavigation() {
		return false;
	}
	
	@Override
	public boolean responsePageLoad() {
		return super.responsePageLoad();
	}
}
