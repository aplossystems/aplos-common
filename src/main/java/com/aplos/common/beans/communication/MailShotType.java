package com.aplos.common.beans.communication;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.beans.AplosBean;


@Entity
@ManagedBean
@SessionScoped
public class MailShotType extends AplosBean {
	private static final long serialVersionUID = 8577369883887938507L;

	private String name;

	@Override
	public String getDisplayName() {
		return getName();
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
