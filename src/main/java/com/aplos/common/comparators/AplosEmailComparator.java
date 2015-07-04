package com.aplos.common.comparators;

import java.util.Comparator;

import com.aplos.common.beans.communication.AplosEmail;

public class AplosEmailComparator implements Comparator<AplosEmail>{
	@Override
	public int compare(AplosEmail aplosEmail1, AplosEmail aplosEmail2) {
		return aplosEmail2.getDateCreated().compareTo( aplosEmail1.getDateCreated() );
	}

}
