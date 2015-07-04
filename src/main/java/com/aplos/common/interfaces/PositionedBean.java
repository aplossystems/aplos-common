package com.aplos.common.interfaces;


import com.aplos.common.beans.SystemUser;

public interface PositionedBean {
	public Long getId();
	public String getDisplayName();
	public boolean isNew();
	public Integer getPositionIdx();
	public void setPositionIdx( Integer positionIdx );
	public boolean saveDetails( SystemUser currentUser );
}
