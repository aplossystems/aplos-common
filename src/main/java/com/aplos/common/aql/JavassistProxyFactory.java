package com.aplos.common.aql;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Set;

public class JavassistProxyFactory implements Serializable {
	private static final long serialVersionUID = -4225291927928906456L;
	
	protected static final Class[] NO_CLASSES = new Class[0];
	private Class persistentClass;
	private String entityName;
	private Class[] interfaces;
	private Method getIdentifierMethod;
	private Method setIdentifierMethod;
	private Class factory;

	public void postInstantiate(
			final String entityName,
			final Class persistentClass,
	        final Set interfaces,
			final Method getIdentifierMethod,
	        final Method setIdentifierMethod) throws Exception {
		this.entityName = entityName;
		this.persistentClass = persistentClass;
		this.interfaces = (Class[]) interfaces.toArray(NO_CLASSES);
		this.getIdentifierMethod = getIdentifierMethod;
		this.setIdentifierMethod = setIdentifierMethod;
		factory = JavassistLazyInitializer.getProxyFactory( persistentClass, this.interfaces );
	}

	public AqlProxy getProxy(Serializable id) {
		return JavassistLazyInitializer.getProxy(
				factory,
		        entityName,
				persistentClass,
		        interfaces,
		        getIdentifierMethod,
				setIdentifierMethod,
		        id
		);
	}

}
