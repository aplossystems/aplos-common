package com.aplos.common.aql;

import java.io.Serializable;

public abstract class AbstractLazyInitializer {
	
	private Object target;
	private boolean initialized;
	private String entityName;
	private Serializable id;
	private boolean unwrap;

	/**
	 * For serialization from the non-pojo initializers (HHH-3309)
	 */
	protected AbstractLazyInitializer() {
	}
	
	protected AbstractLazyInitializer(String entityName, Serializable id) {
		this.id = id;
		this.entityName = entityName;
	}

	public final Serializable getIdentifier() {
		return id;
	}

	public final void setIdentifier(Serializable id) {
		this.id = id;
	}

	public final String getEntityName() {
		return entityName;
	}

	public final boolean isUninitialized() {
		return !initialized;
	}

	public final void initialize()  {
		if (!initialized) {
//			target = session.immediateLoad(entityName, id);
			initialized = true;
		}
		
	}
	
	public final void setImplementation(Object target) {
		this.target = target;
		initialized = true;
	}

	/**
	 * Return the underlying persistent object, initializing if necessary
	 */
	public final Object getImplementation() {
		initialize();
		return target;
	}

	protected final Object getTarget() {
		return target;
	}

	public boolean isUnwrap() {
		return unwrap;
	}

	public void setUnwrap(boolean unwrap) {
		this.unwrap = unwrap;
	}

}

