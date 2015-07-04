package com.aplos.common.persistence;

import java.sql.Connection;
import java.util.HashSet;
import java.util.Set;

import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.utils.ApplicationUtil;

public class PersistentContextTransaction {
	private boolean connectionCreated;
	
	public PersistentContextTransaction() {
	}
	
	public void commitAndCloseOrRollback() {
		if( isConnectionCreated() ) {
			ApplicationUtil.getPersistenceContext().commitAndCloseOrRollback();
		}
	}
	
	public void rollback() {
		if( isConnectionCreated() ) {
			ApplicationUtil.getPersistenceContext().rollback();
		}
	}
	
	public Connection createConnection() {
		Connection connection = ApplicationUtil.getPersistenceContext().getActiveConnection();
		if( connection == null ) {
			connection = ApplicationUtil.getPersistenceContext().getOrCreateConnection();
			setConnectionCreated(true);
		}
		return connection;
	}

	public boolean isConnectionCreated() {
		return connectionCreated;
	}

	public void setConnectionCreated(boolean connectionCreated) {
		this.connectionCreated = connectionCreated;
	}
}
