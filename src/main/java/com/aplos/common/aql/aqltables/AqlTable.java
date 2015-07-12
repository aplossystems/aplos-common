package com.aplos.common.aql.aqltables;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.aql.WhereConditionGroup;
import com.aplos.common.aql.aqlvariables.AqlTableVariable;
import com.aplos.common.aql.aqlvariables.AqlVariable;
import com.aplos.common.aql.aqlvariables.CaseAqlVariable;
import com.aplos.common.aql.aqlvariables.UnevaluatedTableVariable;
import com.aplos.common.aql.aqlvariables.ForeignKeyAqlVariable;
import com.aplos.common.aql.aqlvariables.PolymorphicIdAqlVariable;
import com.aplos.common.aql.aqlvariables.PolymorphicTypeAqlVariable;
import com.aplos.common.aql.selectcriteria.PersistentClassSelectCriteria;
import com.aplos.common.aql.selectcriteria.SelectCriteria;
import com.aplos.common.persistence.JunctionTable;
import com.aplos.common.persistence.PersistableTable;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.PersistentClass.InheritanceType;
import com.aplos.common.persistence.fieldinfo.CollectionFieldInfo;
import com.aplos.common.persistence.fieldinfo.FieldInfo;
import com.aplos.common.persistence.fieldinfo.ForeignFieldInfo;
import com.aplos.common.persistence.fieldinfo.PersistentClassFieldInfo;
import com.aplos.common.persistence.fieldinfo.PolymorphicFieldInfo;

public class AqlTable extends AqlTableAbstract {
	/*
	 * AssociatedFieldInfo is required due to the way that the PersistentCollectionFieldInfo 
	 * works.  This will create an internal and external reference that is not the
	 * PersistentCollectionFieldInfo itself so the reference needs to be kept.
	 */
	private FieldInfo associatedFieldInfo;
	private boolean isJoinInferred = false;
	private boolean isIncludedInJoin = true;
	private CaseAqlVariable caseSelectCriteria;
	private List<AqlTableVariable> registeredSelectCriteria = new ArrayList<AqlTableVariable>();
	private List<AqlTableAbstract> subTables = new ArrayList<AqlTableAbstract>();
	private PersistentClassSelectCriteria persistentClassSelectCriteria;
	private Boolean isIncludedInFromClause;
	private boolean isAddingAllCriteria = false;
	
	/*
	 * A joined table represents the joined table in the db.  This isn't often in
	 * use but it's required to help find variables that exist in superclasses when
	 * JOINED_TABLE is used in the db. 
	 */
	private AqlTable joinedTable;
	private boolean isEvaluatingInheritance = true;
	private PersistableTable persistableTable;
	private FieldInfo parentFieldInfo;
	
	public enum JoinType {
		LEFT_OUTER_JOIN ( "LEFT OUTER JOIN" ),
		LEFT_JOIN ( "LEFT JOIN" ),
		RIGHT_OUTER_JOIN ( "RIGHT OUTER JOIN" ),
		RIGHT_JOIN ( "RIGHT JOIN" ),
		INNER_JOIN ( "INNER JOIN" ),
		COMMA_JOIN ( "," );
		
		private String sql;
		
		private JoinType( String sql ) {
			this.sql = sql;
		}
		
		public String getSql() {
			return sql;
		}
	}
	

	public AqlTable( ProcessedBeanDao processedBeanDao, PersistableTable persistableTable, AqlTableAbstract parentTable, String alias, FieldInfo associatedFieldInfo ) {
		init( processedBeanDao, persistableTable, parentTable, alias, false );
		updateAssociatedFieldInfo(associatedFieldInfo);
	}

	public AqlTable( ProcessedBeanDao processedBeanDao, PersistableTable persistableTable, AqlTableAbstract parentTable, String alias, FieldInfo associatedFieldInfo, boolean isGeneratingAlias ) {
		init( processedBeanDao, persistableTable, parentTable, alias, isGeneratingAlias );
		updateAssociatedFieldInfo(associatedFieldInfo);
	}

	public AqlTable( ProcessedBeanDao processedBeanDao, PersistableTable persistableTable, AqlTableAbstract parentTable, String alias ) {
		init( processedBeanDao, persistableTable, parentTable, alias, false );
	}
	
	public void init( ProcessedBeanDao processedBeanDao, PersistableTable persistableTable, AqlTableAbstract parentTable, String alias, boolean isGeneratingAlias ) {
		setProcessedBeanDao(processedBeanDao);
		updateParentTable( parentTable );
		setPersistentTable(persistableTable);
		if( isGeneratingAlias ) {
			processedBeanDao.getBeanDao().setLatestTableIdx( generateAlias( processedBeanDao.getBeanDao().getLatestTableIdx() ) );
		} else {
			setAlias( alias );
		}
		if( persistableTable instanceof PersistentClass && ((PersistentClass) persistableTable).getTableClass().isInterface() ) {
			setJoinInferred(true);
		}
		setPersistentClassSelectCriteria(new PersistentClassSelectCriteria(this));
	}
	
	public void includeInParents() {
		if( getParentTable() instanceof AqlTable ) {
			AqlTable parentAqlTable = (AqlTable) getParentTable();
			if( parentAqlTable.getPersistentTable() instanceof JunctionTable ) {
				parentAqlTable = (AqlTable) parentAqlTable.getParentTable();
			}
			if( !parentAqlTable.getPersistentClassSelectCriteria().getProcessedSelectCriteria().contains( getPersistentClassSelectCriteria() ) ) {
				parentAqlTable.getPersistentClassSelectCriteria().getProcessedSelectCriteria().add( getPersistentClassSelectCriteria() );
				parentAqlTable.includeInParents();
			}
		}
	}
	
	@Override
	public boolean isIncludedInFromClause() {
		if( isIncludedInFromClause != null ) {
			return isIncludedInFromClause;
		} else {
			setIsIncludedInFromClause( !isJoinInferred || (isIncludedInJoin() && (getRegisteredSelectCriteria().size() > 0 
					|| hasSubTableIncludedInFromClause() || getPersistentClassSelectCriteria().isAllFieldsAdded())));
			return isIncludedInFromClause;
		}
	}
	
	@Override
	public boolean evaluateAqlTable(ProcessedBeanDao processedBeanDao) {
		if( !isEvaluated() ) {
			super.evaluateAqlTable(processedBeanDao);
			if( getPersistentTable() instanceof PersistentClass ) {
				PersistentClass parentPersistentClass = ((PersistentClass) persistableTable).getParentPersistentClass();
				if( parentPersistentClass != null && InheritanceType.JOINED_TABLE.equals( parentPersistentClass.getInheritanceType() ) ) {
					String joinedTableAlias = getAlias() + "_" + parentPersistentClass.getTableClass().getSimpleName();
					setJoinedTable( new AqlTable( getProcessedBeanDao(), parentPersistentClass, this, joinedTableAlias, persistableTable.determinePrimaryKeyFieldInfo() ) );
					getJoinedTable().getPersistentClassSelectCriteria().setJoinedTable( true );
					getJoinedTable().setEvaluatingInheritance(false);
					getProcessedBeanDao().addQueryTable( joinedTableAlias, getJoinedTable() );
					getJoinedTable().evaluateAqlTable(processedBeanDao);
				}
			}
			return true;
		}
		return false;
	}
	

	
	public void addAllCriteria(ProcessedBeanDao processedBeanDao, List<SelectCriteria> criteriaList) {
		AqlVariable tempSelectCriteria = null;
		getPersistentClassSelectCriteria().setAllFieldsAdded( true );
		
		Map<String, FieldInfo> fieldInfosToAddMap;
		if( getPersistentTable() instanceof PersistentClass
				&& InheritanceType.SINGLE_TABLE.equals( ((PersistentClass) getPersistentTable()).getInheritanceType() ) ) {
			fieldInfosToAddMap = ((PersistentClass) getPersistentTable()).getDbPersistentClass().getFullDbFieldInfoMap();
		} else {
			fieldInfosToAddMap = getPersistentTable().getClassDbFieldInfoMap();
		}
		
		for( FieldInfo fieldInfo : fieldInfosToAddMap.values() ) {
			if( fieldInfo instanceof PolymorphicFieldInfo && ((PolymorphicFieldInfo) fieldInfo).getIdFieldInfo() != null ) {
				tempSelectCriteria = new PolymorphicTypeAqlVariable( this, fieldInfo );
			} else if( fieldInfo instanceof PersistentClassFieldInfo ) {
				if( ((PersistentClassFieldInfo)fieldInfo).getPolymorphicFieldInfo() == null ) {
					tempSelectCriteria = new UnevaluatedTableVariable( getAlias() + "." + fieldInfo.getSqlName() );
				} else {
					tempSelectCriteria = new PolymorphicIdAqlVariable( this, fieldInfo );
					// TODO this should be getting picked up by the evaluateCriteriaTypes method but isn't
//					((PolymorphicIdSelectCriteria) tempSelectCriteria).updateFieldInfo( fieldInfo );
				}
			} else {
				tempSelectCriteria = new AqlTableVariable( this, fieldInfo );
			}
			tempSelectCriteria.evaluateCriteriaTypes( processedBeanDao, false ); 
			criteriaList.add( new SelectCriteria( tempSelectCriteria ) );
		}
		
		for( FieldInfo fieldInfo : getPersistentTable().getFullNonDbFieldInfoMap().values() ) {
			if( fieldInfo instanceof CollectionFieldInfo ) {
				getNonDbFieldInfos().add( (CollectionFieldInfo) fieldInfo );
			} else if( fieldInfo instanceof ForeignFieldInfo ) {
				getNonDbFieldInfos().add( (ForeignFieldInfo) fieldInfo );
			}
		}
	}
	
	@Override
	public FieldInfo getFieldInfoFromSqlName(String sqlName,
			boolean searchAllClasses) {
		return getPersistentTable().getFieldInfoFromSqlName(sqlName, searchAllClasses);
	}
	
	/*
	 * This needs to be removed and replaced by a new TableJoin class that carries
	 * the associatedField, internalFieldInfo and externalFieldInfo that is created
	 * by whatever creates the AqlTable
	 */
	public void updateAssociatedFieldInfo( FieldInfo associatedFieldInfo ) {
		setAssociatedFieldInfo(associatedFieldInfo);
		if( associatedFieldInfo != null ) {
			if( getPersistentTable() instanceof PersistentClass 
					&& !((PersistentClass) getPersistentTable()).isEntity() ) {
				return;
			}
			
			if( getAssociatedFieldInfo() != null && getAssociatedFieldInfo().getField() != null ) {
				if( getAssociatedFieldInfo() instanceof PersistentClassFieldInfo ) {
					setSqlName( getAssociatedFieldInfo().getField().getName() );
				}
			}
			FieldInfo internalFieldInfo = null;
			FieldInfo externalFieldInfo = null;
			if( getPersistentTable() instanceof JunctionTable ) {
				if( JoinType.RIGHT_OUTER_JOIN.equals( getJoinType() ) ) {
					externalFieldInfo = ((AqlTable) getParentTable()).getPersistentTable().determinePrimaryKeyFieldInfo();
					internalFieldInfo = associatedFieldInfo;
				} else {
					externalFieldInfo = associatedFieldInfo.getParentPersistentClass().determinePrimaryKeyFieldInfo();
					if( associatedFieldInfo instanceof CollectionFieldInfo ) {
						internalFieldInfo = ((JunctionTable) getPersistentTable()).getPersistentClassFieldInfo();
					} else {
						internalFieldInfo = associatedFieldInfo;
					}
				}
			} else {
				if( JoinType.RIGHT_OUTER_JOIN.equals( getJoinType() ) ) {
					if( associatedFieldInfo instanceof CollectionFieldInfo ) {
						externalFieldInfo = ((JunctionTable) ((AqlTable) getParentTable()).getPersistentTable()).getPersistentClassFieldInfo();
						internalFieldInfo = getPersistentTable().determinePrimaryKeyFieldInfo();	
					} else {
						externalFieldInfo = ((PersistentClassFieldInfo) associatedFieldInfo).getPersistentClass().determinePrimaryKeyFieldInfo();
						internalFieldInfo = associatedFieldInfo;
					}
				} else {
					externalFieldInfo = associatedFieldInfo;
					internalFieldInfo = getPersistentTable().determinePrimaryKeyFieldInfo();
				}
			}
			
			setInternalVariable( new AqlTableVariable( this, internalFieldInfo ) );
			setExternalVariable( new AqlTableVariable( (AqlTable) getParentTable(), externalFieldInfo ) );
		} else {
			setInternalVariable( null );
			setExternalVariable( null );
		}
	}
	
	public void newUpdateAssociatedFieldInfo( FieldInfo associatedFieldInfo ) {
		setAssociatedFieldInfo(associatedFieldInfo);
		if( getAssociatedFieldInfo() != null && getAssociatedFieldInfo().getField() != null ) {
			if( getAssociatedFieldInfo() instanceof PersistentClassFieldInfo ) {
				setSqlName( getAssociatedFieldInfo().getField().getName() );
			}
		}
	}
	
	public boolean hasSubTableIncludedInFromClause() {
		for( int i = 0, n = getSubTables().size(); i < n; i++ ) {
			if( getSubTables().get( i ).isIncludedInFromClause() ) {
				return true;
			}
		}
		return false;
	}
	
	public boolean sqlPathMatches( String sqlTablePath ) {
		return determineSqlVariablePath(false).equals(sqlTablePath);
	}
	
	public FieldInfo findFieldInfo( String variableName ) {
		return getPersistentTable().getClassAliasedFieldInfoMap().get( variableName );
	}
	
	public String toString() {
		StringBuffer strBuf = new StringBuffer( determineDbTableName() );
		if( getAlias() != null ) {
			strBuf.append( " " ).append( getAlias() );
		}
		return strBuf.toString();
	}
	
	public void registerSelectCriteria( AqlTableVariable selectCriteria ) {
		getRegisteredSelectCriteria().add( selectCriteria );
	}
	
	@Override
	public String getSqlString() {
		StringBuffer sqlString = new StringBuffer( getFromClauseTableDescription() );
		sqlString.append( " AS " ).append( getAlias() );
		return sqlString.toString();
	}
	
	@Override
	public void optimiseSelectCriteria() {
		/*
		 * Only optimise one if you can optimise all
		 */
		for( int i = 0, n = getRegisteredSelectCriteria().size(); i < n; i++ ) {
			if( getRegisteredSelectCriteria().get( i ).getForeignKeySelectCriteria() == null ) {
				return;
			}
		}

		ForeignKeyAqlVariable tempForeignKeySelectCriteria;
		for( int i = getRegisteredSelectCriteria().size() - 1; i >= 0; i-- ) {
			tempForeignKeySelectCriteria = getRegisteredSelectCriteria().get( i ).getForeignKeySelectCriteria();
			getRegisteredSelectCriteria().get( i ).setUsingForeignKey( true );
			tempForeignKeySelectCriteria.getAqlTable().registerSelectCriteria(tempForeignKeySelectCriteria);
			getRegisteredSelectCriteria().remove( i );
		}
	}
	
	public String getJoinClause() {
		if( !JoinType.COMMA_JOIN.equals( getJoinType() ) ) {
			return super.getJoinClause();
		}
		return "";
	}
	
	@Override
	public void evaluateInheritance() {
		if( isEvaluatingInheritance() ) {
			if( getPersistentTable() instanceof PersistentClass && isIncludedInFromClause() && !isJoinInferred() ) {
				PersistentClass tempPersistentClass = (PersistentClass) getPersistentTable();
				if( tempPersistentClass != null && tempPersistentClass.getDbPersistentClass().getDiscriminatorFieldInfo() != null && tempPersistentClass != tempPersistentClass.getDbPersistentClass() ) {
					WhereConditionGroup whereConditionGroup = new WhereConditionGroup();
					tempPersistentClass.createPolymorphicWhereClause( getProcessedBeanDao(), this, whereConditionGroup );
					getProcessedBeanDao().getProcessedWhereConditionGroup().addWhereCondition( "AND", whereConditionGroup );
				}
			}
		}
	}
	
	public boolean isVariableInScope( String variableName ) {
		return getPersistentTable().getAliasedFieldInfoMap().get( variableName ) != null;
	}
	
	public boolean isVariableInScope( FieldInfo fieldInfo ) {
		return getPersistentTable().getAliasedFieldInfoMap().containsValue( fieldInfo );
	}
	
	@Override
	public int generateAlias( int latestTableIdx ) {
		String prefix = getPersistentTable().determineSqlTableName();
		if( prefix == null ) {
			prefix = "genAlias";
		}
		setAlias( prefix + latestTableIdx );
		setAliasGenerated( true );
		return ++latestTableIdx;
	}

	public String determineDbTableName() {
		return persistableTable.determineSqlTableName();
	}
	
	public String getFromClauseTableDescription() {
		return determineDbTableName(); 
	}

	public CaseAqlVariable getCaseSelectCriteria() {
		return caseSelectCriteria;
	}

	public void setCaseSelectCriteria(CaseAqlVariable caseSelectCriteria) {
		this.caseSelectCriteria = caseSelectCriteria ;
	}

	public boolean isJoinInferred() {
		return isJoinInferred;
	}

	public void setJoinInferred(boolean isJoinInferred) {
		this.isJoinInferred = isJoinInferred;
	}

	public PersistableTable getPersistentTable() {
		return persistableTable;
	}

	public void setPersistentTable(PersistableTable persistableTable) {
		this.persistableTable = persistableTable;
	}

	public AqlTable getJoinedTable() {
		return joinedTable;
	}

	public void setJoinedTable(AqlTable joinedTable) {
		this.joinedTable = joinedTable;
	}

	public FieldInfo getAssociatedFieldInfo() {
		return associatedFieldInfo;
	}

	public void setAssociatedFieldInfo(FieldInfo associatedFieldInfo) {
		this.associatedFieldInfo = associatedFieldInfo;
	}

	@Override
	public boolean isIncludedInJoin() {
		return isIncludedInJoin;
	}

	public void setIncludedInJoin(boolean isIncludedInJoin) {
		this.isIncludedInJoin = isIncludedInJoin;
	}

	public FieldInfo getParentFieldInfo() {
		return parentFieldInfo;
	}

	public void setParentFieldInfo(FieldInfo parentFieldInfo) {
		this.parentFieldInfo = parentFieldInfo;
	}

	public List<AqlTableAbstract> getSubTables() {
		return subTables;
	}

	public void setSubTables(List<AqlTableAbstract> subTables) {
		this.subTables = subTables;
	}

	public PersistentClassSelectCriteria getPersistentClassSelectCriteria() {
		return persistentClassSelectCriteria;
	}

	public void setPersistentClassSelectCriteria(
			PersistentClassSelectCriteria persistentClassSelectCriteria) {
		this.persistentClassSelectCriteria = persistentClassSelectCriteria;
	}

	public List<AqlTableVariable> getRegisteredSelectCriteria() {
		return registeredSelectCriteria;
	}

	public void setRegisteredSelectCriteria(List<AqlTableVariable> registeredSelectCriteria) {
		this.registeredSelectCriteria = registeredSelectCriteria;
	}

	public boolean isEvaluatingInheritance() {
		return isEvaluatingInheritance;
	}

	public void setEvaluatingInheritance(boolean isEvaluatingInheritance) {
		this.isEvaluatingInheritance = isEvaluatingInheritance;
	}

	public void setIsIncludedInFromClause(Boolean isIncludedInFromClause) {
		this.isIncludedInFromClause = isIncludedInFromClause;
	}

	public boolean isAddingAllCriteria() {
		return isAddingAllCriteria;
	}

	public void setAddingAllCriteria(boolean isAddingAllCriteria) {
		this.isAddingAllCriteria = isAddingAllCriteria;
	}
}
