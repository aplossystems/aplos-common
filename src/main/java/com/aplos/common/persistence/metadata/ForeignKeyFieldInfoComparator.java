package com.aplos.common.persistence.metadata;

import java.util.Comparator;

import com.aplos.common.persistence.fieldinfo.ForeignKeyFieldInfo;

public class ForeignKeyFieldInfoComparator implements Comparator<ForeignKeyFieldInfo> {
	@Override
	public int compare(ForeignKeyFieldInfo o1, ForeignKeyFieldInfo o2) {
		return o1.getSqlName().compareTo( o2.getSqlName() );
	}
}
