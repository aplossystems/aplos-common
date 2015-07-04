package com.aplos.common.interfaces;

import java.sql.SQLException;

import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.persistence.fieldinfo.IndexFieldInfo;

public interface IndexableFieldInfo {
	public void updateIndexFields( AplosAbstractBean aplosAbstractBean ) throws IllegalAccessException, SQLException;
}
