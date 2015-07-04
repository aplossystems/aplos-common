package com.aplos.common.persistence;

import java.util.HashMap;
import java.util.Map;

public class EnumHelper {
	private Class<Enum> enumClass;
	private Map<String,Enum> enumNameMap = new HashMap<String,Enum>();;
	private Enum[] enumValues;
	
	public void registerValues( Enum enumValues[] ) {
		setEnumValues( enumValues );
		for( int i = 0, n = enumValues.length; i < n; i++ ) {
			enumNameMap.put( enumValues[ i ].name(), enumValues[ i ] );
		}
	}
	
	public Class<Enum> getEnumClass() {
		return enumClass;
	}
	public void setEnumClass(Class<Enum> enumClass) {
		this.enumClass = enumClass;
	}
	public Map<String,Enum> getEnumNameMap() {
		return enumNameMap;
	}
	public void setEnumNameMap(Map<String,Enum> enumNameMap) {
		this.enumNameMap = enumNameMap;
	}
	public Enum[] getEnumValues() {
		return enumValues;
	}
	public void setEnumValues(Enum[] enumValues) {
		this.enumValues = enumValues;
	}
}
