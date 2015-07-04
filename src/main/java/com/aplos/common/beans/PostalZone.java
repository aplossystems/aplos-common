package com.aplos.common.beans;

import java.util.ArrayList;
import java.util.List;

import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.ManyToMany;
import com.aplos.common.annotations.persistence.ManyToOne;

@Entity
public class PostalZone extends AplosBean {
	private static final long serialVersionUID = 8742689292241168447L;
	
	private String name;
	@ManyToOne
	private Country primaryCountry;
	private boolean isVisibleAsACountry = false;
	@ManyToMany
	private List<Country> countries = new ArrayList<Country>();
	
//	@Override
//	public void hibernateInitialiseAfterCheck(boolean fullInitialisation) {
//		super.hibernateInitialiseAfterCheck(fullInitialisation);
//		HibernateUtil.initialiseList(getCountries(), fullInitialisation);
//		HibernateUtil.initialise(getPrimaryCountry(), fullInitialisation);
//	}

	public List<Country> getCountries() {
		return countries;
	}

	public void setCountries(List<Country> countries) {
		this.countries = countries;
	}

	public boolean isVisibleAsACountry() {
		return isVisibleAsACountry;
	}

	public void setVisibleAsACountry(boolean isVisibleAsACountry) {
		this.isVisibleAsACountry = isVisibleAsACountry;
	}

	public Country getPrimaryCountry() {
		return primaryCountry;
	}

	public void setPrimaryCountry(Country primaryCountry) {
		this.primaryCountry = primaryCountry;
	}

	public String getName() {
		return name;
	}
	
	public String getDisplayName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	} 
}
