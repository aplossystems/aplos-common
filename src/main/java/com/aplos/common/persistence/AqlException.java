package com.aplos.common.persistence;


public class AqlException extends RuntimeException {
	public AqlException() {
	}
	
	public AqlException( String message ) {
		super( message );
	}
	
	public AqlException( Exception ex ) {
		super( ex );
	}
}
