package com.aplos.common.aql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.aplos.common.aql.antlr.AqlParser;
import com.aplos.common.aql.aqltables.AqlSubTable;
import com.aplos.common.aql.aqltables.AqlTable;
import com.aplos.common.aql.aqltables.AqlTableAbstract;
import com.aplos.common.aql.aqltables.UnionAqlTable;
import com.aplos.common.aql.aqltables.unprocessed.UnprocessedAqlSubTable;
import com.aplos.common.aql.aqlvariables.AqlTableVariable;
import com.aplos.common.aql.aqlvariables.AqlVariable;
import com.aplos.common.aql.aqlvariables.CaseAqlVariable;
import com.aplos.common.aql.aqlvariables.EnumDeterminantAqlVariable;
import com.aplos.common.aql.selectcriteria.PersistentClassSelectCriteria;
import com.aplos.common.aql.selectcriteria.SelectCriteria;
import com.aplos.common.aql.selectcriteria.TransferredCriteria;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.interfaces.PersistenceBean;
import com.aplos.common.persistence.JunctionTable;
import com.aplos.common.persistence.PersistenceContext;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.ResultSetRowDataReceiver;
import com.aplos.common.persistence.RowDataReceiver;
import com.aplos.common.persistence.fieldinfo.FieldInfo;
import com.aplos.common.persistence.fieldinfo.PersistentClassFieldInfo;
import com.aplos.common.persistence.fieldinfo.PersistentCollectionFieldInfo;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;

public class ProcessedBeanDao {
	private static Logger logger = Logger.getLogger( ProcessedBeanDao.class );
	private BeanDao beanDao;
	private List<SelectCriteria> processedSelectCriteriaList = new ArrayList<SelectCriteria>();
	private HashMap<String,AqlTableAbstract> queryTables = new HashMap<String,AqlTableAbstract>();
//	private SelectCriteria rootIdCriteria;
	private List<SelectCriteria> priorityCriteriaList;
	private WhereConditionGroup processedWhereConditionGroup;
	private ProcessedBeanDao parentProcessedBeanDao;
	private List<OrderByCriteria> orderByCriteria = new ArrayList<OrderByCriteria>();
	private List<AqlVariable> groupByCriteria = new ArrayList<AqlVariable>();
	private boolean isCriteriaTypesEvaluated = false;
	private AqlTableAbstract rootTable;
	private boolean isGeneratingBeans = true;
	private boolean isLoadingFromDatabase = true;
	private List<String> namedParameters = new ArrayList<String>();
	
	public ProcessedBeanDao( BeanDao beanDao, ProcessedBeanDao parentProcessedBeanDao ) {
		setBeanDao( beanDao );
		setParentProcessedBeanDao(parentProcessedBeanDao);
	}
	
	public ProcessedBeanDao( BeanDao beanDao ) {
		setBeanDao( beanDao );
	}
	
	public AqlTableAbstract findTable( String tablePath ) {
		AqlTableAbstract aqlTable = getQueryTables().get( tablePath );
		
		if( aqlTable == null ) {
			for( AqlTableAbstract tempAqlTable : getQueryTables().values() ) {
				if( tempAqlTable.getAlias() != null && tempAqlTable.getAlias().equals( tablePath ) ) {
					aqlTable = tempAqlTable;
					break;
				}
			}
		}
		
		if( aqlTable == null && getParentProcessedBeanDao() != null ) {
			aqlTable = getParentProcessedBeanDao().findTable( tablePath );
		}
		
		return aqlTable;
	}
	
	public AqlTable createAqlTable( PersistentClass persistentClass, AqlTable parentTable, FieldInfo fieldInfo, String tablePath, String variableName, boolean addTableAfterCreation ) { 
		if( persistentClass != null ) {
			if( parentTable.getJoinedTable() != null ) {
				while( parentTable.findFieldInfo(fieldInfo.getSqlName()) == null ) {
					parentTable = parentTable.getJoinedTable();
				}
			}
			AqlTable aqlTable = new AqlTable( this, persistentClass, parentTable, null, fieldInfo, true );
			if( addTableAfterCreation ) {
				addQueryTable( tablePath, aqlTable );
			}
//			aqlTable.setVariableName( variableName );
			aqlTable.setJoinInferred( true );
			aqlTable.setParentFieldInfo(fieldInfo);
			aqlTable.evaluateAqlTable(this);
			return aqlTable;
		} else {
			ApplicationUtil.getAplosContextListener().handleError( new Exception( "No class found for " + tablePath ) );
			return null;
		}
	}
	
	public AplosAbstractBean populateNewBean( RowDataReceiver rowDataReceiver, boolean saveBean ) {
		PersistentClassSelectCriteria persistentClassSelectCriteria = (PersistentClassSelectCriteria) getProcessedSelectCriteriaList().get( 0 );
		setLoadingFromDatabase( false );
		/*
		 * This sets the rowIdx in the SelectCriteria
		 */
		getSelectCriteria();
		AplosAbstractBean aplosAbstractBean = null;
		try {
			aplosAbstractBean = (AplosAbstractBean) persistentClassSelectCriteria.convertFieldValues( this, rowDataReceiver );
		} catch( SQLException sqlEx ) {
			ApplicationUtil.handleError(sqlEx);
		}
		return aplosAbstractBean;
	}
	
	public void addNamedParameter( String namedParameter ) {
		getNamedParameters().add( namedParameter );
	}

	public SelectCriteria getProcessedSelectCriteria( int idx ) {
		return getProcessedSelectCriteriaList().get( idx );
	}
	
	public void evaluateCriteriaTypes() {
		SelectCriteria tempSelectCriteria;
		List<SelectCriteria> tempCriteriaList = new ArrayList<SelectCriteria>();
		PersistentClassSelectCriteria beanClassSelectCriteria = null;
		if( isGeneratingBeans() ) {
			if( getRootTable() instanceof UnionAqlTable ) {
				PersistentClass persistentClass = ApplicationUtil.getPersistentClass(getBeanDao().getBeanClass());
				beanClassSelectCriteria = new PersistentClassSelectCriteria( new AqlTable( this, persistentClass, null, getBeanDao().getRootAlias() ) );
			} else {
				beanClassSelectCriteria = ((AqlTable) getRootTable()).getPersistentClassSelectCriteria();
			}
			beanClassSelectCriteria.setBeanClass( getBeanDao().determineInstantingBeanClass() );
		}
		
		AqlTableVariable tempAqlTableVariable;
		for( int i = getBeanDao().getUnprocessedSelectCriteriaList().size() - 1; i > -1; i-- ) {
			tempSelectCriteria = getBeanDao().getUnprocessedSelectCriteriaList().get( i );
			tempAqlTableVariable = null;
			tempSelectCriteria.evaluateCriteriaTypes( this, true );
			if( tempSelectCriteria.getAqlVariable() instanceof AqlTableVariable ) {
				tempAqlTableVariable = (AqlTableVariable) tempSelectCriteria.getAqlVariable();
				if( tempAqlTableVariable.isFullTable()) {
					PersistentClassSelectCriteria persistentClassSelectCriteria = tempAqlTableVariable.getAqlTable().getPersistentClassSelectCriteria();
					persistentClassSelectCriteria.setAqlVariable(tempAqlTableVariable);
					persistentClassSelectCriteria.addAllCriteria( this );
					tempSelectCriteria = persistentClassSelectCriteria;
					/*
					 * This covers the case when the user has referenced the root table alias in the select clause
					 * this can happen when DISTINCT is added for example.
					 */
					if( beanClassSelectCriteria != null && beanClassSelectCriteria.getAqlTable().equals( tempAqlTableVariable.getAqlTable() ) ) {
						beanClassSelectCriteria = null;
					}
				}
			}
			
			if( beanClassSelectCriteria != null ) {
				if( tempSelectCriteria.getAqlVariable() instanceof AqlTableVariable
						&& ((AqlTableVariable) tempSelectCriteria.getAqlVariable()).getAqlTableAbstract() instanceof AqlTable ) {
					AqlTable tempAqlTable = ((AqlTableVariable) tempSelectCriteria.getAqlVariable()).getAqlTable(); 
					if( !tempAqlTable.getPersistentClassSelectCriteria().equals( tempSelectCriteria ) ) {
						tempAqlTable.getPersistentClassSelectCriteria().addProcessedSelectCriteria( tempSelectCriteria );
					}
					tempAqlTable.includeInParents();
				} else {
					beanClassSelectCriteria.addProcessedSelectCriteria( tempSelectCriteria );
				}
			} else {
				tempCriteriaList.add( 0, tempSelectCriteria );
			}
			
			/* 
			 * Is this needed anymore?
			 */
			if( tempAqlTableVariable != null && tempAqlTableVariable.getFieldInfo() instanceof PersistentCollectionFieldInfo
					&& tempAqlTableVariable.getAqlTable().getPersistentTable() instanceof PersistentClass ) {
				PersistentClass persistentClass = (PersistentClass) tempAqlTableVariable.getAqlTable().getPersistentTable();
				JunctionTable collectionTable = ((PersistentCollectionFieldInfo) tempAqlTableVariable.getFieldInfo()).getJunctionTable( persistentClass.getTableClass() );

				findOrCreateAqlTable(collectionTable.getSqlTableName(), false);
			}
		}

		if( tempCriteriaList.size() == 0 ) {
			if( beanClassSelectCriteria != null ) {
				if( beanClassSelectCriteria.getProcessedSelectCriteria().size() == 0 ) {
					beanClassSelectCriteria.addAllCriteria( this );
				}
			} else {
				getRootTable().addAllCriteria( this, tempCriteriaList );
			}
		}
		
		if( beanClassSelectCriteria != null ) {
			tempCriteriaList.add( beanClassSelectCriteria );
		}

		for( int i = 0, n = tempCriteriaList.size(); i < n; i++ ) {
			if( tempCriteriaList.get( i ) instanceof PersistentClassSelectCriteria ) {
				((PersistentClassSelectCriteria) tempCriteriaList.get( i )).addPolymorphicFields( this );
			}
		}

		for( AqlTableAbstract tempAqlTable : getQueryTables().values() ) {
			if( tempAqlTable instanceof AqlSubTable && tempAqlTable.getParentTable() != null ) {
				List<SelectCriteria> childPriorityCriteriaList = ((AqlSubTable) tempAqlTable).getProcessedBeanDao().getProcessedSelectCriteriaList();
				for( int i = 0, n = childPriorityCriteriaList.size(); i < n; i++ ) {
					tempCriteriaList.add( new TransferredCriteria( this, childPriorityCriteriaList.get( i ), tempAqlTable ) );
				}
			}
		}
		
//		AqlTableVariable tempAqlTableSelectCriteria;
//		for( int i = 0, n = tempCriteriaList.size(); i < n; i++ ) {
//			if( tempCriteriaList.get( i ) instanceof AqlTableVariable ) {
//				tempAqlTableSelectCriteria = (AqlTableVariable) tempCriteriaList.get( i );
//				if( tempAqlTableSelectCriteria.getFieldInfo() != null 
//						&& tempAqlTableSelectCriteria.getFieldInfo().isPrimaryKey() 
//						&& tempAqlTableSelectCriteria.getAqlTable().equals( getRootTable() ) ) {
//					setRootIdCriteria( tempCriteriaList.get( i ) );
//					break;
//				}
//			}
//		}
		setProcessedSelectCriteriaList(tempCriteriaList);
		evaluateOrderByCriteria();
		evaluateGroupByCriteria();
		

		processWhereCriteria();

		for( AqlTableAbstract tempAqlTable : getQueryTables().values() ) {
			tempAqlTable.optimiseSelectCriteria();
		}
	}
	
	public void preprocessCriteria() {
		if( !isCriteriaTypesEvaluated() ) {
			getProcessedSelectCriteriaList().clear();
			getQueryTables().clear();
			for( AqlTableAbstract tempAqlTable : getQueryTables().values() ) {
				tempAqlTable.getNonDbFieldInfos().clear();
			}
			setProcessedWhereConditionGroup(new WhereConditionGroup(getBeanDao().getWhereConditionGroup()));
			getBeanDao().preCriteriaEvaluation(this);
			
			setRootTable( getBeanDao().createRootTable(this) );
			addQueryTable( getBeanDao().getRootAlias(), getRootTable() );
			
			if( getRootTable() instanceof AqlTable && ((AqlTable) getRootTable()).getPersistentTable() != null ) {
				logger.debug( "Preprocess criteria for root table " + ((AqlTable) getRootTable()).getPersistentTable().determineSqlTableName() );
			}
			
			getBeanDao().processAqlTables( this );
			
			for( UnprocessedAqlSubTable unprocessedAqlSubTable : getBeanDao().getSubQueryTables() ) {
				unprocessedAqlSubTable.createAndAddSubAqlTable(this);
			}
			
			List<AqlTableAbstract> aqlTableList = new ArrayList<AqlTableAbstract>(getQueryTables().values());
			for( AqlTableAbstract aqlTable : aqlTableList ) {
				aqlTable.evaluateAqlTable(this);
			}
			for( AqlTableAbstract aqlTable : getQueryTables().values() ) {
				if( aqlTable instanceof AqlSubTable ) {
					((AqlSubTable) aqlTable).getProcessedBeanDao().preprocessCriteria();
				} 
			}
			evaluateCriteriaTypes();
			if( !new Boolean( false ).equals( getBeanDao().getIsStrictPolymorphism() ) ) {
				processInheritanceTables();
			}
		}
		getBeanDao().setLatestTableIdx( generateAliases( getBeanDao().getLatestTableIdx() ) );
		if( !isCriteriaTypesEvaluated() ) {
			createPriorityCriteria();
			matchPolymorphicSelectCriterias();
			setCriteriaTypesEvaluated(true);
		}
	}
	
	public void matchPolymorphicSelectCriterias() {
		for( int i = 0, n = getProcessedSelectCriteriaList().size(); i < n; i++ ) {
			if( getProcessedSelectCriteriaList().get( i ) instanceof PersistentClassSelectCriteria ) {
				((PersistentClassSelectCriteria) getProcessedSelectCriteriaList().get( i )).matchPolymorphicSelectCriterias(); 
			}
		}
	}
	
	public void processInheritanceTables() {
		for( AqlTableAbstract tempAqlTable : getQueryTables().values() ) {
			if( tempAqlTable.isIncludedInJoin() ) {
				tempAqlTable.evaluateInheritance();
			}
		}
	}
	
	public int generateAliases( int currentTableIdx ) {
		for( AqlTableAbstract tempAqlTable : getQueryTables().values() ) {
			if( tempAqlTable.getAlias() == null ) {
				if( CommonUtil.isNullOrEmpty( tempAqlTable.getAlias() ) ) {
					currentTableIdx = tempAqlTable.generateAlias( currentTableIdx );
				}
			}
			if( tempAqlTable instanceof AqlSubTable ) {
				currentTableIdx = ((AqlSubTable) tempAqlTable).getProcessedBeanDao().generateAliases( currentTableIdx );
			}
		}
		return currentTableIdx;
	}
	
	public void createPriorityCriteria() {
		setPriorityCriteriaList(new ArrayList<SelectCriteria>());

		for( int i = 0, n = getProcessedSelectCriteriaList().size(); i < n; i++ ) {
			if( getProcessedSelectCriteriaList().get( i ).getAqlVariable() instanceof CaseAqlVariable ) {
				getPriorityCriteriaList().add( new SelectCriteria( new EnumDeterminantAqlVariable( (CaseAqlVariable) getProcessedSelectCriteriaList().get( i ).getAqlVariable() ) ) );
			}
		}
		
		for( AqlTableAbstract aqlTable : getQueryTables().values() ) {
			if( aqlTable instanceof AqlSubTable ) {
				((AqlSubTable) aqlTable).getProcessedBeanDao().createPriorityCriteria();

				List<SelectCriteria> childPriorityCriteriaList = ((AqlSubTable) aqlTable).getProcessedBeanDao().getPriorityCriteriaList();
				for( int i = 0, n = childPriorityCriteriaList.size(); i < n; i++ ) {
					getPriorityCriteriaList().add( new TransferredCriteria( this, childPriorityCriteriaList.get( i ), aqlTable ) );
				}
			} 
		}
		getProcessedSelectCriteriaList().addAll( 0, getPriorityCriteriaList() );
	}
	
	public void addQueryTable( String objPath, AqlTableAbstract aqlTable ) {
		if( CommonUtil.isNullOrEmpty( aqlTable.getAlias() ) ) {
			getBeanDao().setLatestTableIdx( aqlTable.generateAlias( getBeanDao().getLatestTableIdx() ) );
		}
		if( objPath == null ) {
			objPath = aqlTable.getAlias();
		}
		getQueryTables().put( objPath, aqlTable );
	}

	public AqlTableAbstract findOrCreateAqlTable( String tablePath, boolean addTableAfterCreation ) {
		AqlTableAbstract aqlTable = findTable( tablePath );
		if( aqlTable == null && getParentProcessedBeanDao() != null ) {
			getParentProcessedBeanDao().findTable( tablePath );
		}
		if( aqlTable == null ) {
			aqlTable = createAqlTable( tablePath, addTableAfterCreation );
		}
		return aqlTable;
	}
	
	public AqlTable createAqlTable( String tablePath, boolean addTableAfterCreation ) {
		AqlTable aqlTable = null;
		String variableName = null;
		AqlTable parentTable;
		if( tablePath.lastIndexOf( "." ) != -1 ) {
			String parentTablePath = tablePath.substring( 0, tablePath.lastIndexOf( "." ) );
			parentTable = (AqlTable) findTable( parentTablePath );
			if( parentTable == null && parentTablePath.contains( "." ) ) {
				createAqlTable( parentTablePath, true );
				parentTable = (AqlTable) findTable( parentTablePath );
			}
			variableName = tablePath.substring( tablePath.lastIndexOf( "." ) + 1 );	
		} else {
			variableName = tablePath;
			parentTable = (AqlTable) getRootTable();
		}
		PersistentClass parentPersistentClass = (PersistentClass) parentTable.getPersistentTable();
		FieldInfo fieldInfo = null;
		while( fieldInfo == null && parentPersistentClass != null ) {
			fieldInfo = parentPersistentClass.getAliasedFieldInfoMap().get( variableName );
			parentPersistentClass = parentPersistentClass.getParentPersistentClass();
		}
		
		/*
		 * Final check to make sure that the aql table hasn't already been added.  This is sometimes
		 * needed because the variable can be reference bean.address.line1 or address.line1 and the obj
		 * path is different.
		 */
		for( AqlTableAbstract tempAqlTableAbstract : getQueryTables().values() ) {
			if( tempAqlTableAbstract instanceof AqlTable ) {
				AqlTable tempAqlTable = (AqlTable) tempAqlTableAbstract;
				if( tempAqlTable.getAssociatedFieldInfo() != null 
						&& tempAqlTable.getAssociatedFieldInfo().equals( fieldInfo ) 
						&& ((tempAqlTable.getParentTable() == null && parentTable == null) 
								|| tempAqlTable.equals( parentTable )) ) {
					return tempAqlTable;
				}
			}
		}
		

		if( fieldInfo instanceof PersistentClassFieldInfo ) {
			PersistentClass persistentClass = ((PersistentClassFieldInfo) fieldInfo).getPersistentClass();
			aqlTable = createAqlTable( persistentClass, parentTable, fieldInfo, tablePath, variableName, addTableAfterCreation );
		} else if( fieldInfo instanceof PersistentCollectionFieldInfo ) {
			JunctionTable collectionTable = ((PersistentCollectionFieldInfo) fieldInfo).getJunctionTable( ((PersistentClass) parentTable.getPersistentTable()).getTableClass() );
			PersistentClass persistentClass = ((PersistentCollectionFieldInfo) fieldInfo).getPersistentClass(); 
			if( persistentClass != null ) {
				AqlTable junctionAqlTable = new AqlTable( this, collectionTable, parentTable, null, fieldInfo );
				if( addTableAfterCreation ) {
					addQueryTable( collectionTable.getSqlTableName(), junctionAqlTable );
				}
//				junctionAqlTable.setVariableName( variableName );
				junctionAqlTable.setJoinInferred( true );
				junctionAqlTable.setParentFieldInfo(fieldInfo);

				aqlTable = createAqlTable( persistentClass, junctionAqlTable, fieldInfo, tablePath, variableName, addTableAfterCreation );
				if( addTableAfterCreation ) {
					addQueryTable( tablePath, aqlTable );
				}
//				aqlTable.setVariableName( variableName );
				aqlTable.setJoinInferred( true );
				aqlTable.setParentFieldInfo(fieldInfo);
			} else {
				ApplicationUtil.getAplosContextListener().handleError( new Exception( "No class found for " + tablePath ) );
				return null;
			}
		}
		
		if( aqlTable != null && aqlTable.isJoinInferred() ) {
			if( aqlTable.getPersistentTable() instanceof PersistentClass
					&& ((PersistentClass) aqlTable.getPersistentTable()).getDbPersistentClass() == null ) {
				aqlTable.setIncludedInJoin( false );
			}
		}
		return aqlTable;
	}
	
	public AqlTableAbstract findAqlTableForVariable( String name ) {
		List<AqlTableAbstract> foundTables = new ArrayList<AqlTableAbstract>();
		for( AqlTableAbstract tempAqlTable : getQueryTables().values() ) {
			if( tempAqlTable.isVariableInScope( name ) ) {
				foundTables.add( tempAqlTable );
			}
		}
		
		if( foundTables.size() == 1 ) {
			return foundTables.get( 0 );
		} else if( foundTables.size() == 0 ) {
			if( name.equals( "*" ) ) {
				return getRootTable();
			} else if( getQueryTables().get( name ) != null ) {
				return getQueryTables().get( name );
			} else {
				return null;
			}
		} else {
			ApplicationUtil.handleError( new Exception( "Multiple tables found for variable " + name ), false );
			return null;
		}
	}

    @SuppressWarnings({ "rawtypes", "unchecked" })
	private Object addNewObjToResults( ResultSet resultSet ) throws Exception {
		List<Object> results = new ArrayList<Object>();
  		
		Object tempValue = null;
	  
		for( SelectCriteria processedSelectCriteria : getProcessedSelectCriteriaList() ) {
			results.add( processedSelectCriteria.convertFieldValues( this, new ResultSetRowDataReceiver( resultSet ) ) );
		}
		
		Object[] resultAry = new Object[ results.size() ];
		for( int i = 0, n = results.size(); i < n; i++ ) {
			resultAry[ i ] = results.get( i );
		}
  		
  		return resultAry;
    }
	
	public void evaluateOrderByCriteria() {
		if( getOrderByCriteria().size() == 0 && !CommonUtil.isNullOrEmpty( getBeanDao().getOrderByCriteria() ) ) {
			AqlParser aqlParser = AqlParser.getInstance( getBeanDao().getOrderByCriteria() );
			setOrderByCriteria( aqlParser.parseOrderBy( getBeanDao() ) );
		}
		
		for( int i = 0, n = getOrderByCriteria().size(); i < n; i++ ) {
			getOrderByCriteria().get( i ).getAqlVariable().evaluateCriteriaTypes( this, false );
		}

		for( AqlTableAbstract tempAqlTable : getQueryTables().values() ) {
			if( tempAqlTable instanceof AqlSubTable ) {
				((AqlSubTable) tempAqlTable).getProcessedBeanDao().evaluateOrderByCriteria();
			}
		}
	}
	
	public void evaluateGroupByCriteria() {
		if( getGroupByCriteria().size() == 0 && !CommonUtil.isNullOrEmpty( getBeanDao().getGroupByCriteria() ) ) {
			AqlParser aqlParser = AqlParser.getInstance( getBeanDao().getGroupByCriteria() );
			setGroupByCriteria( aqlParser.parseGroupBy( getBeanDao() ) );
		}
		
		for( int i = 0, n = getGroupByCriteria().size(); i < n; i++ ) {
			getGroupByCriteria().get( i ).evaluateCriteriaTypes( this, false );
		}

		for( AqlTableAbstract tempAqlTable : getQueryTables().values() ) {
			if( tempAqlTable instanceof AqlSubTable ) {
				((AqlSubTable) tempAqlTable).getProcessedBeanDao().evaluateGroupByCriteria();
			}
		}
	}

	public String getSelectSql() {
		preprocessCriteria();
		return generateSqlString( true );
	}
	
	public String generateSqlString( boolean includeLimit ) {
		StringBuffer sql = new StringBuffer( "SELECT " ).append( getSelectCriteria() ).append( " FROM " ).append( getFromClause() );
		sql.append( getBaseHql( true, true ) );
		if( includeLimit && getBeanDao().getMaxResults() != -1 ) {
			sql.append( " limit " );
			if( getBeanDao().getFirstRowIdx() != -1 ) {
				sql.append( getBeanDao().getFirstRowIdx() ).append( "," );
			}
			sql.append( getBeanDao().getMaxResults() );
		}
		String sqlStr = sql.toString();
		if( ApplicationUtil.getAplosContextListener().isDebugMode() ) {
			logger.debug( "BeanDao1 : " + getPrintableSqlString( sqlStr ) );
		}
		return sqlStr;
	}
	
	public String getPrintableSqlString( String printableSqlStr ) {
		for( int i = 0, n = getNamedParameters().size(); i < n; i++ ) {
			printableSqlStr = printableSqlStr.replaceFirst( "\\?", getBeanDao().getNamedParameters().get( getNamedParameters().get( i ) ) );
		}
		return printableSqlStr;
	}
    
    public PreparedStatement getAllPreparedStatement( Connection conn ) throws SQLException {    	
    	return getPreparedStatement( conn, getSelectSql());
    }
    
    public PreparedStatement getCountAllPreparedStatement( Connection conn ) throws SQLException {
    	return getPreparedStatement(conn, getCountAllSql());
    }

	public String getCountAllSql() {
		List<SelectCriteria> oldSelectCriteria = getBeanDao().getUnprocessedSelectCriteriaList();
		getBeanDao().setUnprocessedSelectCriteriaList( new ArrayList<SelectCriteria>() );
		String oldOrderBy = getBeanDao().getOrderByCriteria();
		getBeanDao().setOrderByCriteria(null);
		
		if (getBeanDao().getCountFields().equals("")) {
			getBeanDao().addSelectCriteria( "count(*)" );
		} else {
			getBeanDao().addSelectCriteria( "count(" + getBeanDao().getCountFields() + ")" );
		}

		setGeneratingBeans(false);
		preprocessCriteria();
		String sqlString = generateSqlString( false );
		getBeanDao().setUnprocessedSelectCriteriaList( oldSelectCriteria );
		getBeanDao().setOrderByCriteria( oldOrderBy );
		return sqlString;
	}
    
    public PreparedStatement getPreparedStatement( Connection conn, String sql ) throws SQLException {
    	PreparedStatement preparedStatement = null;
    	preparedStatement = ApplicationUtil.getPreparedStatement( conn, sql );
    	for( int i = 0, n = getNamedParameters().size(); i < n; i++ ) {
    		preparedStatement.setString( i + 1, getBeanDao().getNamedParameters().get( getNamedParameters().get( i ) ) );
    	}
    	return preparedStatement;
    }
	
	public AqlTableVariable findAqlVariable( AqlTableAbstract aqlTable, String tableName, String variableName, boolean isTableCreated ) {
		AqlTableVariable aqlVariable = null;
		FieldInfo foundFieldInfo = aqlTable.findFieldInfo( variableName ); 
		if( foundFieldInfo != null ) {
			if( foundFieldInfo.isPrimaryKey() && tableName.contains( "." ) ) {
				ApplicationUtil.handleError( new Exception( "Replace the code below" ) );
//				VariableSelectCriteria tempAqlVariable = (VariableSelectCriteria) parseSingleSearchCriteria( tableName );
//				foundFieldInfo = tempAqlVariable.getAqlTable().getPersistentTable().getFieldInfoFromSqlName( tempAqlVariable.getName() );
//				if( foundFieldInfo != null 
//						&& foundFieldInfo instanceof PersistentClassFieldInfo
//						&& variableName.equals( "id" ) ) {
//					return tempAqlVariable;
//				}
			} 
			
			if( isTableCreated ) {
				addQueryTable( tableName, aqlTable );
			}
				
			aqlVariable = new AqlTableVariable( (AqlTable) aqlTable, foundFieldInfo );
		}
		return aqlVariable;
	}
	
	public AqlVariable findAqlVariableInAllTables( String variableName ) {
		AqlVariable aqlVariable = null;
		for( AqlTableAbstract aqlTable : getQueryTables().values() ) {
			if( aqlTable instanceof AqlSubTable ) {
				aqlVariable = ((AqlSubTable) aqlTable).getProcessedBeanDao().findAqlVariableInAllTables( variableName );
			} else {
				aqlVariable = findAqlVariable(aqlTable, null, variableName, false);
			}
			if( aqlVariable != null ) {
				break;
			}
		}
		return aqlVariable;
	}

	public String getBaseHql( boolean includeJoinFetch, boolean includeOrderBy ) {
		StringBuffer hql = new StringBuffer();

		getProcessedWhereConditionGroup().addNamedParameters( this );
		if( getProcessedWhereConditionGroup().getWhereConditions().size() > 0 ) {
			hql.append( " WHERE " +  getProcessedWhereConditionGroup().generateCondition() );
		}

		if( getGroupByCriteria().size() > 1 ) {
			hql.append( " GROUP BY " );
			for( int i = 0, n = getGroupByCriteria().size(); i < n; i++ ) {
				hql.append( getGroupByCriteria().get( i ).getSqlPath(true) );
				if( i != (n-1) ) {
					hql.append( "," );
				}
			}
		}
		if( includeOrderBy && getOrderByCriteria().size() > 0 ) {
			hql.append( " ORDER BY " );
			for( int i = 0, n = getOrderByCriteria().size(); i < n; i++ ) {
				hql.append( getOrderByCriteria().get( i ).getCriteria() );
				if( i != (n-1) ) {
					hql.append( "," );
				}
			}
		}
		return hql.toString();
	}

	public String getSelectCriteria() {
		List<SelectCriteria> selectCriterias = new ArrayList<SelectCriteria>();

		for( int i = 0, n = getProcessedSelectCriteriaList().size(); i < n; i++ ) {
			getProcessedSelectCriteriaList().get( i ).addSelectCriterias( selectCriterias );
		}
		SelectCriteria distinctCriteria = null;
		for( int i = selectCriterias.size() - 1; i >= 0; i-- ) {
			if( selectCriterias.get( i ).getAqlVariable().isDistinct() ) {
				if( distinctCriteria != null ) {
					ApplicationUtil.handleError( new Exception( "You cannot have more than one distinct column in each query" ) );
				} 
			}
		}
		
		List<SelectCriteria> orderedSelectCriteria = new ArrayList<SelectCriteria>();
		for( int i = 0, n = getBeanDao().getUnprocessedSelectCriteriaList().size(); i < n; i++ ) {
			for( int j = selectCriterias.size() - 1; j >= 0; j-- ) {
				if( selectCriterias.get( j ).getAqlVariable().isDistinct() ) {
					orderedSelectCriteria.add( 0, selectCriterias.get( j ) );
					selectCriterias.remove( j );
					break;
				} else {
					if( selectCriterias.get( j ).equals( getBeanDao().getUnprocessedSelectCriteriaList().get( i ) ) ) {
						orderedSelectCriteria.add( selectCriterias.get( j ) );
						selectCriterias.remove( j );
						break;
					}
				}
			}
		}
		
		for( int i = 0, n = selectCriterias.size(); i < n; i++ ) {
			orderedSelectCriteria.add( selectCriterias.get( i ) );
		}

		
		List<String> selectCriteriaPaths = new ArrayList<String>();
		for( int i = 0, n = orderedSelectCriteria.size(); i < n; i++ ) {
			orderedSelectCriteria.get( i ).setRowDataIdx( i );
			selectCriteriaPaths.add( orderedSelectCriteria.get( i ).getSqlText( getBeanDao() ) );
		}
		
		return StringUtils.join( selectCriteriaPaths, "," );
	}
    
    public Object findCachedBean( PersistentClass persistentClass, Long rootId ) {
    	if( rootId != null ) {
    		PersistenceContext persistenceContext = ApplicationUtil.getPersistenceContext( true );
			AplosAbstractBean cachedBean = persistenceContext.findBean( (Class<? extends AplosAbstractBean>) persistentClass.getDbPersistentClass().getTableClass(), rootId );
			return cachedBean;
    	}
    	return null;
    }

    @SuppressWarnings("rawtypes")
	public List<Object> convertResults( ResultSet resultSet ) throws Exception {
    	PersistenceBean tempPersistenceBean;
    	List<Object> objList = new ArrayList<Object>();
    	while( resultSet.next() ) {
    		objList.add( addNewObjToResults(resultSet));
    	}
		getBeanDao().removeArrayFromSingleResultList(objList);
		
		return objList;
    }

	public String getFromClause() {
		StringBuffer fromClause = new StringBuffer();
		List<AqlTableAbstract> queryTableList = new ArrayList<AqlTableAbstract>( getQueryTables().values() );
		AqlTableAbstract rootTable = getRootTable();
		fromClause.append( rootTable.getSqlString() );
		List<AqlTableAbstract> addedTables = new ArrayList<AqlTableAbstract>();
		List<AqlTableAbstract> newAddedTables = new ArrayList<AqlTableAbstract>();
		queryTableList.remove( rootTable );
		for( int i = queryTableList.size() - 1; i >= 0; i-- ) {
			if( !queryTableList.get( i ).isIncludedInJoin() ) {
				queryTableList.remove( i );
			}
		}
		addedTables.add( rootTable );
		AqlTableAbstract tempTable;
		while( queryTableList.size() > 0 ) {
			for( int i = queryTableList.size() - 1; i >= 0; i-- ) {
				for( int j = addedTables.size() - 1; j >= 0; j-- ) {
					tempTable = queryTableList.get( i );
					/*
					 * Handle sub query tables that are in the select statements
					 */
					if( tempTable.getParentTable() == null && tempTable instanceof AqlSubTable ) {
						newAddedTables.add( tempTable );
						queryTableList.remove( i );
					} else if( tempTable.getParentTable().equals( addedTables.get( j ) ) ) {
						if( tempTable.isIncludedInFromClause() ) {
							fromClause.append( " " ).append( tempTable.getJoinType().getSql() );
							fromClause.append( " " ).append( tempTable.getSqlString() );
							fromClause.append( " " ).append( tempTable.getJoinClause() );
						} 
						newAddedTables.add( tempTable );
						queryTableList.remove( i );
						break;
					}
				}
			}
			addedTables = newAddedTables;
			if( addedTables.size() == 0 ) {
				ApplicationUtil.handleError( new Exception( "Not all query tables could be added to FROM clause" ) );
				break;
			}
			newAddedTables = new ArrayList<AqlTableAbstract>();
		}
		return fromClause.toString();
	}
	
	public void processWhereCriteria() {
		for( int i = 0, n = getBeanDao().getUnprocessedFilterList().size(); i < n; i++ ) {
			getBeanDao().processWhereCriteria( getBeanDao().getUnprocessedFilterList().get( i ), true, getProcessedWhereConditionGroup() );
		}
		for( int i = 0, n = getBeanDao().getUnprocessedSearchList().size(); i < n; i++ ) {
			getBeanDao().processWhereCriteria( getBeanDao().getUnprocessedSearchList().get( i ), false, getProcessedWhereConditionGroup() );
		}


		if( getBeanDao().getIsReturningActiveBeans() != null && getParentProcessedBeanDao() == null ) {
			if( getBeanDao().getIsReturningActiveBeans() ) {
				getBeanDao().processWhereCriteria( getRootTable().getAlias() + ".active = true", true, getProcessedWhereConditionGroup() );
			} else {
				getBeanDao().processWhereCriteria( getRootTable().getAlias() + ".active = false", true, getProcessedWhereConditionGroup() );
			}
		}
		
		getProcessedWhereConditionGroup().evaluateCriteriaTypes(this);
	}
	
	public List getBeanResults( List resultList ) {
		List<Object[]> newResultList = new ArrayList<Object[]>();
		for( int j = 0, p = resultList.size(); j < p; j++ ) {
			newResultList.add( new Object[getProcessedSelectCriteriaList().size()] );
			for( int i = 0, n = getProcessedSelectCriteriaList().size(); i < n; i++ ) {
				newResultList.get( j )[ i ] = getProcessedSelectCriteriaList().get(i).getAqlVariable().getValue( (AplosAbstractBean) resultList.get( j ) );
			}
		}
		return newResultList;
	}
	
	public List getAll() {
		Connection conn = null;
		List resultList = null;
		try {
			conn = ApplicationUtil.getConnection();
			if( CommonUtil.isNullOrEmpty( getBeanDao().getOrderByCriteria() ) 
					&& getBeanDao().isAddingDefaultOrderByToLists()
					&& (getBeanDao().getMaxResults() == -1 || getBeanDao().getMaxResults() > 1 ) ) {
				AqlParser aqlParser = AqlParser.getInstance( getBeanDao().getRootAlias() + ".id desc" );
				setOrderByCriteria( aqlParser.parseOrderBy( getBeanDao() ) );
			}
			PreparedStatement preparedStatement = getAllPreparedStatement(conn);
			ResultSet resultSet = preparedStatement.executeQuery();
			resultList = convertResults(resultSet);
//			if( getBeanDao().getListBeanClass() != null ) {
//				resultList = find( resultList );
//			} else {
//				if( resultList.size() > 0 && ((Object[]) resultList.get( 0 )).length == 1 ) {
//					List oldResultList = new ArrayList(resultList);
//					resultList.clear();
//					for( int i = 0, n = oldResultList.size(); i < n; i++ ) {
//						resultList.add( ((Object[]) oldResultList.get( i ))[ 0 ] );
//					}
//				}
//			}
		} catch( SQLException sqlEx ) {
			ApplicationUtil.handleError(sqlEx);
		} catch( Exception ex ) {
			ApplicationUtil.handleError(ex);
		} finally {
			if( resultList == null ) {
				resultList = new ArrayList();
			}
			ApplicationUtil.closeConnection(conn);
		}
			
		return resultList;
	}

	public BeanDao getBeanDao() {
		return beanDao;
	}

	public void setBeanDao(BeanDao beanDao) {
		this.beanDao = beanDao;
	}

	public HashMap<String,AqlTableAbstract> getQueryTables() {
		return queryTables;
	}

	public void setProcessedSelectCriteriaList(
			List<SelectCriteria> processedSelectCriteriaList) {
		this.processedSelectCriteriaList = processedSelectCriteriaList;
	}

	public List<SelectCriteria> getProcessedSelectCriteriaList() {
		return processedSelectCriteriaList;
	}

	public List<SelectCriteria> getPriorityCriteriaList() {
		return priorityCriteriaList;
	}

	public void setPriorityCriteriaList(List<SelectCriteria> priorityCriteriaList) {
		this.priorityCriteriaList = priorityCriteriaList;
	}

	public WhereConditionGroup getProcessedWhereConditionGroup() {
		return processedWhereConditionGroup;
	}

	public void setProcessedWhereConditionGroup(
			WhereConditionGroup processedWhereConditionGroup) {
		this.processedWhereConditionGroup = processedWhereConditionGroup;
	}

	public ProcessedBeanDao getParentProcessedBeanDao() {
		return parentProcessedBeanDao;
	}

	public void setParentProcessedBeanDao(ProcessedBeanDao parentProcessedBeanDao) {
		this.parentProcessedBeanDao = parentProcessedBeanDao;
	}

	public boolean isCriteriaTypesEvaluated() {
		return isCriteriaTypesEvaluated;
	}

	public void setCriteriaTypesEvaluated(boolean isCriteriaTypesEvaluated) {
		this.isCriteriaTypesEvaluated = isCriteriaTypesEvaluated;
	}

	public AqlTableAbstract getRootTable() {
		return rootTable;
	}

	public void setRootTable(AqlTableAbstract rootTable) {
		this.rootTable = rootTable;
	}

	public boolean isGeneratingBeans() {
		return isGeneratingBeans;
	}

	public void setGeneratingBeans(boolean isGeneratingBeans) {
		this.isGeneratingBeans = isGeneratingBeans;
	}

	public List<String> getNamedParameters() {
		return namedParameters;
	}

	public void setNamedParameters(List<String> namedParameters) {
		this.namedParameters = namedParameters;
	}

	public boolean isLoadingFromDatabase() {
		return isLoadingFromDatabase;
	}

	public void setLoadingFromDatabase(boolean isLoadingFromDatabase) {
		this.isLoadingFromDatabase = isLoadingFromDatabase;
	}

	public List<OrderByCriteria> getOrderByCriteria() {
		return orderByCriteria;
	}

	public void setOrderByCriteria(List<OrderByCriteria> orderByCriteria) {
		this.orderByCriteria = orderByCriteria;
	}

	public List<AqlVariable> getGroupByCriteria() {
		return groupByCriteria;
	}

	public void setGroupByCriteria(List<AqlVariable> groupByCriteria) {
		this.groupByCriteria = groupByCriteria;
	}
}
