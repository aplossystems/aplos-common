package com.aplos.common;

import com.aplos.common.backingpage.BackingPage;
import com.aplos.common.beans.Website;
import com.aplos.common.utils.CommonUtil;

public class BackingPageUrl extends AplosUrl {
	private Class<? extends BackingPage> backingPageClass;
	private String sitePackageName;
	private boolean isAddingExtension;

	public BackingPageUrl(Class<? extends BackingPage> backingPageClass) {
		this( Website.getCurrentWebsiteFromTabSession(), backingPageClass, true );
	}
	
	public BackingPageUrl(Class<? extends BackingPage> backingPageClass, String queryParameters) {
		this( Website.getCurrentWebsiteFromTabSession(), backingPageClass, true );
		addQueryParameters(queryParameters);
	}

	public BackingPageUrl( Class<? extends BackingPage> backingPageClass, boolean addExtension ) {
		this( Website.getCurrentWebsiteFromTabSession(), backingPageClass, addExtension );
	}

	public BackingPageUrl( Website site, Class<? extends BackingPage> backingPageClass, boolean addExtension ) {
		this( (String) null, backingPageClass, addExtension );
		if( site != null ) {
			setSitePackageName( site.getPackageName() );
		}
	}

	public BackingPageUrl( String sitePackageName, Class<? extends BackingPage> backingPageClass, boolean addExtension ) {
		setSitePackageName(sitePackageName);
		setBackingPageClass(backingPageClass);
		setAddingExtension(addExtension);
	}

	@Override
	public String getPath() {
		if( getBackingPageClass() != null ) {
			String viewName = CommonUtil.firstLetterToLowerCase( getBackingPageClass().getSimpleName() );
			String viewUrl = getBackingPageClass().getName().replace( getBackingPageClass().getSimpleName(), viewName );
			viewUrl = "/" + viewUrl.replaceFirst( "^(com\\.aplos\\.)(.*)(backingpage.)(.*)(Page)$", "$2$4" ).replace(".", "/");
	
			String returnString;
			if( CommonUtil.isNullOrEmpty( getSitePackageName() ) || viewUrl.startsWith( "/" + getSitePackageName() + "/" ) ) {
				returnString = viewUrl;
			} else {
				returnString = "/" + getSitePackageName() + viewUrl;
			}
	
			if (isAddingExtension()) {
				returnString += ".jsf";
			}
	
			return returnString;
		} else {
			return null;
		}
	}

	public Class<? extends BackingPage> getBackingPageClass() {
		return backingPageClass;
	}

	public void setBackingPageClass(Class<? extends BackingPage> backingPageClass) {
		this.backingPageClass = backingPageClass;
	}

	public boolean isAddingExtension() {
		return isAddingExtension;
	}

	public void setAddingExtension(boolean isAddingExtension) {
		this.isAddingExtension = isAddingExtension;
	}

	public String getSitePackageName() {
		return sitePackageName;
	}

	public void setSitePackageName(String sitePackageName) {
		this.sitePackageName = sitePackageName;
	}
}
