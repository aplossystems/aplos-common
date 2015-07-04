package com.aplos.common.aql.aqlvariables;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.aql.IndividualWhereCondition;
import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.aql.WhereConditionGroup;
import com.aplos.common.aql.aqltables.AqlTable;
import com.aplos.common.interfaces.WhereCondition;

public class CaseAqlVariable extends AqlTableVariable {
	private WhereCondition whereCondition;
	private AqlVariable thenAqlVariable;
	private AqlVariable elseAqlVariable;
	private int determinantValue;

	public CaseAqlVariable( WhereCondition whereCondition, AqlVariable thenAqlVariable, AqlVariable elseAqlVariable ) {
		setWhereCondition( whereCondition );
		setThenAqlVariable( thenAqlVariable );
		setElseAqlVariable( elseAqlVariable );
	}
	
	@Override
	public String getCriteria( BeanDao aqlBeanDao ) {
		StringBuffer sqlBuf = new StringBuffer( "CASE WHEN " );
		getWhereCondition().appendCondition( sqlBuf );
		sqlBuf.append( " THEN " );
		sqlBuf.append( getThenAqlVariable().getCriteria( aqlBeanDao ) ); 
		sqlBuf.append( " ELSE " );
		sqlBuf.append( getElseAqlVariable().getCriteria( aqlBeanDao ) );
		sqlBuf.append( " END " );
		return sqlBuf.toString();
	}
	
	@Override
	public boolean evaluateCriteriaTypes(ProcessedBeanDao aqlBeanDao, boolean isAllowingFullTables) {
		if( !getWhereCondition().evaluateCriteriaTypes(aqlBeanDao) ) {
			return false;
		}
		if( !thenAqlVariable.evaluateCriteriaTypes(aqlBeanDao, false) ) {
			return false;
		}
		if( !elseAqlVariable.evaluateCriteriaTypes(aqlBeanDao, false) ) {
			return false;
		}
		return true;
	}
	
	@Override
	public AqlTable getAqlTable() {
		if( getWhereCondition() instanceof WhereConditionGroup ) {
			IndividualWhereCondition individualWhereCondition = (IndividualWhereCondition) ((WhereConditionGroup) getWhereCondition()).getWhereConditions().get( 0 );
			return ((AqlTableVariable) individualWhereCondition.getLeftHandVariable()).getAqlTable();
		} else if( getWhereCondition() instanceof IndividualWhereCondition ) {
			return ((AqlTableVariable) ((IndividualWhereCondition) getWhereCondition()).getLeftHandVariable()).getAqlTable();	
		}
		
		return null;
	}
	
	public String getName() {
		if( getDeterminantValue() == 0 ) {
			return thenAqlVariable.getName();
		} else {
			return elseAqlVariable.getName();
		}
	}
	
	public AqlVariable getWinningCriteria() {
		if( getDeterminantValue() == 0 ) {
			return thenAqlVariable;
		} else {
			return elseAqlVariable;
		}
	};

	public AqlVariable getThenAqlVariable() {
		return thenAqlVariable;
	}

	public void setThenAqlVariable(AqlVariable thenCriteria) {
		this.thenAqlVariable = thenCriteria;
	}

	public AqlVariable getElseAqlVariable() {
		return elseAqlVariable;
	}

	public void setElseAqlVariable(AqlVariable elseCriteria) {
		this.elseAqlVariable = elseCriteria;
	}

	public int getDeterminantValue() {
		return determinantValue;
	}

	public void setDeterminantValue(int determinantValue) {
		this.determinantValue = determinantValue;
	}

	public WhereCondition getWhereCondition() {
		return whereCondition;
	}

	public void setWhereCondition(WhereCondition whereCondition) {
		this.whereCondition = whereCondition;
	}
}
