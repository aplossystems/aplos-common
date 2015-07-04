package com.aplos.common.beans;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

@ManagedBean
@SessionScoped
public class PrettyfacesNavigation {
	private String pageUrl = "/test.aplos";

	public PrettyfacesNavigation() {
	}

	public String getPageUrl() {
		return pageUrl;
	}

	public void setPageUrl( String pageUrl ) {
		this.pageUrl = pageUrl;
	}
}
