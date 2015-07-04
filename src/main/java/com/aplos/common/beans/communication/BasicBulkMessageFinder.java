package com.aplos.common.beans.communication;

import java.util.ArrayList;
import java.util.List;

import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.interfaces.BulkEmailFinder;
import com.aplos.common.interfaces.BulkMessageFinderInter;
import com.aplos.common.interfaces.BulkMessageSource;
import com.aplos.common.interfaces.BulkSmsFinder;
import com.aplos.common.utils.CommonUtil;

@Entity
public class BasicBulkMessageFinder extends BulkMessageSourceGroup {
	private static final long serialVersionUID = -8746792004354723646L;
	private Class<? extends BulkMessageFinderInter> bulkMessageFinderClass;

	public BasicBulkMessageFinder() {}

	public BasicBulkMessageFinder( Class<? extends BulkMessageFinderInter> bulkMessageFinderClass ) {
		this.setBulkMessageFinderClass(bulkMessageFinderClass);
	}

	public BasicBulkMessageFinder(String name, String hqlStatement) {
		setName( name );
	}
	
	public BulkEmailFinder getBulkEmailFinderClassInstance() {
		Class<? extends BulkMessageFinderInter> bulkMessageFinderClass = getBulkMessageFinderClass();
		return (BulkEmailFinder) CommonUtil.getNewInstance( bulkMessageFinderClass, null);
	}
	
	public BulkSmsFinder getBulkSmsFinderClassInstance() {
		return (BulkSmsFinder) CommonUtil.getNewInstance( getBulkMessageFinderClass(), null);
	}

	public List<BulkMessageSource> getBulkMessageSources() {
		BulkEmailFinder bulkEmailFinder = getBulkEmailFinderClassInstance();
		return new ArrayList<BulkMessageSource>( bulkEmailFinder.getEmailAutoCompleteSuggestions( null, 15 ) );
	}

	public Class<? extends BulkMessageFinderInter> getBulkMessageFinderClass() {
		return bulkMessageFinderClass;
	}

	public void setBulkMessageFinderClass(Class<? extends BulkMessageFinderInter> bulkMessageFinderClass) {
		this.bulkMessageFinderClass = bulkMessageFinderClass;
	}
}
