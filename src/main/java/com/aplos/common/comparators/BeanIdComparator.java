package com.aplos.common.comparators;

import java.util.Comparator;

import com.aplos.common.beans.AplosAbstractBean;

public class BeanIdComparator implements Comparator<AplosAbstractBean> {
	@Override
	public int compare(AplosAbstractBean o1, AplosAbstractBean o2) {
		if( o1.getId() == null ) {
			return -1;
		} else if( o2.getId() == null ) {
			return 1;
		} else {
			return o1.getId().compareTo( o2.getId() );
		}
	}
}
