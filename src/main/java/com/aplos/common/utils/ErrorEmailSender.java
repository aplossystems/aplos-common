package com.aplos.common.utils;

import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.aplos.common.AplosRequestContext;
import com.aplos.common.appconstants.AplosScopedBindings;
import com.aplos.common.backingpage.BackingPage;
import com.aplos.common.backingpage.BackingPageState;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.TabSessionMap;
import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.beans.communication.MailServerSettings;
import com.aplos.common.listeners.AplosContextListener;
import com.aplos.common.module.CommonConfiguration;

public class ErrorEmailSender {
	private static ConcurrentLinkedQueue<Date> sentMessages = new ConcurrentLinkedQueue<Date>();

	public static synchronized void sendErrorEmail(HttpServletRequest request, AplosContextListener contextListener, Throwable throwable) {
		sendErrorEmail(request, contextListener, throwable, contextListener.getCurrentUrlsHtml());
	}

	public static synchronized void sendErrorEmail(HttpServletRequest request, AplosContextListener contextListener, Throwable throwable, String messageHtmlString) {
		boolean isErrorEmailDisabledHere = false;
		try {
			boolean sendErrorEmail = !AplosContextListener.getAplosContextListener().isDebugMode();
			if( request != null ) {
				sendErrorEmail = !CommonUtil.isLocalHost(request.getServletContext());
			}
			if( sendErrorEmail ) {
				/*
				 * Check the sessionTemp first because checking the tab session calls code which may 
				 * have triggered the error in the first place.  This can happen if a change has been 
				 * made to CommonConfiguration db structure for example.
				 */
				if( JSFUtil.getSessionTemp() == null || JSFUtil.getTabSession() == null ) {
					sendErrorEmail = ApplicationUtil.getAplosContextListener().isErrorEmailActivated();
				} else {
					Boolean isErrorEmailActivated = (Boolean) JSFUtil.getTabSession().get( AplosScopedBindings.ERROR_EMAIL_ACTIVATED );
					if( isErrorEmailActivated == null ) {
						sendErrorEmail = true;
					} else {
						sendErrorEmail = isErrorEmailActivated;
					}
				}
			}
			if (sendErrorEmail) {
				if( JSFUtil.getSessionTemp() == null || JSFUtil.getTabSession() == null ) {
					ApplicationUtil.getAplosContextListener().setErrorEmailActivated( false );
					isErrorEmailDisabledHere = true;
				} else {
					JSFUtil.getTabSession().put( AplosScopedBindings.ERROR_EMAIL_ACTIVATED, false );
					isErrorEmailDisabledHere = true;
				}
				
				Calendar cal = new GregorianCalendar();
				StringBuffer datesRemoved = new StringBuffer();
				cal.setTime( new Date() );
				cal.add(Calendar.MINUTE, -5 );
				while( sentMessages.size() > 0  && sentMessages.peek().before( cal.getTime() ) ) {
					datesRemoved.append( FormatUtil.formatDateTime( sentMessages.peek(), true ) ).append( ", " );
					sentMessages.remove();
				}
				if( sentMessages.size() < 25 ) {
		  			List<Throwable> throwableList = new ArrayList<Throwable>();
		  			throwableList.add( throwable );
		  			int maxCauses = 10;
		  			int maxStackTraceLines = 250;
		  			int count = 0;
		  			while( (throwable = throwable.getCause()) != null && count++ < maxCauses ) {
		    			throwableList.add( throwable );
		  			}

		  			StringBuffer stackTraceStrBuf = new StringBuffer();
		  			stackTraceStrBuf.append(messageHtmlString);

					AplosRequestContext aplosRequestContext = JSFUtil.getAplosRequestContext( request );
					HttpSession session = JSFUtil.getSessionTemp(false);
					if( session != null ) {
						stackTraceStrBuf.append("<br/>Current session: ").append( session.getId() );
					}
					if( request != null ) {
						stackTraceStrBuf.append("<br/>Page created: ").append( CommonUtil.getStringOrEmpty( request.getParameter( AplosScopedBindings.DATE_TIME_PAGE_CREATED ) ) );
						stackTraceStrBuf.append("<br/>Last session ID: ").append( CommonUtil.getStringOrEmpty( request.getParameter( AplosScopedBindings.LAST_SESSION_ID ) ) );
						stackTraceStrBuf.append("<br/>Session last accessed: ").append( CommonUtil.getStringOrEmpty( request.getParameter( AplosScopedBindings.SESSION_LAST_ACCESSED ) ) );
						stackTraceStrBuf.append("<br/>Session max interval: ").append( CommonUtil.getStringOrEmpty( request.getParameter( AplosScopedBindings.SESSION_MAX_INTERVAL ) ) );
					}
					stackTraceStrBuf.append("<br/>Before date: ").append( FormatUtil.formatDate( cal.getTime() ) );
					stackTraceStrBuf.append("<br/>Dates removed: ").append( datesRemoved );
					stackTraceStrBuf.append("<br/>Sent messages size: ").append( sentMessages.size() );
					if( aplosRequestContext != null ) {
						stackTraceStrBuf.append("<br/>Original Url: ").append( CommonUtil.getStringOrEmpty( aplosRequestContext.getOriginalUrlWithQueryString() ));
						stackTraceStrBuf.append("<br/>Current Url: ").append( CommonUtil.getStringOrEmpty( aplosRequestContext.getCurrentUrlWithQueryString() ));
					}
					
		  			stackTraceStrBuf.append("<br/>Error Occured: ");
		  			//once you forward error emails i dont have the time/date information so ive added it
		  			stackTraceStrBuf.append(new Date().toString());
		  			stackTraceStrBuf.append("<br/><br/>");
		  			if( throwableList.size() == maxCauses ) {
		  				stackTraceStrBuf.append("<b>Throwable cause limit of " + maxCauses + " reached</b><br/><br/>");
		  			}
		  			for( int i = Math.min( maxCauses, throwableList.size() - 1); i > -1; i-- ) {
		    			StackTraceElement stackElements[] = throwableList.get( i ).getStackTrace();
		    			stackTraceStrBuf.append(  StringEscapeUtils.escapeHtml( throwableList.get( i ).toString() ) + "<br/>" );
		    			for( int j = 0, p = Math.min( maxStackTraceLines, stackElements.length ); j < p; j++ ) {
		    				stackTraceStrBuf.append( StringEscapeUtils.escapeHtml( stackElements[ j ].toString() ) + "<br/>" );
		    				if( j == (maxStackTraceLines - 1) ) {
		    	  				stackTraceStrBuf.append("<br/><b>Stack trace line limit of " + maxStackTraceLines + " reached, " + stackElements.length + " lines available</b><br/><br/>");
		    				}
		    			}
		  				stackTraceStrBuf.append(  "<br/><br/>" );
		  			}

		  			try {
			  			if( request != null ) {
			  				addUrlHistory( stackTraceStrBuf, request );
			  				addRequestAttributes( stackTraceStrBuf, request );
			  				addViewAttributes( stackTraceStrBuf, request );
			  				addSessionAttributes( stackTraceStrBuf, request );
			  				addTabSessionAttributes(stackTraceStrBuf, request);
			  				addRequestParameters( stackTraceStrBuf, request );
			  				addRequestHeaders( stackTraceStrBuf, request );
			  			}
		  			} catch( Exception ex ) {
		  				/*
		  				 * Fail safe
		  				 */
		  				stackTraceStrBuf.append( "Error occured created Error email sections " + ex.getMessage() );
		  				StackTraceElement stackElements[] = ex.getStackTrace();
		    			stackTraceStrBuf.append(  StringEscapeUtils.escapeHtml( ex.toString() ) + "<br/>" );
		    			for( int j = 0, p = Math.min( maxStackTraceLines, stackElements.length ); j < p; j++ ) {
		    				stackTraceStrBuf.append( StringEscapeUtils.escapeHtml( stackElements[ j ].toString() ) + "<br/>" );
		    				if( j == (maxStackTraceLines - 1) ) {
		    	  				stackTraceStrBuf.append("<br/><b>Stack trace line limit of " + maxStackTraceLines + " reached, " + stackElements.length + " lines available</b><br/><br/>");
		    				}
		    			}
		  			}

					String errorEmail = contextListener.getErrorEmailAddress();
					String emailSubject;
					if( request != null ) {
						emailSubject = "Error " + new URL(request.getRequestURL().toString()).getHost();
					} else if( contextListener.getWebsiteList().size() > 0 ) {
						emailSubject = "Error " + contextListener.getWebsiteList().get( 0 ).getPrimaryHostName();
					} else {
						emailSubject = "Error " + contextListener.getServerUrl();
					}
					AplosEmail aplosEmail = new AplosEmail( emailSubject, stackTraceStrBuf.toString(), false );
					aplosEmail.addToAddress( errorEmail );
					aplosEmail.setFromAddress( errorEmail );
					CommonConfiguration commonConfiguration = CommonConfiguration.getCommonConfiguration(); 
					if( commonConfiguration != null ) {
						aplosEmail.setMailServerSettings( CommonConfiguration.getCommonConfiguration().getErrorMailServerSettings() );
					} else {
						/*
						 * This used to create a default mailServerSettings object with standard
						 * username and password but was removed when we went to open source
						 */
					}
					
					aplosEmail.setSendingViaQueue(false);
					aplosEmail.sendAplosEmailToQueue( false );
					sentMessages.add( new Date() );
				}
			}	
		} catch( Exception ex ) {
			// You don't want to send another email here if there's an error in the code.
			ex.printStackTrace();
			if( throwable != null ) {
				throwable.printStackTrace();
			}
		} finally {
			if( isErrorEmailDisabledHere ) {
				if( JSFUtil.getSessionTemp() == null || JSFUtil.getTabSession() == null ) {
					ApplicationUtil.getAplosContextListener().setErrorEmailActivated( true );
				} else {
					JSFUtil.getTabSession().put( AplosScopedBindings.ERROR_EMAIL_ACTIVATED, true );
				}
			}
		}
	}

	public static void addUrlHistory( StringBuffer stackTraceStrBuf, HttpServletRequest request ) {
		List<BackingPageState> historyList = JSFUtil.getHistoryList();
		if( historyList != null ) {
			stackTraceStrBuf.append(  "<br/><br/><b>URL HISTORY</b><table>" );
			Object tempAssociateBean;

			if( JSFUtil.getFromTabSession( AplosScopedBindings.REQUEST_COMMAND_LIST ) != null  ) {
				List<String> requestCommandParameters = (List<String>) JSFUtil.getFromTabSession( AplosScopedBindings.REQUEST_COMMAND_LIST );
				stackTraceStrBuf.append( "<tr><td>");
				stackTraceStrBuf.append( StringUtils.join( requestCommandParameters, "," ) );
				stackTraceStrBuf.append( "</td></tr>");
			}
			for( int i = historyList.size() - 1; i >=0; i-- ) {
				BackingPageState tempBackingPageState = historyList.get( i );
				stackTraceStrBuf.append( "<tr><td>" );

				if( tempBackingPageState.getStateSetDate() != null ) {
					stackTraceStrBuf.append( tempBackingPageState.getStateSetDate() );
				}
				stackTraceStrBuf.append( "</td><td>");

				stackTraceStrBuf.append( tempBackingPageState.getRedirectUrl() );
				stackTraceStrBuf.append( "</td><td>");

				stackTraceStrBuf.append( tempBackingPageState.getOriginalUrl() );
				stackTraceStrBuf.append( "</td><td>");

				tempAssociateBean = tempBackingPageState.getAssociateBean();
				if( tempAssociateBean instanceof Long ) {
					BackingPage backingPage = (BackingPage) JSFUtil.resolveVariable(tempBackingPageState.getBackingPageClass());
					if( backingPage != null ) {
						stackTraceStrBuf.append( backingPage.getBeanDao().getBeanClass().getSimpleName() );
						stackTraceStrBuf.append( "</td><td>");
						stackTraceStrBuf.append( String.valueOf( tempAssociateBean ) );
					}
				} else if( tempAssociateBean instanceof AplosBean ) {
					stackTraceStrBuf.append( tempAssociateBean.getClass().getSimpleName() );
					stackTraceStrBuf.append( "</td><td>" );
					stackTraceStrBuf.append( tempBackingPageState.getAssociatedBeanId() );
					stackTraceStrBuf.append( " (" );
					stackTraceStrBuf.append( ((AplosAbstractBean) tempAssociateBean).getId() );
					stackTraceStrBuf.append( ")" );
				} else {
					stackTraceStrBuf.append( "</td><td>" );
				}
				stackTraceStrBuf.append( "</td><td>");
				if( tempBackingPageState.getRequestCommandParameters() != null && tempBackingPageState.getRequestCommandParameters().size() > 0 ) {
					stackTraceStrBuf.append( StringUtils.join( tempBackingPageState.getRequestCommandParameters(), "," ) );
				}
				stackTraceStrBuf.append( "</td></tr>" );
			}
			stackTraceStrBuf.append( "</table>" );
		}
	}

	public static void addRequestAttributes( StringBuffer stackTraceStrBuf, HttpServletRequest request ) {
		stackTraceStrBuf.append(  "<br/><br/><b>REQUEST ATTRIBUTES</b><table>" );
		Enumeration<String> requestIter = request.getAttributeNames();
		List<String> requestKeyList = new ArrayList<String>();
		while( requestIter.hasMoreElements() ) {
			requestKeyList.add( requestIter.nextElement() );
		}
		Collections.sort( requestKeyList );

		for( int i = 0, n = requestKeyList.size(); i < n; i++ ) {
			stackTraceStrBuf.append( "<tr><td>" );
			stackTraceStrBuf.append( requestKeyList.get( i ) );
			stackTraceStrBuf.append( "</td><td>");

			Object attrValue = request.getAttribute( requestKeyList.get( i ) );
			if( attrValue != null ) {
				stackTraceStrBuf.append( attrValue.getClass().getSimpleName() );
				if( attrValue instanceof AplosAbstractBean ) {
					stackTraceStrBuf.append( "</td><td>");
					stackTraceStrBuf.append( ApplicationUtil.getIdFromProxy( ((AplosAbstractBean) attrValue) ) );
				}
			} else {
				stackTraceStrBuf.append( "null" );
			}
			stackTraceStrBuf.append( "</td></tr>" );
		}
		stackTraceStrBuf.append( "</table>" );
	}

	public static void addViewAttributes( StringBuffer stackTraceStrBuf, HttpServletRequest request ) {
		Map<String,Object> viewMap = JSFUtil.getViewMap();
		if( viewMap != null ) {
			stackTraceStrBuf.append(  "<br/><br/><b>VIEW ATTRIBUTES</b><table>" );
			List<String> viewKeyList = new ArrayList<String>();
			for( String key : viewMap.keySet() ) {
				viewKeyList.add( key );
			}
			Collections.sort( viewKeyList );
	
			for( int i = 0, n = viewKeyList.size(); i < n; i++ ) {
				stackTraceStrBuf.append( "<tr><td>" );
				stackTraceStrBuf.append( viewKeyList.get( i ) );
				stackTraceStrBuf.append( "</td><td>");
	
				Object attrValue = viewMap.get( viewKeyList.get( i ) );
				if( attrValue != null ) {
					stackTraceStrBuf.append( attrValue.getClass().getSimpleName() );
					if( attrValue instanceof AplosAbstractBean ) {
						stackTraceStrBuf.append( "</td><td>");
						stackTraceStrBuf.append( ApplicationUtil.getIdFromProxy( ((AplosAbstractBean) attrValue) ) );
					}
				} else {
					stackTraceStrBuf.append( "null" );
				}
				stackTraceStrBuf.append( "</td></tr>" );
			}
			stackTraceStrBuf.append( "</table>" );
		}
	}

	public static void addSessionAttributes( StringBuffer stackTraceStrBuf, HttpServletRequest request ) {
		HttpSession session = request.getSession();
		if( session != null ) {
			stackTraceStrBuf.append(  "<br/><br/><b>SESSION ATTRIBUTES</b><table>" );
			Enumeration<String> sessionIter = session.getAttributeNames();

			List<String> sessionKeyList = new ArrayList<String>();
			while( sessionIter.hasMoreElements() ) {
				sessionKeyList.add( sessionIter.nextElement() );
			}
			Collections.sort( sessionKeyList );

			for( int i = 0, n = sessionKeyList.size(); i < n; i++ ) {
				stackTraceStrBuf.append( "<tr valign='top'><td>" );
				stackTraceStrBuf.append( sessionKeyList.get( i ) );
				stackTraceStrBuf.append( "</td><td>");

				Object attrValue = session.getAttribute( sessionKeyList.get( i ) );
				if( attrValue != null ) {
					stackTraceStrBuf.append( attrValue.getClass().getSimpleName() );
					if( attrValue instanceof AplosAbstractBean ) {
						stackTraceStrBuf.append( "</td><td>");
	  					stackTraceStrBuf.append( ApplicationUtil.getIdFromProxy( ((AplosAbstractBean) attrValue) ) );
					}
				} else {
					stackTraceStrBuf.append( "null" );
				}
				stackTraceStrBuf.append( "</td></tr>" );
			}
			stackTraceStrBuf.append( "</table>" );
		}
	}

	public static void addTabSessionAttributes( StringBuffer stackTraceStrBuf, HttpServletRequest request ) {
		TabSessionMap tabSessionMap = (TabSessionMap) request.getSession().getAttribute(AplosScopedBindings.TAB_SESSION);
		if( tabSessionMap != null ) {
			 Map<String, Map[]> tabSessionMapMap = tabSessionMap.getTabSessionMapMap();
			stackTraceStrBuf.append(  "<br/><br/><b>TAB SESSION ATTRIBUTES</b><table>" );
			for( String tempKey : tabSessionMapMap.keySet() ) {
				Map[] tabSessionMapAry = tabSessionMapMap.get(tempKey);

				stackTraceStrBuf.append(  "<br/><br/><b>FRONTEND TAB SESSION ATTRIBUTES " ).append( tempKey ).append( "</b><table>" );
				formatMapValues(stackTraceStrBuf, tabSessionMapAry[ TabSessionMap.FRONT_END_TAB_SESSION_MAP ] );

				stackTraceStrBuf.append(  "<br/><br/><b>BACKEND TAB SESSION ATTRIBUTES " ).append( tempKey ).append( "</b><table>" );
				formatMapValues(stackTraceStrBuf, tabSessionMapAry[ TabSessionMap.BACK_END_TAB_SESSION_MAP ] );
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void formatMapValues( StringBuffer stackTraceStrBuf, Map<String, Object> map ) {
		List<String> sessionKeyList = new ArrayList( map.keySet() );
		Collections.sort( sessionKeyList );

		for( int i = 0, n = sessionKeyList.size(); i < n; i++ ) {
			stackTraceStrBuf.append( "<tr valign='top'><td>" );
			stackTraceStrBuf.append( sessionKeyList.get( i ) );
			stackTraceStrBuf.append( "</td><td>");

			Object attrValue = map.get( sessionKeyList.get( i ) );
			if( attrValue != null ) {
				stackTraceStrBuf.append( attrValue.getClass().getSimpleName() );
				if( attrValue instanceof AplosAbstractBean ) {
					stackTraceStrBuf.append( "</td><td>");
					stackTraceStrBuf.append( ApplicationUtil.getIdFromProxy( ((AplosAbstractBean) attrValue) ) );
				}
			}
			stackTraceStrBuf.append( "</td></tr>" );
		}
		stackTraceStrBuf.append( "</table>" );
	}

	public static void addRequestParameters( StringBuffer stackTraceStrBuf, HttpServletRequest request ) {
		stackTraceStrBuf.append(  "<br/><br/><b>REQUEST PARAMETERS</b><table>" );
		Enumeration<String> requestParameterIter = request.getParameterNames();
		List<String> requestParamsList = new ArrayList<String>();
		while( requestParameterIter.hasMoreElements() ) {
			requestParamsList.add( requestParameterIter.nextElement() );
		}
		Collections.sort( requestParamsList );

		for( int i = 0, n = requestParamsList.size(); i < n; i++ ) {
			String attrName = requestParamsList.get( i );
			boolean isTrigger = attrName.equals( request.getParameter( attrName ) );

			stackTraceStrBuf.append( "<tr><td>" );
			if( isTrigger ) {
				stackTraceStrBuf.append( "<b>" );
			}
			stackTraceStrBuf.append( attrName );
			if( isTrigger ) {
				stackTraceStrBuf.append( "</b>" );
			}
			stackTraceStrBuf.append( "</td><td>");
			stackTraceStrBuf.append( request.getParameter( attrName ) );
			stackTraceStrBuf.append( "</td></tr>" );
		}
		stackTraceStrBuf.append( "</table>" );
	}

	public static void addRequestHeaders( StringBuffer stackTraceStrBuf, HttpServletRequest request ) {
		stackTraceStrBuf.append(  "<br/><br/><b>REQUEST HEADERS</b><table>" );
		Enumeration<String> requestHeaderIter = request.getHeaderNames();
		List<String> requestHeaderList = new ArrayList<String>();
		while( requestHeaderIter.hasMoreElements() ) {
			requestHeaderList.add( requestHeaderIter.nextElement() );
		}
		Collections.sort( requestHeaderList );

		for( int i = 0, n = requestHeaderList.size(); i < n; i++ ) {
			String attrName = requestHeaderList.get( i );
			boolean isTrigger = attrName.equals( request.getParameter( attrName ) );

			stackTraceStrBuf.append( "<tr><td>" );
			if( isTrigger ) {
				stackTraceStrBuf.append( "<b>" );
			}
			stackTraceStrBuf.append( attrName );
			if( isTrigger ) {
				stackTraceStrBuf.append( "</b>" );
			}
			stackTraceStrBuf.append( "</td><td>");
			stackTraceStrBuf.append( request.getHeader( attrName ) );
			stackTraceStrBuf.append( "</td></tr>" );
		}
		stackTraceStrBuf.append( "</table>" );
	}
}
