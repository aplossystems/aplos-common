package com.aplos.common.enums;

public enum JsfScope {
	TAB_SESSION,
	SESSION,
	VIEW,
	FLASH_VIEW,
	FLASH_VIEW_CURRENT,
	REQUEST,
	NOT_SELECTED;  // Hack for null values in Annotations
}
