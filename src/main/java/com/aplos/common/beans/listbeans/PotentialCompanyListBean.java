package com.aplos.common.beans.listbeans;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.marketing.PotentialCompany;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.utils.ApplicationUtil;

public class PotentialCompanyListBean extends PotentialCompany {
	private static final long serialVersionUID = -1562305383204635191L;
	
	private int numberOfInteractions;
	
	@Override
	public PersistentClass getPersistentClass() {
		return ApplicationUtil.getPersistentApplication().getPersistentClassMap().get( PotentialCompany.class );
	}

	public int getNumberOfInteractions() {
		return numberOfInteractions;
	}

	public void setNumberOfInteractions(int numberOfInteractions) {
		this.numberOfInteractions = numberOfInteractions;
	}

	public void redirectToEditPage() {
		PotentialCompany potentialCompany = new BeanDao( PotentialCompany.class ).get( getId() );
		potentialCompany.redirectToEditPage();
	}
}
