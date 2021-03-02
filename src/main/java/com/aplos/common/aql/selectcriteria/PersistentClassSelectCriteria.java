package com.aplos.common.aql.selectcriteria;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.aql.aqltables.AqlTable;
import com.aplos.common.aql.aqltables.AqlTableAbstract;
import com.aplos.common.aql.aqlvariables.AqlTableVariable;
import com.aplos.common.aql.aqlvariables.AqlTableVariableInter;
import com.aplos.common.aql.aqlvariables.PolymorphicIdAqlVariable;
import com.aplos.common.aql.aqlvariables.PolymorphicTypeAqlVariable;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.interfaces.PersistenceBean;
import com.aplos.common.persistence.PersistenceContext;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.RowDataReceiver;
import com.aplos.common.persistence.collection.PersistentAbstractCollection;
import com.aplos.common.persistence.fieldinfo.CollectionFieldInfo;
import com.aplos.common.persistence.fieldinfo.FieldInfo;
import com.aplos.common.persistence.fieldinfo.ForeignFieldInfo;
import com.aplos.common.persistence.fieldinfo.PolymorphicFieldInfo;
import com.aplos.common.persistence.type.applicationtype.ApplicationType;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;

public class PersistentClassSelectCriteria extends SelectCriteria implements AqlTableVariableInter {
	private static Logger logger = Logger.getLogger( PersistentClassSelectCriteria.class );
	private boolean isAllFieldsAdded = false;
	private Class beanClass;
	private List<SelectCriteria> processedSelectCriteria = new ArrayList<SelectCriteria>();
	private SelectCriteria primarySelectCriteria;
	private SelectCriteria polymorphicSelectCriteria;
	private boolean isJoinedTable = false;
	private AqlTable aqlTable;
	
	public PersistentClassSelectCriteria( AqlTable aqlTable ) {
		setAqlTable( aqlTable );
		if( aqlTable.getPersistentTable() instanceof PersistentClass ) {
			setBeanClass( ((PersistentClass) aqlTable.getPersistentTable()).getTableClass() );
		}
	}
	
	public PersistentClassSelectCriteria( AqlTable aqlTable, Class beanClass ) {
		setAqlTable( aqlTable );
		setBeanClass( beanClass );
	}
	
//	@Override
//	public void setField(boolean isLoadingFromDatabase, AplosAbstractBean bean, Object value) throws Exception {
//		getAqlTable().getParentFieldInfo().setValue(isLoadingFromDatabase, bean, value );
//	}
//	
//	@Override
//	public boolean evaluateCriteriaTypes(ProcessedBeanDao processedBeanDao, boolean isAllowingFullTables) {
//		return true;
//	}
	
//	public String getSqlPath( boolean includeGeneratedAliasForName ) {
//		return getAqlTable().determineSqlVariablePath(true) + "." + getSqlName( includeGeneratedAliasForName );
//	}
//	
//	public String getSubSqlPath( boolean includeGeneratedAliasForName ) {
//		return getAqlTable().getParentTable().determineSqlVariablePath(true) + "." + getSqlName( includeGeneratedAliasForName );
//	}
    
	@Override
    public void addSelectCriterias( List<SelectCriteria> selectCriterias ) {
    	if( getAqlVariable() != null && getAqlVariable().isDistinct() ) {
    		getPrimarySelectCriteria().getAqlVariable().setDistinct(getAqlVariable().isDistinct());
    	}
    	for( int i = 0, n = getProcessedSelectCriteria().size(); i < n; i++ ) {
    		getProcessedSelectCriteria().get( i ).addSelectCriterias(selectCriterias);
    	}
    }
	
	@Override
	public Object convertFieldValues( ProcessedBeanDao processedBeanDao, RowDataReceiver rowDataReceiver ) throws SQLException {
		PersistenceContext persistenceContext = ApplicationUtil.getPersistenceContext( true );
    	PersistentClass persistentClass = ApplicationUtil.getPersistentApplication().getPersistentClassMap().get( beanClass );
    	Object tempBaseBean = null;
    	boolean registerBean = false;
    	try {
	    	if( persistentClass == null ) {
	        	/*
	        	 * This happens for the list beans
	        	 */
	    		tempBaseBean = createNewBean( beanClass, processedBeanDao.isLoadingFromDatabase(), null );
	    	} else if( persistentClass.isEntity() 
	    			&& persistentClass.getDbPersistentClass().getDiscriminatorFieldInfo() == null ) {
	    		Long rootId = getRootId( processedBeanDao, rowDataReceiver );
	    		if( rootId != null ) {
		    		tempBaseBean = processedBeanDao.findCachedBean( persistentClass, rootId );
		    	} else if( getAqlTable() != null && getAqlTable().getParentTable() != null && isWithAllEmptyFields(processedBeanDao, rowDataReceiver) ) {
		    		return null;
		    	}
	    		if( tempBaseBean == null ) {
		    		tempBaseBean = createNewBean( beanClass, processedBeanDao.isLoadingFromDatabase(), null );
	    			if( ApplicationUtil.getAplosContextListener().isDebugMode() ) {
	    				logger.debug( "Created " + tempBaseBean.getClass().getSimpleName() + " with id " + rootId );
	    			}
	    			registerBean = true;
	    		} else if( ApplicationUtil.getAplosContextListener().isDebugMode() ) {
	    			logger.debug( "Pulled cache of " + tempBaseBean.getClass().getSimpleName() + " with id " + rootId );
	    		
	    		}
	    	} else {
	    		
	    		String mapKey;
	    		PersistentClass subPersistentClass;
	    		Long rootId = getRootId( processedBeanDao, rowDataReceiver );
	    		tempBaseBean = processedBeanDao.findCachedBean( persistentClass, rootId );
	    		
	    		if( tempBaseBean == null ) {
		    		mapKey = (String) getPolymorphicSelectCriteria().convertFieldValues(processedBeanDao, rowDataReceiver);
		    		if( persistentClass.getDbPersistentClass() != null ) {
		    			subPersistentClass = persistentClass.getDbPersistentClass().getPersistentClassFamilyMap().get( mapKey );
		    		} else {
		    			subPersistentClass = persistentClass.getPersistentClassFamilyMap().get( mapKey );
		    		}
		    		tempBaseBean = createNewBean( subPersistentClass.getTableClass(), processedBeanDao.isLoadingFromDatabase(), null );
	    			if( ApplicationUtil.getAplosContextListener().isDebugMode() ) {
	    				logger.debug( "Created polymorphic " + tempBaseBean.getClass().getSimpleName() + " with id " + rootId );
	    			}
	
	    			registerBean = true;
	    		} else if( ApplicationUtil.getAplosContextListener().isDebugMode() ) {
	    			logger.debug( "Pulled polymorphic cache of " + tempBaseBean.getClass().getSimpleName() + " with id " + rootId );
	    		}
	    	}

	    	updateBean( processedBeanDao, (AplosAbstractBean) tempBaseBean, rowDataReceiver );
	    	
	    	if( !processedBeanDao.isLoadingFromDatabase() ) {
	    		((AplosAbstractBean) tempBaseBean).saveDetails();
	    	}

	    	/*
	    	 *  This used to have getBeanDao().isCachingResults() && !InheritanceType.SINGLE_TABLE.equals( persistentClass.getInheritanceType() )
	    	 *  but I removed them as I couldn't see why you wouldn't want to cache results coming back or 
	    	 *  why you wouldn't want to cache SINGLE_TABLE inheritanceBeans
	    	 */
	    	if( registerBean && persistentClass != null && persistentClass.isEntity() ) {
	    		if( ((AplosAbstractBean) tempBaseBean).getId() != null ) {
	    			persistenceContext.registerBean( (AplosAbstractBean) tempBaseBean );
	    		}
	    	}
    	} catch( IllegalAccessException iaex ) {
			ApplicationUtil.handleError( iaex );
    	} catch( InstantiationException iex ) {
			ApplicationUtil.handleError( iex );
    	}
    	return tempBaseBean;
	}
	
	public static AplosAbstractBean createNewBean( Class newBeanClass, boolean isLoadingFromDatabase, Long id ) throws IllegalAccessException, InstantiationException {
		AplosAbstractBean tempBaseBean = (AplosAbstractBean) newBeanClass.newInstance();
		if( id != null ) {
			tempBaseBean.setId( id );
		}
		tempBaseBean.persistenceBeanCreated();
		tempBaseBean.setLazilyLoaded( isLoadingFromDatabase );
		tempBaseBean.setInDatabase( isLoadingFromDatabase );
		if( isLoadingFromDatabase ) {
			tempBaseBean.setReadOnly( true );
		}
		return tempBaseBean;
	}
	
	public void updateBean( ProcessedBeanDao processedBeanDao, AplosAbstractBean tempBaseBean, RowDataReceiver rowDataReceiver ) {
		try {
			if( ((PersistenceBean) tempBaseBean).isLazilyLoaded() || ((PersistenceBean) tempBaseBean).getId() == null ) {
				Object tempConvertedValue;
		    	for( int i = 0, n = getProcessedSelectCriteria().size(); i < n; i++ ) {/*
					 * This if statement stops single_table inheritance trying to fill fields which aren't in that
					 * particular subclass.
					 */
					if( getProcessedSelectCriteria().get( i ).getFieldInfo() == null ||
							getProcessedSelectCriteria().get( i ).getFieldInfo().getParentPersistentClass().getTableClass().isAssignableFrom( tempBaseBean.getClass() ) ) {
						if( getProcessedSelectCriteria().get( i ) instanceof PersistentClassSelectCriteria ||
								tempBaseBean.isLazilyLoaded() || !tempBaseBean.isInDatabase() ) {
							getProcessedSelectCriteria().get( i ).convertAndSetField( processedBeanDao, tempBaseBean, rowDataReceiver );
						}
					}
		    	}
	
		    	for(int i = 0, n = getAqlTable().getNonDbFieldInfos().size(); i < n; i++) {
					Class<? extends PersistentAbstractCollection> persistentCollectionType = null;
					if( getAqlTable().getNonDbFieldInfos().get( i ) instanceof CollectionFieldInfo ) {
						persistentCollectionType = ((CollectionFieldInfo) getAqlTable().getNonDbFieldInfos().get( i )).getPersistentCollectionType();
					} else if( getAqlTable().getNonDbFieldInfos().get( i ) instanceof ForeignFieldInfo ) {
						persistentCollectionType = ((ForeignFieldInfo) getAqlTable().getNonDbFieldInfos().get( i )).getPersistentCollectionType();
					}
					PersistentAbstractCollection persistentCollection = null;
					if( persistentCollectionType != null ) {
						persistentCollection = (PersistentAbstractCollection) CommonUtil.getNewInstance( persistentCollectionType );
						persistentCollection.setFieldInfo(getAqlTable().getNonDbFieldInfos().get( i ));
					}
	
					/*
					 * This if statement stops single_table inheritance trying to fill fields which aren't in that
					 * particular subclass.
					 */
					if( getAqlTable().getNonDbFieldInfos().get( i ) == null ||
							getAqlTable().getNonDbFieldInfos().get( i ).getParentPersistentClass().getTableClass().isAssignableFrom( tempBaseBean.getClass() ) ) {
						getAqlTable().getNonDbFieldInfos().get( i ).setValue( processedBeanDao.isLoadingFromDatabase(), tempBaseBean, persistentCollection);
					}
				}

				if( isAllFieldsAdded() ) {
					((PersistenceBean) tempBaseBean).addLoadedTable(getAqlTable());
				}
			}
		}	catch( Exception ex ) {
			ApplicationUtil.handleError( ex );
			throw new RuntimeException( ex );
		}
	}
	
	public boolean isWithAllEmptyFields( ProcessedBeanDao processedBeanDao, RowDataReceiver rowDataReceiver ) throws SQLException {
		int rowDataIdx;
		ApplicationType applicationType;
		List<SelectCriteria> postSelectCriteriaList = new ArrayList<SelectCriteria>();
    	for( int i = 0, n = getProcessedSelectCriteria().size(); i < n; i++ ) {
    		if( getProcessedSelectCriteria().get( i ) instanceof PersistentClassSelectCriteria ) {
    			/*
    			 * This makes it much more efficient as otherwise it can do a lot of unneccessary searching
    			 */
    			postSelectCriteriaList.add( getProcessedSelectCriteria().get( i ) );
    		} else {
	    		rowDataIdx = getProcessedSelectCriteria().get( i ).getRowDataIdx();
	    		applicationType = getProcessedSelectCriteria().get( i ).getAqlVariable().getApplicationType();
	    		if( rowDataReceiver.getObject( rowDataIdx, applicationType ) != null ) {
	    			return false;
	    		}
    		}
    	}
    	for( int i = 0, n = postSelectCriteriaList.size(); i < n; i++ ) {
    		rowDataIdx = postSelectCriteriaList.get( i ).getRowDataIdx();
    		if( postSelectCriteriaList.get( i ).convertFieldValues(processedBeanDao, rowDataReceiver) != null ) {
    			return false;
    		}
    	}
		return true;
	}
	
	@Override
	public void convertAndSetField(ProcessedBeanDao processedBeanDao,
			AplosAbstractBean tempBaseBean, RowDataReceiver rowDataReceiver) throws Exception {
		if( isJoinedTable() ) {
			updateBean(processedBeanDao, tempBaseBean, rowDataReceiver);
		} else {
			Object newBean = convertFieldValues( processedBeanDao, rowDataReceiver);
			getAqlTable().getParentFieldInfo().getField().setAccessible(true);
			getAqlTable().getParentFieldInfo().getField().set( tempBaseBean, newBean);
		}
	}
	
	public void matchPolymorphicSelectCriterias() {
		for( int i = 0, n = getProcessedSelectCriteria().size(); i < n; i++ ) {
			if( getProcessedSelectCriteria().get( i ) instanceof PersistentClassSelectCriteria ) {
				((PersistentClassSelectCriteria) getProcessedSelectCriteria().get( i )).matchPolymorphicSelectCriterias(); 
			}
			
			if( getProcessedSelectCriteria().get( i ).getAqlVariable() instanceof PolymorphicTypeAqlVariable ) {
				PolymorphicTypeAqlVariable polymorphicTypeSelectCriteria = (PolymorphicTypeAqlVariable) getProcessedSelectCriteria().get( i ).getAqlVariable();
				if( polymorphicTypeSelectCriteria.getFieldInfo() instanceof PolymorphicFieldInfo && polymorphicTypeSelectCriteria.getPolymorphicIdSelectCriteria() == null ) {
					PolymorphicFieldInfo polymorphicFieldInfo = (PolymorphicFieldInfo) polymorphicTypeSelectCriteria.getFieldInfo();
					for( int j = 0, p = getProcessedSelectCriteria().size(); j < p; j++ ) {
						if( getProcessedSelectCriteria().get( j ).getAqlVariable() instanceof PolymorphicIdAqlVariable ) {
							PolymorphicIdAqlVariable polymorphicIdSelectCriteria = (PolymorphicIdAqlVariable) getProcessedSelectCriteria().get( j ).getAqlVariable();
							if( polymorphicFieldInfo.getIdFieldInfo().equals( polymorphicIdSelectCriteria.getFieldInfo() ) ) {
								polymorphicIdSelectCriteria.setPolymorphicTypeSelectCriteria(polymorphicTypeSelectCriteria);
								polymorphicTypeSelectCriteria.setPolymorphicIdSelectCriteria(polymorphicIdSelectCriteria);
							}
						}
					}
				}
			}
		}
	}
    
	public Long getRootId( ProcessedBeanDao processedBeanDao, RowDataReceiver rowDataReceiver ) throws SQLException {
		if (getPrimarySelectCriteria() != null) {
			return (Long) getPrimarySelectCriteria().convertFieldValues(processedBeanDao, rowDataReceiver);
		}
		return null;
	}
	
	public void addPolymorphicFields( ProcessedBeanDao processedBeanDao ) {
		Boolean addPolymorphicField = null;
		SelectCriteria tempSelectCriteria;
		for( int i = 0, n = getProcessedSelectCriteria().size(); i < n; i++ ) {
			if( getProcessedSelectCriteria().get( i ) instanceof PersistentClassSelectCriteria ) {
				((PersistentClassSelectCriteria) getProcessedSelectCriteria().get( i )).addPolymorphicFields( processedBeanDao );
			}
		}
		
		PersistentClass persistentClass = (PersistentClass) getAqlTable().getPersistentTable();
		FieldInfo discriminatorFieldInfo = persistentClass.getDbPersistentClass().getDiscriminatorFieldInfo();  
		if( discriminatorFieldInfo == null ) {
			addPolymorphicField = false;
		} else {
			addPolymorphicField = true;
			for( int i = 0, n = getProcessedSelectCriteria().size(); i < n; i++ ) {
				tempSelectCriteria = getProcessedSelectCriteria().get( i );
				if( tempSelectCriteria.getAqlVariable() instanceof AqlTableVariable && discriminatorFieldInfo.equals( ((AqlTableVariable) tempSelectCriteria.getAqlVariable()).getFieldInfo() ) ) {
					addPolymorphicField = false;
					setPolymorphicSelectCriteria(tempSelectCriteria);
					break;
				}
			}
		}
		
		if( addPolymorphicField ) {
			tempSelectCriteria = new SelectCriteria( new AqlTableVariable(getAqlTable(), discriminatorFieldInfo) );
			tempSelectCriteria.getAqlVariable().evaluateCriteriaTypes( processedBeanDao, false );
			setPolymorphicSelectCriteria(tempSelectCriteria);
			getProcessedSelectCriteria().add( tempSelectCriteria );
		}
	}

	public void addProcessedSelectCriteria( SelectCriteria processedSelectCriteria ) {
		getProcessedSelectCriteria().add( processedSelectCriteria );
		if( processedSelectCriteria.getFieldInfo() != null && processedSelectCriteria.getFieldInfo().isPrimaryKey() ) {
			setPrimarySelectCriteria( processedSelectCriteria );
		}
	}
	
	public void addAllCriteria( ProcessedBeanDao processedBeanDao ) {
		List<SelectCriteria> selectCriteriaList = new ArrayList<SelectCriteria>();
		getAqlTable().addAllCriteria( processedBeanDao, selectCriteriaList );
		
		for( AqlTableAbstract tempAqlTable : processedBeanDao.getQueryTables().values() ) {
			if( tempAqlTable instanceof AqlTable ) {
				if( ((AqlTable) tempAqlTable).getParentTable() != null && ((AqlTable) tempAqlTable).getParentTable().equals( getAqlTable() ) && ((AqlTable) tempAqlTable).isAddingAllCriteria() ) {
					for( int i = 0, n = selectCriteriaList.size(); i < n; i++ ) {
						if( selectCriteriaList.get(i).getAqlVariable() != null && selectCriteriaList.get(i).getAqlVariable().getName().equals( ((AqlTable) tempAqlTable).getExternalVariable().getSqlName(true) ) ) {
							selectCriteriaList.set(i, ((AqlTable) tempAqlTable).getPersistentClassSelectCriteria());
							((AqlTable) tempAqlTable).getPersistentClassSelectCriteria().addAllCriteria(processedBeanDao);
							break;
						}
					}
				}
			}
		}
		for( int j = 0, p = selectCriteriaList.size(); j < p; j++ ) {
			addProcessedSelectCriteria( selectCriteriaList.get( j ) );
		}
		if(getAqlTable().getJoinedTable() != null) {
			PersistentClassSelectCriteria joinedClassSelectCriteria = getAqlTable().getJoinedTable().getPersistentClassSelectCriteria();
			joinedClassSelectCriteria.addAllCriteria(processedBeanDao);
			joinedClassSelectCriteria.setJoinedTable( true );
			addProcessedSelectCriteria( joinedClassSelectCriteria );
		}
	}
	
	public AqlTable getAqlTable() {
		return (AqlTable) getAqlTableAbstract();
	}
	
	public void setAqlTable( AqlTable aqlTable ) {
		this.aqlTable = aqlTable;
	}
	
	@Override
	public AqlTableAbstract getAqlTableAbstract() {
		return aqlTable;
	}

	@Override
	public void setAqlTableAbstract(AqlTableAbstract aqlTableAbstract) {
		this.aqlTable = (AqlTable) aqlTableAbstract;
	}

	public List<SelectCriteria> getProcessedSelectCriteria() {
		return processedSelectCriteria;
	}

	public void setProcessedSelectCriteria(List<SelectCriteria> processedSelectCriteria) {
		this.processedSelectCriteria = processedSelectCriteria;
	}

	public SelectCriteria getPrimarySelectCriteria() {
		return primarySelectCriteria;
	}

	public void setPrimarySelectCriteria(SelectCriteria primarySelectCriteria) {
		this.primarySelectCriteria = primarySelectCriteria;
	}

//	public boolean isLoadingFields() {
//		return isLoadingFields;
//	}
//
//	public void setLoadingFields(boolean isLoadingFields) {
//		this.isLoadingFields = isLoadingFields;
//	}

	public boolean isAllFieldsAdded() {
		return isAllFieldsAdded;
	}

	public void setAllFieldsAdded(boolean isAllFieldsAdded) {
		this.isAllFieldsAdded = isAllFieldsAdded;
	}

	public Class getBeanClass() {
		return beanClass;
	}

	public void setBeanClass(Class beanClass) {
		this.beanClass = beanClass;
	}

	public SelectCriteria getPolymorphicSelectCriteria() {
		return polymorphicSelectCriteria;
	}

	public void setPolymorphicSelectCriteria(SelectCriteria polymorphicSelectCriteria) {
		this.polymorphicSelectCriteria = polymorphicSelectCriteria;
	}

	public boolean isJoinedTable() {
		return isJoinedTable;
	}

	public void setJoinedTable(boolean isJoinedTable) {
		this.isJoinedTable = isJoinedTable;
	}
}
