package com.aplos.common.persistence.type.applicationtype;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;

public class VarCharType extends ApplicationType {
	
	public VarCharType() {
		if( ApplicationUtil.getPersistentApplication() != null ) {
			setColumnSize(ApplicationUtil.getPersistentApplication().getMaxCharLength());
		}
	}
	
	@Override
	public boolean isEmpty(Field field, AplosAbstractBean aplosAbstractBean) {
		Object value = getValue( field, aplosAbstractBean );
		if( CommonUtil.isNullOrEmpty((String) value) ) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public Class<?> getJavaClass() {
		return String.class;
	}
	
	@Override
	public MySqlType getMySqlType() {
		return MySqlType.VARCHAR;
	}
	
	@Override
	public Object getResultSetObject(ResultSet resultSet, int columnIdx) throws SQLException {
		return resultSet.getString( columnIdx );
	}
}
