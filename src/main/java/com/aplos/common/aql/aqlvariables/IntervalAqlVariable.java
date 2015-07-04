package com.aplos.common.aql.aqlvariables;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.aql.aqltables.AqlTable;

/*
 * Special criteria for the date_add function
 */
public class IntervalAqlVariable extends AqlTableVariable {
	private AqlVariable expression;
	private String type;

	public IntervalAqlVariable( AqlVariable expresssion, String type ) {
		setExpression( expresssion );
		setType( type );
	}
	
	@Override
	public String getCriteria( BeanDao aqlBeanDao ) {
		StringBuffer sqlBuf = new StringBuffer( "INTERVAL " );
		sqlBuf.append( getExpression().getCriteria(aqlBeanDao) );
		sqlBuf.append( " " ).append( getType() );
		return sqlBuf.toString();
	}
	
	@Override
	public String getSqlPath(boolean includeGeneratedAliasForName) {
		StringBuffer sqlBuf = new StringBuffer( "INTERVAL " );
		sqlBuf.append( getExpression().getSqlPath(includeGeneratedAliasForName) );
		sqlBuf.append( " " ).append( getType() );
		return sqlBuf.toString();
	}
	
	@Override
	public boolean evaluateCriteriaTypes(ProcessedBeanDao aqlBeanDao, boolean isAllowingFullTables) {
		if( !getExpression().evaluateCriteriaTypes(aqlBeanDao, false) ) {
			return false;
		}
		return true;
	}
	
	@Override
	public AqlTable getAqlTable() {
		if( getExpression() instanceof AqlTableVariable ) {
			return ((AqlTableVariable) getExpression()).getAqlTable();
		}
		return null;
	}
	
	public String getName() {
		return "Interval";
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public AqlVariable getExpression() {
		return expression;
	}

	public void setExpression(AqlVariable expression) {
		this.expression = expression;
	}
}
