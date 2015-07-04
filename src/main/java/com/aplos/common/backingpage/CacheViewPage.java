package com.aplos.common.backingpage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.fieldinfo.CollectionFieldInfo;
import com.aplos.common.persistence.fieldinfo.FieldInfo;
import com.aplos.common.persistence.fieldinfo.PersistentClassFieldInfo;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;

@ManagedBean
@ViewScoped
public class CacheViewPage extends BackingPage {
	private static final long serialVersionUID = 8431791396100166416L;
	private String beanClassName;
	private Class<? extends AplosAbstractBean> beanClass;
	private String beanIdStr;
	private Map<String,Set<String>> beanNameMap = new HashMap<String,Set<String>>();
	private AplosAbstractBean currentBean;
	private PersistentClass currentPersistentClass;
	private AplosAbstractBean currentChildBean;
	private Collection currentChildCollection;
	private List<KeyValue> currentBeanKeyValueList;
	private List<KeyValue> childBeanKeyValueList;
	
	public CacheViewPage() {
		Set<String> tempHashSet;
		Map<Long,AplosAbstractBean> innnerBeanMap;
		for( Class<? extends AplosAbstractBean> tempBeanClass : ApplicationUtil.getPersistenceContext().getBeanMap().keySet() ) {
			innnerBeanMap = ApplicationUtil.getPersistenceContext().getBeanMap().get( tempBeanClass );
			tempHashSet = new HashSet<String>();
			beanNameMap.put( tempBeanClass.getName(), tempHashSet );
			for( Long beanId : innnerBeanMap.keySet() ) {
				tempHashSet.add( String.valueOf( beanId ) );
			}
		}
	}
	
	public String getCollectionInfo( Object value ) {
		Collection collection =  (Collection) value;
		StringBuffer collectionInfoStrBuf = new StringBuffer();
		if( collection != null ) {
			collectionInfoStrBuf.append( "(" );
			collectionInfoStrBuf.append( collection.getClass().getSimpleName() );
			collectionInfoStrBuf.append( ")" );
		}
		return collectionInfoStrBuf.toString();
	}
	
	public List<String> suggestBeanClassNames( String searchStr ) {
		List<String> matchingBeanClassNames = new ArrayList<String>();
		for( String beanClassName : beanNameMap.keySet() ) {
			if( beanClassName.contains( searchStr ) ) {
				matchingBeanClassNames.add( beanClassName );
			}
		}
		return matchingBeanClassNames;
	}
	
	public void beanClassNameUpdated() {
		if( !CommonUtil.isNullOrEmpty( getBeanClassName() ) ) {
			try {
				setBeanClass( (Class<? extends AplosAbstractBean>) Class.forName( getBeanClassName() ) );
			} catch( ClassNotFoundException cnfex ) {
				ApplicationUtil.handleError( cnfex );
			}
		} else {
			setBeanClass( null );
		}
	}

	public List<String> suggestBeanIds( String searchStr ) {
		List<String> matchingBeanIds = new ArrayList<String>();
		Iterator<String> iter = beanNameMap.get( getBeanClassName() ).iterator();
		String tempBeanId;
		while( iter.hasNext() ) {
			tempBeanId = iter.next();
			if( tempBeanId.contains( searchStr ) ) {
				matchingBeanIds.add( tempBeanId );
			}
		}
		return matchingBeanIds;
	}
	
	public void beanIdUpdated() {
		if( !CommonUtil.isNullOrEmpty( getBeanIdStr() ) ) {
			Long beanId = Long.parseLong( getBeanIdStr() );
			setCurrentBean( ApplicationUtil.getPersistenceContext().getBeanMap().get( getBeanClass() ).get( beanId ) );
			setCurrentPersistentClass( getCurrentBean().getPersistentClass() );
		} else {
			setCurrentBean( null );
			setCurrentPersistentClass( null );
		}
	}
	
	public boolean isCurrentChildCollectionIsMap() {
		return getCurrentChildCollection() instanceof Map;
	}
	
	public Set getCurrentChildCollectionKeySet() {
		if( getCurrentChildCollection() instanceof Map ) {
			return ((Map) getCurrentChildCollection()).keySet();
		}
		return null;
	}
	
	public List getCurrentChildCollectionValues() {
		if( getCurrentChildCollection() instanceof Set ) {
			return new ArrayList( (Set) getCurrentChildCollection() );
		} else if( getCurrentChildCollection() instanceof List ) {
			return (List) getCurrentChildCollection();
		}
		return null;
	}
	
	public boolean isAplosAbstractBean( Object value ) {
		return value instanceof AplosAbstractBean; 
	}
	
	public boolean isCollection( Object value ) {
		return value instanceof Collection; 
	}
	
	public void showAplosAbstractBean( Object value ) {
		setCurrentChildBean( (AplosAbstractBean) value );
	}
	
	public void showCollection( Object value ) {
		setCurrentChildCollection( (Collection) value );
	}
	
	public void showPersistentClass( FieldInfo fieldInfo ) {
		setCurrentChildBean( (AplosAbstractBean) fieldInfo.getValue( getCurrentBean() ) );
	}
	
	public void showCollectionFromFieldInfo( FieldInfo fieldInfo ) {
		setCurrentChildCollection( (Collection) fieldInfo.getValue( getCurrentBean() ) );
	}
	
	public boolean isPersistentClassFieldInfo( FieldInfo fieldInfo ) {
		return fieldInfo instanceof PersistentClassFieldInfo;
	}
	
	public boolean isCollectionFieldInfo( FieldInfo fieldInfo ) {
		return fieldInfo instanceof CollectionFieldInfo;
	}
	
	public String getBeanClassName() {
		return beanClassName;
	}
	public void setBeanClassName(String beanClassName) {
		this.beanClassName = beanClassName;
	}

	public String getBeanIdStr() {
		return beanIdStr;
	}

	public void setBeanIdStr(String beanIdStr) {
		this.beanIdStr = beanIdStr;
	}

	public AplosAbstractBean getCurrentBean() {
		return currentBean;
	}

	public void setCurrentBean(AplosAbstractBean currentBean) {
		setCurrentBeanKeyValueList(createKeyValueList(currentBean));
		this.currentBean = currentBean;
	}

	public PersistentClass getCurrentPersistentClass() {
		return currentPersistentClass;
	}

	public void setCurrentPersistentClass(PersistentClass currentPersistentClass) {
		this.currentPersistentClass = currentPersistentClass;
	}

	public AplosAbstractBean getCurrentChildBean() {
		return currentChildBean;
	}

	public void setCurrentChildBean(AplosAbstractBean currentChildBean) {
		setChildBeanKeyValueList(createKeyValueList(currentChildBean));
		this.currentChildBean = currentChildBean;
	}
	
	public List<KeyValue> createKeyValueList( AplosAbstractBean bean ) {
		List<KeyValue> keyValueList = new ArrayList<KeyValue>();
		List<FieldInfo> fieldInfos = bean.getPersistentClass().getFieldInfos();
		keyValueList.add( new KeyValue( "Identity Hashcode", System.identityHashCode(bean), null ) );
		for( FieldInfo fieldInfo: fieldInfos ) {
			keyValueList.add( new KeyValue( fieldInfo.getSqlName(), fieldInfo.getValue( bean ), fieldInfo ) );
		}
		PersistentClass parentPersistentClass = bean.getPersistentClass().getParentPersistentClass();
		while( parentPersistentClass != null ) {
			fieldInfos = parentPersistentClass.getFieldInfos();
			for( FieldInfo fieldInfo: fieldInfos ) {
				keyValueList.add( new KeyValue( fieldInfo.getSqlName(), fieldInfo.getValue( bean ), fieldInfo ) );
			}
			parentPersistentClass = parentPersistentClass.getParentPersistentClass();
		}
		keyValueList.add( new KeyValue( "Read only", bean.isReadOnly(), null ) );
		keyValueList.add( new KeyValue( "Lazily loaded", bean.isLazilyLoaded(), null ) );
		keyValueList.add( new KeyValue( "In database", bean.isInDatabase(), null ) );
		keyValueList.add( new KeyValue( "Is already saving", bean.isAlreadySaving(), null ) );
		return keyValueList;
	}

	public Collection getCurrentChildCollection() {
		return currentChildCollection;
	}

	public void setCurrentChildCollection(Collection currentChildCollection) {
		this.currentChildCollection = currentChildCollection;
	}

	public Class<? extends AplosAbstractBean> getBeanClass() {
		return beanClass;
	}

	public void setBeanClass(Class<? extends AplosAbstractBean> beanClass) {
		this.beanClass = beanClass;
	}
	
	public List<KeyValue> getCurrentBeanKeyValueList() {
		return currentBeanKeyValueList;
	}

	public void setCurrentBeanKeyValueList(List<KeyValue> currentBeanKeyValueList) {
		this.currentBeanKeyValueList = currentBeanKeyValueList;
	}

	public List<KeyValue> getChildBeanKeyValueList() {
		return childBeanKeyValueList;
	}

	public void setChildBeanKeyValueList(List<KeyValue> childBeanKeyValueList) {
		this.childBeanKeyValueList = childBeanKeyValueList;
	}

	public class KeyValue {
		private Object key;
		private Object value;
		private FieldInfo fieldInfo;
		private boolean isCollection;
		private boolean isPersistentClass;
		
		public KeyValue( Object key, Object value, FieldInfo fieldInfo ) {
			setKey( key );
			setValue( value );
			setFieldInfo( fieldInfo );
			
			if( value instanceof Collection ) {
				setCollection( true );
			}
			if( value instanceof AplosAbstractBean ) {
				setPersistentClass( true );
			}
		}

		public boolean isCollection() {
			return isCollection;
		}

		public void setCollection(boolean isCollection) {
			this.isCollection = isCollection;
		}

		public boolean isPersistentClass() {
			return isPersistentClass;
		}

		public void setPersistentClass(boolean isPersistentClass) {
			this.isPersistentClass = isPersistentClass;
		}

		public Object getKey() {
			return key;
		}

		public void setKey(Object key) {
			this.key = key;
		}

		public Object getValue() {
			return value;
		}

		public void setValue(Object value) {
			this.value = value;
		}

		public FieldInfo getFieldInfo() {
			return fieldInfo;
		}

		public void setFieldInfo(FieldInfo fieldInfo) {
			this.fieldInfo = fieldInfo;
		}
	}
}
