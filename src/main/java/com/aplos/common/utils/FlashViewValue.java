package com.aplos.common.utils;



public class FlashViewValue {
	private String currentViewId;
	private boolean isFinalView = false;
	private Object value;

	public FlashViewValue( String currentViewId, Object value ) {
		setCurrentViewId(currentViewId);
		setValue(value);
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public String getCurrentViewId() {
		return currentViewId;
	}

	public void setCurrentViewId(String currentViewId) {
		this.currentViewId = currentViewId;
	}

	public boolean isFinalView() {
		return isFinalView;
	}

	public void setFinalView(boolean isFinalView) {
		this.isFinalView = isFinalView;
	}
}
