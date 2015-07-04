package com.aplos.common.comparators;

import java.util.Comparator;

import com.aplos.common.interfaces.DisplayName;
import com.aplos.common.utils.CommonUtil;

public class DisplayNameComparator implements Comparator<DisplayName> {
	
	@Override
	public int compare(DisplayName o1, DisplayName o2) {
		if( o1 == null ) {
			return -1;
		} else if( o2 == null ) {
			return 1;
		} else {
			return CommonUtil.compare( CommonUtil.getStringOrEmpty( o1.getDisplayName() ), CommonUtil.getStringOrEmpty( o2.getDisplayName() ) );
		}
	}
}
