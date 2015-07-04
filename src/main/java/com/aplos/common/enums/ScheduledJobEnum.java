package com.aplos.common.enums;

import com.aplos.common.ScheduledJob;

public interface ScheduledJobEnum {
	public Class<? extends ScheduledJob<?>> getScheduledJobClass();
	public void setActive(boolean isActive);
	public boolean isActive();

}
