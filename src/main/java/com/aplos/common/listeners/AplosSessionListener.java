package com.aplos.common.listeners;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import com.aplos.common.AplosRequestContext;
import com.aplos.common.appconstants.AplosScopedBindings;
import com.aplos.common.beans.TabSessionMap;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.JSFUtil;

public class AplosSessionListener implements HttpSessionListener {

	@Override
	public void sessionCreated( HttpSessionEvent event ) {
		event.getSession().setAttribute( AplosScopedBindings.SESSION_CREATED, true );
		event.getSession().setAttribute( AplosScopedBindings.TAB_SESSION, new TabSessionMap() );
	}

	@Override
	public void sessionDestroyed( HttpSessionEvent event ) {
		ApplicationUtil.getAplosContextListener().sessionDestroyed( event );
	}
	
	
}
