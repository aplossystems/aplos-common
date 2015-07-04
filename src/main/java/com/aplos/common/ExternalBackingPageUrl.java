package com.aplos.common;

import com.aplos.common.backingpage.BackingPage;
import com.aplos.common.beans.Website;
import com.aplos.common.enums.SslProtocolEnum;
import com.aplos.common.utils.ApplicationUtil;

public class ExternalBackingPageUrl extends BackingPageUrl {

	public ExternalBackingPageUrl(Class<? extends BackingPage> backingPageClass) {
		this( Website.getCurrentWebsiteFromTabSession(), backingPageClass );
	}

	public ExternalBackingPageUrl(Website website, Class<? extends BackingPage> backingPageClass ) {
		this( website, backingPageClass, true );
	}

	public ExternalBackingPageUrl( Class<? extends BackingPage> backingPageClass, boolean addExtension ) {
		this( Website.getCurrentWebsiteFromTabSession(), backingPageClass, addExtension );
	}

	public ExternalBackingPageUrl( Website site, Class<? extends BackingPage> backingPageClass, boolean addExtension ) {
		this( site, backingPageClass, addExtension, true );
	}
	
	public ExternalBackingPageUrl( Website site, Class<? extends BackingPage> backingPageClass, boolean addExtension, boolean useSessionLogin ) {
		super( site, backingPageClass, addExtension );
		setHost( site );
		SslProtocolEnum sslProtocolEnum = ApplicationUtil.getAplosContextListener().getBackingPageMetaData(backingPageClass).getSsLProtocol();
		setScheme( sslProtocolEnum );
    	// It only does something when it's true so don't add it to the URL otherwise
		if( useSessionLogin ) {
			addQueryParameter("sessionLogin", useSessionLogin);
		}
	}
	
}
