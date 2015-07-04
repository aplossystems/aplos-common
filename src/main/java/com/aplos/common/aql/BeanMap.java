package com.aplos.common.aql;

import java.util.HashMap;
import java.util.Map;

import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.utils.ApplicationUtil;

public class BeanMap {

	private Map<Class<? extends AplosAbstractBean>,Map<Long,AplosAbstractBean>> beanMap = new HashMap<Class<? extends AplosAbstractBean>,Map<Long,AplosAbstractBean>>();
	

	public AplosAbstractBean findBean( AplosAbstractBean bean ) {
		return findBean( bean.getClass(), bean.getId() );
	}
	
	public AplosAbstractBean findBean( Class<? extends AplosAbstractBean> beanClass, Long id ) {
		PersistentClass persistentClass = ApplicationUtil.getPersistentApplication().getPersistentClassMap().get( beanClass );
		Map<Long,AplosAbstractBean> innerBeanMap = beanMap.get( persistentClass.getDbPersistentClass().getTableClass() );
		if( innerBeanMap != null ) {
			return innerBeanMap.get( id );
		}
		return null;
	}
	
	public void removeBean( AplosAbstractBean aplosAbstractBean ) {
		PersistentClass persistentClass = ApplicationUtil.getPersistentClass( aplosAbstractBean.getClass() );
		if( persistentClass != null ) {
			Map<Long,AplosAbstractBean> innerBeanMap = beanMap.get( persistentClass.getDbPersistentClass().getTableClass() );
			if( innerBeanMap != null ) {
				innerBeanMap.remove( aplosAbstractBean.getId() );
			}
		}
	}
	
	public void registerBean( AplosAbstractBean aplosAbstractBean ) {
		PersistentClass persistentClass = ApplicationUtil.getPersistentApplication().getPersistentClassMap().get( aplosAbstractBean.getClass() );
		/*
		 * PersistentClass can sometimes be null with the ListBeans
		 */
		if( persistentClass != null ) {
			Map<Long,AplosAbstractBean> innerBeanMap = beanMap.get( persistentClass.getDbPersistentClass().getTableClass() );
			if( innerBeanMap == null ) {
				innerBeanMap = new HashMap<Long,AplosAbstractBean>(); 
				beanMap.put( (Class<? extends AplosAbstractBean>) persistentClass.getDbPersistentClass().getTableClass(), innerBeanMap );
			}
			if( innerBeanMap.get( aplosAbstractBean.getId() ) == null ) {
				innerBeanMap.put( aplosAbstractBean.getId(), aplosAbstractBean );
			}
		}
	}
}
