package com.aplos.common.beans;

import java.util.Date;

import com.aplos.common.beans.SystemUser;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.utils.FormatUtil;

public class ActiveLogin extends AplosBean {
	private SystemUser systemUser;
	private Date loggedInTime;
	private Date loggedOutTime;
	private String loggedOutLocation;
	private String sessionId;
	private int maxInactiveInterval;
	private Date lastAccessedTime;
	private String requestHeaders;
	private String cookieDomain;
	private String cookieMaxAge;
	private String cookiePath;
	private String httpOnly;
	
	public String getLoggedInTimeStdStr() {
		return FormatUtil.formatDateTime( getLoggedInTime(), true );
	}
	
	public String getLoggedOutTimeStdStr() {
		return FormatUtil.formatDateTime( getLoggedOutTime(), true );
	}
	
	public String getLastAccessedTimeStdStr() {
		return FormatUtil.formatDateTime( getLastAccessedTime(), true );
	}
	
	public SystemUser getSystemUser() {
		return systemUser;
	}
	public void setSystemUser(SystemUser systemUser) {
		this.systemUser = systemUser;
	}
	public Date getLoggedInTime() {
		return loggedInTime;
	}
	public void setLoggedInTime(Date loggedInTime) {
		this.loggedInTime = loggedInTime;
	}
	public Date getLoggedOutTime() {
		return loggedOutTime;
	}
	public void setLoggedOutTime(Date loggedOutTime) {
		this.loggedOutTime = loggedOutTime;
	}
	public String getLoggedOutLocation() {
		return loggedOutLocation;
	}
	public void setLoggedOutLocation(String loggedOutLocation) {
		this.loggedOutLocation = loggedOutLocation;
	}
	public String getSessionId() {
		return sessionId;
	}
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public Date getLastAccessedTime() {
		return lastAccessedTime;
	}

	public void setLastAccessedTime(Date lastAccessedTime) {
		this.lastAccessedTime = lastAccessedTime;
	}

	public String getRequestHeaders() {
		return requestHeaders;
	}

	public void setRequestHeaders(String requestHeaders) {
		this.requestHeaders = requestHeaders;
	}

	public String getCookieDomain() {
		return cookieDomain;
	}

	public void setCookieDomain(String cookieDomain) {
		this.cookieDomain = cookieDomain;
	}

	public String getCookieMaxAge() {
		return cookieMaxAge;
	}

	public void setCookieMaxAge(String cookieMaxAge) {
		this.cookieMaxAge = cookieMaxAge;
	}

	public String getCookiePath() {
		return cookiePath;
	}

	public void setCookiePath(String cookiePath) {
		this.cookiePath = cookiePath;
	}

	public int getMaxInactiveInterval() {
		return maxInactiveInterval;
	}

	public void setMaxInactiveInterval(int maxInactiveInterval) {
		this.maxInactiveInterval = maxInactiveInterval;
	}

	public String getHttpOnly() {
		return httpOnly;
	}

	public void setHttpOnly(String httpOnly) {
		this.httpOnly = httpOnly;
	}
}
