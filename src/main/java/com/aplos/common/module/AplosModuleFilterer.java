package com.aplos.common.module;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;

import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.Currency;
import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.beans.communication.InboundSms;
import com.aplos.common.enums.CommonWorkingDirectory;
import com.aplos.common.interfaces.BulkEmailSource;
import com.aplos.common.listeners.AplosContextListener;
import com.aplos.common.utils.JSFUtil;
import com.aplos.common.utils.UserLevelUtil;

public class AplosModuleFilterer {
	private List<AplosModule> aplosModuleList;
	private AplosContextListener aplosContextListener;

	public AplosModuleFilterer( AplosContextListener aplosContextListener ) {
		this.aplosContextListener = aplosContextListener;
		aplosModuleList = aplosContextListener.getAplosModuleList();
	}

	public void newTableAdded( Class<?> tableClass ) {
		for( int i = 0, n = aplosModuleList.size(); i < n; i++ ) {
			if( aplosModuleList.get( i ).getDatabaseLoader() != null ) {
				aplosModuleList.get( i ).getDatabaseLoader().newTableAdded( tableClass );
			}
		}
	}
	
	public void registerInboundSms( InboundSms inboundSms ) {
		for( int i = 0, n = aplosModuleList.size(); i < n; i++ ) {
			aplosModuleList.get( i ).registerInboundSms( inboundSms );
		}
	}

	public void clearCache() {
		for( int i = 0, n = aplosModuleList.size(); i < n; i++ ) {
			aplosModuleList.get( i ).clearCache();
		}
	}

	public boolean getFaceletEvent( String url ) {
		Boolean tempResult;
		for( int i = 0, n = aplosModuleList.size(); i < n; i++ ) {
			tempResult = aplosModuleList.get( i ).getFaceletEvent(url);
			if( tempResult != null ) {
				return tempResult;
			}
		}
		return false;
	}

	public boolean processHardDeleteEvent( AplosAbstractBean aplosAbstractBean ) {
		Boolean tempResult;
		for( int i = 0, n = aplosModuleList.size(); i < n; i++ ) {
			tempResult = aplosModuleList.get( i ).processHardDeleteEvent(aplosAbstractBean);
			if( tempResult != null ) {
				return tempResult;
			}
		}
		return false;
	}

	public void registerIncomingEmail( AplosEmail aplosEmail ) {
		for( int i = 0, n = aplosModuleList.size(); i < n; i++ ) {
			aplosModuleList.get( i ).registerIncomingEmail( aplosEmail );
		}
	}

	public Currency updateSessionCurrency( HttpServletRequest request ) {
		Currency currency;
		for( int i = 0, n = aplosModuleList.size(); i < n; i++ ) {
			if( (currency = aplosModuleList.get( i ).updateSessionCurrency(request)) != null ) {
				return currency;
			}
		}
//		return HibernateUtil.getImplementation( CommonConfiguration.getCommonConfiguration().getDefaultCurrency(), true, true );
		return CommonConfiguration.getCommonConfiguration().getDefaultCurrency();

	}

	public String getJDynamiTeValue(  String variableKey, AplosEmail aplosEmail, BulkEmailSource bulkEmailSource  ) {
		String variableValue = null;
		for( int i = 0, n = aplosModuleList.size(); i < n; i++ ) {
			variableValue = aplosModuleList.get( i ).getJDynamiTeValue( variableKey, aplosEmail, bulkEmailSource );
			if( variableValue != null ) {
				return variableValue;
			}
		}
		return null;
	}

	public void tabSessionCreated(  HttpSession session, Map<String, Object> tabSessionMap  ) {
		for( int i = 0, n = aplosModuleList.size(); i < n; i++ ) {
			aplosModuleList.get( i ).tabSessionCreated(session, tabSessionMap);
		}

	}
	
	public boolean determineIsFrontEnd(boolean defaultValue) {
		return determineIsFrontEnd(JSFUtil.getRequest(), defaultValue);
	}
	
	public boolean determineIsFrontEnd(HttpServletRequest request, boolean defaultValue) {
		return determineIsFrontEnd(JSFUtil.getAplosContextOriginalUrl(request), defaultValue);
	}
	
	public boolean determineIsFrontEnd(String requestUrl, boolean defaultValue) {
		Boolean isFrontEnd;
		for( int i = 0, n = aplosModuleList.size(); i < n; i++ ) {
			if( (isFrontEnd = aplosModuleList.get( i ).determineIsFrontEnd(requestUrl)) != null ) {
				return isFrontEnd;
			}
		}
		
		return defaultValue;
	}
	
	public boolean rewriteUrl( HttpServletRequest request, HttpServletResponse response ) {
		Boolean urlRewritten;
		try {
			for( int i = 0, n = aplosModuleList.size(); i < n; i++ ) {
				if( (urlRewritten = aplosModuleList.get( i ).rewriteUrl( request, response )) != null ) {
					return urlRewritten;
				}
			}
		} catch( IOException ioex ) {
			aplosContextListener.handleError( request, response, ioex, aplosContextListener.getCurrentUrlsHtml(),true );
		} catch( ServletException sEx ) {
			aplosContextListener.handleError( request, response, sEx, aplosContextListener.getCurrentUrlsHtml(), true );
		}
		
		return false;
	}

	public SelectItem[] getCountrySelectItems() {
		SelectItem[] selectItems;
		for( int i = 0, n = aplosModuleList.size(); i < n; i++ ) {
			if( (selectItems = aplosModuleList.get( i ).getCountrySelectItems()) != null ) {
				return selectItems;
			}
		}
		return new SelectItem[ 0 ];
	}

	public UserLevelUtil createUserLevelUtil() {
		UserLevelUtil userLevelUtil;
		for( int i = 0, n = aplosModuleList.size(); i < n; i++ ) {
			if( (userLevelUtil = aplosModuleList.get( i ).createUserLevelUtil()) != null ) {
				return userLevelUtil;
			}
		}
		return null;
	}

	public URL getResourceFilterUrl(String resourceName, String contextPath) {
		URL url;
		for( int i = 0, n = aplosModuleList.size(); i < n; i++ ) {
			if( (url = aplosModuleList.get( i ).getResourceFilterUrl(resourceName, contextPath)) != null ) {
				return url;
			}
		}
		return null;
	}
}
