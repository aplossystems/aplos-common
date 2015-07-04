package com.aplos.common.aql;

import com.aplos.common.aql.aqltables.AqlTable;
import com.aplos.common.aql.aqltables.AqlTableAbstract;
import com.aplos.common.aql.aqltables.unprocessed.JoinedAqlTable;
import com.aplos.common.aql.aqlvariables.AqlTableVariable;
import com.aplos.common.aql.aqlvariables.UnevaluatedTableVariable;
import com.aplos.common.aql.selectcriteria.SelectCriteria;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.persistence.PersistableTable;
import com.aplos.common.persistence.fieldinfo.PersistentClassFieldInfo;
import com.aplos.common.persistence.fieldinfo.PersistentCollectionFieldInfo;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;


public class SubBeanDao extends BeanDao {
	private SelectCriteria rootTableCriteria;
	private JoinedAqlTable joinedAqlTable;
	private String joinedWhereTableAlias;

	public SubBeanDao( SelectCriteria rootTableVariable ) {
		super();
		setRootTableCriteria(rootTableVariable);
	}
	
	@Override
	public boolean isGeneratingBeans() {
		return false;
	}
	
	public SubBeanDao(Class<? extends AplosAbstractBean> beanClass) {
		super( beanClass );
	}
	
	@Override
	public AqlTableAbstract createRootTable( ProcessedBeanDao processedBeanDao ) {
		AqlTable aqlTable = null;
		if( getRootTableCriteria() != null ) {
			AqlTable foundAqlTable = null;
			if( getRootTableCriteria().getAqlVariable() instanceof AqlTableVariable ) {
				UnevaluatedTableVariable tempSelectCriteria = (UnevaluatedTableVariable) getRootTableCriteria().getAqlVariable(); 
				setRootAlias( getRootTableCriteria().getAlias() );
				foundAqlTable = (AqlTable) findAqlTable( processedBeanDao, tempSelectCriteria );
				if( foundAqlTable != null ) {
					tempSelectCriteria.evaluateCriteriaTypes(processedBeanDao, true);
					if( tempSelectCriteria.getFieldInfo() instanceof PersistentClassFieldInfo 
							|| tempSelectCriteria.getFieldInfo() instanceof PersistentCollectionFieldInfo ) {
						String alias = foundAqlTable.getAlias() + getLatestTableIdx();
						setJoinedWhereTableAlias( foundAqlTable.getAlias() );
						setLatestTableIdx( getLatestTableIdx() + 1 );
						String joinPath = tempSelectCriteria.getOriginalPath().replaceFirst( foundAqlTable.getAlias(), alias );

						setJoinedAqlTable( new JoinedAqlTable( getRootTableCriteria().getAlias(), joinPath ) );
						
						aqlTable = new AqlTable( null, foundAqlTable.getPersistentTable(), null, alias );
						setRootAlias( alias );
					}
				}
			}
			
			if( aqlTable == null ) {
				if( foundAqlTable != null ) {
					aqlTable = new AqlTable( null, null, null, getRootAlias() );
				} else {
					PersistableTable persistableTable = ApplicationUtil.getPersistentApplication().getPersistableTableBySqlNameMap().get( getRootTableCriteria().getAqlVariable().getName().toLowerCase() );
					if( persistableTable != null ) {
						aqlTable = new AqlTable( null, persistableTable, null, getRootAlias() );
						if( CommonUtil.isNullOrEmpty( getRootAlias() ) ) {
							setLatestTableIdx( aqlTable.generateAlias( getLatestTableIdx() ) );
						}
					} else {
						ApplicationUtil.handleError( new Exception( "Cannot find aqlTable for variable " + getRootTableCriteria().getAqlVariable().getName() ) );
					}
				}
			}
		}
		if( aqlTable != null ) {
			aqlTable.setProcessedBeanDao(processedBeanDao);
			return aqlTable;
		} else {
			return super.createRootTable(processedBeanDao);
		}
	}
	
	@Override
	public void processAqlTables(ProcessedBeanDao processedBeanDao) {
		if( getJoinedAqlTable() != null ) {
			getJoinedAqlTable().createAqlTable( processedBeanDao, true );
		}
		super.processAqlTables(processedBeanDao);
		if( getJoinedAqlTable() != null ) {
			processWhereCriteria( getRootAlias() + ".id = " + getJoinedWhereTableAlias() + ".id", true, processedBeanDao.getProcessedWhereConditionGroup() );
		}
	}
	
	public AqlTableAbstract findAqlTable( ProcessedBeanDao processedBeanDao, UnevaluatedTableVariable tempSelectCriteria ) {
		tempSelectCriteria.init(tempSelectCriteria.getOriginalPath());
		AqlTableAbstract aqlTable = null;
		
		if( !CommonUtil.isNullOrEmpty( tempSelectCriteria.getTablePath() ) ) {
			aqlTable = processedBeanDao.findTable( tempSelectCriteria.getTablePath() );
			if( aqlTable == null && processedBeanDao.getParentProcessedBeanDao() != null ) {
				processedBeanDao.getParentProcessedBeanDao().findTable( tempSelectCriteria.getTablePath() );
			}
		} else {
			if( processedBeanDao.getQueryTables().get( tempSelectCriteria.getName() ) != null ) {
				aqlTable = processedBeanDao.getQueryTables().get( tempSelectCriteria.getName() );
			} else {
				aqlTable = processedBeanDao.findAqlTableForVariable( tempSelectCriteria.getName() ); 
			}
		}
		return aqlTable;
	}
	
	public void setFieldInformationRequired( boolean isFieldInformationRequired ) {
		for( int i = 0, n = getUnprocessedSelectCriteriaList().size(); i < n; i++ ) {
			getUnprocessedSelectCriteriaList().get( i ).setFieldInformationRequired(isFieldInformationRequired);
		}
	}

	public SelectCriteria getRootTableCriteria() {
		return rootTableCriteria;
	}

	public void setRootTableCriteria(SelectCriteria rootTableCriteria) {
		this.rootTableCriteria = rootTableCriteria;
	}

	public JoinedAqlTable getJoinedAqlTable() {
		return joinedAqlTable;
	}

	public void setJoinedAqlTable(JoinedAqlTable joinedAqlTable) {
		this.joinedAqlTable = joinedAqlTable;
	}

	public String getJoinedWhereTableAlias() {
		return joinedWhereTableAlias;
	}

	public void setJoinedWhereTableAlias(String joinedWhereTableAlias) {
		this.joinedWhereTableAlias = joinedWhereTableAlias;
	}
}
