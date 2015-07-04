package com.aplos.common.persistence;

import java.sql.SQLException;
import java.util.List;

import com.aplos.common.persistence.type.applicationtype.ApplicationType;

public class ListRowDataReceiver extends RowDataReceiver {
	private List<Object> dataList;
	
	public ListRowDataReceiver( List<Object> dataList ) {
		setDataList( dataList );
	}
	
	@Override
	public Object getObject(int columnIdx, ApplicationType applicationType) throws SQLException {
		return dataList.get( columnIdx );
	}

	public List<Object> getDataList() {
		return dataList;
	}

	public void setDataList(List<Object> dataList) {
		this.dataList = dataList;
	}
}
