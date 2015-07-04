package com.aplos.common.persistence;

import java.sql.SQLException;

import com.aplos.common.persistence.type.applicationtype.ApplicationType;

public abstract class RowDataReceiver {
	public abstract Object getObject( int columnIdx, ApplicationType applicationType ) throws SQLException;
}
