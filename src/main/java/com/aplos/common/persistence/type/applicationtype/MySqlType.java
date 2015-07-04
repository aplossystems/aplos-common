package com.aplos.common.persistence.type.applicationtype;

import com.aplos.common.utils.CommonUtil;

public enum MySqlType {
	BIGINT ( BigIntType.class ),
	BIT ( BooleanType.class ),
	DATETIME ( DateTimeType.class ),
	DECIMAL ( DecimalType.class ),
	DOUBLE ( DoubleType.class ),
	INT ( IntType.class ),
	LONGTEXT ( LongTextType.class ),
	MEDIUMTEXT ( MediumTextType.class ),
	TEXT ( TextType.class ),
	VARCHAR ( VarCharType.class );
	
	private Class<? extends ApplicationType> dbTypeClass;
	
	private MySqlType( Class<? extends ApplicationType> dbTypeClass ) {
		this.dbTypeClass = dbTypeClass;
	}
	
	public Class<? extends ApplicationType> getDbTypeClass() {
		return dbTypeClass;
	}
	
	public ApplicationType getNewDbType() {
		return (ApplicationType) CommonUtil.getNewInstance( dbTypeClass, null );
	}
}
