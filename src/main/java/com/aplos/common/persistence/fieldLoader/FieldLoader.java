package com.aplos.common.persistence.fieldLoader;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.aql.aqltables.AqlTable;
import com.aplos.common.aql.aqlvariables.CollectionInformation;
import com.aplos.common.aql.aqlvariables.AqlTableVariable;
import com.aplos.common.aql.selectcriteria.PersistentClassSelectCriteria;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.interfaces.PersistenceBean;
import com.aplos.common.module.ModuleUpgrader;
import com.aplos.common.persistence.ArrayRowDataReceiver;
import com.aplos.common.persistence.PersistenceContext;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.PersistentContextTransaction;
import com.aplos.common.persistence.collection.PersistentList;
import com.aplos.common.persistence.fieldinfo.FieldInfo;
import com.aplos.common.persistence.fieldinfo.PersistentCollectionFieldInfo;
import com.aplos.common.utils.ApplicationUtil;

public abstract class FieldLoader {
	private static Logger logger = Logger.getLogger( ModuleUpgrader.class );
	private String propertyName;
	private PersistentClass persistentClass;
	private FieldInfo fieldInfo;
	
	public FieldLoader( String propertyName, PersistentClass persistentClass, FieldInfo fieldInfo ) {
		this.setPropertyName(propertyName);
		this.setPersistentClass(persistentClass);
		this.setFieldInfo(fieldInfo);
	}
	
	public List getCollectionResults( ProcessedBeanDao processedBeanDao, AqlTableVariable variableSelectCriteria, AplosAbstractBean parentBean, CollectionInformation collectionInformation ) {
		processedBeanDao.getBeanDao().getUnprocessedSelectCriteriaList().clear();
		processedBeanDao.getBeanDao().setInitialisingPaths(false);
		AqlTable intermediateTable = collectionInformation.getIntermediateTable();

		AqlTable leafTable;
		if( collectionInformation.getCollectionTable() != null ) {
			leafTable = collectionInformation.getCollectionTable();
		} else {
			leafTable = intermediateTable;
		}
		
		leafTable.addAllCriteria(processedBeanDao,processedBeanDao.getBeanDao().getUnprocessedSelectCriteriaList());
		processedBeanDao.setGeneratingBeans(false);
		processedBeanDao.getBeanDao().setCreatingPersistentSelectCriteria(false);
		processedBeanDao.evaluateCriteriaTypes();

		processedBeanDao.matchPolymorphicSelectCriterias();
		
		boolean isAddingCollectionTable = collectionInformation.getCollectionTable() != null && !collectionInformation.isPolymorphic();
		
		StringBuffer sqlBuf = new StringBuffer( "SELECT " );
		sqlBuf.append( processedBeanDao.getSelectCriteria() );
		sqlBuf.append( " FROM " );
		sqlBuf.append( intermediateTable.determineDbTableName() );
		sqlBuf.append( " AS " ).append( intermediateTable.getAlias() );
		if( isAddingCollectionTable ) {
			sqlBuf.append( " LEFT OUTER JOIN " );
			sqlBuf.append( collectionInformation.getCollectionTable().determineDbTableName() );
			sqlBuf.append( " AS " ).append( collectionInformation.getCollectionTable().getAlias() );
			sqlBuf.append( " ON " ).append( intermediateTable.getAlias() );
			sqlBuf.append( "." ).append( collectionInformation.getCollectionTable().getExternalVariable().getName() );
			sqlBuf.append( " = " ).append( collectionInformation.getCollectionTable().getAlias() );
			sqlBuf.append( "." ).append( collectionInformation.getCollectionTable().getInternalVariable().getName() );
		}
		sqlBuf.append( " WHERE " ).append( intermediateTable.getAlias() );
		sqlBuf.append( "." ).append( intermediateTable.getInternalVariable().getName() );
		sqlBuf.append( " = " ).append( String.valueOf( parentBean.getId() ) );
		if( isAddingCollectionTable ) {
			sqlBuf.append( " AND " ).append( intermediateTable.getAlias() );
			sqlBuf.append( "." ).append( collectionInformation.getCollectionTable().getExternalVariable().getName() );
			sqlBuf.append( " IS NOT NULL" );
		}

		if( ApplicationUtil.getAplosContextListener().isDebugMode() ) {
			logger.info( getClass().getSimpleName() + " : " + sqlBuf.toString() );
		}
		
		List resultList = null;
		PersistentContextTransaction persistentContextTransaction = new PersistentContextTransaction();
		try {
			Connection conn = persistentContextTransaction.createConnection();
			resultList = ApplicationUtil.getResults( conn, processedBeanDao.getPreparedStatement( conn, sqlBuf.toString() ) );

			if( collectionInformation.isPolymorphic() ) {
				PersistentList newResultList = new PersistentList();
				newResultList.setFieldInfo( variableSelectCriteria.getFieldInfo() );
	    		int polymorphicCriteriaIdx = -1;
	    		
	    		PersistentCollectionFieldInfo persistentCollectionFieldInfo = (PersistentCollectionFieldInfo) variableSelectCriteria.getFieldInfo(); 
	    		String mapKey;
	    		PersistentClass subPersistentClass;
	    		PersistentClass persistentClass = persistentCollectionFieldInfo.getPersistentClass();
	    		Object tempBaseBean;
	    		PersistenceContext persistenceContext = ApplicationUtil.getPersistenceContext( true );
	    		int idIdx = -1;
	    		for( int i = 0, n = processedBeanDao.getProcessedSelectCriteriaList().size(); i < n; i ++ ) {
	    			if( persistentCollectionFieldInfo.equals( processedBeanDao.getProcessedSelectCriteriaList().get( i ).getFieldInfo() ) ) {
	    				idIdx = i;
	    			} else if( persistentCollectionFieldInfo.getPolymorphicFieldInfo().equals( processedBeanDao.getProcessedSelectCriteriaList().get( i ).getFieldInfo() ) ) {
	    				polymorphicCriteriaIdx = i;
	    			}
	    		}
		    	for( int i = 0, n = resultList.size(); i < n; i++ ) {
		    		Long id = (Long) ((Object[]) resultList.get( i ))[ idIdx ];
		    		mapKey = (String) ((Object[]) resultList.get( i ))[ polymorphicCriteriaIdx ];
		    		if( persistentClass.getDbPersistentClass() != null ) {
		    			subPersistentClass = persistentClass.getDbPersistentClass().getPersistentClassFamilyMap().get( mapKey );
		    		} else {
		    			subPersistentClass = persistentClass.getPersistentClassFamilyMap().get( mapKey );
		    		}
		    		
		    		if( subPersistentClass != null ) {
			    		tempBaseBean = persistenceContext.findBean( (Class<? extends AplosAbstractBean>) subPersistentClass.getDbPersistentClass().getTableClass(), id );
			    		
			    		if( tempBaseBean == null ) {
				    		tempBaseBean = subPersistentClass.getTableClass().newInstance();
				    		persistentCollectionFieldInfo.getParentPersistentClass().determinePrimaryKeyFieldInfo().setValue( processedBeanDao.isLoadingFromDatabase(), (AplosAbstractBean) tempBaseBean, id );
				    		((PersistenceBean)tempBaseBean).setLazilyLoaded(true);
				    		((PersistenceBean)tempBaseBean).setInDatabase(true);
			    		}
						newResultList.add( tempBaseBean );
		    		} else {
		    			String tableName = variableSelectCriteria.getAqlTable().getPersistentTable().determineSqlTableName();
		    			String variableName = variableSelectCriteria.getField().getName();
		    			ApplicationUtil.handleError( new Exception( "Could not find match for key " + mapKey + " on field " + tableName + "." + variableName ), false);
		    		}
		    	}
		    	resultList = newResultList;
			} else if( collectionInformation.getCollectionTable() != null ) {
//	    		for( int i = 0, n = processedBeanDao.getProcessedSelectCriteriaList().size(); i < n; i ++ ) {
//	    			if( processedBeanDao.getProcessedSelectCriteriaList().get( i ).getFieldInfo() != null
//	    					&& processedBeanDao.getProcessedSelectCriteriaList().get( i ).getFieldInfo().isPrimaryKey() ) {
//	    				processedBeanDao.setRootIdCriteria( processedBeanDao.getProcessedSelectCriteriaList().get( i ) );
//	    				break;
//	    			}
//	    		}
				PersistentClassSelectCriteria persistentClassSelectCriteria = new PersistentClassSelectCriteria( leafTable );
				persistentClassSelectCriteria.setProcessedSelectCriteria( processedBeanDao.getProcessedSelectCriteriaList() );
				persistentClassSelectCriteria.addPolymorphicFields(processedBeanDao);
				persistentClassSelectCriteria.matchPolymorphicSelectCriterias();
				List newResultList = new ArrayList();
				for( int i = 0, n = resultList.size(); i < n; i++ ) {
					newResultList.add( persistentClassSelectCriteria.convertFieldValues( processedBeanDao, new ArrayRowDataReceiver( (Object[]) resultList.get( i ) ) ) );
				}
				resultList = newResultList;
//				resultList = processedBeanDao.find( resultList, ((PersistentClass)collectionInformation.getCollectionTable().getPersistentTable()).getTableClass() );
			} else {
				List newResultList = new ArrayList<Object[]>();
				Object[] tempObjAry;
				for( int i = 0, n = resultList.size(); i < n; i++ ) {
					tempObjAry = new Object[ ((Object[]) resultList.get( i )).length ];
					for( int j = 0, p = tempObjAry.length; j < p; j++ ) {
						if( processedBeanDao.getProcessedSelectCriteria( j ).getField() != null ) {
							tempObjAry[ j ] = processedBeanDao.getProcessedSelectCriteria( j ).convertFieldValue( ((Object[]) resultList.get( i ))[ j ] );
						} else {
							tempObjAry[ j ] = ((Object[]) resultList.get( i ))[ j ];
						}
					}
					newResultList.add( tempObjAry );
				}
				return newResultList;
			}
		} catch( SQLException sqlEx ) {
			ApplicationUtil.handleError(sqlEx);
		} catch( Exception ex ) {
			ApplicationUtil.handleError(ex);
		} finally {
			persistentContextTransaction.commitAndCloseOrRollback();
		}
		return resultList;
	}
	
	public abstract Object getValue( PersistenceBean targetBean, Object returnedObject, boolean isTargetBeanLoaded );

	public String getPropertyName() {
		return propertyName;
	}

	private void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	protected FieldInfo getFieldInfo() {
		return fieldInfo;
	}

	private void setFieldInfo(FieldInfo fieldInfo) {
		this.fieldInfo = fieldInfo;
	}

	public PersistentClass getPersistentClass() {
		return persistentClass;
	}

	public void setPersistentClass(PersistentClass persistentClass) {
		this.persistentClass = persistentClass;
	}
}
