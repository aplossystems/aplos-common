package com.aplos.common.interfaces;

import java.util.Map;

import com.aplos.common.persistence.PersistentClass;

public interface PersistenceBean {
	
	public Long getId();

	public boolean isLazilyLoaded();

	public boolean isInDatabase();

	public boolean isAlreadyLoading();

	public void setInDatabase( boolean isInDatabase );
	
	public void setLazilyLoaded( boolean isLazilyLoaded );
	
	public void setAlreadyLoading( boolean isAlreadyLoading );
	
	public void copyFields( PersistenceBean srcBean );

	public Map<String, Object> getHiddenFieldsMap();

	public Map<String, Boolean> getProceedMap();

	public PersistentClass getPersistentClass();
}
