package com.aplos.common.annotations.persistence;
//$Id: AnyMetaDef.java 14736 2008-06-04 14:23:42Z hardy.ferentschik $

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
* Defines @Any and @manyToAny metadata
*
* @author Emmanuel Bernard
*/
@java.lang.annotation.Target( { PACKAGE, TYPE, METHOD, FIELD } )
@Retention( RUNTIME )
public @interface AnyMetaDef {
	/**
	 * If defined, assign a global meta definition name to be used in an @Any or @ManyToAny annotation
	 * If not defined, the metadata applies to the current property or field
	 */
	String name() default "";

	/**
	 * meta discriminator Hibernate type
	 */
	String metaType();

	/**
	 * Hibernate type of the id column
	 * @return
	 */
	String idType();

	/**
	 * Matching discriminator values with their respective entity
	 */
	MetaValue[] metaValues();
}

