package com.aplos.common.persistence.type.applicationtype;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.utils.ApplicationUtil;

public abstract class ApplicationType {
	private int columnSize = 0;
	private int decimalDigits = 0;
	private boolean isNullable = true;
	private Object defaultValue;
	
	public abstract MySqlType getMySqlType();

	public void appendCreateTableStr( StringBuffer strBuf ) {
		strBuf.append( getMySqlType().name() );
		if( includeColumnSizeInInsertOrAlter() ) {
			strBuf.append( "(" ).append( getColumnSize() );
			
			if( isUsingDecimalDigits() ) {
				strBuf.append( "," ).append( getDecimalDigits() );
			}
			strBuf.append( ")" );
		}
	}
	
	public abstract Class<?> getJavaClass();
	
	public boolean isEmpty( Field field, AplosAbstractBean aplosAbstractBean ) {
		Object value = getValue( field, aplosAbstractBean );
		if( value == null ) {
			return true;
		} else {
			return false;
		}
	}
	
	public Object getValue( Field field, AplosAbstractBean aplosAbstractBean ) {
		try {
			field.setAccessible(true);
			return field.get(aplosAbstractBean);
		} catch (IllegalArgumentException e) {
			ApplicationUtil.getAplosContextListener().handleError(e);
		} catch (IllegalAccessException e) {
			ApplicationUtil.getAplosContextListener().handleError(e);
		}
		return null;
	}
	
	public abstract Object getResultSetObject( ResultSet resultSet, int columnIdx ) throws SQLException;
	
	public boolean isUsingDecimalDigits() {
		return false;
	}
	
	public boolean includeColumnSizeInInsertOrAlter() {
		return true;
	}

	public int getColumnSize() {
		return columnSize;
	}

	public void setColumnSize(int columnSize) {
		this.columnSize = columnSize;
	}

	public int getDecimalDigits() {
		return decimalDigits;
	}

	public void setDecimalDigits(int decimalDigits) {
		this.decimalDigits = decimalDigits;
	}

	public boolean isNullable() {
		return isNullable;
	}

	public void setNullable(boolean isNullable) {
		this.isNullable = isNullable;
	}

	public Object getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}
}
