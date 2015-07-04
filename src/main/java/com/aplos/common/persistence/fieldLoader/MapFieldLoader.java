package com.aplos.common.persistence.fieldLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.aql.aqltables.AqlTable;
import com.aplos.common.aql.aqlvariables.AqlTableVariable;
import com.aplos.common.aql.aqlvariables.CollectionInformation;
import com.aplos.common.aql.aqlvariables.UnevaluatedTableVariable;
import com.aplos.common.aql.selectcriteria.SelectCriteria;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.interfaces.PersistenceBean;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.collection.PersistentMap;
import com.aplos.common.persistence.fieldinfo.FieldInfo;
import com.aplos.common.persistence.fieldinfo.MapKeyFieldInfo;
import com.aplos.common.persistence.fieldinfo.PersistentCollectionFieldInfo;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.ParentAndField;
import com.aplos.common.utils.ReflectionUtil;

public class MapFieldLoader extends FieldLoader {
	
	public MapFieldLoader( String propertyName, PersistentClass persistentClass, FieldInfo fieldInfo ) {
		super( propertyName, persistentClass, fieldInfo );
	}

	@Override
	public Object getValue(PersistenceBean targetBean, Object returnedObject, boolean isTargetBeanLoaded) {
		try {  
			ParentAndField parentAndField = ReflectionUtil.getField( targetBean, getPropertyName() );
			if( parentAndField != null ) {
				Map myMap = (Map) getFieldInfo().getValue( (AplosAbstractBean) targetBean );
				PersistentClass persistentClass = ApplicationUtil.getPersistentApplication().getPersistentClassMap().get( targetBean.getClass() );

				if( persistentClass != null ) {
					if( myMap instanceof PersistentMap && !((PersistentMap) myMap).isInitialized() ) {
						loadMap( (PersistentMap) myMap, new UnevaluatedTableVariable("bean." + getFieldInfo().getField().getName()), (AplosAbstractBean) targetBean);
					} else { 
						return returnedObject;
					}
				}
				
				return myMap;
			}
		} catch( Throwable t ) {
			ApplicationUtil.handleError( t );
		}
		return null;
	}
	
	public void loadMap( PersistentMap persistentMap, AqlTableVariable variableSelectCriteria, AplosAbstractBean parentBean ) {
		BeanDao aqlBeanDao = new BeanDao( (Class<? extends AplosAbstractBean>) getPersistentClass().getTableClass() );
		ProcessedBeanDao processedBeanDao = aqlBeanDao.createProcessedBeanDao();
		processedBeanDao.preprocessCriteria();
		variableSelectCriteria.evaluateCriteriaTypes(processedBeanDao, true);
		CollectionInformation collectionInformation = variableSelectCriteria.loadCollectionInformation(processedBeanDao);
		AqlTable intermediateTable = collectionInformation.getIntermediateTable();
		
		StringBuffer sqlBuf = new StringBuffer( "SELECT " );
		List<String> selectCriteriaNames = new ArrayList<String>();
		Map<String, FieldInfo> intermediateFieldInfoMap = intermediateTable.getPersistentTable().getFullDbFieldInfoMap();
		int mapKeyIdx = -1;
		int collectionFieldIdx = -1;
		FieldInfo collectionFieldInfo = null;
		int count = 0;
		for( FieldInfo tempFieldInfo : intermediateFieldInfoMap.values() ) {
			selectCriteriaNames.add( tempFieldInfo.getSqlName() );
			if( tempFieldInfo instanceof MapKeyFieldInfo ) {
				mapKeyIdx = count;
			} else if( tempFieldInfo.equals( variableSelectCriteria.getFieldInfo() ) ) {
				collectionFieldIdx = count;
				collectionFieldInfo = tempFieldInfo;
			}
			count++;
		}
		sqlBuf.append( StringUtils.join( selectCriteriaNames, "," ) );
		sqlBuf.append( " FROM " );
		sqlBuf.append( intermediateTable.determineDbTableName() );
		sqlBuf.append( " WHERE " );
		sqlBuf.append( intermediateTable.getInternalVariable().getName() );
		sqlBuf.append( " = " ).append( String.valueOf( parentBean.getId() ) );
		List<Object[]> intermediateResults = ApplicationUtil.getResults( sqlBuf.toString() );
		
		
		Map collectionMap = new HashMap();
		if( variableSelectCriteria.getFieldInfo() instanceof PersistentCollectionFieldInfo ) {

			
			Map collectionKeyMap = new HashMap();
			for( Object[] resultsRow : intermediateResults ) {
				if( collectionInformation.getMapKeyTable() != null ) {
					collectionKeyMap.put( resultsRow[ mapKeyIdx ], resultsRow[ collectionFieldIdx ] );
				} else {
					collectionKeyMap.put( resultsRow[ collectionFieldIdx ], resultsRow[ mapKeyIdx ] );
				}
			}

			List resultList = getCollectionResults(processedBeanDao,variableSelectCriteria, parentBean, collectionInformation);
			if( collectionInformation.getMapKeyTable() != null ) {
				List mapKeyList = getMapKeyResults(variableSelectCriteria, parentBean.getId(), collectionInformation);
				Map<Long,AplosAbstractBean> resultListMap = new HashMap<Long,AplosAbstractBean>();
				for( int i = 0, n = resultList.size(); i < n; i++ ) {
					resultListMap.put( ((AplosAbstractBean) resultList.get( i )).getId(), (AplosAbstractBean) resultList.get( i ) ); 
				}
				Long tempId;
				for( int i = 0, n = mapKeyList.size(); i < n; i++ ) {
					tempId = (Long) collectionKeyMap.get( ((AplosAbstractBean) mapKeyList.get( i )).getId() );
					collectionMap.put( mapKeyList.get( i ), resultListMap.get( tempId ) );
				}
			} else {
				for( int i = 0, n = resultList.size(); i < n; i++ ) {
					collectionMap.put( collectionKeyMap.get( ((AplosAbstractBean) resultList.get( i )).getId() ), resultList.get( i ) );
				}
			}
		} else {
			if( collectionInformation.getMapKeyTable() != null ) {
				List mapKeyList = getMapKeyResults(variableSelectCriteria, parentBean.getId(), collectionInformation);
				for( int i = 0, n = mapKeyList.size(); i < n; i++ ) {
					collectionMap.put( mapKeyList.get( i ), intermediateResults.get( i )[ collectionFieldIdx ] );
				}
			} else {
				Object convertedValue;
				for( int i = 0, n = intermediateResults.size(); i < n; i++ ) {
					convertedValue = SelectCriteria.convertFieldValue(intermediateResults.get( i )[ collectionFieldIdx ], collectionFieldInfo, null);
					collectionMap.put( intermediateResults.get( i )[ mapKeyIdx ], convertedValue );
				}
			}
		}
			
		persistentMap.setMap( collectionMap );
		persistentMap.setInitialized(true);
	}
	
	public List getMapKeyResults( AqlTableVariable variableSelectCriteria, Long beanId, CollectionInformation collectionInformation ) {
		AqlTable intermediateTable = collectionInformation.getIntermediateTable();
		PersistentClass persistentClass = (PersistentClass) collectionInformation.getMapKeyTable().getPersistentTable();
		BeanDao collectionDao = new BeanDao( (Class<? extends AplosAbstractBean>) persistentClass.getTableClass() );
		String leftCondition = "col." + collectionInformation.getMapKeyTable().getExternalVariable().getName();
		String rightCondition = "bean." + collectionInformation.getMapKeyTable().getInternalVariable().getName();
		collectionDao.addQueryTable( "col", intermediateTable.getPersistentTable(), leftCondition, rightCondition );
		collectionDao.addWhereCriteria( "col." + intermediateTable.getInternalVariable().getName() + " = " + String.valueOf( beanId ) );
		return collectionDao.getAll();
	}
}
