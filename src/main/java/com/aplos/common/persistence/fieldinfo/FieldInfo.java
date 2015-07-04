package com.aplos.common.persistence.fieldinfo;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.aplos.common.annotations.persistence.Cascade;
import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.EnumType;
import com.aplos.common.annotations.persistence.Enumerated;
import com.aplos.common.aql.selectcriteria.SelectCriteria;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.persistence.PersistableTable;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.metadata.ColumnIndex;
import com.aplos.common.persistence.metadata.ColumnIndex.ColumnIndexType;
import com.aplos.common.persistence.type.applicationtype.ApplicationType;
import com.aplos.common.persistence.type.applicationtype.BigIntType;
import com.aplos.common.persistence.type.applicationtype.BooleanType;
import com.aplos.common.persistence.type.applicationtype.ClassType;
import com.aplos.common.persistence.type.applicationtype.DateTimeType;
import com.aplos.common.persistence.type.applicationtype.DecimalType;
import com.aplos.common.persistence.type.applicationtype.DoubleType;
import com.aplos.common.persistence.type.applicationtype.EnumIntType;
import com.aplos.common.persistence.type.applicationtype.EnumStringType;
import com.aplos.common.persistence.type.applicationtype.IntType;
import com.aplos.common.persistence.type.applicationtype.LongTextType;
import com.aplos.common.persistence.type.applicationtype.PrimitiveBooleanType;
import com.aplos.common.persistence.type.applicationtype.TextType;
import com.aplos.common.persistence.type.applicationtype.VarCharType;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;

public class FieldInfo {
	private boolean isPrimaryKey = false;
	private boolean isUnique = false;
	private String sqlName;
	private Field field; 
	private List<FieldInfo> additionalDeclaringClasses = new ArrayList<FieldInfo>();
	private PersistentClass parentPersistentClass;
	private ApplicationType applicationType;
	private Column columnAnnotation;
	private Cascade cascadeAnnotation;
	private static Map<Class<?>, Class<? extends ApplicationType>> mySqlTypeMap = new HashMap<Class<?>,Class<? extends ApplicationType>>();
	
	private static Logger logger = Logger.getLogger( FieldInfo.class );
	
	static {
		mySqlTypeMap.put( int.class, IntType.class );
		mySqlTypeMap.put( Integer.class, IntType.class );
		mySqlTypeMap.put( String.class, VarCharType.class );
		mySqlTypeMap.put( boolean.class, PrimitiveBooleanType.class );
		mySqlTypeMap.put( Boolean.class, BooleanType.class );

		mySqlTypeMap.put( Date.class, DateTimeType.class );
		mySqlTypeMap.put( BigDecimal.class, DecimalType.class );
		mySqlTypeMap.put( double.class, DoubleType.class );
		mySqlTypeMap.put( Double.class, DoubleType.class );
		mySqlTypeMap.put( long.class, BigIntType.class );
		mySqlTypeMap.put( Long.class, BigIntType.class );
		mySqlTypeMap.put( float.class, DecimalType.class );
		mySqlTypeMap.put( Float.class, DecimalType.class );
		mySqlTypeMap.put( Class.class, ClassType.class );
	}
	
	public FieldInfo( PersistentClass parentPersistentClass, Field field ) {
		this.setParentPersistentClass(parentPersistentClass);
		this.field = field;
		if( field != null ) {
			columnAnnotation = field.getAnnotation( Column.class );
		}
	}
	
	public void addAdditionalDeclaringClass( FieldInfo fieldInfo ) {
		getAdditionalDeclaringClasses().add( fieldInfo );
	}
	
	public void appendCreateTableStr( StringBuffer strBuf, PersistableTable persistableTable ) {
		strBuf.append( "`" ).append( getSqlName() ).append( "` " );
		getApplicationType().appendCreateTableStr( strBuf );
		strBuf.append( " " );
		if( !getApplicationType().isNullable() ) {
			strBuf.append( "NOT NULL " );
		} 
		
		if( getApplicationType().getDefaultValue() != null ) {
			strBuf.append( "DEFAULT " + getApplicationType().getDefaultValue() );
		} else if( getApplicationType().isNullable() ) {
			strBuf.append( "DEFAULT NULL " );
		}
			
		if( isPrimaryKey() && persistableTable instanceof PersistentClass ) {
			strBuf.append( "AUTO_INCREMENT " );
		}
	}
	
	public void setValue( boolean isLoadingFromDatabase, AplosAbstractBean bean, Object value ) throws Exception {
		SelectCriteria.standardFieldUpdate(this, getField(), bean, value);
	}
	
	public String determineFieldMapKeyName() {
		if( getField() == null ) {
			return getSqlName();
		} else {
			/*
			 * This is mainly so that when a persistantClassFieldInfo is called then
			 * it doesn't put in the _id for the map key as people refer to it as the
			 * object.
			 * Also as some of the members are in subtables having the sqlName can
			 * lead to duplicate keys in a table. 
			 */
			return getField().getName();
		}
	}
	
	public void init() {
		if( CommonUtil.isNullOrEmpty( getSqlName() ) ) {
			if( columnAnnotation != null && !CommonUtil.isNullOrEmpty( columnAnnotation.name() ) ) {
				setSqlName( columnAnnotation.name() );
			} else {
				setSqlName( field.getName() );
			}
		}
		setApplicationType( determineDbType( field, field.getType() ) );
		if( field.getType().isPrimitive() ) {
			getApplicationType().setNullable(false);
		}
		if( columnAnnotation != null ) {
			setUnique( columnAnnotation.unique() );
		}
		if( columnAnnotation != null ) {
			getApplicationType().setNullable(columnAnnotation.nullable() );
		}
	}
	
	public boolean isEmpty( AplosAbstractBean aplosAbstractBean ) {
		return getApplicationType().isEmpty( getField(), aplosAbstractBean );
	}
	
	public Object getConvertedValue( AplosAbstractBean aplosAbstractBean ) {
		Object value = getValue( aplosAbstractBean );
		return SelectCriteria.convertFieldValue(value, this, this.getField() );
	}
	
	public Field findFieldWithAssignableDeclaringClass( AplosAbstractBean aplosAbstractBean ) {
		if( getField().getDeclaringClass().isAssignableFrom( aplosAbstractBean.getClass() ) ) {
			return getField();
		} 

		for( FieldInfo tempFieldInfo : getAdditionalDeclaringClasses() ) {
			if( tempFieldInfo.getField().getDeclaringClass().isAssignableFrom( aplosAbstractBean.getClass() ) ) {
				return tempFieldInfo.getField();
			}
		}
		return null;
	}
	
	public Object getValue( AplosAbstractBean aplosAbstractBean ) {
		Field field = findFieldWithAssignableDeclaringClass( aplosAbstractBean ); 
		if( field != null ) {
			return getApplicationType().getValue( field, aplosAbstractBean );
		} else {
			return null;
		}
	}
	
	public void addAdditionalFieldInfos( PersistableTable persistableTable ) {
	 	
	}
	
	public void addForeignKeysAndIndexes( List<ForeignKeyFieldInfo> foreignKeys, List<ColumnIndex> columnIndexes ) {
		if( isPrimaryKey() ) {
			columnIndexes.add( new ColumnIndex( this, ColumnIndexType.PRIMARY ) );
		}
		if( isUnique() ) {
			columnIndexes.add( new ColumnIndex( this, ColumnIndexType.UNIQUE ) );
		}
	}
	
	public static ApplicationType determineDbType( Field field, Class<?> fieldTypeClass ) {
		ApplicationType applicationType = null;
		if( fieldTypeClass.equals( String.class ) ) {
			Column columnAnnotation = field.getAnnotation(Column.class);
			if( columnAnnotation != null && columnAnnotation.columnDefinition().equals( "LONGTEXT" ) ) {
				applicationType = new LongTextType();	
			} else if( columnAnnotation != null && columnAnnotation.columnDefinition().equals( "TEXT" ) ) {
				applicationType = new TextType();	
			} else {
				applicationType = new VarCharType();
			}
		} else if( fieldTypeClass.isEnum() ) {
			Enumerated enumeratedAnnotation = field.getAnnotation(Enumerated.class);
			
			if( enumeratedAnnotation != null && enumeratedAnnotation.value().equals( EnumType.STRING ) ) {
				applicationType = new EnumStringType();	
			} else {
				applicationType = new EnumIntType();
			}
		} else {
			applicationType = (ApplicationType) CommonUtil.getNewInstance( mySqlTypeMap.get( fieldTypeClass ) );
		}
		if( applicationType == null ) {
			ApplicationUtil.getAplosContextListener().handleError( new Exception( "MySqlType could not be found for " + fieldTypeClass ) );
		}
		return applicationType;
	}
	
	@Override
	public String toString() {
		StringBuffer strBuf = new StringBuffer();
		strBuf.append( getSqlName() ).append( " " );
		strBuf.append( getApplicationType().getMySqlType().getDbTypeClass().getSimpleName() ).append( " " );
		strBuf.append( getApplicationType().getColumnSize() ).append( " " );
		strBuf.append( getApplicationType().getDecimalDigits() ).append( " " );
		strBuf.append( getApplicationType().isNullable() ).append( " " );
		strBuf.append( getApplicationType().getDefaultValue() ).append( " " );
		return strBuf.toString();
	}
	
	public void addColumnToDb( PersistableTable persistableTable, FieldInfo previousColumn, Connection conn ) throws SQLException {
		addColumnToDb(persistableTable, previousColumn, persistableTable.determineSqlTableName(), conn );
	}
	
	public void addColumnToDb( PersistableTable persistableTable, FieldInfo previousColumn, String tableName, Connection conn ) throws SQLException {
		StringBuffer strBuf = new StringBuffer( "ALTER TABLE " );
		strBuf.append( tableName );
		strBuf.append( " ADD COLUMN " ).append( getSqlName() ).append( " " );
		strBuf.append( getApplicationType().getMySqlType().name() );
		if( getApplicationType().includeColumnSizeInInsertOrAlter() ) {
			strBuf.append( "(" ).append( getApplicationType().getColumnSize() );
			
			if( getApplicationType().isUsingDecimalDigits() ) {
				strBuf.append( "," ).append( getApplicationType().getDecimalDigits() );
			}
			strBuf.append( ")" );
		}
		if( !getApplicationType().isNullable() ) {
			strBuf.append( " NOT NULL" );  
		}
		
		if( getApplicationType().getDefaultValue() != null ) {
			strBuf.append( " DEFAULT " + getApplicationType().getDefaultValue() );
		} else if( getApplicationType().isNullable() ) {
			strBuf.append( " DEFAULT NULL" );
		}
		
		if( isPrimaryKey() && persistableTable instanceof PersistentClass ) {
			strBuf.append( " AUTO_INCREMENT" );
		}

		if( previousColumn != null ) {
			strBuf.append( " AFTER " ).append( previousColumn.getSqlName() );
		}
		ApplicationUtil.executeSql( strBuf.toString(), conn );
	}
	
	public void alterColumn( PersistableTable persistableTable, String tableName, Connection conn ) throws SQLException {
		StringBuffer strBuf = new StringBuffer( "ALTER TABLE " );
		strBuf.append( tableName );
		strBuf.append( " CHANGE COLUMN  " ).append( getSqlName() );
		strBuf.append( " ").append( getSqlName() ).append( " " );
		strBuf.append( getApplicationType().getMySqlType().name() );
		if( getApplicationType().includeColumnSizeInInsertOrAlter() ) {
			strBuf.append( "(" ).append( getApplicationType().getColumnSize() );
			
			if( getApplicationType().isUsingDecimalDigits() ) {
				strBuf.append( "," ).append( getApplicationType().getDecimalDigits() );
			}
			strBuf.append( ")" );
		}
		if( !getApplicationType().isNullable() ) {
			strBuf.append( " NOT NULL" );  
		}
		
		if( getApplicationType().getDefaultValue() != null ) {
			strBuf.append( " DEFAULT " + getApplicationType().getDefaultValue() );
		} else if( getApplicationType().isNullable() ) {
			strBuf.append( " DEFAULT NULL" );
		}
		
		if( isPrimaryKey() && persistableTable instanceof PersistentClass ) {
			strBuf.append( " AUTO_INCREMENT" );
		}
			
		logger.debug( strBuf.toString() );
		ApplicationUtil.executeSql( strBuf.toString(), conn );
	}
	
	public String getSqlName() {
		return sqlName;
	}
	public void setSqlName(String sqlName) {
		this.sqlName = sqlName;
	}
	public Field getField() {
		return field;
	}
	public void setField(Field field) {
		this.field = field;
	}

	public PersistentClass getParentPersistentClass() {
		return parentPersistentClass;
	}

	public void setParentPersistentClass(PersistentClass parentPersistentClass) {
		this.parentPersistentClass = parentPersistentClass;
	}

	public boolean isPrimaryKey() {
		return isPrimaryKey;
	}

	public void setPrimaryKey(boolean isPrimaryKey) {
		this.isPrimaryKey = isPrimaryKey;
	}

	public boolean isUnique() {
		return isUnique;
	}

	public void setUnique(boolean isUnique) {
		this.isUnique = isUnique;
	}

	public ApplicationType getApplicationType() {
		return applicationType;
	}

	public void setApplicationType(ApplicationType applicationType) {
		this.applicationType = applicationType;
	}

	public Cascade getCascadeAnnotation() {
		return cascadeAnnotation;
	}

	public void setCascadeAnnotation(Cascade cascadeAnnotation) {
		this.cascadeAnnotation = cascadeAnnotation;
	}

	public List<FieldInfo> getAdditionalDeclaringClasses() {
		return additionalDeclaringClasses;
	}

	public void setAdditionalDeclaringClasses(
			List<FieldInfo> additionalDeclaringClasses) {
		this.additionalDeclaringClasses = additionalDeclaringClasses;
	}
}
