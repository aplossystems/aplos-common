package com.aplos.common.comparators;

import java.util.Comparator;

import com.aplos.common.beans.AplosBean;

public class DateCreatedComparator implements Comparator<AplosBean> {
	private boolean ascending = true;
	
	public DateCreatedComparator() {
	}
	
	public DateCreatedComparator( boolean ascending ) {
		this.ascending = ascending;
	}
	
	@Override
	public int compare(AplosBean o1, AplosBean o2) {
		if( o1.getDateCreated() == null ) {
			return -1;
		} else if( o2.getDateCreated() == null ) {
			return 1;
		} else {
			if( ascending ) {
				return o1.getDateCreated().compareTo(o2.getDateCreated());
			} else {
				return o2.getDateCreated().compareTo(o1.getDateCreated());
			}
		}
	}
}
