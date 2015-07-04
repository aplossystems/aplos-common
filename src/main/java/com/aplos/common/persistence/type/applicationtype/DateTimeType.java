package com.aplos.common.persistence.type.applicationtype;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

public class DateTimeType extends ApplicationType {
	
	public DateTimeType() {
		setColumnSize(19);
	}

	public void appendCreateTableStr( StringBuffer strBuf ) {
		strBuf.append( getMySqlType().name() );
	}
	
	@Override
	public Class<?> getJavaClass() {
		return Date.class;
	}
	
	@Override
	public boolean includeColumnSizeInInsertOrAlter() {
		return false;
	}
	
	@Override
	public MySqlType getMySqlType() {
		return MySqlType.DATETIME;
	}
	
	@Override
	public Object getResultSetObject(ResultSet resultSet, int columnIdx) throws SQLException {
		Timestamp timestamp = resultSet.getTimestamp( columnIdx );
		if( timestamp != null ) {
			return new Date( resultSet.getTimestamp( columnIdx ).getTime() );
		} else {
			return null;
		}
	}
}
