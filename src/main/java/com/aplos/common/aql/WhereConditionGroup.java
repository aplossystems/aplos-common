package com.aplos.common.aql;

import java.util.ArrayList;
import java.util.List;

import com.aplos.common.interfaces.WhereCondition;
import com.aplos.common.utils.CommonUtil;

public class WhereConditionGroup implements WhereCondition {
	private List<WhereCondition> whereConditions = new ArrayList<WhereCondition>();
	private List<String> logicalOperators = new ArrayList<String>();
	private BeanDao aqlBeanDao;
	
	public WhereConditionGroup() {
	}
	
	public WhereConditionGroup(WhereConditionGroup sourceWhereConditionGroup) {
		copy(sourceWhereConditionGroup);
	}
	
	public WhereConditionGroup( BeanDao aqlBeanDao ) {
		setAqlBeanDao(aqlBeanDao);
	}
	
	public void copy(WhereConditionGroup sourceWhereConditionGroup) {
		for( int i = 0, n = sourceWhereConditionGroup.getWhereConditions().size(); i < n; i++ ) {
			getWhereConditions().add( sourceWhereConditionGroup.getWhereConditions().get( i ).copy() );
		}
		setLogicalOperators( new ArrayList<String>( sourceWhereConditionGroup.getLogicalOperators() ) );
		setAqlBeanDao( sourceWhereConditionGroup.getAqlBeanDao() );
	}
	
	@Override
	public WhereCondition copy() {
		return new WhereConditionGroup( this );
	}
	
	@Override
	public void appendCondition(StringBuffer strBuf) {
		strBuf.append( "(" );
		for( int i = 0, n = whereConditions.size(); i < n; i++ ) {
			whereConditions.get( i ).appendCondition(strBuf);
			if( i < (whereConditions.size() - 1) ) {
				strBuf.append( " " ).append( logicalOperators.get( i ) ).append( " ");
			}
		}
		strBuf.append( ")" );
	}
	
	@Override
	public boolean evaluateCriteriaTypes(ProcessedBeanDao aqlBeanDao) {
		boolean completedSuccessfully = true;
		for( int i = 0, n = whereConditions.size(); i < n; i++ ) {
			completedSuccessfully = whereConditions.get( i ).evaluateCriteriaTypes( aqlBeanDao ) && completedSuccessfully;
		}
		return completedSuccessfully;
	}
	
	public void addNamedParameters( ProcessedBeanDao processedBeanDao ) {
		for( int i = 0, n = whereConditions.size(); i < n; i++ ) {
			whereConditions.get( i ).addNamedParameters( processedBeanDao );
		}
	}
	
	public String generateCondition() {
		StringBuffer strBuf = new StringBuffer();
		appendCondition( strBuf );
		return strBuf.toString();
	}
	
	public void addWhereCondition( String logicalOperator, WhereCondition whereCondition ) {
		whereConditions.add( whereCondition );
		if( whereConditions.size() > 1 && !CommonUtil.isNullOrEmpty( logicalOperator ) ) {
			logicalOperators.add( logicalOperator );
		}
	}
	
	public void clearConditions() {
		getWhereConditions().clear();
		getLogicalOperators().clear();
	}
	
	public List<WhereCondition> getWhereConditions() {
		return whereConditions;
	}
	public void setWhereConditions(List<WhereCondition> whereConditions) {
		this.whereConditions = whereConditions;
	}
	public List<String> getLogicalOperators() {
		return logicalOperators;
	}
	public void setLogicalOperators(List<String> logicalOperator) {
		this.logicalOperators = logicalOperator;
	}

	public BeanDao getAqlBeanDao() {
		return aqlBeanDao;
	}

	public void setAqlBeanDao(BeanDao aqlBeanDao) {
		this.aqlBeanDao = aqlBeanDao;
	}
	
	@Override
	public String toString() {
		StringBuffer strBuf = new StringBuffer();
		appendCondition(strBuf);
		return strBuf.toString();
	}
}
