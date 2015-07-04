package com.aplos.common.persistence;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.persistence.fieldinfo.FieldInfo;
import com.aplos.common.persistence.fieldinfo.PersistentCollectionFieldInfo;
import com.aplos.common.utils.ApplicationUtil;

public class CascadeMemory {
	private AplosAbstractBean removedBean;
	private FieldInfo fieldInfo;
	private Object collectionKey;
	private PreparedStatement preparedStatement;
	
	public CascadeMemory( FieldInfo fieldInfo, AplosAbstractBean removedBean ) {
		setFieldInfo(fieldInfo);
		setRemovedBean(removedBean);
	}
	
	public String reinstateBean( AplosAbstractBean beanToSave ) throws IllegalAccessException, SQLException {
		getFieldInfo().getField().setAccessible(true);
		getFieldInfo().getField().set( beanToSave, getRemovedBean() );
		StringBuffer strBuf = new StringBuffer( getFieldInfo().getSqlName() );
		return strBuf.append( " = " ).append( removedBean.getId() ).toString();
	}
	
	public AplosAbstractBean getRemovedBean() {
		return removedBean;
	}
	public void setRemovedBean(AplosAbstractBean removedBean) {
		this.removedBean = removedBean;
	}
	public FieldInfo getFieldInfo() {
		return fieldInfo;
	}
	public void setFieldInfo(FieldInfo fieldInfo) {
		this.fieldInfo = fieldInfo;
	}
	public Object getCollectionKey() {
		return collectionKey;
	}
	public void setCollectionKey(Object collectionKey) {
		this.collectionKey = collectionKey;
	}

	public PreparedStatement getPreparedStatement() {
		return preparedStatement;
	}

	public void setPreparedStatement(PreparedStatement preparedStatement) {
		this.preparedStatement = preparedStatement;
	}
}
