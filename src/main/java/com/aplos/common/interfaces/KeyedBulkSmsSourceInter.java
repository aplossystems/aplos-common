package com.aplos.common.interfaces;


public interface KeyedBulkSmsSourceInter extends BulkEmailSource { 
	public String getEmailAddress( int key );
	public String getFirstName( int key ); 
	public String getSurname( int key ); 
	public String getKeyLabel( int key );
	public String getJDynamiTeValue( int key, String variableKey );
}
