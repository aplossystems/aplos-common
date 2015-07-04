package com.aplos.common.beans;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.faces.model.SelectItem;

import com.aplos.common.annotations.persistence.MappedSuperclass;
import com.aplos.common.annotations.persistence.Transient;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.utils.ApplicationUtil;

@MappedSuperclass
public abstract class AplosPriorityBean extends AplosBean {

	private static final long serialVersionUID = -6899054772052530372L;
	private Long priority; //position in results
	@Transient
	private Long whereWeWantToMove=null;
	
	@Override
	public void saveBean(SystemUser currentUser) {
		if (this.getPriority() == null) {
			this.setPriority(getNextMaxPriority());
		}
		super.saveBean(currentUser);
	}

	public void makeLowestPriority() {
		this.reSynchronisePriorities();
		Long originalPriority = getPriority();
		BigInteger dbMax = (BigInteger) ApplicationUtil.getResults( "SELECT MAX(bean.priority) FROM " + AplosBean.getTableName(getClass()) + getPriorityJoins() + getPriorityWhere()).get( 0 )[ 0 ];
		ApplicationUtil.executeSql("UPDATE " + AplosBean.getTableName(getClass()) + getPriorityJoins() + " SET bean.priority=-888" + getPriorityWhere() + " AND bean.priority=" + originalPriority);
		ApplicationUtil.executeSql("UPDATE " + AplosBean.getTableName(getClass()) + getPriorityJoins() + " SET bean.priority=bean.priority-1" + getPriorityWhere() + " AND bean.priority > " + originalPriority);
		ApplicationUtil.executeSql("UPDATE " + AplosBean.getTableName(getClass()) + getPriorityJoins() + " SET bean.priority=" + dbMax + getPriorityWhere() + " AND bean.priority=-888");
	}

	@Override
	public void delete() {
		this.makeLowestPriority();
		super.delete();
	}

	@Override
	public void reinstate() {
		super.reinstate();
		this.makeLowestPriority();
	}

	public Long getNextMaxPriority() {
		reSynchronisePriorities();
		BigInteger dbMax = (BigInteger) ApplicationUtil.getFirstResult("SELECT MAX(bean.priority) FROM " + AplosBean.getTableName(getClass()) + getPriorityJoins() + getPriorityWhere())[ 0 ];
		Long position = 0l;
		if (dbMax != null) {
			position = (Long.parseLong(dbMax.toString())) +1;
		}
		return position;
	}

	public void reSynchronisePriorities() {
		reSynchronisePriorities(getClass(), getPriorityJoins(), getPriorityWhere());
	}

	public String getPriorityJoins() {
		return " bean";
	}

	@SuppressWarnings("rawtypes")
	public static void reSynchronisePriorities(Class clazz, String priorityJoins, String priorityWhere) {
		String sql = "SELECT bean.id,bean.priority FROM " + AplosBean.getTableName(clazz) + priorityJoins + priorityWhere + " ORDER BY bean.priority";
		@SuppressWarnings("unchecked")
		ArrayList<Object[]> prioritizedItems = (ArrayList<Object[]>) ApplicationUtil.getResults(sql);
		for (int i=1; i < prioritizedItems.size()+1; i++) {
			Object[] record = prioritizedItems.get(i-1);
			if (record[1] == null || ((BigInteger)record[1]).intValue() != i) {
				//HibernateUtil.getCurrentSession().createSQLQuery("UPDATE " + AplosBean.getTableName(getClass()) + getPriorityJoins() + " SET bean.priority=" + i + getPriorityWhere() + " AND bean.id=" + record[0]).executeUpdate();
				ApplicationUtil.executeSql("UPDATE " + AplosBean.getTableName(clazz) + " SET priority=" + i + " WHERE id=" + record[0]);
			}
		}
	}

	//should be overridden if not using full table
	public String getPriorityWhere() {
		return " WHERE active=1";
	}

	public void setPriority(Long priority) {
		this.priority = priority;
	}

	public Long getPriority() {
		return priority;
	}

	public void setWhereWeWantToMove(Long whereWeWantToMove) {
		this.whereWeWantToMove = whereWeWantToMove;
	}

	public Long getWhereWeWantToMove() {
		if (whereWeWantToMove==null) {
			return priority;
		}
		return whereWeWantToMove;
	}

	@Override
	public SelectItem[] getSelectItemBeans() {
		BeanDao aqlBeanDao = new BeanDao( getClass() );
		aqlBeanDao.addWhereCriteria( "active=true" );
		aqlBeanDao.setOrderBy( "priority" );
		return getSelectItemBeans( aqlBeanDao.getAll() );
	}

	public static List<? extends AplosPriorityBean> sortByPriority( List<? extends AplosPriorityBean> priorityBeanList ) {
		Collections.sort( priorityBeanList, new Comparator<AplosPriorityBean>() {
			@Override
			public int compare(AplosPriorityBean aplosBean1, AplosPriorityBean aplosBean2) {
				if ( (aplosBean1 == null || aplosBean1.getPriority() == null )
						&& (aplosBean2 == null || aplosBean2.getPriority() == null)) {
					return 0;
				}
				if (aplosBean1 == null || aplosBean1.getPriority() == null) {
					return 1;
				}
				if (aplosBean2 == null || aplosBean2.getPriority() == null) {
					return -1;
				}
				return aplosBean1.getPriority().compareTo( aplosBean2.getPriority() );
			}
		});
		return priorityBeanList;
	}
}
