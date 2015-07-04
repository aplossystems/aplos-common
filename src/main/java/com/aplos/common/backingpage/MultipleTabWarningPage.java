package com.aplos.common.backingpage;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.GlobalAccess;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@GlobalAccess
public class MultipleTabWarningPage extends BackingPage {
	private static final long serialVersionUID = -349014217260841503L;

	public MultipleTabWarningPage() {
	}

	@Override
	public boolean responsePageLoad() {
		super.responsePageLoad();
		JSFUtil.getResponse().setStatus( 409 );
		return true;
	}
}
