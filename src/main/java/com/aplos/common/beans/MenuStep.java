package com.aplos.common.beans;

import java.io.Serializable;

import com.aplos.common.interfaces.MenuStepBacking;
import com.aplos.common.utils.JSFUtil;

public class MenuStep implements Serializable {
	private static final long serialVersionUID = -2736899488288154818L;
	
	private String label;
	private String viewUrl;
	private int idx;
	private MenuWizard menuWizard;
	private Class<? extends MenuStepBacking> menuStepBackingClass;
	private boolean isVisible = true;

	public MenuStep( MenuWizard menuWizard, int idx, String label, String viewUrl, Class<? extends MenuStepBacking> menuStepBacking ) {
		setMenuStepBackingClass( menuStepBacking );
		setMenuWizard( menuWizard );
		setIdx( idx );
		this.setLabel(label);
		this.setViewUrl(viewUrl);
	}

	public String getBackgroundColour() {
		if (isCurrentStep()) {
			return "#9fdbac";
		}
		return "#dfdf88";
	}

	public boolean validate( MenuStep nextMenuStep ) { return true; }

	public boolean isImmediate() {
		if ( getIdx() <= getMenuWizard().getCurrentStepIdx() ) {
			return true;
		} else {
			return false;
		}
	}

	public boolean responsePageLoad() {
		MenuStepBacking menuStepBacking = resolveMenuStepBacking();
		if( menuStepBacking != null ) {
			menuStepBacking.responsePageLoad();
		}
		return true;
	}

	public MenuStepBacking resolveMenuStepBacking() {
		if( getMenuStepBackingClass() != null ) {
			return (MenuStepBacking) JSFUtil.resolveVariable( getMenuStepBackingClass() );
		} else {
			return null;
		}
	}


	public void setLabel(String label) {
		this.label = label;
	}
	public String getLabel() {
		return label;
	}

	public void redirectToMenuStep() {
		MenuStepBacking menuStepBacking = getMenuWizard().getMenuSteps().get( getMenuWizard().getCurrentStepIdx() ).resolveMenuStepBacking();
		if( menuStepBacking != null ) {
			menuStepBacking.beforeLeavingMenuStep( this );
		}
		MenuStep nextMenuStep = (MenuStep) JSFUtil.getRequest().getAttribute( "menuStep" );
		boolean validationRequired = !isOnlyValidatingGoingForward() || (nextMenuStep.getIdx() > getMenuWizard().getCurrentStepIdx());
		if( !validationRequired || getMenuWizard().getMenuSteps().get( getMenuWizard().getCurrentStepIdx() ).validate( nextMenuStep ) ) {
			getMenuWizard().setCurrentStepIdx( nextMenuStep.getIdx() );
			if( nextMenuStep.getIdx() > getMenuWizard().getLatestStepIdx() ) {
				getMenuWizard().setLatestStepIdx( nextMenuStep.getIdx() );
			}
		}
		getMenuWizard().getParent().saveDetails();
	}
	
	public boolean isOnlyValidatingGoingForward() {
		return true;
	}

	public boolean isClickable() {
		if( getMenuWizard().getLatestStepIdx() == getMenuWizard().getCurrentStepIdx() &&
				getIdx() == (getMenuWizard().getLatestStepIdx() + 1) ) {
			return true;
		} else if( getIdx() <= getMenuWizard().getLatestStepIdx() ) {
			return true;
		} else {
			return false;
		}
	}

	public String getStyleClass() {
		if( getIdx() == getMenuWizard().getCurrentStepIdx() ) {
			return "aplos-active";
		} else if( isClickable() ) {
			return "aplos-enabled";
		} else {
			return "aplos-disabled";
		}
	}

	public void setViewUrl(String viewUrl) {
		this.viewUrl = viewUrl;
	}

	public String getViewUrl() {
		return viewUrl;
	}

	public boolean isCurrentStep() {
		return getIdx() == getMenuWizard().getCurrentStepIdx();
	}

	public void setMenuWizard(MenuWizard menuWizard) {
		this.menuWizard = menuWizard;
	}

	public MenuWizard getMenuWizard() {
		return menuWizard;
	}

	public void setIdx(int idx) {
		this.idx = idx;
	}

	public int getIdx() {
		return idx;
	}

	public void setMenuStepBackingClass(Class<? extends MenuStepBacking> menuStepBackingClass) {
		this.menuStepBackingClass = menuStepBackingClass;
	}

	public Class<? extends MenuStepBacking> getMenuStepBackingClass() {
		return menuStepBackingClass;
	}

	public boolean isVisible() {
		return isVisible;
	}

	public void setVisible(boolean isVisible) {
		this.isVisible = isVisible;
	}
}
