package com.aplos.common.beans;

import java.util.List;

import com.aplos.common.annotations.persistence.Cache;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.utils.JSFUtil;

@Entity
@Cache
public class CountryArea extends AplosBean {
	private static final long serialVersionUID = 950787836072874441L;

	private String name;
	@ManyToOne
	private Country country;
	private String areaCode;

	@Override
	public boolean isValidForSave() {
		BeanDao aqlBeanDao = new BeanDao( CountryArea.class );
		aqlBeanDao.setWhereCriteria( "name = :name" );
		aqlBeanDao.setNamedParameter( "name", getName() );
		List<CountryArea> countryAreaList = aqlBeanDao.getAll();
		if( countryAreaList.size() > 0 ) {
			boolean containsActive = false;
			for( int i = 0, n = countryAreaList.size(); i < n; i++ ) {
				if( countryAreaList.get( i ).isActive() == true ) {
					containsActive = true;
				}
			}

			if( containsActive ) {
				JSFUtil.addMessage( "There is already a country area with this name" );
			} else {
				countryAreaList.get( 0 ).setActive( true );
				JSFUtil.addMessage( "A country area with this name already existed, this how now been reactivated" );
				countryAreaList.get( 0 ).saveDetails();
			}
			return false;
		}
		return super.isValidForSave();
	}

	@Override
	public String getDisplayName() {
		return name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Country getCountry() {
		return country;
	}

	public void setCountry(Country country) {
		this.country = country;
	}

	public void setAreaCode(String areaCode) {
		this.areaCode = areaCode;
	}

	public String getAreaCode() {
		return areaCode;
	}



}
