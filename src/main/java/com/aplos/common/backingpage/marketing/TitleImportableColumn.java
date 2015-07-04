package com.aplos.common.backingpage.marketing;

import java.util.HashMap;
import java.util.Map;

import com.aplos.common.ImportableColumn;
import com.aplos.common.enums.ContactTitle;

public class TitleImportableColumn extends ImportableColumn {
	private static Map<String, Integer> titleMap;
	
	public TitleImportableColumn() {
		super("Salutation", "bean.address.contactTitle");
	}
	
	@Override
	public void init() {
		titleMap = new HashMap<String,Integer>();
		for( ContactTitle contactTitle : ContactTitle.values() ) {
			titleMap.put( contactTitle.getLabel().toLowerCase(), contactTitle.ordinal() );
		}
	}
	
	@Override
	public Object format(String value) {
		if( value != null ) {
			return titleMap.get( value.toLowerCase() );
		} else {
			return null;
		}
	}
}
