package com.aplos.common.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.aplos.common.persistence.type.applicationtype.ApplicationType;

public class ResultSetRowDataReceiver extends RowDataReceiver {
	private ResultSet resultSet;
	
	public ResultSetRowDataReceiver( ResultSet resultSet ) {
		setResultSet( resultSet );
	}
	
	@Override
	public Object getObject(int columnIdx, ApplicationType applicationType) throws SQLException {
		if( applicationType != null ) {
			return applicationType.getResultSetObject(getResultSet(), columnIdx + 1);
		} else {
			return getResultSet().getObject( columnIdx + 1 );
		}
	}

	public ResultSet getResultSet() {
		return resultSet;
	}

	public void setResultSet(ResultSet resultSet) {
		this.resultSet = resultSet;
	}

}
