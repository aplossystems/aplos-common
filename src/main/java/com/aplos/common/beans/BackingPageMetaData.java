package com.aplos.common.beans;

import com.aplos.common.enums.SslProtocolEnum;

public class BackingPageMetaData {
	private SslProtocolEnum ssLProtocol = SslProtocolEnum.FORCE_SSL;
	private boolean requiresWindowId = true;

	public SslProtocolEnum getSsLProtocol() {
		return ssLProtocol;
	}

	public void setSsLProtocol(SslProtocolEnum ssLProtocol) {
		this.ssLProtocol = ssLProtocol;
	}

	public boolean isRequiresWindowId() {
		return requiresWindowId;
	}

	public void setRequiresWindowId(boolean requiresWindowId) {
		this.requiresWindowId = requiresWindowId;
	}
}
