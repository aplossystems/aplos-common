package com.aplos.common.comparators;

import java.util.Comparator;

import javax.faces.model.SelectItem;

import com.aplos.common.utils.CommonUtil;

public class SelectItemLabelComparator implements Comparator<SelectItem> {
	@Override
	public int compare(SelectItem o1, SelectItem o2) {
		return CommonUtil.compare( o1.getLabel(), o2.getLabel() );
	}
}
