package com.aplos.common.aql;

import com.aplos.common.aql.aqlvariables.AqlTableVariableInter;
import com.aplos.common.aql.aqlvariables.AqlVariable;
import com.aplos.common.aql.aqlvariables.AqlTableVariable;
import com.aplos.common.aql.selectcriteria.SelectCriteria;
import com.aplos.common.interfaces.WhereCondition;
import com.aplos.common.persistence.type.applicationtype.DateTimeType;

public class IndividualWhereCondition implements WhereCondition {
	private BeanDao beanDao;
	private AqlVariable leftHandVariable;
	private AqlVariable rightHandVariable;
	private String conditionalOperator;
	
	public IndividualWhereCondition( BeanDao beanDao ) {
		setBeanDao(beanDao);
	}
	
	public IndividualWhereCondition( IndividualWhereCondition sourceCondition ) {
		copy( sourceCondition ); 
	}
	
	public IndividualWhereCondition( BeanDao beanDao, AqlVariable leftHandVariable, String conditionalOperator, AqlVariable rightHandVariable ) {
		this.setBeanDao(beanDao);
		this.leftHandVariable = leftHandVariable;
		this.conditionalOperator = conditionalOperator;
		this.setRightHandVariable(rightHandVariable);
	}
	
	public void copy( IndividualWhereCondition sourceCondition ) {
		setBeanDao( sourceCondition.getBeanDao() );
		setLeftHandVariable( sourceCondition.getLeftHandVariable() );
		setRightHandVariable( sourceCondition.getRightHandVariable() );
		setConditionalOperator( sourceCondition.getConditionalOperator() );
	}
	
	@Override
	public WhereCondition copy() {
		return new IndividualWhereCondition(this);
	}
	
	@Override
	public void addNamedParameters(ProcessedBeanDao processedBeanDao) {
		getRightHandVariable().addNamedParameters( processedBeanDao );
	}

	@Override
	public boolean evaluateCriteriaTypes(ProcessedBeanDao processedBeanDao) {
		if( getLeftHandVariable().evaluateCriteriaTypes( processedBeanDao, false ) 
				&& getRightHandVariable().evaluateCriteriaTypes( processedBeanDao, false ) ) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public void appendCondition(StringBuffer strBuf) {
		if( !(leftHandVariable instanceof AqlTableVariableInter) || ((AqlTableVariableInter) leftHandVariable).getAqlTableAbstract().getProcessedBeanDao().getBeanDao().equals( getBeanDao() ) ) {
			if( leftHandVariable instanceof AqlTableVariable ) {
				strBuf.append( ((AqlTableVariable) leftHandVariable).getCriteria(getBeanDao()) ).append( " " );
			} else {
				strBuf.append( leftHandVariable.getSqlPath(false) ).append( " " );
			}
		} else {
			strBuf.append( leftHandVariable.getSubSqlPath(true) ).append( " " );
		}
		if( leftHandVariable instanceof AqlTableVariable 
				&& ((AqlTableVariable) leftHandVariable).getFieldInfo() != null
				&& ((AqlTableVariable) leftHandVariable).getFieldInfo().getApplicationType() instanceof DateTimeType 
				&& conditionalOperator.equals( "!=" ) ) {
			strBuf.append( "IS NOT " );	
		} else {
			strBuf.append( conditionalOperator ).append( " " );
		}
		if( getRightHandVariable() instanceof AqlTableVariable ) {
			strBuf.append( ((AqlTableVariable) getRightHandVariable()).getCriteria(getBeanDao()) );
		} else {
			strBuf.append( getRightHandVariable().getSqlPath(false) );
		}
	}
	
	@Override
	public String toString() {
		StringBuffer strBuf = new StringBuffer();
		appendCondition( strBuf );
		return strBuf.toString();
	}
	
	public AqlVariable getLeftHandVariable() {
		return leftHandVariable;
	}
	public void setLeftHandVariable(AqlVariable leftHandVariable) {
		this.leftHandVariable = leftHandVariable;
	}
	public String getConditionalOperator() {
		return conditionalOperator;
	}
	public void setConditionalOperator(String conditionalOperator) {
		this.conditionalOperator = conditionalOperator;
	}

	public BeanDao getBeanDao() {
		return beanDao;
	}

	public void setBeanDao(BeanDao beanDao) {
		this.beanDao = beanDao;
	}

	public AqlVariable getRightHandVariable() {
		return rightHandVariable;
	}

	public void setRightHandVariable(AqlVariable rightHandVariable) {
		this.rightHandVariable = rightHandVariable;
	}
}
