package com.aplos.common.backingpage.communication;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.GlobalAccess;
import com.aplos.common.backingpage.BackingPage;

@ManagedBean
@ViewScoped
@GlobalAccess
public class AplosEmailContentPage extends BackingPage {
	private static final long serialVersionUID = -6749873070427613437L;

	public AplosEmailContentPage() {
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
