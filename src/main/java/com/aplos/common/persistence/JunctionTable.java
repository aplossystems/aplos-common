package com.aplos.common.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aplos.common.annotations.persistence.ManyToAny;
import com.aplos.common.module.ModuleUpgrader;
import com.aplos.common.persistence.PersistentClass.InheritanceType;
import com.aplos.common.persistence.fieldinfo.CollectionFieldInfo;
import com.aplos.common.persistence.fieldinfo.FieldInfo;
import com.aplos.common.persistence.fieldinfo.PersistentClassFieldInfo;
import com.aplos.common.persistence.fieldinfo.PersistentCollectionFieldInfo;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;


public class JunctionTable extends PersistableTable {
	/*
	 * A collection table can have multiple fieldInfos, this is because the parent table 
	 * can be linked to another table through multiple fields.  For example this happens with
	 * the TemplateContent which is connected to cmsAtom through the cmsAtomList and cmsAtomPassedThroughMap.
	 */
	private List<FieldInfo> fieldInfos = new ArrayList<FieldInfo>();
	private String sqlTableName;
	private PersistentClassFieldInfo persistentClassFieldInfo;
	private CollectionFieldInfo collectionFieldInfo;

	public JunctionTable( CollectionFieldInfo collectionFieldInfo, PersistentClass persistentClass, String sqlTableName ) {
		this.setSqlTableName(sqlTableName);
		setPersistentClassFieldInfo(new PersistentClassFieldInfo(persistentClass, collectionFieldInfo.getParentPersistentClass(), null ));
		getPersistentClassFieldInfo().setSqlName( persistentClass.determineSqlTableName() + "_id" );
		getPersistentClassFieldInfo().init();
		getPersistentClassFieldInfo().getApplicationType().setNullable( false );
		updateCollectionFieldInfo( collectionFieldInfo );

		if( CommonUtil.isNullOrEmpty( sqlTableName ) ) {
			setSqlTableName(generateSqlTableName( collectionFieldInfo, getPersistentClass() ) );
		}
	}
	
	public void addFieldInfoToMap(java.util.Map<String,FieldInfo> fullDbFieldInfos, String mapKey, FieldInfo fieldInfo) {
		fullDbFieldInfos.put( mapKey, fieldInfo );
	}
	
	public void clearDbData( Connection conn ) throws SQLException {
		ModuleUpgrader.dropTable( determineSqlTableName(), true, conn );
		createTable(false, conn);
	}
	
	public static String generateSqlTableName( CollectionFieldInfo fieldInfo, PersistentClass persistentClass ) {
		StringBuffer strBuf = new StringBuffer( persistentClass.getTableClass().getSimpleName() );
		strBuf.append( "_" );
				
		/*
		 * This used to merge junction tables that referenced the same object type through different
		 * field infos but it got complicated with the different constraints on the columns
		 * (non unqiue, primary etc.), and I couldn't see the great benefit so commented it out.
		 */
//		if( fieldInfo instanceof PersistentCollectionFieldInfo ) {
//			PersistentClass exampleFieldPersistentClass = ApplicationUtil.getPersistentApplication().getPersistentClassMap().get( ((CollectionFieldInfo)fieldInfo).getFieldClass() );
//			if( !(((CollectionFieldInfo)fieldInfo).getRelationshipAnnotation() instanceof ManyToAny && !InheritanceType.SINGLE_TABLE.equals( exampleFieldPersistentClass.getInheritanceType() )) ) {
//				strBuf.append( ((PersistentCollectionFieldInfo)fieldInfo).getPersistentClass().determineSqlTableName() );
//			} else {
//				strBuf.append( fieldInfo.getField().getName() );
//			}
//		} else {
			strBuf.append( fieldInfo.getField().getName() );
//		}
		
		return strBuf.toString().toLowerCase();
	}

	@Override
	public FieldInfo determinePrimaryKeyFieldInfo() {
		return getPrimaryKeyFieldInfo();
	}
	
	@Override
	public String determineSqlTableName() {
		return getSqlTableName();
	}
	
	public PersistentClass getPersistentClass() {
		return getPersistentClassFieldInfo().getPersistentClass();
	}
	
	@Override
	public void createDbFieldInfoLists() {
		Map<String, FieldInfo> fullDbFieldInfos = new HashMap<String, FieldInfo>();
		for( int i = 0, n = fieldInfos.size(); i < n; i++ ) {
			addFieldInfoToMap( fullDbFieldInfos, fieldInfos.get( i ).determineFieldMapKeyName(), fieldInfos.get( i ) );
		}
		
		addFieldInfoToMap( fullDbFieldInfos, getPersistentClassFieldInfo().determineFieldMapKeyName(), getPersistentClassFieldInfo() );
		setAliasedFieldInfoMap( new HashMap<String, FieldInfo>(fullDbFieldInfos) );

		FieldInfo tempFieldInfo;
		for( String mapKey : new ArrayList<String>(getAliasedFieldInfoMap().keySet()) ) {
			tempFieldInfo = fullDbFieldInfos.get( mapKey ); 
			if( tempFieldInfo instanceof CollectionFieldInfo ) {
				getAliasedFieldInfoMap().put( tempFieldInfo.getSqlName(), tempFieldInfo);
			}
		}
		
		setFullDbFieldInfoMap( fullDbFieldInfos );
	}
	
	@Override
	public String toString() {
		return determineSqlTableName();
	}
	
	@Override
	public void createForeignKeysAndIndexes() {
		for( int i = 0, n = getFullDbFieldInfos().size(); i < n; i++ ) {
			if( getFullDbFieldInfos().get( i ) instanceof CollectionFieldInfo ) {
				((CollectionFieldInfo) getFullDbFieldInfos().get( i )).markAsPrimaryOrUnique(this);
			}
		}
		for( int i = 0, n = getFullDbFieldInfos().size(); i < n; i++ ) {
			getFullDbFieldInfos().get( i ).addForeignKeysAndIndexes( getForeignKeys(), getColumnIndexes() );
		}	
	}
	
	public void updateCollectionFieldInfo( CollectionFieldInfo collectionFieldInfo ) {
//		if( getFieldInfos().size() == 1 ) {
//			getFieldInfos().get( 0 ).getApplicationType().setNullable(true);
//			collectionFieldInfo.getApplicationType().setNullable(true);
//		} else if( getFieldInfos().size() > 1 ) {
//			collectionFieldInfo.getApplicationType().setNullable(true);
//		}
		if( getCollectionFieldInfo() == null ) {
			setCollectionFieldInfo( collectionFieldInfo );
			getFieldInfos().add( collectionFieldInfo );
			collectionFieldInfo.addAdditionalFieldInfos(this);
			collectionFieldInfo.readCollectionTableNameFromJoinTable(this);
		} else {
			ApplicationUtil.handleError( new RuntimeException( "More than one collection field info added" ) );
		}
	}
	
	@Override
	public Map<String, FieldInfo> getClassDbFieldInfoMap() {
		return getFullDbFieldInfoMap();
	}
	
	@Override
	public Map<String, FieldInfo> getClassAliasedFieldInfoMap() {
		return getAliasedFieldInfoMap();
	}

	public List<FieldInfo> getFieldInfos() {
		return fieldInfos;
	}

	public void setFieldInfos(List<FieldInfo> fieldInfos) {
		this.fieldInfos = fieldInfos;
	}

	public PersistentClassFieldInfo getPersistentClassFieldInfo() {
		return persistentClassFieldInfo;
	}

	public void setPersistentClassFieldInfo(PersistentClassFieldInfo persistentClassFieldInfo) {
		this.persistentClassFieldInfo = persistentClassFieldInfo;
	}

	public String getSqlTableName() {
		return sqlTableName;
	}

	public void setSqlTableName(String sqlTableName) {
		this.sqlTableName = sqlTableName;
	}

	public CollectionFieldInfo getCollectionFieldInfo() {
		return collectionFieldInfo;
	}

	public void setCollectionFieldInfo(CollectionFieldInfo collectionFieldInfo) {
		this.collectionFieldInfo = collectionFieldInfo;
	}
}
