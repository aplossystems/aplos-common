package com.aplos.common.persistence.fieldLoader;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.interfaces.PersistenceBean;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.PersistentClass.InheritanceType;
import com.aplos.common.persistence.fieldinfo.EmbeddedFieldInfo;
import com.aplos.common.persistence.fieldinfo.FieldInfo;
import com.aplos.common.persistence.fieldinfo.ForeignFieldInfo;
import com.aplos.common.persistence.fieldinfo.PersistentClassFieldInfo;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.ParentAndField;
import com.aplos.common.utils.ReflectionUtil;

public class PersistentClassFieldLoader extends FieldLoader {
	private ParentAndField parentAndField;
	private boolean isInterface = false;
	
	public PersistentClassFieldLoader( PersistenceBean targetBean, String propertyName, PersistentClass persistentClass, FieldInfo fieldInfo ) {
		super( propertyName, persistentClass, fieldInfo );
		try {
			setParentAndField( ReflectionUtil.getField( targetBean, getPropertyName() ) );
			setInterface( getFieldInfo().getField().getGenericType().getClass().isInterface() );
		} catch( Throwable t ) {
			ApplicationUtil.handleError( t );
		}
	}

	@Override
	public Object getValue( PersistenceBean targetBean, Object returnedObject, boolean isTargetBeanLoaded) {
		try {
			if( returnedObject instanceof AplosAbstractBean && !((PersistenceBean) returnedObject).isInDatabase() ) {
				return returnedObject;
			}
			
			if( getParentAndField() != null ) {
				if( isInterface() ) {
					if( ReflectionUtil.getField(targetBean, getPropertyName()) == null ) {
						return returnedObject;
					}
				}
				
				if( getFieldInfo() instanceof EmbeddedFieldInfo ) {
					return getFieldInfo().getValue( (AplosAbstractBean) targetBean );
				}
				
				AplosAbstractBean resultBean = (AplosAbstractBean) getFieldInfo().getValue( (AplosAbstractBean) targetBean );
				if( resultBean != null ) {
					if( resultBean.isReadOnly() && ((PersistenceBean) resultBean).isInDatabase() ) {		
						if( resultBean.getId() == null ) {
							PersistentClass persistentClass = ApplicationUtil.getPersistentApplication().getPersistentClassMap().get( targetBean.getClass() );
							ForeignFieldInfo foreignFieldInfo = (ForeignFieldInfo) persistentClass.getFieldInfoFromSqlName( getPropertyName(), true );
							BeanDao resultBeanDao = new BeanDao( (Class<? extends AplosAbstractBean>) foreignFieldInfo.getPersistentClass().getTableClass() );
							resultBeanDao.setWhereCriteria( "bean." + foreignFieldInfo.getForeignFieldInfo().getField().getName() + ".id = " + targetBean.getId() );
							AplosAbstractBean newResultBean = resultBeanDao.getFirstBeanResult();
							if( newResultBean != null ) {
								resultBean.setReadOnly(false);
								resultBean.setId( newResultBean.getId() );
								resultBean.setReadOnly(true);
							}
							resultBean = newResultBean;
						} else {
							PersistentClass persistentClass = ApplicationUtil.getPersistentClass( resultBean.getClass() );
							if( InheritanceType.SINGLE_TABLE.equals( persistentClass.getInheritanceType() )
									|| InheritanceType.JOINED_TABLE.equals( persistentClass.getInheritanceType() ) ) {
								resultBean = BeanDao.loadLazyValues( (AplosAbstractBean) resultBean, false, true );
							} else {
								resultBean = BeanDao.loadLazyValues( (AplosAbstractBean) resultBean, true, true );
							}
						}
						if( !((AplosAbstractBean) targetBean).isReadOnly() && getFieldInfo().getCascadeAnnotation() != null ) {
							if( resultBean != null ) {
								resultBean = resultBean.getSaveableBean();
							}
							parentAndField.getField().setAccessible(true);
							parentAndField.getField().set(targetBean, resultBean);
						}
					}
				} else if( getFieldInfo() instanceof PersistentClassFieldInfo 
						&& ((PersistentClassFieldInfo) getFieldInfo()).isRemoveEmpty() ) {
					resultBean = ((PersistentClassFieldInfo) getFieldInfo()).createAndSetEmptyBean( (AplosAbstractBean) targetBean );
				} else if( targetBean.getHiddenFieldsMap().get( getPropertyName() ) != null ) {
					long beanId = (Long) targetBean.getHiddenFieldsMap().get( getPropertyName() );
					resultBean = new BeanDao( (Class<? extends AplosAbstractBean>) ((PersistentClassFieldInfo) getFieldInfo()).getPersistentClass().getTableClass() ).get( beanId );

					if( !((AplosAbstractBean) targetBean).isReadOnly() && getFieldInfo().getCascadeAnnotation() != null ) {
						if( resultBean != null ) {
							resultBean = resultBean.getSaveableBean();
						}
						parentAndField.getField().setAccessible(true);
						parentAndField.getField().set(targetBean, resultBean);
					}
				}
				
//				if( returnedObject != resultBean ) {
//					parentAndField.getField().setAccessible(true);
//					parentAndField.getField().set(targetBean, resultBean);
//				}
				
				return resultBean;
			}
		} catch( Throwable t ) {
			ApplicationUtil.handleError( t );
		}
		return null;
	}

	private ParentAndField getParentAndField() {
		return parentAndField;
	}

	private void setParentAndField(ParentAndField parentAndField) {
		this.parentAndField = parentAndField;
	}

	private boolean isInterface() {
		return isInterface;
	}

	private void setInterface(boolean isInterface) {
		this.isInterface = isInterface;
	}

}
