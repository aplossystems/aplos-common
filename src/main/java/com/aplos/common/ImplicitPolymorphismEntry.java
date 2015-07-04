package com.aplos.common;

public class ImplicitPolymorphismEntry {
	private String fullClassName;
	private String classIdentifier;

	public ImplicitPolymorphismEntry() {

	}

	public ImplicitPolymorphismEntry( Class<?> entryClass ) {
		this.classIdentifier = entryClass.getSimpleName();
		this.fullClassName = entryClass.getName();
	}

	public ImplicitPolymorphismEntry(String fullClassName,String classIdentifier) {
		this.fullClassName = fullClassName;
		this.classIdentifier = classIdentifier;
	}

	public void setFullClassName(String fullClassName) {
		this.fullClassName = fullClassName;
	}

	public String getFullClassName() {
		return fullClassName;
	}

	public void setClassIdentifier(String classIdentifier) {
		this.classIdentifier = classIdentifier;
	}

	public String getClassIdentifier() {
		return classIdentifier;
	}
}
