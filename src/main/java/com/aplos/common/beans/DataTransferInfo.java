package com.aplos.common.beans;

public class DataTransferInfo {
	private Class<?> dataTransferClass;
	private Long dataTransferId;
	private DataTransferInfo transferedBy;

	public void setDataTransferClass(Class<?> dataTransferClass) {
		this.dataTransferClass = dataTransferClass;
	}
	public Class<?> getDataTransferClass() {
		return dataTransferClass;
	}
	public void setDataTransferId(Long dataTransferId) {
		this.dataTransferId = dataTransferId;
	}
	public Long getDataTransferId() {
		return dataTransferId;
	}
	public void setTransferedBy(DataTransferInfo transferedBy) {
		this.transferedBy = transferedBy;
	}
	public DataTransferInfo getTransferedBy() {
		return transferedBy;
	}
}
