package com.aplos.common.persistence.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.aql.BeanMap;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.interfaces.PersistenceBean;

public class PersistentList extends PersistentAbstractCollection implements List {
	
	private List list = new ArrayList();
	
	public PersistentList() {
	}
	
	@Override
	public void replaceCollectionWithLazyBeans() {
		for( int i = 0, n = list.size(); i < n; i++ ) {
			if( list.get( i ) instanceof AplosAbstractBean ) {
				if( !((AplosAbstractBean) list.get( i )).isLazilyLoaded() ) { 
					list.set( i, ((AplosAbstractBean) list.get( i )).getLazyBean() );
				}
			}
		}
	}
	
	@Override
	public void replaceCollection(PersistentAbstractCollection persistentCollection) {
		setList( new ArrayList( ((PersistentList) persistentCollection).getList() ) );
	}
	
	public PersistentList( PersistentList srcList ) {
		copy( srcList );
		setList( srcList.getList() );
	}
	
	@Override
	public PersistentAbstractCollection getCopy() {
		return new PersistentList( this );
	}

	@Override
	public void add(int index, Object element) {
		getList().add(index,element);
	}
	
	@Override
	public boolean add(Object e) {
		return getList().add(e);
	}
	
	@Override
	public boolean addAll(Collection c) {
		return getList().addAll(c);
	}
	
	@Override
	public boolean addAll(int index, Collection c) {
		return getList().addAll(index, c);
	}
	
	@Override
	public void clear() {
		getList().clear();
	}
	
	@Override
	public boolean contains(Object o) {
		return getList().contains(o);
	}
	
	@Override
	public boolean containsAll(Collection c) {
		return getList().containsAll(c);
	}

	@Override
	public boolean equals(Object obj) {
		return getList().equals(obj);
	}
	
	@Override
	public void convertToSaveableBeans( BeanMap beanMap ) {
		setBeanMap(beanMap);
		setSaveable(true);
		AplosAbstractBean tempBean;
		for( int i = 0, n = getList().size(); i < n; i++ ) {
			tempBean = (AplosAbstractBean) getList().get( i );
			if( !tempBean.isLazilyLoaded() && tempBean.isReadOnly() ) {
				Object bean = tempBean.getSaveableBean(beanMap);
				getList().set(i, bean);
			}
		}
	}
	
	@Override
	public Object get(int index) {
		Object value = getList().get(index);
		
		if( value instanceof AplosAbstractBean ) {
			if( isSaveable() && getFieldInfo().getCascadeAnnotation() != null ) {
				if( ((AplosAbstractBean) value).isReadOnly() ) {
					value = ((AplosAbstractBean) value).getSaveableBean( getBeanMap() );
					getList().set(index, value);
				}
			} else if( ((PersistenceBean) value).isLazilyLoaded() ) {
				value = BeanDao.loadLazyValues( (AplosAbstractBean) value, true, true );
			}
		}
		return value;
	}
	
	@Override
	public int hashCode() {
		return getList().hashCode();
	}
	
	@Override
	public int indexOf(Object o) {
		return getList().indexOf(o);
	}

	@Override
	public boolean isEmpty() {
		return getList().isEmpty();
	}
	
	@Override
	public Iterator iterator() {
		return new IteratorProxy( list.iterator() );
	}
	
	@Override
	public int lastIndexOf(Object o) {
		return getList().lastIndexOf(o);
	}
	
	@Override
	public ListIterator listIterator() {
		return getList().listIterator();
	}
	
	@Override
	public ListIterator listIterator(int index) {
		return getList().listIterator(index);
	}
	
	@Override
	public Object remove(int index) {
		return getList().remove(index);
	}
	
	@Override
	public boolean remove(Object o) {
		return getList().remove(o);
	}
	
	@Override
	public boolean removeAll(Collection c) {
		return getList().removeAll(c);
	}
	
	@Override
	public boolean retainAll(Collection c) {
		return getList().retainAll(c);
	}
	
	@Override
	public Object set(int index, Object element) {
		return getList().set(index, element);
	}
	
	@Override
	public int size() {
		return getList().size();
	}
	
	@Override
	public List subList(int fromIndex, int toIndex) {
		return getList().subList(fromIndex, toIndex);
	}
	
	@Override
	public Object[] toArray() {
		return getList().toArray();
	}
	
	@Override
	public Object[] toArray(Object[] a) {
		return getList().toArray(a);
	}

	public List getList() {
		return list;
	}

	public void setList(List list) {
		this.list = list;
	}
}
