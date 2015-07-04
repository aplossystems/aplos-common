package com.aplos.common.aql;

import java.io.Serializable;
import java.lang.reflect.Method;

import com.aplos.common.utils.ApplicationUtil;

/**
 * Lazy initializer for POJOs
 * 
 * @author Gavin King
 */
public abstract class BasicLazyInitializer extends AbstractLazyInitializer {

	protected static final Object INVOKE_IMPLEMENTATION = new MarkerObject("INVOKE_IMPLEMENTATION");

	protected Class persistentClass;
	protected Method getIdentifierMethod;
	protected Method setIdentifierMethod;
	protected boolean overridesEquals;
	private Object replacement;
	private static final Class[] OBJECT = new Class[] { Object.class };
	private static final Method OBJECT_EQUALS;
	
	static {
		Method eq = null;
		Method hash;
		try {
			eq = Object.class.getMethod("equals", OBJECT);
		} catch (Exception e) {
			ApplicationUtil.handleError(new Exception("Could not find Object.equals() or Object.hashCode()", e));
		}
		OBJECT_EQUALS = eq;
	}

	protected BasicLazyInitializer(
			String entityName,
	        Class persistentClass,
	        Serializable id,
	        Method getIdentifierMethod,
	        Method setIdentifierMethod ) {
		super(entityName, id);
		this.persistentClass = persistentClass;
		this.getIdentifierMethod = getIdentifierMethod;
		this.setIdentifierMethod = setIdentifierMethod;
		overridesEquals = overridesEquals(persistentClass);
	}

	public static boolean overridesEquals(Class clazz) {
		Method equals;
		try {
			equals = clazz.getMethod("equals", OBJECT);
		}
		catch (NoSuchMethodException nsme) {
			return false; //its an interface so we can't really tell anything...
		}
		return !OBJECT_EQUALS.equals(equals);
	}

	protected final Object invoke(Method method, Object[] args, Object proxy) throws Throwable {

		String methodName = method.getName();
		int params = args.length;

		if ( params==0 ) {

			if ( "writeReplace".equals(methodName) ) {
				return getReplacement();
			}
			else if ( !overridesEquals && "hashCode".equals(methodName) ) {
				return new Integer( System.identityHashCode(proxy) );
			}
			else if ( isUninitialized() && method.equals(getIdentifierMethod) ) {
				return getIdentifier();
			}

			else if ( "getHibernateLazyInitializer".equals(methodName) ) {
				return this;
			}

		}
		else if ( params==1 ) {

			if ( !overridesEquals && "equals".equals(methodName) ) {
				return args[0]==proxy ? Boolean.TRUE : Boolean.FALSE;
			}
			else if ( method.equals(setIdentifierMethod) ) {
				initialize();
				setIdentifier( (Serializable) args[0] );
				return INVOKE_IMPLEMENTATION;
			}

		}

		// otherwise:
		return INVOKE_IMPLEMENTATION;

	}

	private Object getReplacement() {
//		if ( isUninitialized() && session != null && session.isOpen()) {
//			final EntityKey key = new EntityKey(
//					getIdentifier(),
//			        session.getFactory().getEntityPersister( getEntityName() ),
//			        session.getEntityMode()
//				);
//			final Object entity = session.getPersistenceContext().getEntity(key);
//			if (entity!=null) setImplementation( entity );
//		}
//
//		if ( isUninitialized() ) {
//			if (replacement==null) {
//				replacement = serializableProxy();
//			}
//			return replacement;
//		}
//		else {
//			return getTarget();
//		}
		return null;
	}

	public final Class getPersistentClass() {
		return persistentClass;
	}

}

