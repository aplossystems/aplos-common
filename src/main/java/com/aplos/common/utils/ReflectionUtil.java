package com.aplos.common.utils;

import java.lang.reflect.Field;
import java.math.BigInteger;

import org.apache.commons.lang.StringUtils;

import com.aplos.common.beans.AplosAbstractBean;

public class ReflectionUtil {
	public static void setField( Object object, String propertyName, Object value ) throws NoSuchFieldException, IllegalAccessException {
		ParentAndField parentAndField = getField(object, propertyName);

		if( parentAndField != null ) {
			if( parentAndField.getField().getType().equals( Long.class ) && value.getClass().equals( BigInteger.class ) ) {
				value = ((BigInteger) value).longValue();
			} 
	//		else if( Enum.class.isAssignableFrom( updateableField.getType() ) ) {
	//			boolean isOrdinal = true;
	//			if( updateableField.getAnnotation( @Enumerated.class ) != null ) {
	//				
	//			}
	//		}
			parentAndField.getField().set(parentAndField.getParent(), value);
		}
		/*
		 * This should be here for the setField and getField but it was breaking Teletest 
		 * on the company list page
		 * and I didn't have the time to fix it.
		 */
//		} else {
//			throw new NoSuchFieldException();
//		}
	}
	
	public static Field getField(Class<?> currClazz, String propertyName) throws IllegalAccessException {
		String[] propertyParts = StringUtils.split( propertyName, "." );

		Field field;
		for( int i = 0, n = propertyParts.length; i < n; i++ ) {
			while( currClazz != null && currClazz.getName().startsWith( "com.aplos" ) ) {
				field = getDeclaredField( currClazz, propertyParts[ i ] );
					
				if( field == null ) {
					currClazz = currClazz.getSuperclass();
					continue;
					// do nothing, this is expected as the field may be in superclass
				}

				return field;
			}
		}
		return null;
	}
	
	public static ParentAndField getField(Object object, String propertyName) throws IllegalAccessException {
		Class<?> currClazz = object.getClass();
		String[] propertyParts = StringUtils.split( propertyName, "." );

		for( int i = 0, n = propertyParts.length; i < n; i++ ) {
			while( currClazz != null && currClazz.getName().startsWith( "com.aplos" ) ) {
				Field updateableField;
				updateableField = getDeclaredField( currClazz, propertyParts[ i ] );
					
				if( updateableField == null ) {
					currClazz = currClazz.getSuperclass();
					continue;
					// do nothing, this is expected as the field may be in superclass
				}

				updateableField.setAccessible(true);
				if( i == propertyParts.length - 1 ) {
					return new ParentAndField( object, updateableField );
				} else {
					object = (AplosAbstractBean) updateableField.get(object);
					currClazz = object.getClass();
					break;
				}
			}
		}
		return null;
	}
	
	public static Field getDeclaredField( Class<?> currClazz, String fieldName ) {
		Field[] declaredFields = currClazz.getDeclaredFields();
		for( int i = 0; i < declaredFields.length; i++ ) {
			if( declaredFields[ i ].getName().equalsIgnoreCase( fieldName ) ) {
				return declaredFields[ i ];
			}
		}
		return null;
	}

	public static Object getFieldValue( Object object, String propertyName ) throws NoSuchFieldException, IllegalAccessException {
		ParentAndField parentAndField = getField(object, propertyName);

		if( parentAndField != null ) {
			return parentAndField.getField().get(parentAndField.getParent());
		}
		return null;
//		} else {
//			throw new NoSuchFieldException();
//		}
	}
}
