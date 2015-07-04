package com.aplos.common.module;

import com.aplos.common.beans.lookups.LookupBean;
import com.aplos.common.utils.ApplicationUtil;

public abstract class DatabaseLoader {
	private AplosModuleImpl aplosModule;

	public DatabaseLoader(AplosModuleImpl aplosModule) {
		this.setAplosModule(aplosModule);
	}

	public abstract void loadTables();

	public abstract void newTableAdded( Class<?> tableClass );

	public boolean isTableAlreadyLoaded( String tableName ) {
		Integer count = ((Long) ApplicationUtil.getFirstResult("select count(*) from " + tableName )[0]).intValue();
		if( count != 0 ) {
			return true;
		} else {
			return false;
		}
	}

	public void insertRows( LookupBean lookupBeanType, String[] nameValues ) {
		try {
		    for( int i = 0, n = nameValues.length; i < n; i++ ) {
		    	LookupBean lookupBean = lookupBeanType.getClass().newInstance();
		    	lookupBean.setName( nameValues[ i ] );
			   	lookupBean.saveDetails();
		    }
		} catch( IllegalAccessException iaex ) {
			iaex.printStackTrace();
		} catch( InstantiationException instEx ) {
			instEx.printStackTrace();
		}
	}

	public void setAplosModule(AplosModuleImpl newAplosModule) {
		aplosModule = newAplosModule;
	}

	public AplosModuleImpl getAplosModule() {
		return aplosModule;
	}
}
