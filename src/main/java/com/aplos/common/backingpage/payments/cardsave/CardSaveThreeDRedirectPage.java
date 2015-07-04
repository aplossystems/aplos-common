package com.aplos.common.backingpage.payments.cardsave;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.GlobalAccess;
import com.aplos.common.annotations.WindowId;
import com.aplos.common.backingpage.BackingPage;

@ManagedBean
@ViewScoped
@WindowId(required=false)
@GlobalAccess
public class CardSaveThreeDRedirectPage extends BackingPage {
	private static final long serialVersionUID = -1601650547674765152L;

	@Override
	public boolean responsePageLoad() {
		super.responsePageLoad();
//		Cookie jSessionIdCookie = new Cookie("JSESSIONID", JSFUtil.getRequestParameter( "jsessionid" ));
//		jSessionIdCookie.setPath(JSFUtil.getContextPath());
//		jSessionIdCookie.setMaxAge( -1 ); //  Set to one year
//		JSFUtil.getResponse().addCookie(jSessionIdCookie);
		return true;
	}
}












