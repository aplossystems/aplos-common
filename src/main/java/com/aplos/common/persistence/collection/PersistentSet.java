package com.aplos.common.persistence.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.aplos.common.aql.BeanMap;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.communication.BasicEmailFolder;

public class PersistentSet extends PersistentAbstractCollection implements Set {
	private Set set = new HashSet();
	
	public PersistentSet() {
	}
	
	public PersistentSet( PersistentSet srcSet ) {
		copy( srcSet );
		setSet( new HashSet( srcSet ) );
	}
	
	@Override
	public void replaceCollectionWithLazyBeans() {
		List<Object> setItemList = new ArrayList<Object>(getSet());
		for( Object bean : setItemList ) {
			if( bean instanceof AplosAbstractBean ) {
				if( !((AplosAbstractBean) bean).isLazilyLoaded() ) {
					getSet().remove( bean );
					getSet().add( ((AplosAbstractBean) bean).getLazyBean() );
				}
			}
		}
	}
	
	@Override
	public void replaceCollection(PersistentAbstractCollection persistentCollection) {
		setSet( new HashSet( ((PersistentSet) persistentCollection).getSet() ) );
	}
	
	
	@Override
	public PersistentAbstractCollection getCopy() {
		return new PersistentSet( this );
	}

	public Set getSet() {
		return set;
	}

	public void setSet(Set set) {
		this.set = set;
	}
	
	@Override
	public void convertToSaveableBeans(BeanMap beanMap) {
		setBeanMap(beanMap);
		setSaveable(true);
		List<Object> setItemList = new ArrayList<Object>(getSet());
		AplosAbstractBean tempBean;
		for( Object bean : setItemList ) {
			tempBean = (AplosAbstractBean) bean;
			if( !tempBean.isLazilyLoaded() && tempBean.isReadOnly() ) {
				getSet().remove( bean );
				bean = tempBean.getSaveableBean(beanMap);
				getSet().add( bean );
			}
		}
	}
	
	@Override
	public boolean add(Object e) {
		return getSet().add(e);
	}
	
	@Override
	public boolean addAll(Collection c) {
		return getSet().addAll(c);
	}
	
	@Override
	public void clear() {
		getSet().clear();
	}
	
	@Override
	public boolean contains(Object o) {
		return getSet().contains(o);
	}
	
	@Override
	public boolean containsAll(Collection c) {
		return getSet().containsAll(c);
	}
	
	@Override
	public boolean equals(Object obj) {
		return getSet().equals(obj);
	}
	
	@Override
	public int hashCode() {
		return getSet().hashCode();
	}
	
	@Override
	public boolean isEmpty() {
		return getSet().isEmpty();
	}
	
	@Override
	public Iterator iterator() {
		return new IteratorProxy( getSet().iterator() );
	}
	
	@Override
	public boolean remove(Object o) {
		return getSet().remove(o);
	}  
	
	@Override
	public boolean removeAll(Collection c) {
		return getSet().removeAll(c);
	}
	
	@Override
	public boolean retainAll(Collection c) {
		return getSet().retainAll(c);
	}  
	
	@Override
	public int size() {
		loadIfRequired();
		return getSet().size();
	}
	
	@Override
	public Object[] toArray() {
		Object[] objectArray = new Object[ getSet().size() ];
		Iterator iter = new IteratorProxy( getSet().iterator() );
		int count = 0;
		while( iter.hasNext() ) {
			objectArray[ count++ ] = iter.next();
		}
		return objectArray;
	}
	
	@Override
	public Object[] toArray(Object[] a) {
		Object[] objectArray = a.length >= size() ? a :
        (Object[])java.lang.reflect.Array
        .newInstance(a.getClass().getComponentType(), size());
		Iterator iter = new IteratorProxy( getSet().iterator() );
		int count = 0;
		while( iter.hasNext() ) {
			objectArray[ count++ ] = iter.next();
		}
		return objectArray;
	}
}
