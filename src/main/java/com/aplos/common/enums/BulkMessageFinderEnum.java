package com.aplos.common.enums;

import com.aplos.common.interfaces.BulkMessageFinderInter;

public interface BulkMessageFinderEnum {
	public String getBulkMessageFinderName();
	public Class<? extends BulkMessageFinderInter> getBulkMessageFinderClass();
	public void setActive(boolean isActive);
	public boolean isActive();
}
