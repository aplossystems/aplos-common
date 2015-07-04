package com.aplos.common.filters;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.aplos.common.AplosRequestContext;
import com.aplos.common.appconstants.AplosScopedBindings;
import com.aplos.common.listeners.AplosContextListener;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

public class UrlRewriteFilter implements Filter {
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		
	}

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		ServletContext servletContext = request.getSession().getServletContext();
		AplosContextListener aplosContextListener = (AplosContextListener) servletContext.getAttribute( AplosScopedBindings.CONTEXT_LISTENER );
		String requestUrl = request.getRequestURL().toString();
		
		boolean isResourceRequest = CommonUtil.isResourceRequest( requestUrl );
		
		if( !isResourceRequest ) {
			AplosRequestContext aplosRequestContext = JSFUtil.getAplosRequestContext( request, true );
			
			if( aplosContextListener != null && !res.isCommitted() ) {
				if( ApplicationUtil.getAplosContextListener().getAplosModuleFilterer().rewriteUrl( request, response ) ) {
					if( aplosRequestContext.getDynamicViewEl() != null ) {
						req.getRequestDispatcher("/common/aplosUrlRewriter.jsf").forward(req, response);
					} else {
						aplosRequestContext.setCurrentUrl( aplosRequestContext.getRedirectionUrl() );
						/*
						 * If sendRedirect is used then a redirection URL isn't set.
						 */
						if( aplosRequestContext.getRedirectionUrl() != null ) {
							RequestDispatcher requestDispatcher = req.getRequestDispatcher( aplosRequestContext.getRedirectionUrl() );
							requestDispatcher.forward( req, response );
						}
					}
					return;
				}
			} 
		}
		
		chain.doFilter(request, response);
	}
	
	@Override
	public void destroy() {
		
		
	}

	
}
