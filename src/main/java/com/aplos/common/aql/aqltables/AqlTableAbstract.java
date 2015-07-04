package com.aplos.common.aql.aqltables;

import java.util.ArrayList;
import java.util.List;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.aql.aqltables.AqlTable.JoinType;
import com.aplos.common.aql.aqlvariables.AqlTableVariable;
import com.aplos.common.aql.selectcriteria.SelectCriteria;
import com.aplos.common.persistence.PersistableTable;
import com.aplos.common.persistence.fieldinfo.FieldInfo;


public abstract class AqlTableAbstract {
	private JoinType joinType = JoinType.LEFT_OUTER_JOIN;
	private AqlTableAbstract parentTable;
	private AqlTableVariable internalVariable;
	private AqlTableVariable externalVariable;
	private String alias;
	private boolean isAliasGenerated = false;
	private ProcessedBeanDao processedBeanDao;
	/*
	 * Not sure that this is needed as AqlTable is created every call so why
	 * would evaluateAqlTable be called twice?
	 */
	private boolean isEvaluated = false;
	private List<FieldInfo> nonDbFieldInfos = new ArrayList<FieldInfo>();
	private String sqlName;
	
	public AqlTableAbstract() {
	}
	
	public AqlTableAbstract( String alias ) {
		setAlias( alias );
	}
	
	public abstract String getSqlString();
	
	public String getJoinClause() {
		StringBuffer strBuf = new StringBuffer( " ON " );
		strBuf.append( getInternalVariable().getSqlPath(true) );
		strBuf.append( "=" );
		strBuf.append( getExternalVariable().getSqlPath(true) );
		return strBuf.toString();
	}

	public AqlTableAbstract getParentTable() {
		return parentTable;
	}

	public void addAllCriteria(ProcessedBeanDao processedBeanDao, List<SelectCriteria> criteriaList) {
		
	}
	
	public abstract FieldInfo getFieldInfoFromSqlName( String sqlName, boolean searchAllClasses );

	public void updateParentTable(AqlTableAbstract parentTable) {
		this.parentTable = parentTable;
//		if( parentTable != null ) {
//			parentTable.getSubTables().add( this );
//		}
	}
	

	public FieldInfo findFieldInfo( String variableName ) {
		return null;
	}
	
	public String determineSqlVariablePath( boolean includeGeneratedAliases ) {
		if( getAlias() != null && (includeGeneratedAliases || !isAliasGenerated()) ) {
			return getAlias();
		}
		

		if( getParentTable() != null ) {
			StringBuffer strBuf = new StringBuffer();
			strBuf.append( getParentTable().determineSqlVariableParentPath( includeGeneratedAliases ) );
			strBuf.append( "." ).append( getSqlName() );
			return strBuf.toString();
		} 
		
		return getSqlName();
	}
	
	public String determineSqlVariableParentPath( boolean includeGeneratedAliases ) {
		if( getAlias() != null && (includeGeneratedAliases || !isAliasGenerated()) ) {
			return getAlias();
		}
		

		if( getParentTable() != null ) {
			StringBuffer strBuf = new StringBuffer();
			strBuf.append( getParentTable().determineSqlVariableParentPath( includeGeneratedAliases ) );
			return strBuf.toString();
		} 
		
		return getSqlName();
	}
	
	public abstract int generateAlias( int latestTableIdx );
	
	public void evaluateCriteria() {
		
	}

	public void optimiseSelectCriteria() {}
	
	public void evaluateInheritance() {
		
	}
	
	public boolean isVariableInScope( String variableName ) {
		return false;
	}
	
	public boolean evaluateAqlTable(ProcessedBeanDao processedBeanDao) {
		setEvaluated(true);
		if( getInternalVariable() != null ) {
			getInternalVariable().evaluateCriteriaTypes(processedBeanDao, false);
		}
		if( getExternalVariable() != null ) {
			getExternalVariable().evaluateCriteriaTypes(processedBeanDao, false);
		}
		return true;
	}
	
	public boolean isIncludedInJoin() { 
		return true;
	}

	public boolean isIncludedInFromClause() {
		return true;
	}

	public JoinType getJoinType() {
		return joinType;
	}

	public void setJoinType(JoinType joinType) {
		this.joinType = joinType ;
	}

	public String getAlias() {
		return alias;
	}
	
	public void setAlias(String alias) {
		this.alias = alias;
	}

	public ProcessedBeanDao getProcessedBeanDao() {
		return processedBeanDao;
	}

	public void setProcessedBeanDao(ProcessedBeanDao aqlBeanDao) {
		this.processedBeanDao = aqlBeanDao;
	}

	public boolean isAliasGenerated() {
		return isAliasGenerated;
	}

	public void setAliasGenerated(boolean isAliasGenerated) {
		this.isAliasGenerated = isAliasGenerated;
	}

	public List<FieldInfo> getNonDbFieldInfos() {
		return nonDbFieldInfos;
	}

	public void setNonDbFieldInfos(List<FieldInfo> nonDbFieldInfos) {
		this.nonDbFieldInfos = nonDbFieldInfos;
	}

	public boolean isEvaluated() {
		return isEvaluated;
	}

	public void setEvaluated(boolean isEvaluated) {
		this.isEvaluated = isEvaluated;
	}

	public AqlTableVariable getInternalVariable() {
		return internalVariable;
	}

	public void setInternalVariable(AqlTableVariable internalVariable) {
		this.internalVariable = internalVariable;
	}

	public AqlTableVariable getExternalVariable() {
		return externalVariable;
	}

	public void setExternalVariable(AqlTableVariable externalVariable) {
		this.externalVariable = externalVariable;
	}

	public String getSqlName() {
		return sqlName;
	}

	public void setSqlName(String sqlName) {
		this.sqlName = sqlName;
	}
}
