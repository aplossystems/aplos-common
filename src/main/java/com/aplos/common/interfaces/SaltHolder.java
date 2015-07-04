package com.aplos.common.interfaces;

import com.aplos.common.beans.AplosAbstractBean;

public interface SaltHolder<T extends AplosAbstractBean> {
	public String getEncryptionSalt();
	public void setEncryptionSalt( String encryptionSalt );
	public <T extends AplosAbstractBean> T getSaveableBean();
	public boolean saveDetails();
}
