package com.aplos.common.persistence;

import java.sql.SQLException;

import com.aplos.common.persistence.type.applicationtype.ApplicationType;

public class ArrayRowDataReceiver extends RowDataReceiver {
	private Object[] dataArray;
	private Object[] additionalColumns;
	
	public ArrayRowDataReceiver( Object[] dataArray, Object[] additionalColumns ) {
		setDataArray( dataArray );
		setAdditionalColumns(additionalColumns);
	}
	
	public ArrayRowDataReceiver( Object[] dataArray ) {
		setDataArray( dataArray );
	}
	
	@Override
	public Object getObject(int columnIdx, ApplicationType applicationType) throws SQLException {
		if( columnIdx < dataArray.length ) {
			return dataArray[ columnIdx ];
		} else {
			return additionalColumns[ columnIdx - dataArray.length ];
		}
	}

	public Object[] getDataArray() {
		return dataArray;
	}

	public void setDataArray(Object[] dataArray) {
		this.dataArray = dataArray;
	}

	public Object[] getAdditionalColumns() {
		return additionalColumns;
	}

	public void setAdditionalColumns(Object[] additionalColumns) {
		this.additionalColumns = additionalColumns;
	}
}
