package com.aplos.common.comparators;

import java.util.Comparator;

import com.aplos.common.interfaces.PersistenceBean;

public class PersistenceBeanPositionComparator implements Comparator<PersistenceBean> {
	private String fieldName;
	
	public PersistenceBeanPositionComparator( String fieldName ) {
		this.fieldName = fieldName;
	}
	
	@Override
	public int compare(PersistenceBean o1, PersistenceBean o2) {
		Integer position1 = ((Integer) o1.getHiddenFieldsMap().get( fieldName ));
		Integer position2 = (Integer) o2.getHiddenFieldsMap().get( fieldName );
		if( position1 == null && position2 == null ) {
			return 0;
		} else if( position1 == null ) {
			return -1;
		} else if( position2 == null ) {
			return 1;
		}
		return position1.compareTo( position2 );
	}
}
