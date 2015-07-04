package com.aplos.common.beans;

import java.util.List;

import com.aplos.common.annotations.persistence.FetchType;
import com.aplos.common.annotations.persistence.ManyToMany;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.annotations.persistence.MappedSuperclass;
import com.aplos.common.utils.JSFUtil;

@MappedSuperclass
public abstract class AplosSiteBean extends AplosBean {
	private static final long serialVersionUID = 6122433122264167620L;

	@ManyToOne(fetch=FetchType.LAZY)
	private Website parentWebsite;

	@ManyToMany(fetch=FetchType.LAZY)
	private List<Website> siteVisibility;

	public void setParentWebsite(Website parentWebsite) {
		this.parentWebsite = parentWebsite;
	}

	public Website getParentWebsite() {
		return parentWebsite;
	}

	public void setSiteVisibility(List<Website> siteVisibility) {
		this.siteVisibility = siteVisibility;
	}

	public List<Website> getSiteVisibility() {
		return siteVisibility;
	}

//	@Override
//	public void hibernateInitialiseAfterCheck( boolean fullInitialisation ) {
//		super.hibernateInitialiseAfterCheck( fullInitialisation );
//		HibernateUtil.initialise( getParentWebsite(), false );
//	}

	public void saveDetails( Website website ) {
		setParentWebsite(website);
		super.saveDetails( JSFUtil.getLoggedInUser() );
	}
	
	 @Override
	public void saveBean(SystemUser currentUser) {
		if( isNew() && parentWebsite == null ) {
			setParentWebsite(Website.getCurrentWebsiteFromTabSession());
		}
		super.saveBean(currentUser);
	}

	public void saveDetails( Website website, SystemUser systemUser ) {
		setParentWebsite(website);
		saveDetails( systemUser );
	}
}
