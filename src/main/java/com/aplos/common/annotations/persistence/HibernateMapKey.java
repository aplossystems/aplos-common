package com.aplos.common.annotations.persistence;

//$Id: MapKey.java 14736 2008-06-04 14:23:42Z hardy.ferentschik $

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.FIELD;

/**
* Define the map key columns as an explicit column holding the map key
* This is completly different from {@link javax.persistence.MapKey} which use an existing column
* This annotation and {@link javax.persistence.MapKey} are mutually exclusive
*
* @author Emmanuel Bernard
*/
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface HibernateMapKey {
	Column[] columns() default {};
	/**
	 * Represent the key class in a Map
	 * Only useful if the collection does not use generics
	 */
	Class targetElement() default void.class;

	/**
	 * The optional map key type. Guessed if default
	 */
	Type type() default @Type(type = ""); 
}

