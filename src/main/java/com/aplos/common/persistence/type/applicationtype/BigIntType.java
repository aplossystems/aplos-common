package com.aplos.common.persistence.type.applicationtype;

import java.sql.ResultSet;
import java.sql.SQLException;

public class BigIntType extends ApplicationType {
	
	public BigIntType() {
		setColumnSize(19);
	}
	
	@Override
	public MySqlType getMySqlType() {
		return MySqlType.BIGINT;
	}
	
	@Override
	public Class<?> getJavaClass() {
		return Long.class;
	}
	
	@Override
	public Object getResultSetObject(ResultSet resultSet, int columnIdx) throws SQLException {
		Long longResult = resultSet.getLong( columnIdx );
		if( resultSet.wasNull() ) {
			return null;
		} else {
			return longResult;
		}
	}
}
