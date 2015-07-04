package com.aplos.common.aql.aqlvariables;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.AplosAbstractBean;

public class EnumDeterminantAqlVariable extends AqlTableVariable {
	private CaseAqlVariable caseSelectCriteria;

	public EnumDeterminantAqlVariable() {
	}

	public EnumDeterminantAqlVariable( CaseAqlVariable caseSelectCriteria ) {
		setCaseSelectCriteria(caseSelectCriteria);
	}

	@Override
    public void setField( boolean isLoadingFromDatabase, AplosAbstractBean bean, Object value ) throws Exception {
    	getCaseSelectCriteria().setDeterminantValue( (Integer) value );
    }

	@Override
	public String getCriteria( BeanDao aqlBeanDao ) {
		StringBuffer sqlBuf = new StringBuffer( "CASE WHEN " );
		caseSelectCriteria.getWhereCondition().appendCondition(sqlBuf);
		sqlBuf.append( " THEN " );
		sqlBuf.append( 0 ); 
		sqlBuf.append( " ELSE " );
		sqlBuf.append( 1 );
		sqlBuf.append( " END " );
		return sqlBuf.toString();
	}

	public CaseAqlVariable getCaseSelectCriteria() {
		return caseSelectCriteria;
	}

	public void setCaseSelectCriteria(CaseAqlVariable caseSelectCriteria) {
		this.caseSelectCriteria = caseSelectCriteria;
	}
}
