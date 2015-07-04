package com.aplos.common;

public class PageTracker {
	  private String prevRenderResponseURL;
	  private String redirectURL = null;
	  private String restoreViewURL = "";
	  private String renderResponseURL = "";
	  private boolean pageAlreadyLoaded = false;

	  public void setRestoreViewURL( String restoreViewURL ) {
		  this.restoreViewURL = restoreViewURL;
		  if( restoreViewURL.equals( renderResponseURL ) ) {
			  pageAlreadyLoaded = true;
		  } else {
			  pageAlreadyLoaded = false;
		  }
	  }

	  public void setRenderResponseURL( String renderResponseURL ) {
		  prevRenderResponseURL = this.renderResponseURL;
		  this.renderResponseURL = renderResponseURL;
		  if( restoreViewURL.equals( renderResponseURL )&& restoreViewURL.equals( prevRenderResponseURL ) ) {
			  pageAlreadyLoaded = true;
		  } else {
			  pageAlreadyLoaded = false;
		  }
	  }

	  public String getRestoreViewURL() {
		  return restoreViewURL;
	  }

	  public String getRenderResponseURL() {
		  return renderResponseURL;
	  }

	  public boolean isPageAlreadyLoaded() {
		  return pageAlreadyLoaded;
	  }

	  public void setRedirectURL( String redirectURL ) {
		  this.redirectURL = redirectURL;
	  }

	  public String getRedirectURL() {
		  return redirectURL;
	  }
}
