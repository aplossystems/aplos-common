package com.aplos.common.beans.translations;

import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.beans.AplosTranslation;

@Entity
public class DynamicBundleEntryTranslation extends AplosTranslation {
	private static final long serialVersionUID = -9141340017478218762L;

	@Column(columnDefinition="LONGTEXT")
	private String entryValue;

	public void setEntryValue(String entryValue) {
		this.entryValue = entryValue;
	}

	public String getValue() {
		return entryValue;
	}
}
