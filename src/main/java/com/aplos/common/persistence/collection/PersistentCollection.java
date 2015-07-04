package com.aplos.common.persistence.collection;

import java.util.Collection;
import java.util.Iterator;

import com.aplos.common.aql.BeanMap;
import com.aplos.common.persistence.fieldinfo.FieldInfo;

public class PersistentCollection extends PersistentAbstractCollection implements Collection {
	
	private Collection collection;
	private PersistentAbstractCollection parentCollection;
	
	public PersistentCollection( Collection collection, PersistentAbstractCollection parentCollection ) {
		this.setCollection(collection);
		this.setParentCollection(parentCollection);
	}
	
	@Override
	public boolean isSaveable() {
		return getParentCollection().isSaveable();
	}
	
	@Override
	public boolean isInitialized() {
		return getParentCollection().isInitialized();
	}
	
	@Override
	public boolean isDirty() {
		return getParentCollection().isDirty();
	}
	
	@Override
	public FieldInfo getFieldInfo() {
		return getParentCollection().getFieldInfo();
	}
	
	@Override
	public int getCachedSize() {
		return getParentCollection().getCachedSize();
	}
	
	@Override
	protected BeanMap getBeanMap() {
		return getParentCollection().getBeanMap();
	}
	
	@Override
	public void replaceCollection(PersistentAbstractCollection persistentCollection) {
	}
	
	@Override
	public void convertToSaveableBeans(BeanMap beanMap) {
	}
	
	@Override
	public PersistentAbstractCollection getCopy() {
		return null;
	}
	
	@Override
	public void replaceCollectionWithLazyBeans() {	
	}

	@Override
	public Iterator iterator() {
		return new IteratorProxy( getCollection().iterator() );
	}
	
	@Override
	public boolean removeAll(Collection c) {
		return getCollection().removeAll(c);
	}
	
	@Override
	public boolean remove(Object o) {
		return getCollection().remove(o);
	}
	
	@Override
	public boolean add(Object e) {
		return getCollection().add(e);
	}
	
	@Override
	public boolean addAll(Collection c) {
		return getCollection().addAll(c);
	}
	
	@Override
	public boolean contains(Object o) {
		return getCollection().contains(o);
	}
	
	@Override
	public boolean containsAll(Collection c) {
		return getCollection().containsAll(c);
	}
	
	@Override
	public boolean isEmpty() {
		return getCollection().isEmpty();
	}
	
	@Override
	public int size() {
		return getCollection().size();
	}
	
	@Override
	public Object[] toArray() {
		return getCollection().toArray();
	}
	
	@Override
	public Object[] toArray(Object[] a) {
		return getCollection().toArray(a);
	}
	
	@Override
	public void clear() {
		getCollection().clear();
	}
	
	@Override
	public boolean retainAll(Collection c) {
		return getCollection().retainAll(c);
	}

	private Collection getCollection() {
		return collection;
	}

	private void setCollection(Collection collection) {
		this.collection = collection;
	}

	private PersistentAbstractCollection getParentCollection() {
		return parentCollection;
	}

	private void setParentCollection(PersistentAbstractCollection parentCollection) {
		this.parentCollection = parentCollection;
	}
}
