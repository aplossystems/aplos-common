package com.aplos.common.backingpage;

import javax.faces.application.FacesMessage;

import com.aplos.common.enums.CommonBundleKey;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.JSFUtil;

public class SaveBtnListener extends AplosEventListener {
	private static final long serialVersionUID = -2349943751201435133L;

	public SaveBtnListener( BackingPage backingPage ) {
		setBackingPage( backingPage );
	}

	@Override
	public void actionPerformed( boolean redirect ) {
		boolean savedSuccessfully = getBackingPage().saveBeanWithWrap();
		if( savedSuccessfully ) {
			JSFUtil.addMessage(ApplicationUtil.getAplosContextListener().translateByKey( CommonBundleKey.SAVED_SUCCESSFULLY ),FacesMessage.SEVERITY_INFO);
		}
	}
}
