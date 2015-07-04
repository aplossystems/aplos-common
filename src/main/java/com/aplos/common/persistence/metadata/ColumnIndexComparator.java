package com.aplos.common.persistence.metadata;

import java.util.Comparator;

public class ColumnIndexComparator implements Comparator<ColumnIndex> {
	@Override
	public int compare(ColumnIndex o1, ColumnIndex o2) {
		int columnNameCompare = o1.getColumnName().compareTo( o2.getColumnName() );
		if( columnNameCompare == 0 ) {
			return o1.getType().compareTo( o2.getType() );
		}
		return columnNameCompare;
	}
}
