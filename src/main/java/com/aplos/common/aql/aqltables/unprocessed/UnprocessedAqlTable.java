package com.aplos.common.aql.aqltables.unprocessed;

import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.aql.aqltables.AqlTable.JoinType;
import com.aplos.common.aql.aqltables.AqlTableAbstract;
import com.aplos.common.aql.aqlvariables.CaseAqlVariable;

public abstract class UnprocessedAqlTable {
	/*
	 * It seems that there are three ways to join at the moment.  The most common
	 * is to join based on a path to a collection e.g. bean.children.  The second
	 * is a comma join and the third the classic ON join with left and right conditions.
	 * It might be wise to separate these UnprocessedAqlTables up later and then feed in 
	 * the correct information into the AqlTable.
	 */
	private String alias;
	private JoinType joinType = JoinType.LEFT_OUTER_JOIN;
	private CaseAqlVariable caseSelectCriteria;
	private boolean isAddingAllCriteria = false;
	
	public UnprocessedAqlTable() {
	}
	
	public abstract AqlTableAbstract createAqlTable(ProcessedBeanDao processedBeanDao, boolean isAddingTable );
	
	public String getAlias() {
		return alias;
	}
	public void setAlias(String alias) {
		this.alias = alias;
	}

	public JoinType getJoinType() {
		return joinType;
	}

	public void setJoinType(JoinType joinType) {
		this.joinType = joinType;
	}

	public CaseAqlVariable getCaseSelectCriteria() {
		return caseSelectCriteria;
	}

	public void setCaseSelectCriteria(CaseAqlVariable caseSelectCriteria) {
		this.caseSelectCriteria = caseSelectCriteria;
	}

	public boolean isAddingAllCriteria() {
		return isAddingAllCriteria;
	}

	public void setAddingAllCriteria(boolean isAddingAllCriteria) {
		this.isAddingAllCriteria = isAddingAllCriteria;
	}
}
