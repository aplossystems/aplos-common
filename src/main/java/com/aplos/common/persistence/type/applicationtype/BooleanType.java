package com.aplos.common.persistence.type.applicationtype;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.utils.CommonUtil;

public class BooleanType extends ApplicationType {
	
	public BooleanType() {
		setColumnSize(1);
		// Hibernate didn't set this for boolean types but it should be set.
//		setNullable(false);
	}
	
	@Override
	public Class<?> getJavaClass() {
		return Boolean.class;
	}
	
	@Override
	public boolean isEmpty(Field field, AplosAbstractBean aplosAbstractBean) {
		Object value = getValue( field, aplosAbstractBean );
		Boolean booleanValue = (Boolean) value;
		if( booleanValue == null || !booleanValue ) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public MySqlType getMySqlType() {
		return MySqlType.BIT;
	}
	
	@Override
	public Object getResultSetObject(ResultSet resultSet, int columnIdx) throws SQLException {
		Boolean booleanResult = resultSet.getBoolean( columnIdx );
		if( resultSet.wasNull() ) {
			return null;
		} else {
			return booleanResult;
		}
	}
}
