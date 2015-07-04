package com.aplos.common.utils;

import java.lang.reflect.Field;

public class ParentAndField {
	private Object parent;
	private Field field;
	
	public ParentAndField( Object parent, Field field ) {
		this.parent = parent;
		this.field = field;
	}

	public Object getParent() {
		return parent;
	}

	public void setParent(Object parent) {
		this.parent = parent;
	}

	public Field getField() {
		return field;
	}

	public void setField(Field field) {
		this.field = field;
	}
}
