package com.aplos.common.aql.aqlvariables;

public class ForeignKeyAqlVariable extends AqlTableVariable {
	private AqlTableVariable variableSelectCriteria;
	
	public ForeignKeyAqlVariable( AqlTableVariable variableSelectCriteria ) {
		setVariableSelectCriteria(variableSelectCriteria);
	}

	public AqlTableVariable getVariableSelectCriteria() {
		return variableSelectCriteria;
	}

	public void setVariableSelectCriteria(AqlTableVariable variableSelectCriteria) {
		this.variableSelectCriteria = variableSelectCriteria;
	}
}
