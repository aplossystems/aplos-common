package com.aplos.common.backingpage;

import java.util.concurrent.TimeUnit;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.beans.communication.BasicEmailFolder;
import com.aplos.common.utils.ApplicationUtil;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=BasicEmailFolder.class)
public class SpeedTestPage extends ListPage {
	private static final long serialVersionUID = -7771574708313609495L;
	private long processingDuration;
	private long databaseCreateDuration;
	private long databaseInsertDuration;
	private long databaseSelectDuration;
	private long databaseDeleteDuration;
	
	@Override
	public boolean responsePageLoad() {
		calculateProcessingDuration();
		calculateDatabaseCreateDuration();
		calculateDatabaseInsertDuration();
		calculateDatabaseSelectDuration();
		calculateDatabaseDeleteDuration();
		return super.responsePageLoad();
	}
	
	public void calculateProcessingDuration() {
		long startTime = System.nanoTime();
		int a = 1;
		int b = 3;
		for( int i = 0, n = 10000000; i < n; i++ ) {
			a = a + b;
			a = Math.max( a, b );
			a = a % 10;
		}
		setProcessingDuration( TimeUnit.MILLISECONDS.convert( System.nanoTime() - startTime, TimeUnit.NANOSECONDS ) );
	}
	
	public void calculateDatabaseCreateDuration() {
		long startTime = System.nanoTime();
		StringBuffer strBuf = new StringBuffer();
		strBuf.append( "CREATE TABLE aplos_systemTest ( " );
		strBuf.append( "id INT NOT NULL AUTO_INCREMENT" );
		strBuf.append( ",textColumn VARCHAR(100) NOT NULL" );
		strBuf.append( ",PRIMARY KEY ( id )");
		strBuf.append( ")" );
		ApplicationUtil.executeSql( strBuf.toString() );
		setDatabaseCreateDuration( TimeUnit.MILLISECONDS.convert( System.nanoTime() - startTime, TimeUnit.NANOSECONDS ) );
	}
	
	public void calculateDatabaseInsertDuration() {
		long startTime = System.nanoTime();
		for( int i = 0, n = 100; i < n; i++ ) {
			ApplicationUtil.executeSql( "INSERT INTO aplos_systemTest ( textColumn ) VALUES ('" + i + "')" );
		}
		setDatabaseInsertDuration( TimeUnit.MILLISECONDS.convert( System.nanoTime() - startTime, TimeUnit.NANOSECONDS ) );
	}
	
	public void calculateDatabaseSelectDuration() {
		long startTime = System.nanoTime();
		StringBuffer strBuf = new StringBuffer();
		ApplicationUtil.getResults( "SELECT * FROM aplos_systemTest WHERE id IN (1,5,10,25,50) AND id > (SELECT MIN(id) FROM aplos_systemTest)");
		setDatabaseSelectDuration( TimeUnit.MILLISECONDS.convert( System.nanoTime() - startTime, TimeUnit.NANOSECONDS ) );
	}
	
	public void calculateDatabaseDeleteDuration() {
		long startTime = System.nanoTime();
		StringBuffer strBuf = new StringBuffer();
		ApplicationUtil.executeSql( "DROP TABLE aplos_systemTest" );
		setDatabaseDeleteDuration( TimeUnit.MILLISECONDS.convert( System.nanoTime() - startTime, TimeUnit.NANOSECONDS ) );
	}
	
	public long getProcessingDuration() {
		return processingDuration;
	}
	public void setProcessingDuration(long processingDuration) {
		this.processingDuration = processingDuration;
	}
	public long getDatabaseCreateDuration() {
		return databaseCreateDuration;
	}
	public void setDatabaseCreateDuration(long databaseCreateDuration) {
		this.databaseCreateDuration = databaseCreateDuration;
	}
	public long getDatabaseInsertDuration() {
		return databaseInsertDuration;
	}
	public void setDatabaseInsertDuration(long databaseInsertDuration) {
		this.databaseInsertDuration = databaseInsertDuration;
	}
	public long getDatabaseSelectDuration() {
		return databaseSelectDuration;
	}
	public void setDatabaseSelectDuration(long databaseSelectDuration) {
		this.databaseSelectDuration = databaseSelectDuration;
	}
	public long getDatabaseDeleteDuration() {
		return databaseDeleteDuration;
	}
	public void setDatabaseDeleteDuration(long databaseDeleteDuration) {
		this.databaseDeleteDuration = databaseDeleteDuration;
	}
	
}
