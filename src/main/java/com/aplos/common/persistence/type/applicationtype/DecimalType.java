package com.aplos.common.persistence.type.applicationtype;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DecimalType extends ApplicationType {
	
	public DecimalType() {
		setColumnSize(19);
		setDecimalDigits(2);
	}
	
	@Override
	public boolean isUsingDecimalDigits() {
		return true;
	}
	
	@Override
	public Class<?> getJavaClass() {
		return BigDecimal.class;
	}
	
	@Override
	public MySqlType getMySqlType() {
		return MySqlType.DECIMAL;
	}
	
	@Override
	public Object getResultSetObject(ResultSet resultSet, int columnIdx) throws SQLException {
		return resultSet.getBigDecimal( columnIdx );
	}
}
