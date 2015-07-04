package com.aplos.common.aql;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;

import com.aplos.common.utils.ApplicationUtil;

public class JavassistLazyInitializer extends BasicLazyInitializer implements MethodHandler {

	private static final MethodFilter FINALIZE_FILTER = new MethodFilter() {
		public boolean isHandled(Method m) {
			// skip finalize methods
			return !( m.getParameterTypes().length == 0 && m.getName().equals( "finalize" ) );
		}
	};

	private Class[] interfaces;
	private boolean constructed = false;

	private JavassistLazyInitializer(
			final String entityName,
	        final Class persistentClass,
	        final Class[] interfaces,
	        final Serializable id,
	        final Method getIdentifierMethod,
	        final Method setIdentifierMethod) {
		super( entityName, persistentClass, id, getIdentifierMethod, setIdentifierMethod );
		this.interfaces = interfaces;
	}

	public static AqlProxy getProxy(
			final String entityName,
	        final Class persistentClass,
	        final Class[] interfaces,
	        final Method getIdentifierMethod,
	        final Method setIdentifierMethod,
	        final Serializable id)  {
		// note: interface is assumed to already contain HibernateProxy.class
		try {
			final JavassistLazyInitializer instance = new JavassistLazyInitializer(
					entityName,
			        persistentClass,
			        interfaces,
			        id,
			        getIdentifierMethod,
			        setIdentifierMethod
			);
			ProxyFactory factory = new ProxyFactory();
			factory.setSuperclass( interfaces.length == 1 ? persistentClass : null );
			factory.setInterfaces( interfaces );
			factory.setFilter( FINALIZE_FILTER );
			Class cl = factory.createClass();
			final AqlProxy proxy = ( AqlProxy ) cl.newInstance();
			( ( ProxyObject ) proxy ).setHandler( instance );
			instance.constructed = true;
			return proxy;
		}
		catch ( Exception ex ) {
			ApplicationUtil.handleError( ex );
			return null;
		}
	}

	public static AqlProxy getProxy(
			final Class factory,
	        final String entityName,
	        final Class persistentClass,
	        final Class[] interfaces,
	        final Method getIdentifierMethod,
	        final Method setIdentifierMethod,
	        final Serializable id) {

		final JavassistLazyInitializer instance = new JavassistLazyInitializer(
				entityName,
		        persistentClass,
		        interfaces, id,
		        getIdentifierMethod,
		        setIdentifierMethod
		);

		final AqlProxy proxy;
		try {
			proxy = ( AqlProxy ) factory.newInstance();
		}
		catch ( Exception e ) {
			ApplicationUtil.handleError( e );
			return null;
		}
		( ( ProxyObject ) proxy ).setHandler( instance );
		instance.constructed = true;
		return proxy;
	}

	public static Class getProxyFactory(
			Class persistentClass,
	        Class[] interfaces) {
		// note: interfaces is assumed to already contain HibernateProxy.class

		try {
			ProxyFactory factory = new ProxyFactory();
			factory.setSuperclass( interfaces.length == 1 ? persistentClass : null );
			factory.setInterfaces( interfaces );
			factory.setFilter( FINALIZE_FILTER );
			return factory.createClass();
		}
		catch ( Exception ex ) {
			ApplicationUtil.handleError( ex );
			return null;
		}
	}

	public Object invoke(
			final Object proxy,
			final Method thisMethod,
			final Method proceed,
			final Object[] args) throws Throwable {
		if ( this.constructed ) {
			Object result;
			try {
				result = this.invoke( thisMethod, args, proxy );
			}
			catch ( Throwable t ) {
				throw new Exception( t.getCause() );
			}
			if ( result == INVOKE_IMPLEMENTATION ) {
				Object target = getImplementation();
				final Object returnValue;
				try {
					if ( isPublic( persistentClass, thisMethod ) ) {
						if ( !thisMethod.getDeclaringClass().isInstance( target ) ) {
							throw new ClassCastException( target.getClass().getName() );
						}
						returnValue = thisMethod.invoke( target, args );
					}
					else {
						if ( !thisMethod.isAccessible() ) {
							thisMethod.setAccessible( true );
						}
						returnValue = thisMethod.invoke( target, args );
					}
					return returnValue == target ? proxy : returnValue;
				}
				catch ( InvocationTargetException ite ) {
					throw ite.getTargetException();
				}
			}
			else {
				return result;
			}
		}
		else {
			// while constructor is running
			if ( thisMethod.getName().equals( "getHibernateLazyInitializer" ) ) {
				return this;
			}
			else {
				return proceed.invoke( proxy, args );
			}
		}
	}

	public static boolean isPublic(Class clazz, Member member) {
		return Modifier.isPublic( member.getModifiers() ) && Modifier.isPublic( clazz.getModifiers() );
	}
}

