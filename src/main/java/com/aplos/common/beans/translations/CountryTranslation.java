package com.aplos.common.beans.translations;

import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.AplosTranslation;

@Entity
public class CountryTranslation extends AplosTranslation {

	private static final long serialVersionUID = 4580957467518373750L;
	private String name;

	public CountryTranslation() {}

	public CountryTranslation(AplosBean untranslatedBean) {
		super(untranslatedBean);
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}


}
