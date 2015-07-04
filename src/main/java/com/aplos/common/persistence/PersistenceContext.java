package com.aplos.common.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.aplos.common.aql.BeanMap;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;

public class PersistenceContext {
	private Connection connection;
	/*
	 * This is static at the moment which means that eventually everything from the database will
	 * be held in memory.  This should be replaced by a central facilty that manages what stays in 
	 * the DB as well as this private cache which would get replaced each context.
	 */
	private static Map<Class<? extends AplosAbstractBean>,Map<Long,AplosAbstractBean>> beanMap = new HashMap<Class<? extends AplosAbstractBean>,Map<Long,AplosAbstractBean>>();
	
	private List<AplosAbstractBean> newBeans;
	
	public AplosAbstractBean findBean( Class<? extends AplosAbstractBean> beanClass, Long id ) {
		Map<Long,AplosAbstractBean> innerBeanMap = beanMap.get( beanClass );
		if( innerBeanMap != null ) {
			return innerBeanMap.get( id );
		}
		return null;
	}
	
	public void removeBean( AplosAbstractBean aplosAbstractBean ) {
		PersistentClass persistentClass = ApplicationUtil.getPersistentClass( aplosAbstractBean.getClass() );
		if( persistentClass != null ) {
			Map<Long,AplosAbstractBean> innerBeanMap = beanMap.get( persistentClass.getDbPersistentClass().getTableClass() );
			if( innerBeanMap != null ) {
				innerBeanMap.remove( aplosAbstractBean.getId() );
			}
		}
	}
	
	public void registerBean( AplosAbstractBean aplosAbstractBean ) {
		PersistentClass persistentClass = ApplicationUtil.getPersistentApplication().getPersistentClassMap().get( aplosAbstractBean.getClass() );
		/*
		 * PersistentClass can sometimes be null with the ListBeans
		 */
		if( persistentClass != null ) {
			Map<Long,AplosAbstractBean> innerBeanMap = beanMap.get( persistentClass.getDbPersistentClass().getTableClass() );
			if( innerBeanMap == null ) {
				innerBeanMap = new HashMap<Long,AplosAbstractBean>(); 
				beanMap.put( (Class<? extends AplosAbstractBean>) persistentClass.getDbPersistentClass().getTableClass(), innerBeanMap );
			}
			if( innerBeanMap.get( aplosAbstractBean.getId() ) == null ) {
				if( aplosAbstractBean.isReadOnly() ) {
					innerBeanMap.put( aplosAbstractBean.getId(), aplosAbstractBean );
				}
			}
		}
	}
	
	public void updateCachedBean( AplosAbstractBean aplosAbstractBean ) {
		PersistentClass persistentClass = ApplicationUtil.getPersistentApplication().getPersistentClassMap().get( aplosAbstractBean.getClass() );
		AplosAbstractBean cachedBean = findBean( (Class<? extends AplosAbstractBean>) persistentClass.getDbPersistentClass().getTableClass(), aplosAbstractBean.getId() );
		if( cachedBean != null ) {
			synchronized (cachedBean) {
				persistentClass.copyFieldsExceptPersistentClasses( aplosAbstractBean, cachedBean );
			}
		}
	}
	
	public void beginTransaction() {
		if( connection != null ) {
			ApplicationUtil.handleError( new Exception( "Cannot begin transaction, connection already exists"));
		}
		try {
			setConnection( ApplicationUtil.getConnection() );
			getConnection().setAutoCommit( false );
			newBeans = new ArrayList<AplosAbstractBean>();
		} catch( SQLException sqlEx ) {
			ApplicationUtil.handleError( sqlEx );
		}
	}
	
	public void commitTransaction() {
		try {
			if( getConnection() != null ) {
				getConnection().commit();
				ApplicationUtil.closeConnection(getConnection());
				setConnection(null);
				for( int i = getNewBeans().size() - 1; i >= 0; i-- ) {
					AplosAbstractBean copiedBean = newBeans.get( i ).getPersistenceCopyBean( new BeanMap() );
					ApplicationUtil.getPersistenceContext().registerBean(copiedBean);
					getNewBeans().remove( i );
				}
			}
		} catch( SQLException sqlEx ) {
			ApplicationUtil.handleError( sqlEx );
		}
	}
	
	public void commitAndCloseOrRollback() {
		if( getConnection() != null ) {
			commitTransaction();
			if( getConnection() != null ) {
				rollback();
			}
			if( getConnection() != null ) {
				closeConnection();
			}
		}
	}
	
	public void commitAndBeginNewTransaction() {
		commitTransaction();
		beginTransaction();
	}
	
	public Connection getOrCreateConnection() {
		if( getConnection() == null ) {
			beginTransaction();
		}
		return getConnection();
	}
	
	public void rollback() {
		try {
			getConnection().rollback();
			setConnection(null);
	    } catch(SQLException sqlEx2) {
	    	ApplicationUtil.handleError( sqlEx2 );
	    }
	}
	
	public Connection getActiveConnection() {
		try {
			if( getConnection() != null && !getConnection().isClosed() ) {
				return getConnection();
			}
		} catch( SQLException sqlEx ) {
			ApplicationUtil.handleError( sqlEx );
		}
		
		if( getConnection() != null ) {
			ApplicationUtil.handleError( new Exception( "Connection not closed properly" ) );
		}
		return null;
	}
	
	public void closeConnection() {
		ApplicationUtil.closeConnection(getConnection());
		setConnection( null );
	}

	public Connection getConnection() {
		return connection;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	public Map<Class<? extends AplosAbstractBean>,Map<Long,AplosAbstractBean>> getBeanMap() {
		return beanMap;
	}

	public void setBeanMap(Map<Class<? extends AplosAbstractBean>,Map<Long,AplosAbstractBean>> beanMap) {
		this.beanMap = beanMap;
	}

	private List<AplosAbstractBean> getNewBeans() {
		return newBeans;
	}

	public void addNewBeans(AplosAbstractBean newBean) {
		getNewBeans().add(newBean);
	}
}
