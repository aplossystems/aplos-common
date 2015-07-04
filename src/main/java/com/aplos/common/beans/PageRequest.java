package com.aplos.common.beans;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.Transient;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@Entity
public class PageRequest extends AplosBean {
	private static final long serialVersionUID = -5277595015109604750L;
	@Column(columnDefinition="LONGTEXT")
	private String pageUrl; 
	private String refererUrl;
	private Date pageRequestedDateTime;
	private long pageRequestedTime;
	private long restoreViewBeforeBindTime;
	private long restoreViewAfterBindTime;
	private long applyRequestValuesTime;
	private long processValidationsTime;
	private long updateModelValuesTime;
	private long invokeApplicationTime;
	private long renderResponseBeforeBindTime;
	private long renderResponseAfterBindTime;
	private long renderResponseCompleteTime;
	private long duration;
	@Transient
	private long timeMarker;
	private boolean isStatus404 = false;
	private boolean isFrontend = false;
	
	public void setInitialValues( HttpServletRequest httpRequest ) {
		setPageRequestedTime(System.nanoTime());
		setPageRequestedDateTime(new Date());
		setFrontend(JSFUtil.determineIsFrontEnd(httpRequest));
		String refererUrl = httpRequest.getHeader( "referer" );
		if( !CommonUtil.isNullOrEmpty( refererUrl ) ) {
			if( refererUrl.length() > 191 ) {
				refererUrl = refererUrl.substring( 0, 191 );
			}
		}
		setRefererUrl( refererUrl );
	}
	
	public String getPageUrlEnding() {
		if( getPageUrl() != null && getPageUrl().contains( "/" ) ) {
			return getPageUrl().substring( getPageUrl().lastIndexOf( "/" ) );
		}
		return getPageUrl();
	}
	
	public String getRefererUrlEnding() {
		if( getRefererUrl() != null && getRefererUrl().contains( "/" ) ) {
			return getRefererUrl().substring( getRefererUrl().lastIndexOf( "/" ) );
		}
		return getRefererUrl();
	}
	
	public void updateTimeMarker() {
		setTimeMarker( System.nanoTime() );
	}
	
	public int getDurationInMillis() {
		return (int) (getDuration() / 1000000);
	}
	
	public String getPageUrl() {
		return pageUrl;
	}
	public void setPageUrl(String pageUrl) {
		this.pageUrl = pageUrl;
	}
	public long getPageRequestedTime() {
		return pageRequestedTime;
	}
	public void setPageRequestedTime(long pageRequestedTime) {
		this.pageRequestedTime = pageRequestedTime;
	}
	public long getDuration() {
		return duration;
	}
	public void setDuration(long duration) {
		this.duration = duration;
	}

	public int getApplyRequestValuesTimeInMillis() {
		return (int) applyRequestValuesTime / 1000000;
	}

	public long getApplyRequestValuesTime() {
		return applyRequestValuesTime;
	}

	public void setApplyRequestValuesTime(long applyRequestValuesTime) {
		this.applyRequestValuesTime = applyRequestValuesTime;
	}

	public int getProcessValidationsTimeInMillis() {
		return (int) processValidationsTime / 1000000;
	}

	public long getProcessValidationsTime() {
		return processValidationsTime;
	}

	public void setProcessValidationsTime(long processValidationsTime) {
		this.processValidationsTime = processValidationsTime;
	}

	public int getUpdateModelValuesTimeInMillis() {
		return (int) updateModelValuesTime / 1000000;
	}

	public long getUpdateModelValuesTime() {
		return updateModelValuesTime;
	}

	public void setUpdateModelValuesTime(long updateModelValuesTime) {
		this.updateModelValuesTime = updateModelValuesTime;
	}

	public int getInvokeApplicationTimeInMillis() {
		return (int) invokeApplicationTime / 1000000;
	}

	public long getInvokeApplicationTime() {
		return invokeApplicationTime;
	}

	public void setInvokeApplicationTime(long invokeApplicationTime) {
		this.invokeApplicationTime = invokeApplicationTime;
	}

	public int getRestoreViewBeforeBindTimeInMillis() {
		return (int) restoreViewBeforeBindTime / 1000000;
	}

	public long getRestoreViewBeforeBindTime() {
		return restoreViewBeforeBindTime;
	}

	public void setRestoreViewBeforeBindTime(long restoreViewBeforeBindTime) {
		this.restoreViewBeforeBindTime = restoreViewBeforeBindTime;
	}

	public int getRestoreViewAfterBindTimeInMillis() {
		return (int) restoreViewAfterBindTime / 1000000;
	}

	public long getRestoreViewAfterBindTime() {
		return restoreViewAfterBindTime;
	}

	public void setRestoreViewAfterBindTime(long restoreViewAfterBindTime) {
		this.restoreViewAfterBindTime = restoreViewAfterBindTime;
	}

	public int getRenderResponseBeforeBindTimeInMillis() {
		return (int) renderResponseBeforeBindTime / 1000000;
	}

	public long getRenderResponseBeforeBindTime() {
		return renderResponseBeforeBindTime;
	}

	public void setRenderResponseBeforeBindTime(long renderResponseBeforeBindTime) {
		this.renderResponseBeforeBindTime = renderResponseBeforeBindTime;
	}

	public int getRenderResponseAfterBindTimeInMillis() {
		return (int) renderResponseAfterBindTime / 1000000;
	}

	public long getRenderResponseAfterBindTime() {
		return renderResponseAfterBindTime;
	}

	public void setRenderResponseAfterBindTime(long renderResponseAfterBindTime) {
		this.renderResponseAfterBindTime = renderResponseAfterBindTime;
	}

	public int getRenderResponseCompleteTimeInMillis() {
		return (int) renderResponseCompleteTime / 1000000;
	}

	public long getRenderResponseCompleteTime() {
		return renderResponseCompleteTime;
	}

	public void setRenderResponseCompleteTime(long renderResponseCompleteTime) {
		this.renderResponseCompleteTime = renderResponseCompleteTime;
	}

	public long getTimeMarker() {
		return timeMarker;
	}

	public void setTimeMarker(long timeMarker) {
		this.timeMarker = timeMarker;
	}

	public boolean isStatus404() {
		return isStatus404;
	}

	public void setStatus404(boolean isStatus404) {
		this.isStatus404 = isStatus404;
	}

	public boolean isFrontend() {
		return isFrontend;
	}

	public void setFrontend(boolean isFrontend) {
		this.isFrontend = isFrontend;
	}

	public Date getPageRequestedDateTime() {
		return pageRequestedDateTime;
	}

	public void setPageRequestedDateTime(Date pageRequestedDateTime) {
		this.pageRequestedDateTime = pageRequestedDateTime;
	}

	public String getRefererUrl() {
		return refererUrl;
	}

	public void setRefererUrl(String refererUrl) {
		this.refererUrl = refererUrl;
	}
	
}
