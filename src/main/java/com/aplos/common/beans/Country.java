package com.aplos.common.beans;


import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.model.SelectItem;

import com.aplos.common.annotations.PluralDisplayName;
import com.aplos.common.annotations.persistence.Cache;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.FetchType;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.translations.CountryTranslation;
import com.aplos.common.enums.VatExemption;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@SessionScoped
@Entity
@Cache
@PluralDisplayName(name="countries")
public class Country extends AplosBean {
	private static final long serialVersionUID = -3686773892819301841L;

	private String iso2;
	private String iso3;
	private String name;

	private String iddCode;

	//@ManyToOne //replaced by PostalZone
	//private RoyalMailZone royalMailZone;
	
	private Integer gmtDifference;

	private String vatComment;
	private VatExemption vatExemption = VatExemption.EXEMPT;

	@ManyToOne(fetch=FetchType.LAZY)
	private Currency defaultCurrency;

	public Country() {}

	@Override
	public String getDisplayName() {
		return getName();
	}
	
	@Override
	public boolean isValidForSave() {
		BeanDao aqlBeanDao = new BeanDao( Country.class );
		aqlBeanDao.setWhereCriteria( "LOWER(name) = LOWER(:name)" );
		aqlBeanDao.setNamedParameter( "name", "%" + this.getName() + "%" );
		List<Country> countryList = aqlBeanDao.getAll();
		if( countryList.size() > 0 ) {

			boolean containsActive = false;
			boolean safeToSave = false;

			for( int i = 0, n = countryList.size(); i < n; i++ ) {
				if ( countryList.get( i ).getId().equals(this.getId()) ) {
					safeToSave=true;
					break;
				} else if( countryList.get( i ).isActive() == true ) {
					containsActive = true;
					break;
				}
			}

			if (!safeToSave) {
				if( containsActive ) {
					JSFUtil.addMessage( "There is already a country with this name" );
				} else {
					countryList.get( 0 ).setActive( true );
					countryList.get( 0 ).setIso2(iso2);
					countryList.get( 0 ).setIso3(iso3);
					countryList.get( 0 ).setName(name);
					countryList.get( 0 ).saveDetails();
				}
				return false;
			}
		}
		return super.isValidForSave();
	}

	@Override
	public SelectItem[] getSelectItemBeans() {
		return getSelectItemBeans( this.getClass() );
	}

	public void setIso2(String iso2) {
		this.iso2 = iso2;
	}
	public String getIso2() {
		return iso2;
	}
	public void setIso3(String iso3) {
		this.iso3 = iso3;
	}
	public String getIso3() {
		return iso3;
	}

	public void setName(String name) {
		CountryTranslation trans = getAplosTranslation();
		if (trans != null) {
			trans.setName(name);
		} else {
			this.name = name;
		}
	}

	public String getName(String language) {
		CountryTranslation trans = getAplosTranslation(language);
		if (trans != null) {
			return trans.getName();
		}
		return name;
	}

	public String getName() {
		return getName(CommonUtil.getContextLocale().getLanguage());
	}

	public String getNameRaw() {
		return name;
	}

	public void setDefaultCurrency(Currency defaultCurrency) {
		this.defaultCurrency = defaultCurrency;
	}

	public Currency getDefaultCurrency() {
		return defaultCurrency;
	}

	public void setIddCode(String iddCode) {
		this.iddCode = iddCode;
	}

	public String getIddCode() {
		return iddCode;
	}

//	public void setRoyalMailZone(RoyalMailZone royalMailZone) {
//		this.royalMailZone = royalMailZone;
//	}
//
//	public RoyalMailZone getRoyalMailZone() {
//		return royalMailZone;
//	}

	public void setVatComment(String vatComment) {
		this.vatComment = vatComment;
	}

	public String getVatComment() {
		return vatComment;
	}

	public void setGmtDifference(Integer gmtDifference) {
		this.gmtDifference = gmtDifference;
	}

	public Integer getGmtDifference() {
		return gmtDifference;
	}

	public VatExemption getVatExemption() {
		return vatExemption;
	}

	public void setVatExemption(VatExemption vatExemption) {
		this.vatExemption = vatExemption;
	}
}
