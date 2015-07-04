package com.aplos.common.persistence.fieldLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.aql.aqlvariables.AqlTableVariable;
import com.aplos.common.aql.aqlvariables.CollectionInformation;
import com.aplos.common.aql.aqlvariables.UnevaluatedTableVariable;
import com.aplos.common.aql.selectcriteria.SelectCriteria;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.comparators.PersistenceBeanPositionComparator;
import com.aplos.common.interfaces.PersistenceBean;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.collection.PersistentList;
import com.aplos.common.persistence.fieldinfo.CollectionFieldInfo;
import com.aplos.common.persistence.fieldinfo.FieldInfo;
import com.aplos.common.persistence.fieldinfo.ForeignFieldInfo;
import com.aplos.common.persistence.fieldinfo.PersistentCollectionFieldInfo;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.ParentAndField;
import com.aplos.common.utils.ReflectionUtil;

public class ListFieldLoader extends FieldLoader {
	
	public ListFieldLoader( String propertyName, PersistentClass persistentClass, FieldInfo fieldInfo ) {
		super( propertyName, persistentClass, fieldInfo );
	}

	@Override
	public Object getValue( PersistenceBean targetBean, Object returnedObject, boolean isTargetBeanLoaded ) {
		try { 
			ParentAndField parentAndField = ReflectionUtil.getField( targetBean, getPropertyName() );
			if( parentAndField != null ) {
				List myList = (List) getFieldInfo().getValue( (AplosAbstractBean) targetBean );
				if( myList instanceof PersistentList && !((PersistentList) myList).isInitialized() ) {
					PersistentClass persistentClass = ApplicationUtil.getPersistentApplication().getPersistentClassMap().get( targetBean.getClass() );
					if( persistentClass != null ) {
						if( getFieldInfo() instanceof ForeignFieldInfo ) {	
							BeanDao aqlBeanDao = new BeanDao( (Class<? extends AplosAbstractBean>) ((ForeignFieldInfo) getFieldInfo()).getPersistentClass().getTableClass() );
							aqlBeanDao.addWhereCriteria( "bean." + ((ForeignFieldInfo) getFieldInfo()).getForeignFieldInfo().getField().getName() + "." + ((ForeignFieldInfo) getFieldInfo()).getParentPersistentClass().determinePrimaryKeyFieldInfo().getSqlName() + " = " + targetBean.getId() );
							if( ((ForeignFieldInfo) getFieldInfo()).getIndexFieldInfo() != null ) {
								aqlBeanDao.setOrderBy( "bean." + ((ForeignFieldInfo) getFieldInfo()).getIndexFieldInfo().getSqlName() + " ASC" );
							}
							List sortedList = aqlBeanDao.getAll();
							if( ((ForeignFieldInfo) getFieldInfo()).getIndexFieldInfo() != null ) {
								Collections.sort( (List<PersistenceBean>) sortedList, new PersistenceBeanPositionComparator(((ForeignFieldInfo) getFieldInfo()).getIndexFieldInfo().getSqlName()) );	
							}
							((PersistentList) myList).setList(sortedList);
							((PersistentList) myList).setInitialized(true);
						} else if( getFieldInfo() instanceof CollectionFieldInfo ) {
							loadList( (PersistentList) myList, new UnevaluatedTableVariable( "bean." + getFieldInfo().getField().getName()), (AplosAbstractBean) targetBean );
						}
					}
				}
				
				return myList;
			}
		} catch( Throwable t ) {
			ApplicationUtil.handleError( t );
		}
		return null;
	}
	
	public void loadList( PersistentList persistentList, AqlTableVariable variableSelectCriteria, AplosAbstractBean parentBean ) {
		BeanDao aqlBeanDao = new BeanDao( (Class<? extends AplosAbstractBean>) getPersistentClass().getTableClass() );
		ProcessedBeanDao processedBeanDao = aqlBeanDao.createProcessedBeanDao();
		processedBeanDao.preprocessCriteria();
		variableSelectCriteria.evaluateCriteriaTypes(processedBeanDao, true);
		CollectionInformation collectionInformation = variableSelectCriteria.loadCollectionInformation(processedBeanDao);

		List resultsList = getCollectionResults(processedBeanDao,variableSelectCriteria, parentBean, collectionInformation);
		
		if( variableSelectCriteria.getFieldInfo() instanceof PersistentCollectionFieldInfo ) {
			persistentList.setList( resultsList );
			persistentList.setInitialized( true );
		} else {
			int collectionFieldIdx = -1;
			int count = 0;
			for( SelectCriteria selectCriteria : processedBeanDao.getProcessedSelectCriteriaList() ) {
				if( selectCriteria.getFieldInfo() == null || selectCriteria.getFieldInfo() instanceof CollectionFieldInfo ) {
					collectionFieldIdx = count;
					break;
				}
				count++;
			}
			List myList = new ArrayList();
			for( int i = 0, n = resultsList.size(); i < n; i++ ) {
				myList.add( ((Object[]) resultsList.get( i ))[ collectionFieldIdx ] );	
			}
			persistentList.setList( myList );
			persistentList.setInitialized( true );
		}
	}

}
