package com.aplos.common.persistence.collection;

import java.util.Iterator;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.aql.BeanMap;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.interfaces.PersistenceBean;
import com.aplos.common.persistence.fieldinfo.FieldInfo;

public abstract class PersistentAbstractCollection {
	private boolean isInitialized;
	private boolean isDirty;
	private FieldInfo fieldInfo;
	private int cachedSize;
	private boolean isSaveable = false;
	private BeanMap beanMap;
	
	public abstract void convertToSaveableBeans( BeanMap beanMap );
	
	public abstract PersistentAbstractCollection getCopy();
	
	public abstract void replaceCollectionWithLazyBeans();
	
	public abstract void replaceCollection( PersistentAbstractCollection persistentCollection );
	
	public void copy( PersistentAbstractCollection persistentCollection ) {
		setInitialized( persistentCollection.isInitialized() );
		setDirty( persistentCollection.isDirty() );
		setFieldInfo( persistentCollection.getFieldInfo() );
		setCachedSize( persistentCollection.getCachedSize() );
		setSaveable( persistentCollection.isSaveable() );
	}
	
	public void loadIfRequired() {
		
	}
	
	public boolean isDirty() {
		return isDirty;
	}
	public void setDirty(boolean isDirty) {
		this.isDirty = isDirty;
	}
	public int getCachedSize() {
		return cachedSize;
	}
	public void setCachedSize(int cachedSize) {
		this.cachedSize = cachedSize;
	}
	public boolean isInitialized() {
		return isInitialized;
	}
	public void setInitialized(boolean isInitialized) {
		this.isInitialized = isInitialized;
	}

	public FieldInfo getFieldInfo() {
		return fieldInfo;
	}

	public void setFieldInfo(FieldInfo fieldInfo) {
		this.fieldInfo = fieldInfo;
	}

	public boolean isSaveable() {
		return isSaveable;
	}

	public void setSaveable(boolean isSaveable) {
		this.isSaveable = isSaveable;
	}

	protected BeanMap getBeanMap() {
		return beanMap;
	}

	protected void setBeanMap(BeanMap beanMap) {
		this.beanMap = beanMap;
	}

	final class IteratorProxy implements Iterator {
		private final Iterator iter;
		IteratorProxy(Iterator iter) {
			this.iter=iter;
		}
		public boolean hasNext() {
			return iter.hasNext();
		}

		public Object next() {
			Object value = iter.next();

			if( value instanceof AplosAbstractBean ) {
				if( isSaveable() 
						&& getFieldInfo().getCascadeAnnotation() != null ) {
					value = ((AplosAbstractBean) value).getSaveableBean();
				} else if( ((PersistenceBean) value).isLazilyLoaded() ) {
					value = BeanDao.loadLazyValues( (AplosAbstractBean) value, true, true );
				}
			}
			return value;
		}

		public void remove() {
			iter.remove();
		}

	}
}
