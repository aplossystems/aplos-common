package com.aplos.common.persistence.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.aql.BeanMap;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.interfaces.PersistenceBean;

public class PersistentMap extends PersistentAbstractCollection implements Map {
	
	private Map map = new HashMap();
	
	public PersistentMap() {
	}
	
	public PersistentMap( PersistentMap srcMap ) {
		copy( srcMap );
		setMap( srcMap.getMap() );
	}

	@Override
	public void replaceCollectionWithLazyBeans() {
		List<Object> keys = new ArrayList<Object>(getMap().keySet());
		AplosAbstractBean tempBean;
		for( int i = 0, n = keys.size(); i < n; i++ ) {
			if( getMap().get( keys.get( i ) ) instanceof AplosAbstractBean ) {
				tempBean = (AplosAbstractBean) getMap().get( keys.get( i ) );
				if( !tempBean.isLazilyLoaded() ) {
					getMap().put(keys.get( i ), tempBean.getLazyBean());
				}
			}
		}
	}
	
	@Override
	public void replaceCollection(PersistentAbstractCollection persistentCollection) {
		setMap( new HashMap( ((PersistentMap) persistentCollection).getMap() ) );
	}
	
	@Override
	public PersistentAbstractCollection getCopy() {
		return new PersistentMap( this );
	}
	
	@Override
	public void clear() {
		getMap().clear();
	}
	
	@Override
	public boolean containsKey(Object key) {
		return getMap().containsKey(key);
	}
	
	@Override
	public boolean containsValue(Object value) {
		return getMap().containsValue(value);
	}
	
	@Override
	public Set entrySet() {
		return getMap().entrySet();
	}
	
	@Override
	public boolean equals(Object obj) {
		return getMap().equals(obj);
	}
	
	@Override
	public void convertToSaveableBeans(BeanMap beanMap) {
		setBeanMap(beanMap);
		setSaveable(true);
		List<Object> keys = new ArrayList<Object>(getMap().keySet());
		AplosAbstractBean tempBean;
		for( int i = 0, n = keys.size(); i < n; i++ ) {
			tempBean = (AplosAbstractBean) getMap().get( keys.get( i ) );
			if( !tempBean.isLazilyLoaded() && tempBean.isReadOnly() ) {
				Object bean = tempBean.getSaveableBean(beanMap);
				getMap().put(keys.get( i ), bean);
			}
		}
	}
	
	@Override
	public Object get(Object key) {
		Object value = getMap().get(key);
		
		/* 
		 * Could be a collectionOfElements
		 */
		if( value instanceof PersistenceBean ) {
			if( isSaveable() && getFieldInfo().getCascadeAnnotation() != null ) {
				if( ((AplosAbstractBean) value).isReadOnly() ) {
					value = ((AplosAbstractBean) value).getSaveableBean();
					getMap().put(key, value);
				}
			} else if( value != null && ((PersistenceBean) value).isLazilyLoaded() ) {
				value = BeanDao.loadLazyValues( (AplosAbstractBean) value, true, true );
			}
		}
		return value;
	}
	
	@Override
	public int hashCode() {
		return getMap().hashCode();
	}
	
	@Override
	public boolean isEmpty() {
		return getMap().isEmpty();
	}
	
	@Override
	public Set keySet() {
		return getMap().keySet();
	}
	
	@Override
	public Object put(Object key, Object value) {
		return getMap().put(key,value);
	}
	
	@Override
	public void putAll(Map m) {
		getMap().putAll(m);
	}
	
	@Override
	public Object remove(Object key) {
		return getMap().remove(key);
	}

	public Map getMap() {
		return map;
	}

	public void setMap(Map map) {
		this.map = map;
	}
	
	public int size() {
		return getMap().size();
	}
	
	@Override
	public Collection values() {
		return new PersistentCollection( getMap().values(), this );
	}

}
