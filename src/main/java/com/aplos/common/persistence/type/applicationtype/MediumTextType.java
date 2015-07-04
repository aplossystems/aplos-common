package com.aplos.common.persistence.type.applicationtype;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MediumTextType extends ApplicationType {
	
	public MediumTextType() {
		setColumnSize(Integer.MAX_VALUE);
	}
	
	public boolean includeColumnSizeInInsertOrAlter() {
		return false;
	}
	
	@Override
	public MySqlType getMySqlType() {
		return MySqlType.MEDIUMTEXT;
	}
	
	@Override
	public Class<?> getJavaClass() {
		return String.class;
	}
	
	@Override
	public Object getResultSetObject(ResultSet resultSet, int columnIdx) throws SQLException {
		return resultSet.getString( columnIdx );
	}
}
