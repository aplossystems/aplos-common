package com.aplos.common.aql.aqlvariables;

import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.aql.aqltables.AqlTable;
import com.aplos.common.aql.aqltables.AqlTableAbstract;
import com.aplos.common.persistence.JunctionTable;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.fieldinfo.FieldInfo;
import com.aplos.common.persistence.fieldinfo.PersistentClassFieldInfo;
import com.aplos.common.persistence.fieldinfo.PersistentCollectionFieldInfo;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;

public class UnevaluatedTableVariable extends AqlTableVariable {
	private String originalPath;
	private String tablePath;
	
	public UnevaluatedTableVariable( String originalPath ) {
		setOriginalPath(originalPath);
	}
	
	@Override
	public String getOriginalText() {
		return getOriginalPath();
	}
	
	public void init( String criteria ) {
		setOriginalPath( criteria );
		if( criteria.contains( "." ) ) {
			setTablePath(criteria.substring( 0, criteria.lastIndexOf( "." ) ));
			setName(criteria.substring( criteria.lastIndexOf( "." ) + 1 ));
		} else {
			setTablePath("");
			setName(criteria);
		}
	}
	
	@Override
	public boolean evaluateCriteriaTypes( ProcessedBeanDao processedBeanDao, boolean isAllowingFullTables ) {
		setAqlTable(null);
		init(getOriginalPath());
		setFullTable( false );
		
		if( !CommonUtil.isNullOrEmpty( getTablePath() ) ) {
			setAqlTableAbstract( processedBeanDao.findOrCreateAqlTable( getTablePath(), true ) );
		} else {
			if( processedBeanDao.getQueryTables().get( getName() ) != null ) {
				setAqlTable( (AqlTable) processedBeanDao.getQueryTables().get( getName() ) );
				if( !isAllowingFullTables ) {
					if( getAqlTable().getPersistentTable() instanceof JunctionTable ) {
						setName( getAqlTable().getAssociatedFieldInfo().getSqlName() );
					} else {
						setName( getAqlTable().getPersistentTable().determinePrimaryKeyFieldInfo().getSqlName() );
					}
				} else {
					setFullTable( true );
				}
			} else {
				AqlTableAbstract aqlTable = processedBeanDao.findAqlTableForVariable( getName() ); 
				if( aqlTable == null ) {
					ApplicationUtil.handleError( new Exception( "Table couldn't be found for variable " + getName() ), false );
				}
				setAqlTableAbstract( aqlTable );
			}
		}
		
		if( getName().equalsIgnoreCase( "class" ) ) { 
			if( getAqlTable().getPersistentTable() instanceof PersistentClass ) {
				if( ((PersistentClass) getAqlTable().getPersistentTable()).isDbTable() ) {
					setName( ((PersistentClass) getAqlTable().getPersistentTable()).getDbPersistentClass().getDiscriminatorFieldInfo().getSqlName() );
				} else {
					if( getAqlTable().getAssociatedFieldInfo() instanceof PersistentClassFieldInfo ) {
						setName( ((PersistentClassFieldInfo) getAqlTable().getAssociatedFieldInfo()).getPolymorphicFieldInfo().getSqlName() );
					} else {
						setName( ((PersistentCollectionFieldInfo) getAqlTable().getAssociatedFieldInfo()).getPolymorphicFieldInfo().getSqlName() );
					}
					setAqlTable( (AqlTable) getAqlTable().getParentTable() );
				}
			} else {
				setName( ((PersistentCollectionFieldInfo) getAqlTable().getAssociatedFieldInfo()).getPolymorphicFieldInfo().getSqlName() );
			}
		}
		if( getAqlTableAbstract() == null ) {
			return false;
		}
		
		if( getAqlTableAbstract() instanceof AqlTable && getAqlTable().getParentFieldInfo() != null && 
				getName().equals( "id" ) ) {
			createForeignKeySelectCriteria( processedBeanDao );
		}
		
		if( !isFullTable() ) {
			FieldInfo fieldInfo = findFieldInfo();
	
			if( fieldInfo instanceof PersistentClassFieldInfo ) {
				if( !getName().equals( fieldInfo.getSqlName() ) ) {
					if( isAllowingFullTables ) {
						setAqlTable( (AqlTable) processedBeanDao.findOrCreateAqlTable( getOriginalPath(), true ) );
						setFullTableFieldInfo( fieldInfo );
						fieldInfo = getAqlTable().getPersistentTable().determinePrimaryKeyFieldInfo();
						setFullTable( true );
					} 
					
					setName( fieldInfo.getSqlName() );
				}
			}
			updateFieldInfo( fieldInfo );
			if( getAqlTableAbstract() instanceof AqlTable ) {
				getAqlTable().registerSelectCriteria( this );
			}
		}
		return true;
	}

	public String getTablePath() {
		return tablePath;
	}

	private void setTablePath(String tablePath) {
		this.tablePath = tablePath;
	}


	public String getOriginalPath() {
		return originalPath;
	}

	public void setOriginalPath(String originalPath) {
		this.originalPath = originalPath;
	}

}
