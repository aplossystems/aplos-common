package com.aplos.common.aql;

import java.util.HashMap;
import java.util.Map;

import com.aplos.common.aql.aqlvariables.AqlTableVariable;
import com.aplos.common.aql.aqlvariables.AqlVariable;

public class OrderByCriteria {
	private AqlVariable aqlVariable;
	private OrderByDirection orderByDirection;
	private BeanDao aqlBeanDao;
	public static Map<String, OrderByDirection> orderByDirectionMap = new HashMap<String, OrderByDirection>();
	
	public enum OrderByDirection {
		ASC,
		DESC;
	}
	
	static {
		orderByDirectionMap.put("asc" , OrderByDirection.ASC );
		orderByDirectionMap.put("ascending" , OrderByDirection.ASC );
		orderByDirectionMap.put("desc" , OrderByDirection.DESC );
		orderByDirectionMap.put("descending" , OrderByDirection.DESC );
	}
	
	public OrderByCriteria( BeanDao aqlBeanDao, AqlVariable selectCriteria ) {
		setAqlBeanDao(aqlBeanDao);
		setAqlVariable( selectCriteria );
	}
	
	public String getCriteria() {
		StringBuffer strBuf = new StringBuffer( getAqlVariable().getSqlPath( true ) );
		if( getOrderByDirection() != null ) {
			strBuf.append( " " ).append( getOrderByDirection().name() );
		}
		return strBuf.toString();
	}

	public AqlVariable getAqlVariable() {
		return aqlVariable;
	}

	public void setAqlVariable(AqlVariable aqlVariable) {
		this.aqlVariable = aqlVariable;
	}

	public OrderByDirection getOrderByDirection() {
		return orderByDirection;
	}

	public void setOrderByDirection(OrderByDirection orderByDirection) {
		this.orderByDirection = orderByDirection;
	}

	public BeanDao getAqlBeanDao() {
		return aqlBeanDao;
	}

	public void setAqlBeanDao(BeanDao aqlBeanDao) {
		this.aqlBeanDao = aqlBeanDao;
	}
}
