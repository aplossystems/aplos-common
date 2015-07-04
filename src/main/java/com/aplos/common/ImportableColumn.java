package com.aplos.common;



public class ImportableColumn {
	private String name;
	private String variablePath;
	private boolean isRequired = false;
	
	public ImportableColumn() {
		init();
	}
	
	public ImportableColumn( String name, String variablePath ) {
		setName( name );
		setVariablePath(variablePath);
		init();
	}
	
	public void init() {
		
	}
	
	public String getDefaultValue() {
		return "";
	}
	
	public Object format( String value ) {
		value = value.trim();
		return value;
	}
	
	public boolean validate( Object value ) {
		return true;
	}
	
	public boolean duplicateCheck( Object value ) {
		return true;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getVariablePath() {
		return variablePath;
	}
	public void setVariablePath(String variablePath) {
		this.variablePath = variablePath;
	}

	public boolean isRequired() {
		return isRequired;
	}

	public void setRequired(boolean isRequired) {
		this.isRequired = isRequired;
	}
}
