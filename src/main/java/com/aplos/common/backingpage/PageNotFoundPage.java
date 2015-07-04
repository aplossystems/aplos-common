package com.aplos.common.backingpage;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.GlobalAccess;
import com.aplos.common.appconstants.AplosScopedBindings;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@GlobalAccess
public class PageNotFoundPage extends BackingPage {

	private static final long serialVersionUID = -922279587971772469L;

	public PageNotFoundPage() {
	}
	
	public String getPageNotFoundUrl() {
		return (String) JSFUtil.getFromFlashViewMap( AplosScopedBindings.PAGE_NOT_FOUND );
	}

	@Override
	public boolean responsePageLoad() {
		super.responsePageLoad();
		JSFUtil.getResponse().setStatus( 404 );
		return true;
	}
}
