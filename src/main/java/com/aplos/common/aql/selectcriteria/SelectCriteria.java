package com.aplos.common.aql.selectcriteria;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.aql.aqlvariables.AqlTableVariable;
import com.aplos.common.aql.aqlvariables.AqlVariable;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.persistence.EnumHelper;
import com.aplos.common.persistence.RowDataReceiver;
import com.aplos.common.persistence.fieldinfo.CollectionFieldInfo;
import com.aplos.common.persistence.fieldinfo.FieldInfo;
import com.aplos.common.persistence.type.applicationtype.EnumIntType;
import com.aplos.common.persistence.type.applicationtype.EnumStringType;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.ConversionUtil;

public class SelectCriteria extends AbstractSelectCriteria {
	private AqlVariable aqlVariable;
	
	public SelectCriteria() {
	}
	
	public SelectCriteria( AqlVariable aqlVariable ) {
		setAqlVariable( aqlVariable );
		aqlVariable.setAssociatedSelectCriteria(this);
	}
	
	public SelectCriteria( AqlVariable aqlVariable, String alias ) {
		setAqlVariable( aqlVariable );
		aqlVariable.setAssociatedSelectCriteria(this);
		setAlias( alias );
	}
	
	public boolean evaluateCriteriaTypes( ProcessedBeanDao processedBeanDao, boolean isAllowingFullTables ) {
		return getAqlVariable().evaluateCriteriaTypes(processedBeanDao, isAllowingFullTables);
	}
    
    public void convertAndSetField( ProcessedBeanDao processedBeanDao, AplosAbstractBean tempBaseBean, RowDataReceiver rowDataReceiver ) throws Exception {
		Object value = convertFieldValues(processedBeanDao, rowDataReceiver );
		getAqlVariable().setField( processedBeanDao.isLoadingFromDatabase(), (AplosAbstractBean) tempBaseBean, value );
    }
    
	@Override
    public Object convertFieldValues( ProcessedBeanDao processedBeanDao, RowDataReceiver rowDataReceiver ) throws SQLException {
    	return getAqlVariable().convertFieldValues( getRowDataIdx(), rowDataReceiver );
    }
    
    public Field getField() {
    	if( getAqlVariable() instanceof AqlTableVariable ) {
    		return ((AqlTableVariable) getAqlVariable()).getField();
    	} else {
    		return null;
    	}
    }
	
	public Object convertFieldValue(Object value)  {
		return convertFieldValue(value, getAqlVariable());
	}
	
	public String getSqlText( BeanDao beanDao ) {
		StringBuffer strBuf = new StringBuffer();
    	if( getAqlVariable().isDistinct() ) {
    		strBuf.append( "DISTINCT " );
    	}
		strBuf.append( getAqlVariable().getCriteria(beanDao) );
		if( !CommonUtil.isNullOrEmpty( getAlias() ) ) {
			strBuf.append( " as " ).append( getAlias() );
		}
		return strBuf.toString();
	}
	
    public void addSelectCriterias( List<SelectCriteria> selectCriterias ) {
    	selectCriterias.add( this );
    }
	
	public FieldInfo getFieldInfo() {
		if( getAqlVariable() instanceof AqlTableVariable ) {
			return ((AqlTableVariable)getAqlVariable()).getFieldInfo();
		}
		return null;
	}
	
	public Object convertFieldValue(Object value, AqlVariable aqlVariable)  {
		FieldInfo fieldInfo = null;
		Field field = null;
		if( getAqlVariable() instanceof AqlTableVariable ) {
			fieldInfo = ((AqlTableVariable) getAqlVariable()).getFieldInfo();
			field = ((AqlTableVariable) getAqlVariable()).getField();
		}
		return convertFieldValue(value, fieldInfo, field);
	}
    
    public static void standardFieldUpdate( FieldInfo fieldInfo, Field field, AplosAbstractBean bean, Object value ) throws Exception {
//    	value = convertFieldValue(value, fieldInfo, field);
		if( field != null ) {
			field.setAccessible(true);
			field.set(bean, value);
    	}
    }
	
	public static Object convertFieldValue(Object value, FieldInfo fieldInfo, Field field )  {
		if( value == null  ) {
			return null;
		} else if( value instanceof Collection ) {
			return value;
		}
		if( fieldInfo != null ) {
			Class fieldClass = null;
			if( fieldInfo instanceof CollectionFieldInfo ) {
				fieldClass = ((CollectionFieldInfo) fieldInfo).getFieldClass();
			} else if( fieldInfo.getField() != null ) {
				fieldClass = fieldInfo.getField().getType();
			}
			if( fieldClass != null ) {
				if( fieldInfo.getApplicationType() instanceof EnumIntType ) {
					EnumHelper enumHelper = ApplicationUtil.getPersistentApplication().getEnumHelperMap().get( fieldClass );
		    		return enumHelper.getEnumValues()[ (Integer) value ];
		    	} else if ( fieldInfo.getApplicationType() instanceof EnumStringType ) {
					EnumHelper enumHelper = ApplicationUtil.getPersistentApplication().getEnumHelperMap().get( fieldClass );
	    			return enumHelper.getEnumNameMap().get( (String) value );
		    	}
			}
		}
		
		if( field != null ) {
			if( field.getType().isAssignableFrom(int.class) || field.getType().isAssignableFrom(Integer.class) ) {
				value = ConversionUtil.convertToInteger(value);
			} else if( field.getType().isAssignableFrom(long.class) || field.getType().isAssignableFrom(Long.class) ) {
				value = ConversionUtil.convertToLong(value);
			} else if( field.getType().isAssignableFrom(Class.class) ) {
				value = ConversionUtil.convertToClass(value);
			}
			if( value instanceof BigInteger ) {
				if( field.getType().isAssignableFrom(int.class) || field.getType().isAssignableFrom(Integer.class) ) {
					value = ((BigInteger) value).intValue();
				} else if( field.getType().isAssignableFrom(long.class) || field.getType().isAssignableFrom(Long.class) ) {
					value = ((BigInteger) value).longValue();
				}
			} else if( value instanceof Long ) {
				if( field.getType().isAssignableFrom(int.class) || field.getType().isAssignableFrom(Integer.class) ) {
					value = ((Long) value).intValue();
				}
			} else if( value instanceof String ) {
				if( field.getType().isAssignableFrom(Class.class) ) {
				}
			}
		}
		
    	return value;
	}
	
	public void updateAqlVariable( AqlVariable aqlVariable ) {
		setAqlVariable(aqlVariable);
	}

	public AqlVariable getAqlVariable() {
		return aqlVariable;
	}

	public void setAqlVariable(AqlVariable aqlVariable) {
		this.aqlVariable = aqlVariable;
	}

}
