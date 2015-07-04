package com.aplos.common.backingpage;

import com.aplos.common.utils.JSFUtil;

public class CancelBtnListener extends AplosEventListener{
	private static final long serialVersionUID = -5037980537205764124L;

	public CancelBtnListener( BackingPage backingPage ) {
		this.setBackingPage(backingPage);
	}
	@Override
	public void actionPerformed(boolean redirect) {
		if(JSFUtil.getNavigationStack().navigateBack() == null) { // this will do a redirect if a previous element exists
			JSFUtil.redirect(getBackingPage().getBeanDao().getListPageClass());
		}
	}
}
