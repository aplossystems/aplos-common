package com.aplos.common.beans;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.PropertyUtils;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.utils.ReflectionUtil;

public class TransferCyclicUpdate {
	private AplosAbstractBean parentBean;
	private PropertyDescriptor propertyDescriptor;
	private Object key;
	private Class<? extends AplosAbstractBean> childBeanClass;
	private Long childBeanId;

	public TransferCyclicUpdate( AplosAbstractBean parentBean, PropertyDescriptor propertyDescriptor,
			Object key, Class<? extends AplosAbstractBean> childBeanClass, Long childBeanId ) {
		this.parentBean = parentBean;
		this.propertyDescriptor = propertyDescriptor;
		this.key = key;
		this.childBeanClass = childBeanClass;
		this.childBeanId = childBeanId;
	}

	public void updateProperty() {
		try {
			AplosAbstractBean childBean = (AplosAbstractBean) new BeanDao( childBeanClass ).get( childBeanId );
			String propertyName = getPropertyDescriptor().getName();
			if( AplosAbstractBean.class.isAssignableFrom( propertyDescriptor.getReadMethod().getReturnType() ) ) {
				ReflectionUtil.setField( parentBean, propertyName, childBean );
			} else if( Set.class.isAssignableFrom( getPropertyDescriptor().getReadMethod().getReturnType() ) ) {
				Set parentSet = (Set) PropertyUtils.getProperty(parentBean, propertyName);
				parentSet.add(childBean);
			} else if( List.class.isAssignableFrom( getPropertyDescriptor().getReadMethod().getReturnType() ) ) {
				List parentList = (List) PropertyUtils.getProperty(parentBean, propertyName);
				parentList.set( Integer.parseInt( String.valueOf( key ) ), childBean);
			} else if( Map.class.isAssignableFrom( getPropertyDescriptor().getReadMethod().getReturnType() ) ) {
				Map parentMap = (Map) PropertyUtils.getProperty(parentBean, propertyName);
				parentMap.put( key, childBean );
			}
		} catch( NoSuchMethodException nsmEx ) {
			nsmEx.printStackTrace();
		} catch( NoSuchFieldException nsfEx ) {
			nsfEx.printStackTrace();
		} catch( IllegalAccessException iaEx ) {
			iaEx.printStackTrace();
		} catch( InvocationTargetException itEx ) {
			itEx.printStackTrace();
		}
	}

	public void setParentBean(AplosAbstractBean parentBean) {
		this.parentBean = parentBean;
	}
	public AplosAbstractBean getParentBean() {
		return parentBean;
	}
	public void setKey(Object key) {
		this.key = key;
	}
	public Object getKey() {
		return key;
	}

	public void setPropertyDescriptor(PropertyDescriptor propertyDescriptor) {
		this.propertyDescriptor = propertyDescriptor;
	}

	public PropertyDescriptor getPropertyDescriptor() {
		return propertyDescriptor;
	}

	public void setChildBeanClass(Class<? extends AplosAbstractBean> childBeanClass) {
		this.childBeanClass = childBeanClass;
	}

	public Class<? extends AplosAbstractBean> getChildBeanClass() {
		return childBeanClass;
	}

	public void setChildBeanId(Long childBeanId) {
		this.childBeanId = childBeanId;
	}

	public Long getChildBeanId() {
		return childBeanId;
	}
}
