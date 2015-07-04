package com.aplos.common.beans;

import com.aplos.common.annotations.persistence.Entity;

@Entity
public class BasicContactTag extends AplosBean {
	private static final long serialVersionUID = -5009967893723174864L;
	
	private String tagName;
	
	public String getDisplayName() {
		return getTagName();
	}

	public String getTagName() {
		return tagName;
	}

	public void setTagName(String tagName) {
		this.tagName = tagName;
	}

}
