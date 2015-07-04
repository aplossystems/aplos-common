package com.aplos.common;

import com.aplos.common.utils.ApplicationUtil;

public class ImplicitPolymorphismVariable {
	private String fullClassName;
	private String variableName;

	public ImplicitPolymorphismVariable() {}

	public ImplicitPolymorphismVariable( String fullClassName, String variableName ) {
		this.fullClassName = fullClassName;
		this.variableName = variableName;
	}
	
	public void addEntry( ImplicitPolymorphismEntry implicitPolymorphismEntry ) {
		ApplicationUtil.getAplosContextListener().addImplicitPolymorphismEntry( this, implicitPolymorphismEntry );
	}
	
	public void addEntry( Class<?> entryClass ) {
		addEntry( new ImplicitPolymorphismEntry( entryClass ) );
	}

	public void setFullClassName(String fullClassName) {
		this.fullClassName = fullClassName;
	}
	public String getFullClassName() {
		return fullClassName;
	}
	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}
	public String getVariableName() {
		return variableName;
	}
}
