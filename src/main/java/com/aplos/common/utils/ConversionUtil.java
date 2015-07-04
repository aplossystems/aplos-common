package com.aplos.common.utils;

import java.math.BigDecimal;
import java.math.BigInteger;

public class ConversionUtil {
	public static Long convertToLong( Object obj ) {
		if( obj instanceof Long ) {
			return (Long) obj;
		} else if( obj instanceof BigInteger ) {
			return ((BigInteger) obj).longValue();
		} else if( obj instanceof Integer ) {
			return ((Integer) obj).longValue();
		} else if( obj instanceof String ) {
			return Long.parseLong((String)obj);
		}
		return null;
	}
	public static Double convertToDouble( Object obj ) {
		if( obj instanceof Double ) {
			return (Double) obj;
		} else if( obj instanceof BigInteger ) {
			return ((BigInteger) obj).doubleValue();
		} else if( obj instanceof BigDecimal ) {
			return ((BigDecimal) obj).doubleValue();
		} else if( obj instanceof Integer ) {
			return ((Integer) obj).doubleValue();
		} else if( obj instanceof String ) {
			return Double.parseDouble((String)obj);
		}
		return null;
	}
	
	public static Integer convertToInteger( Object value ) {
		if( value instanceof BigInteger ) {
			return ((BigInteger) value).intValue();
		} else if( value instanceof Long ) {
			return ((Long) value).intValue();
		} else if( value instanceof String ) {
			return Integer.parseInt( (String) value );
		}
		return (Integer) value;
	}
	
	public static Class<?> convertToClass( Object value ) {
		try {
			return ApplicationUtil.getPersistentApplication().classForName( (String) value );
		} catch( ClassNotFoundException cnfEx ) {
			ApplicationUtil.handleError( cnfEx );
		}
		return null;
	}
}
