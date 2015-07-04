package com.aplos.common.interfaces;

import com.aplos.common.beans.MenuStep;

public interface MenuStepBacking {
	public void beforeLeavingMenuStep( MenuStep nextMenuStep );
	public boolean responsePageLoad();
}
