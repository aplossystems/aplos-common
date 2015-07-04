package com.aplos.common.backingpage;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import com.aplos.common.annotations.GlobalAccess;
import com.aplos.common.utils.ApplicationUtil;

@ManagedBean
@RequestScoped
@GlobalAccess
public class ErrorCheckPage extends BackingPage {
	private static final long serialVersionUID = -9170212571122201665L;

	public ErrorCheckPage() {
	}

	@Override
	public boolean responsePageLoad() {
		ApplicationUtil.getAplosContextListener().handleError( new NullPointerException() );
		return true;
	}

}
