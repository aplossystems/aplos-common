package com.aplos.common.application;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;

import com.aplos.common.utils.JSFUtil;
import com.sun.faces.facelets.impl.DefaultResourceResolver;

public class TemplateResolver extends DefaultResourceResolver {
	private static Logger logger = Logger.getLogger( TemplateResolver.class );

	@Override
	public URL resolveUrl(String path) {
        URL url = super.resolveUrl(path);

//        System.err.println( "TemplateResolver: " + path );
        if ( url == null ) {
            /* classpath resources don't start with / */
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
        	if ( path.equals( "/" ) ) {
            	path = "/index.xhtml";
            }

            String queryString = null;
            if( path.indexOf( "?" ) != -1 ) {
            	queryString = path.substring( path.indexOf( "?" ) );
            	path = path.replace( queryString, "" );
            }

            if ( path.endsWith( ".xhtml" ) ) {
            	boolean allowOverride = true;
            	if( queryString != null && queryString.contains( "allowOverride=false" ) ) {
            		allowOverride = false;
            	}
            	url = JSFUtil.checkFileLocations( path, "/resources/faceletviews", allowOverride );
            }

            //TODO: Can this be removed with the new file system in place?
            //  This code allows the resolver to go through the
            //  containers filters again, this is especially important
            //  for the /content/ pages which are used in the CMS
        	if( url == null ) {
	    		logger.debug( "Resolving URL " + path );
	    		if( queryString != null ) {
	    			path = path + queryString;
	    		}
	            path = path.replace( "/.xhtml", "/" );
	            try {
	            	String contextRoot = JSFUtil.getContextPath();
	            	if (contextRoot.equals("/")) { contextRoot = ""; }

	            	//SystemUser currentUser = JSFUtil.getCurrentUser();
	            	//String uid = (currentUser != null ? currentUser.getId().toString() : null);
	            	// TODO this looks like it's a fairly large security leak
	        		URL currentUrl = new URL(JSFUtil.getRequest().getRequestURL().toString());
	        		String newUrlString = "http://" + currentUrl.getHost() + ":" + currentUrl.getPort() + contextRoot + path;
					url = new URL(newUrlString);
				} catch( MalformedURLException e ) {
					e.printStackTrace();
				}
        	}
        }

        return url;
    }

}
