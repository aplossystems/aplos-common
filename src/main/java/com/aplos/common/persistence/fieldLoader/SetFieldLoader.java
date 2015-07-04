package com.aplos.common.persistence.fieldLoader;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.aql.aqlvariables.AqlTableVariable;
import com.aplos.common.aql.aqlvariables.CollectionInformation;
import com.aplos.common.aql.aqlvariables.UnevaluatedTableVariable;
import com.aplos.common.aql.selectcriteria.SelectCriteria;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.interfaces.PersistenceBean;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.collection.PersistentSet;
import com.aplos.common.persistence.fieldinfo.CollectionFieldInfo;
import com.aplos.common.persistence.fieldinfo.FieldInfo;
import com.aplos.common.persistence.fieldinfo.ForeignFieldInfo;
import com.aplos.common.persistence.fieldinfo.PersistentCollectionFieldInfo;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.ParentAndField;
import com.aplos.common.utils.ReflectionUtil;

public class SetFieldLoader extends FieldLoader {
	
	public SetFieldLoader( String propertyName, PersistentClass persistentClass, FieldInfo fieldInfo ) {
		super( propertyName, persistentClass, fieldInfo );
	}

	@Override
	public Object getValue(PersistenceBean targetBean, Object returnedObject, boolean isTargetBeanLoaded) {
		try {  
			ParentAndField parentAndField = ReflectionUtil.getField( targetBean, getPropertyName() );
			if( parentAndField != null ) {
				Set mySet = (Set) getFieldInfo().getValue( (AplosAbstractBean) targetBean );
				if( mySet instanceof PersistentSet && !((PersistentSet) mySet).isInitialized() ) {
					FieldInfo fieldInfo = getPersistentClass().getFieldInfoFromSqlName( getPropertyName(), true );

					if( fieldInfo instanceof ForeignFieldInfo ) {	
						BeanDao aqlBeanDao = new BeanDao( (Class<? extends AplosAbstractBean>) ((ForeignFieldInfo) fieldInfo).getPersistentClass().getTableClass() );
						aqlBeanDao.addWhereCriteria( "bean." + ((ForeignFieldInfo) fieldInfo).getForeignFieldInfo().getField().getName() + "." + ((ForeignFieldInfo) fieldInfo).getParentPersistentClass().determinePrimaryKeyFieldInfo().getSqlName() + " = " + targetBean.getId() );
						if( ((ForeignFieldInfo) fieldInfo).getIndexFieldInfo() != null ) {
							aqlBeanDao.setOrderBy( "bean." + ((ForeignFieldInfo) fieldInfo).getIndexFieldInfo().getSqlName() + " ASC" );
						}
						List resultsList = aqlBeanDao.getAll();
						mySet = new HashSet();
						for( int i = 0, n = resultsList.size(); i < n; i++ ) {
							mySet.add( resultsList.get( i ) );
						}
						((PersistentSet) mySet).setSet(mySet);
						((PersistentSet) mySet).setInitialized(true);
					} else if( fieldInfo instanceof CollectionFieldInfo ) {
						loadSet( (PersistentSet) mySet, new UnevaluatedTableVariable( "bean." + fieldInfo.getField().getName()), (AplosAbstractBean) targetBean);
					}
				}

				return mySet;
			}
		} catch( Throwable t ) {
			ApplicationUtil.handleError( t );
		}
		return null;
	}
	
	public void loadSet( PersistentSet persistentSet, AqlTableVariable variableSelectCriteria, AplosAbstractBean parentBean ) {
		BeanDao aqlBeanDao = new BeanDao( (Class<? extends AplosAbstractBean>) getPersistentClass().getTableClass() );
		ProcessedBeanDao processedBeanDao = aqlBeanDao.createProcessedBeanDao();
		processedBeanDao.preprocessCriteria();
		variableSelectCriteria.evaluateCriteriaTypes(processedBeanDao, true);
		CollectionInformation collectionInformation = variableSelectCriteria.loadCollectionInformation(processedBeanDao);

		Set mySet = new HashSet();
		List resultsList = getCollectionResults(processedBeanDao,variableSelectCriteria, parentBean, collectionInformation);
		
		if( variableSelectCriteria.getFieldInfo() instanceof PersistentCollectionFieldInfo ) {
			for( int i = 0, n = resultsList.size(); i < n; i++ ) {
				mySet.add( resultsList.get( i ) );	
			}
		} else {
			int collectionFieldIdx = -1;
			int count = 0;
			CollectionFieldInfo collectionFieldInfo = null;
			for( SelectCriteria selectCriteria : processedBeanDao.getProcessedSelectCriteriaList() ) {
				if( selectCriteria.getFieldInfo() instanceof CollectionFieldInfo ) {
					collectionFieldIdx = count;
					collectionFieldInfo = (CollectionFieldInfo) selectCriteria.getFieldInfo();
					break;
				}
				count++;
			}
			Object convertedValue;
			for( int i = 0, n = resultsList.size(); i < n; i++ ) {
				convertedValue = SelectCriteria.convertFieldValue( ((Object[])resultsList.get( i ))[ collectionFieldIdx ], collectionFieldInfo, null);
				mySet.add( convertedValue );	
			}
		}
		persistentSet.setSet( mySet );
		persistentSet.setInitialized( true );
	}
}
