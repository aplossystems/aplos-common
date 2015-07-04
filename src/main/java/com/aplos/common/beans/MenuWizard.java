package com.aplos.common.beans;

import java.util.List;

import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.Inheritance;
import com.aplos.common.annotations.persistence.InheritanceType;
import com.aplos.common.annotations.persistence.Transient;
import com.aplos.common.utils.ApplicationUtil;

@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public abstract class MenuWizard extends AplosAbstractBean {
	private static final long serialVersionUID = -3935482889150555817L;

	@Transient
	private List<MenuStep> menuSteps;

	@Transient
	private AplosBean parent;

	private int currentStepIdx = 0;
	private int latestStepIdx = 0;

	public MenuWizard getCopy() {
		try {
			MenuWizard destMenuWizard = (MenuWizard) this.clone();
			return destMenuWizard;
		} catch ( CloneNotSupportedException cnsEx ) {
			ApplicationUtil.getAplosContextListener().handleError( cnsEx );
		}
		return null;
	}

	public boolean isLastMenuStep( MenuStep menuStep ) {
		if( menuSteps.get( menuSteps.size() - 1 ).equals( menuStep ) ) {
			return true;
		} else {
			return false;
		}
	}

	public String getCurrentViewUrl() {
		return getMenuSteps().get( getCurrentStepIdx() ).getViewUrl();
	}

	public void refreshMenu( AplosBean parent ) {
		if( parent != null && !parent.equals( this.getParent() ) ) {
			this.setParent(parent);
			createMenuSteps();
		}
	}

	protected abstract void createMenuSteps();

	public void setMenuSteps(List<MenuStep> menuSteps) {
		this.menuSteps = menuSteps;
	}

	public List<MenuStep> getMenuSteps() {
		return menuSteps;
	}

	public void setCurrentStepIdx(int currentStepIdx) {
		this.currentStepIdx = currentStepIdx;
	}

	public int getCurrentStepIdx() {
		return currentStepIdx;
	}

	public void setLatestStepIdx(int latestStepIdx) {
		this.latestStepIdx = latestStepIdx;
	}

	public int getLatestStepIdx() {
		return latestStepIdx;
	}

	public void setParent(AplosBean parent) {
		this.parent = parent;
	}

	public AplosBean getParent() {
		return parent;
	}
}
