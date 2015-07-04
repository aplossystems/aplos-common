package com.aplos.common.beans.translations;

import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.AplosTranslation;

@Entity
public class SystemUserTranslation extends AplosTranslation {

	private static final long serialVersionUID = -3234046132366352340L;
	private String title;
	private String firstName;
	private String surname;

	public SystemUserTranslation() {}

	public SystemUserTranslation(AplosBean untranslatedBean) {
		super(untranslatedBean);
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public String getSurname() {
		return surname;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

}
