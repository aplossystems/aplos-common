package com.aplos.common.aql.aqlvariables;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.interfaces.WhereCondition;

/*
 * Special criteria for the date_add function
 */
public class BetweenWhereCondition implements WhereCondition {
	private BeanDao beanDao;
	private AqlVariable leftHandVariable;
	private AqlVariable firstDateExpression;
	private AqlVariable secondDateExpression;

	public BetweenWhereCondition( BeanDao beanDao, AqlVariable leftHandVariable, AqlVariable firstDateExpression, AqlVariable secondDateExpression ) {
		setBeanDao(beanDao);
		setLeftHandVariable(leftHandVariable);
		setFirstDateExpression( firstDateExpression );
		setSecondDateExpression( secondDateExpression );
	}
	
	@Override
	public void addNamedParameters(ProcessedBeanDao processedBeanDao) {
		getFirstDateExpression().addNamedParameters( processedBeanDao );
		getSecondDateExpression().addNamedParameters( processedBeanDao );
	}
	
	@Override
	public void appendCondition(StringBuffer strBuf) {
		if( getLeftHandVariable() instanceof AqlTableVariable ) {
			strBuf.append( ((AqlTableVariable) getLeftHandVariable()).getCriteria(getBeanDao()) ).append( " " );
		} else {
			strBuf.append( getLeftHandVariable().getSqlPath(false) ).append( " " );
		}	
		strBuf.append( " BETWEEN " );
		if( getFirstDateExpression() instanceof AqlTableVariable ) {
			strBuf.append( ((AqlTableVariable) getFirstDateExpression()).getCriteria(getBeanDao()) ).append( " " );
		} else {
			strBuf.append( getFirstDateExpression().getSqlPath(false) ).append( " " );
		}
		strBuf.append( " AND " );
		if( getSecondDateExpression() instanceof AqlTableVariable ) {
			strBuf.append( ((AqlTableVariable) getSecondDateExpression()).getCriteria(getBeanDao()) ).append( " " );
		} else {
			strBuf.append( getSecondDateExpression().getSqlPath(false) ).append( " " );
		}
		
	}
	
	@Override
	public boolean evaluateCriteriaTypes(ProcessedBeanDao processedBeanDao) {
		if( getLeftHandVariable().evaluateCriteriaTypes( processedBeanDao, false ) 
				&& getFirstDateExpression().evaluateCriteriaTypes( processedBeanDao, false ) 
				&& getSecondDateExpression().evaluateCriteriaTypes( processedBeanDao, false ) ) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public WhereCondition copy(BeanDao beanDao) {
		return new BetweenWhereCondition(beanDao,getLeftHandVariable(),getFirstDateExpression(),getSecondDateExpression());
	}

	public AqlVariable getSecondDateExpression() {
		return secondDateExpression;
	}

	public void setSecondDateExpression(AqlVariable secondDateExpression) {
		this.secondDateExpression = secondDateExpression;
	}

	public AqlVariable getFirstDateExpression() {
		return firstDateExpression;
	}

	public void setFirstDateExpression(AqlVariable firstDateExpression) {
		this.firstDateExpression = firstDateExpression;
	}

	public BeanDao getBeanDao() {
		return beanDao;
	}

	public void setBeanDao(BeanDao beanDao) {
		this.beanDao = beanDao;
	}

	public AqlVariable getLeftHandVariable() {
		return leftHandVariable;
	}

	public void setLeftHandVariable(AqlVariable leftHandVariable) {
		this.leftHandVariable = leftHandVariable;
	}
}
