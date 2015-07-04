package com.aplos.common.comparators;

import java.util.Comparator;

import com.aplos.common.utils.CommonUtil;

public class StringComparator implements Comparator<String> {
	@Override
	public int compare(String o1, String o2) {
		return CommonUtil.compare( o1, o2 );
	}
}
