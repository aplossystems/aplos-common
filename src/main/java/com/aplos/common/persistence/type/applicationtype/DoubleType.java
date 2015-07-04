package com.aplos.common.persistence.type.applicationtype;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DoubleType extends ApplicationType {
	
	public DoubleType() {
		setColumnSize(22);
	}
	
	@Override
	public boolean includeColumnSizeInInsertOrAlter() {
		return false;
	}
	
	@Override
	public MySqlType getMySqlType() {
		return MySqlType.DOUBLE;
	}
	
	@Override
	public Class<?> getJavaClass() {
		return Double.class;
	}
	
	@Override
	public Object getResultSetObject(ResultSet resultSet, int columnIdx) throws SQLException {
		Double doubleResult = resultSet.getDouble( columnIdx );
		if( resultSet.wasNull() ) {
			return null;
		} else {
			return doubleResult;
		}
	}
}
