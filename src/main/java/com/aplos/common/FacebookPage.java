package com.aplos.common;

import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.beans.AplosBean;

@Entity
public class FacebookPage extends AplosBean {
	private static final long serialVersionUID = -8166734013747546800L;
	private boolean connected = true;
	private String pageId;
	private String pageName;
	@Column(columnDefinition="LONGTEXT")
	private String accessToken;
	
	public FacebookPage() {
	}
	
	public FacebookPage( String pageId, String pageName, String accessToken ) {
		setPageId( pageId );
		setPageName( pageName );
		setAccessToken( accessToken );
	}
	
	public boolean isConnected() {
		return connected;
	}
	public void setConnected(boolean connected) {
		this.connected = connected;
	}
	public String getAccessToken() {
		return accessToken;
	}
	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}
	public String getPageId() {
		return pageId;
	}
	public void setPageId(String pageId) {
		this.pageId = pageId;
	}

	public String getPageName() {
		return pageName;
	}

	public void setPageName(String pageName) {
		this.pageName = pageName;
	}
}
