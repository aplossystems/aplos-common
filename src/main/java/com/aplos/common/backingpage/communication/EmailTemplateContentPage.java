package com.aplos.common.backingpage.communication;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.GlobalAccess;
import com.aplos.common.backingpage.BackingPage;

@ManagedBean
@ViewScoped
@GlobalAccess
public class EmailTemplateContentPage extends BackingPage {
	private static final long serialVersionUID = -6622165487228583273L;

	public EmailTemplateContentPage() {
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
