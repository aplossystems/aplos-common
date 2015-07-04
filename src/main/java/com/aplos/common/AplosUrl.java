package com.aplos.common;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.aplos.common.appconstants.AplosAppConstants;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.Website;
import com.aplos.common.enums.SslProtocolEnum;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;


public class AplosUrl {
	public enum Protocol {
		HTTP,
		HTTPS;
	}
	private String scheme = null;
    private String host = null;
    private String path = null;
    private String contextPath = null;
    private boolean isAddingLanguage = false;
    private boolean isCompletingResponseAfterRedirect = true;
    private Map<String,String> queryParameterMap = new HashMap<String,String>();

    public AplosUrl() {
    	CommonConfiguration commonConfiguration = CommonConfiguration.getCommonConfiguration();
    	if( commonConfiguration != null && commonConfiguration.getIsInternationalizedApplication() ) {
    		setAddingLanguage( true );
    	}
    }

    public AplosUrl( String url ) {
    	this( url, false );
    }

    public AplosUrl( String url, String queryParameters ) {
    	this( url );
    	this.addQueryParameters(queryParameters);
    }

    public AplosUrl( String url, boolean includesHost ) {
    	if( CommonConfiguration.getCommonConfiguration().getIsInternationalizedApplication() ) {
    		setAddingLanguage( true );
    	}

    	if( url.contains( "?" ) ) {
    		String queryString = url.substring( url.indexOf( "?" ) + 1, url.length() );
    		addQueryParameters( queryString );
    		url = url.substring( 0, url.indexOf( "?" ) );
    	}

    	if( url.indexOf( "/", 1 ) != -1 && includesHost ) {
        	if( url.contains( "://" ) ) {
        		setScheme( url.substring( 0, url.indexOf( "://" ) ) );
            	url = url.substring( url.indexOf( "://" ) + 3 );
        	}

    		setHost( url.substring( 0, url.indexOf( "/" ) ) );
    		setPath( url.substring( url.indexOf( "/" ) + 1 ) );
    	} else {
    		setPath( url );
    	}
    }

    public AplosUrl( String url, boolean includesHost, String queryParameters ) {
    	this( url, includesHost );
    	this.addQueryParameters(queryParameters);
    }

    public AplosUrl( String url, boolean includesHost, SslProtocolEnum sslProtocolEnum, String queryParameters ) {
    	this( url, includesHost, queryParameters );
    	this.setScheme(sslProtocolEnum);
    }

    public void setHost( Website website ) {
    	setHost( website, false );
    }

    public void setHost( Website website, boolean ignoreLocalHost ) {
    	if( website != null ) {
			String websitePrimaryHost = website.getPrimaryHostName( ignoreLocalHost );
			if ( !ignoreLocalHost && JSFUtil.isLocalHost() ) {
				addContextPath();
			} else if( websitePrimaryHost != null ) {
				if( !websitePrimaryHost.startsWith( "www." ) && !CommonUtil.isNullOrEmpty( website.getSubdomain() ) ) {
					websitePrimaryHost = website.getSubdomain() + "." + websitePrimaryHost;
				}
			}
			setHost( websitePrimaryHost );
    	}
    }

    @Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public String toString() {
    	StringBuffer queryString = new StringBuffer();
    	boolean questionMarkAdded = false;
    	Set<Entry<String,String>> entrySet = new HashSet<Entry<String,String>>(queryParameterMap.entrySet());
    	if( !queryParameterMap.containsKey( AplosAppConstants.LANG ) && isAddingLanguage ) {
    		entrySet.add( new AbstractMap.SimpleEntry( AplosAppConstants.LANG, CommonUtil.getContextLocale().getLanguage() ) );
    	}
    	try {
	    	for( Entry<String,String> entry : entrySet ) {
	    		if( questionMarkAdded ) {
	    			queryString.append( "&" );
	    		} else {
	    			queryString.append( "?" );
	    			questionMarkAdded = true;
	    		}
	    		queryString.append( entry.getKey() ).append( "=" ).append( URLEncoder.encode( entry.getValue(), "UTF-8" ) );
	    	}
    	} catch( UnsupportedEncodingException ueex ) {
    		ApplicationUtil.getAplosContextListener().handleError( ueex );
    	}
    	StringBuffer urlBuff = new StringBuffer();
    	if( getScheme() != null && !getScheme().contains( "://" ) && getHost() != null ) {
    		urlBuff.append( getScheme() ).append( "://" );
    	}

		if( Protocol.HTTPS.name().toLowerCase().equals( getScheme() ) && getHost() != null && getHost().contains( ":8080" ) ) {
			setHost( getHost().replace( ":8080", ":8443" ) );
		}
    	if( getHost() != null ) {
    		urlBuff.append( getHost() );
    	}
    	if( !CommonUtil.isNullOrEmpty( getContextPath() ) ) {
    		urlBuff.append( "/" ).append( getContextPath().replace( "/", "" ) );
    	}

    	if( getPath() != null ) {
    		if( !getPath().startsWith( "/" ) && urlBuff.length() > 0 ) {
    			urlBuff.append( "/" );
    		}
			urlBuff.append( getPath() );
    	}

    	return urlBuff.append( queryString ).toString();
    }

	public void addWindowId() {
		addWindowId(JSFUtil.getWindowId());
	}

	public void addWindowId(String windowId) {
		if( !CommonUtil.isNullOrEmpty( windowId ) && !windowId.equals( "null") && CommonConfiguration.getCommonConfiguration().isUsingWindowId() ) {
			addQueryParameter( AplosAppConstants.WINDOW_ID, windowId );
		}
	}

	public void addContextPath( boolean addContextPath ) {
		if( addContextPath ) {
			setContextPath( JSFUtil.getContextPath() );
		} else {
			setContextPath( null );
		}
	}

	public void addContextPath() {
		setContextPath( JSFUtil.getContextPath() );
	}

	public void addCurrentQueryString() {
		addQueryParameters( JSFUtil.getRequest().getQueryString() );
	}

    public void addQueryParameters( String queryParameters ) {
    	if( queryParameters != null ) {
	    	String[] queries = queryParameters.split( "&" );
			for( int i = 0, n = queries.length; i < n; i++ ) {
				String[] queryParts = queries[ i ].split( "=" );
				if( queryParts.length == 2 ) {
					queryParameterMap.put( queryParts[ 0 ], queryParts[ 1 ] );
				}
			}
    	}
    }

    /**
     *
     * @param key
     * @param value
     * @return
     * Returns the old value of the key or null if there
     * was no old value.
     */
    public String addQueryParameter( String key, String value ) {
    	return queryParameterMap.put( key, value );
    }

    public String addQueryParameter( String key, Object value ) {
    	if( value != null ) {
    		return queryParameterMap.put( key, String.valueOf( value ) );
    	} else {
    		return null;
    	}
    }

    public String addQueryParameter( String key, AplosAbstractBean aplosAbstractBean ) {
    	if( aplosAbstractBean == null || aplosAbstractBean.getId() == null ) {
    		return null;
    	} else {
    		return queryParameterMap.put( key, String.valueOf( aplosAbstractBean.getId() ) );
    	}
    }

	public String getScheme() {
		return scheme;
	}
	public void setScheme(SslProtocolEnum sslProtocolEnum) {
		if( SslProtocolEnum.FORCE_HTTP.equals( sslProtocolEnum ) ) {
			setScheme( Protocol.HTTP );
		} else if( SslProtocolEnum.FORCE_SSL.equals( sslProtocolEnum ) && !JSFUtil.isLocalHost() ) {
			setScheme( Protocol.HTTPS );
		} else if( JSFUtil.getRequest() != null ) {
			setScheme( JSFUtil.getRequest().getScheme() );
		} else {
			setScheme( Protocol.HTTP );
		}
	}
	public void setScheme(Protocol protocol) {
		this.scheme = protocol.name().toLowerCase();
	}
	public void setScheme(String protocol) {
		this.scheme = protocol;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}

	public boolean isAddingLanguage() {
		return isAddingLanguage;
	}

	public void setAddingLanguage(boolean isAddingLanguage) {
		this.isAddingLanguage = isAddingLanguage;
	}

	public String getContextPath() {
		return contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public boolean isCompletingResponseAfterRedirect() {
		return isCompletingResponseAfterRedirect;
	}

	public void setCompletingResponseAfterRedirect(
			boolean isCompletingResponseAfterRedirect) {
		this.isCompletingResponseAfterRedirect = isCompletingResponseAfterRedirect;
	}
}
