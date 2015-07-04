package com.aplos.common.backingpage.communication;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.GlobalAccess;
import com.aplos.common.backingpage.BackingPage;
import com.aplos.common.listeners.PageBindingPhaseListener;

@ManagedBean
@ViewScoped
@GlobalAccess
public class AplosEmailHtmlPreviewPage extends BackingPage {
	private static final long serialVersionUID = -2459671820828188723L;

	public AplosEmailHtmlPreviewPage() {
	}
	
	public String getHtmlPreview() {
		AplosEmailEditPage aplosEmailEditPage = (AplosEmailEditPage) PageBindingPhaseListener.resolveBackingPage( AplosEmailEditPage.class );
		return aplosEmailEditPage.getHtmlPreview();
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
