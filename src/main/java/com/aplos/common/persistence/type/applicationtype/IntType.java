package com.aplos.common.persistence.type.applicationtype;

import java.sql.ResultSet;
import java.sql.SQLException;

public class IntType extends ApplicationType {
	
	public IntType() {
		setColumnSize(10);
	}
	
	@Override
	public MySqlType getMySqlType() {
		return MySqlType.INT;
	}
	
	@Override
	public Class<?> getJavaClass() {
		return Integer.class;
	}
	
	@Override
	public Object getResultSetObject(ResultSet resultSet, int columnIdx) throws SQLException {
		Integer intResult = resultSet.getInt( columnIdx );
		if( resultSet.wasNull() ) {
			return null;
		} else {
			return intResult;
		}
	}
}
