package com.aplos.common.beans.communication;

import java.util.List;

import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.interfaces.BulkMessageSource;

@Entity
public class HqlMessageSourceFinder extends BulkMessageSourceGroup {
	private static final long serialVersionUID = -6517907769945718094L;
	
	@Column(columnDefinition="LONGTEXT")
	private String hql;
	
	@Override
	public List<BulkMessageSource> getBulkMessageSources() {
		List<BulkMessageSource> bulkMessageSources = new BeanDao( getHql() ).getAll();
		return bulkMessageSources;
	}

	public String getHql() {
		return hql;
	}

	public void setHql(String hql) {
		this.hql = hql;
	}
}
