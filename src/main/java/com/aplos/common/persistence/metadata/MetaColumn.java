package com.aplos.common.persistence.metadata;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.aplos.common.persistence.type.applicationtype.ApplicationType;
import com.aplos.common.persistence.type.applicationtype.MySqlType;
import com.aplos.common.utils.ApplicationUtil;

public class MetaColumn {
	private String name;
	private ApplicationType applicationType; 
	private boolean isPrimaryKey = false;
	
	public MetaColumn() {
	}
	
	public MetaColumn( DatabaseMetaData meta, ResultSet rs ) throws SQLException  {
		setName(rs.getString("COLUMN_NAME"));
		MySqlType mySqlType;
		try {
			mySqlType = MySqlType.valueOf( rs.getString("TYPE_NAME") );
			setApplicationType( mySqlType.getNewDbType() );
		} catch( IllegalArgumentException iaEx ) {
			ApplicationUtil.getAplosContextListener().handleError( new Exception( "MySqlType not found" ) );
		}
		
		getApplicationType().setColumnSize(rs.getInt("COLUMN_SIZE"));
		getApplicationType().setDecimalDigits(rs.getInt("DECIMAL_DIGITS"));
		getApplicationType().setNullable(rs.getBoolean("IS_NULLABLE"));
		getApplicationType().setDefaultValue( rs.getString("COLUMN_DEF") );
	}
	
	public String toString() {
		StringBuffer strBuf = new StringBuffer();
		strBuf.append( getName() ).append( " " );
		strBuf.append( getApplicationType().getClass().getSimpleName() ).append( " " );
		strBuf.append( getApplicationType().getColumnSize() ).append( " " );
		strBuf.append( getApplicationType().getDecimalDigits() ).append( " " );
		strBuf.append( getApplicationType().isNullable() ).append( " " );
		strBuf.append( getApplicationType().getDefaultValue() ).append( " " );
		return strBuf.toString();
	};

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ApplicationType getApplicationType() {
		return applicationType;
	}

	public void setApplicationType(ApplicationType applicationType) {
		this.applicationType = applicationType;
	}

	public boolean isPrimaryKey() {
		return isPrimaryKey;
	}

	public void setPrimaryKey(boolean isPrimaryKey) {
		this.isPrimaryKey = isPrimaryKey;
	}
}
