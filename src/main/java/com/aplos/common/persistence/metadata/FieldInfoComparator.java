package com.aplos.common.persistence.metadata;

import java.util.Comparator;

import com.aplos.common.persistence.fieldinfo.FieldInfo;

public class FieldInfoComparator implements Comparator<FieldInfo> {
	@Override
	public int compare(FieldInfo o1, FieldInfo o2) {
		if( o1.isPrimaryKey() && !o2.isPrimaryKey() ) {
			return -1;
		} else if( !o1.isPrimaryKey() && o2.isPrimaryKey() ) {
			return 1;
		} else {
			return o1.getSqlName().toLowerCase().compareTo( o2.getSqlName().toLowerCase() );
		}
	}
}
