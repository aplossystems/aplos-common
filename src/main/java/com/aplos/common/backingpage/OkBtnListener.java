package com.aplos.common.backingpage;

import com.aplos.common.enums.CommonBundleKey;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.JSFUtil;

public class OkBtnListener extends AplosEventListener {
	private static final long serialVersionUID = -4593396973496116273L;
	private boolean showSavedMessage = false;

	public OkBtnListener( BackingPage backingPage ) {
		setBackingPage( backingPage );
	}

	@Override
	public void actionPerformed( boolean redirect ) {
		boolean beanSaved = getBackingPage().saveBeanWithWrap();
		if( beanSaved ) {
			ApplicationUtil.startNewTransaction();
			if( isShowSavedMessage() ) {
				JSFUtil.addMessage( ApplicationUtil.getAplosContextListener().translateByKey( CommonBundleKey.SAVED_SUCCESSFULLY ) );
			}
	
			if( redirect ) {
				getBackingPage().navigateToPreviousPage();
			}
		}
	}

	public void setShowSavedMessage(boolean showSavedMessage) {
		this.showSavedMessage = showSavedMessage;
	}

	public boolean isShowSavedMessage() {
		return showSavedMessage;
	}
}
