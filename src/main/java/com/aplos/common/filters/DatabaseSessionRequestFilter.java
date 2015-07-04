package com.aplos.common.filters;

import java.io.IOException;
import java.net.SocketException;
import java.util.Date;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.ClientAbortException;
import org.apache.log4j.Logger;

import com.aplos.common.AplosRequestContext;
import com.aplos.common.appconstants.AplosAppConstants;
import com.aplos.common.appconstants.AplosScopedBindings;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.PageRequest;
import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.beans.communication.SingleEmailRecord;
import com.aplos.common.listeners.AplosContextListener;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.ErrorEmailSender;
import com.aplos.common.utils.JSFUtil;
import com.aplos.common.utils.UrlEncrypter;

public class DatabaseSessionRequestFilter implements javax.servlet.Filter {

	private final Logger logger = Logger.getLogger( getClass().getName() );

	@Override
	public void doFilter( ServletRequest request, ServletResponse response, FilterChain chain ) throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest)request;
		try {
			String requestUri = httpRequest.getRequestURI();
			requestUri = requestUri.replace( "//", "/" );
			if( requestUri.startsWith( JSFUtil.getContextPath() ) ) {
				requestUri = requestUri.substring( JSFUtil.getContextPath().length() );
			}
			boolean isResourceRequest = requestUri.contains( "/javax.faces.resource" ) || requestUri.startsWith( "/resources" ) || requestUri.startsWith( "/media" );
			AplosRequestContext aplosRequestContext = null;

			if( !isResourceRequest ) {
				aplosRequestContext = JSFUtil.getAplosRequestContext( httpRequest, true ); 
				aplosRequestContext.setPageRequest(new PageRequest());
				String pageUrl = CommonUtil.getStringOrEmpty( httpRequest.getRequestURL().toString() );
				if( pageUrl.length() > ApplicationUtil.getPersistentApplication().getMaxCharLength() ) {
					pageUrl = pageUrl.substring( 0, ApplicationUtil.getPersistentApplication().getMaxCharLength() - 1 );
				}
				aplosRequestContext.getPageRequest().setPageUrl( pageUrl );
				aplosRequestContext.getPageRequest().setInitialValues(httpRequest);
				/*
				 * This doesn't appear to be needed as it's the default for JSF and
				 * called in the initView method of MultiViewHandler called from
				 * the RestoreViewPhase method doPhase().
				 * 
				 * It may have been put in because of richfaces which is still on the
				 * server.  So I've put it back in and we can look at retesting later
				 * I tested by simply adding a £ to repair product textarea in
				 * teletest returns 28 Mar 2012 AM
				 */
				request.setCharacterEncoding("UTF-8");
				response.setCharacterEncoding("UTF-8");
			}
			
			if( request.getParameter( AplosAppConstants.EMAIL_TRACKING ) != null
    				&& request.getParameter( AplosScopedBindings.ID ) != null ) {
    			String trackingCode = request.getParameter( AplosAppConstants.EMAIL_TRACKING );
    			String aplosEmailIdStr = request.getParameter( AplosScopedBindings.ID );
    			try {
    				Long aplosEmailId = Long.parseLong( aplosEmailIdStr );
    				AplosEmail aplosEmail = new BeanDao( AplosEmail.class ).get( aplosEmailId );
    				
    				if( aplosEmail != null ) {
		    			UrlEncrypter urlEncrypter = aplosEmail.createUrlEncrypter();
		    			String idStr = urlEncrypter.decrypt(trackingCode);
	    				Long singleEmailRecordId = Long.parseLong( idStr );
	    				SingleEmailRecord singleEmailRecord = new BeanDao( SingleEmailRecord.class ).get( singleEmailRecordId );
	    				if( singleEmailRecord != null ) {
		    				singleEmailRecord = singleEmailRecord.getSaveableBean();
		    				if( "a".equals( request.getParameter( AplosAppConstants.TRACKING_TYPE ) ) ) {
		    					singleEmailRecord.setActionedDate(new Date());
		    				} else {
		    					singleEmailRecord.setOpenedDate(new Date());
		    				}
		    				singleEmailRecord.saveDetails();
	    				}
    				}
    			} catch( NumberFormatException nfex ) {
    				ApplicationUtil.handleError( nfex );
    			}
    		}

			// Call the next filter (continue request processing)
			chain.doFilter( request, response );
//
			if( !isResourceRequest ) {
//				logger.debug( "Committing the database transaction" );
//
				aplosRequestContext.getPageRequest().setDuration(System.nanoTime() - aplosRequestContext.getPageRequest().getPageRequestedTime());
				aplosRequestContext.getPageRequest().saveDetails();
				if( aplosRequestContext.getPageRequest().getId() % 10000 == 0 ) {
					ApplicationUtil.executeSql( "DELETE FROM " + AplosBean.getTableName( PageRequest.class ) + " WHERE id < " + (aplosRequestContext.getPageRequest().getId() - 10000) );
				}
				
				if( ApplicationUtil.getPersistenceContext().getConnection() != null ) {
					ApplicationUtil.getPersistenceContext().getConnection().commit();
				}
			}
//
//		} catch( StaleObjectStateException staleEx ) {
//			logger.error( "This interceptor does not implement optimistic concurrency control!" );
//			logger.error( "Your application will not work until you add compensation actions!" );
//			// Rollback, close everything, possibly compensate for any permanent
//			// changes
//			// during the conversation, and finally restart business
//			// conversation. Maybe
//			// give the user of the application a chance to merge some of his
//			// work with
//			// fresh data... what you do here depends on your applications
//			// design.
//			throw staleEx;
        } catch ( ClientAbortException cae ) {
//			hibernateRollback();
        	logger.debug("Aborting transfer.  it happens, no problem.", cae);
        } catch ( SocketException cae ) {
//			hibernateRollback();
        	logger.debug("Aborting transfer.  it happens, no problem.", cae);
		} catch( Throwable ex ) {
//			hibernateRollback();
			Throwable exCause = ex;
			int count = 0;
			while( exCause != null && exCause != exCause.getCause() && count < 10 ) {
				count++;
				exCause = exCause.getCause();
				if( exCause instanceof ClientAbortException ) {
					return;
				}
			}
			AplosContextListener aplosContextListener = (AplosContextListener) httpRequest.getSession().getServletContext().getAttribute( AplosScopedBindings.CONTEXT_LISTENER );

			if( count == 10 ) {
				ErrorEmailSender.sendErrorEmail(httpRequest, aplosContextListener,ex,"Error count = 10");
			}

			aplosContextListener.handleError(httpRequest, (HttpServletResponse) response, ex, aplosContextListener.getCurrentUrlsHtml(httpRequest), true);

			// Why would you need to do this, when the context listener is handling the error?
//			if( !aplosContextListener.isDebugMode() ) {
//				throw new ServletException( ex );
//			}
		} finally {
			persistentContextRollbackIfRequired();
		}
	}

	public void persistentContextRollbackIfRequired() {
		// Rollback only
		try {
			if( ApplicationUtil.getPersistenceContext().getConnection() != null ) {
				logger.debug( "Trying to rollback database transaction after exception" );
				ApplicationUtil.getPersistenceContext().rollback();
			}
		} catch( Throwable rbEx ) {
			logger.error( "Could not rollback transaction after exception!", rbEx );
		}
	}

	@Override
	public void init( FilterConfig filterConfig ) throws ServletException {
		logger.debug( "Initializing filter..." );
		logger.debug( "Obtaining SessionFactory from static HibernateUtil singleton" );
	}

	@Override
	public void destroy() {}

}
