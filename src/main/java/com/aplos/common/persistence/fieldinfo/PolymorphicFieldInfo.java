package com.aplos.common.persistence.fieldinfo;

import java.sql.Connection;
import java.sql.SQLException;

import com.aplos.common.annotations.persistence.DiscriminatorColumn;
import com.aplos.common.annotations.persistence.DiscriminatorValue;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.persistence.PersistableTable;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.PersistentClass.InheritanceType;
import com.aplos.common.persistence.type.applicationtype.MySqlType;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;

public class PolymorphicFieldInfo extends FieldInfo {
	private FieldInfo idFieldInfo;
	
	public PolymorphicFieldInfo( PersistentClass parentPersistentClass, FieldInfo idFieldInfo ) {
		super( parentPersistentClass, null );
		setApplicationType( MySqlType.VARCHAR.getNewDbType() );
		getApplicationType().setColumnSize(31);
		getApplicationType().setNullable(false);
		setIdFieldInfo( idFieldInfo );
	}
	
	public void init() {
		if( CommonUtil.isNullOrEmpty( getSqlName() ) ) {
			String sqlName = "DTYPE";
			DiscriminatorColumn discriminatorColumnAnnotation = getParentPersistentClass().getTableClass().getAnnotation( DiscriminatorColumn.class ); 
			if( discriminatorColumnAnnotation != null ) {
				if( !CommonUtil.isNullOrEmpty( discriminatorColumnAnnotation.name() ) ) { 
					sqlName = discriminatorColumnAnnotation.name();
				}
				getApplicationType().setColumnSize(discriminatorColumnAnnotation.length());
			}
			setSqlName( sqlName );
		}
	}
	
	@Override
	public void setValue(boolean isLoadingFromDatabase, AplosAbstractBean bean, Object value) throws Exception {
		// do nothing
	}
	
	public String determineFieldMapKeyName() {
		return getSqlName();
	}
	
	public Object getValue( AplosAbstractBean aplosAbstractBean ) {
		Class<?> polymorphicClass = null;
		if( getIdFieldInfo() != null ) {
			Object value = getIdFieldInfo().getValue(aplosAbstractBean);
			if( value != null ) {
				polymorphicClass = value.getClass();
			} else {
				return null;
			}
		} else {
			/*
			 * This will happen when the polymorphism is for the class itself and not a field.
			 */
			polymorphicClass = aplosAbstractBean.getClass();
		}
		return ApplicationUtil.getPersistentApplication().getPersistentClassMap().get( polymorphicClass ).getDiscriminatorValue();
	}
	
	@Override
	public void addColumnToDb( PersistableTable persistableTable, FieldInfo previousColumn, Connection conn) throws SQLException {
		super.addColumnToDb(persistableTable, previousColumn, conn);
		PersistentClass persistentClass = (PersistentClass) persistableTable;
		if( InheritanceType.JOINED_TABLE.equals( persistentClass.getInheritanceType() ) ) {
			updateSubClassDiscrimantorColumns((PersistentClass) persistableTable);
		} 
		
		if( this.equals( getParentPersistentClass().getDiscriminatorFieldInfo() ) ) {
			StringBuffer strBuf = new StringBuffer( "UPDATE " );
			strBuf.append( persistentClass.determineSqlTableName() );
			strBuf.append( " SET " ).append( getSqlName() ).append( " = '" );
			String discriminatorValue;
			if( persistentClass.getTableClass().getAnnotation( DiscriminatorValue.class ) != null ) {
				discriminatorValue = persistentClass.getTableClass().getAnnotation( DiscriminatorValue.class ).value();
			} else {
				discriminatorValue = persistentClass.getTableClass().getSimpleName();
			}
			strBuf.append( discriminatorValue ).append( "' WHERE " ).append( getSqlName() ).append( " IS NULL OR ");
			strBuf.append( getSqlName() ).append( " = ''");
			ApplicationUtil.executeSql( strBuf.toString() );
		}
	}
	
	public void updateSubClassDiscrimantorColumns( PersistentClass persistentClass ) {
		for( PersistentClass subPersistentClass : persistentClass.getSubPersistentClasses() ) {
			updateSubClassDiscrimantorColumns(subPersistentClass);
			updateJoinedDiscriminatorColumn( subPersistentClass );
		}
	}
	
	public void updateJoinedDiscriminatorColumn( PersistentClass persistentClass ) {
		if( persistentClass.isDbTable() && persistentClass.isIncludeInApp() ) {
			StringBuffer strBuf = new StringBuffer( "UPDATE " );
			strBuf.append( persistentClass.determineSqlTableName() );
			strBuf.append( " mainTable LEFT OUTER JOIN " );
			strBuf.append( persistentClass.getDbPersistentClass().determineSqlTableName() );
			strBuf.append( " baseTable ON mainTable.id = baseTable.id SET baseTable." ).append( getSqlName() ).append( " = '" );
			strBuf.append( persistentClass.getDiscriminatorValue() );
			strBuf.append( "' WHERE " ).append( getSqlName() ).append( " IS NULL OR ");
			strBuf.append( getSqlName() ).append( " = ''");
			ApplicationUtil.executeSql( strBuf.toString() );
		}
	}

	public FieldInfo getIdFieldInfo() {
		return idFieldInfo;
	}

	public void setIdFieldInfo(FieldInfo idFieldInfo) {
		this.idFieldInfo = idFieldInfo;
	}
}
