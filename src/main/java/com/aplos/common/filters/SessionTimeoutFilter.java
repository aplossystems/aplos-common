package com.aplos.common.filters;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.aplos.common.AplosRequestContext;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

public class SessionTimeoutFilter implements Filter {

	// This should be your default Home or Login page
	// "login.seam" if you use Jboss Seam otherwise "login.jsf"
	// "login.xhtml" or whatever

	@Override
	public void init( FilterConfig filterConfig ) throws ServletException {	}

	@Override
	public void doFilter( ServletRequest request, ServletResponse response, FilterChain filterChain ) throws IOException, ServletException {
		if ( ( request instanceof HttpServletRequest ) && ( response instanceof HttpServletResponse ) ) {
			HttpServletRequest httpServletRequest = (HttpServletRequest)request;
			String requestUrl = httpServletRequest.getRequestURL().toString();
			// is session invalid ?
			if( !CommonUtil.isResourceRequest( requestUrl ) && isSessionInvalid( httpServletRequest ) ) {
				AplosRequestContext aplosRequestContext = JSFUtil.getAplosRequestContext( httpServletRequest, true );
				aplosRequestContext.setSessionInvalid( true );
			}
		}
		filterChain.doFilter( request, response );
	}

	private boolean isSessionInvalid( HttpServletRequest httpServletRequest ) {
		boolean sessionInValid = ( httpServletRequest.getRequestedSessionId() != null ) && !httpServletRequest.isRequestedSessionIdValid();
		return sessionInValid;
	}

	@Override
	public void destroy() {

	}

}

