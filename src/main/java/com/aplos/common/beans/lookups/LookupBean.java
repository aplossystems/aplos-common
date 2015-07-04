package com.aplos.common.beans.lookups;

import java.util.List;

import javax.faces.model.SelectItem;

import com.aplos.common.annotations.persistence.MappedSuperclass;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.AplosBean;

@MappedSuperclass
public abstract class LookupBean extends AplosBean {
	private static final long serialVersionUID = 2527093940336097893L;
	private String name;

	public LookupBean() {}

	public void setName( String name ) {
		this.name = name;
	}
	public String getName() {
		return name;
	}

	@Override
	public String getDisplayName() {
		return getName();
	}

	public List<LookupBean> getLookupBeanList() {
		return new BeanDao( getClass() ).getAll();
	}

	public LookupBean getLookupBean( Long id ) {
		return (LookupBean) new BeanDao( getClass() ).get( id );
	}

	@Override
	public String getTableName() {
		return getClass().getSimpleName();
	}

	public SelectItem[] getSelectItemsWithNotSelected() {
		return getSelectItemsWithNotSelected( "Not Selected" );
	}
}
