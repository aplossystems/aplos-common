package com.aplos.common.annotations.persistence;
//$Id: Any.java 14736 2008-06-04 14:23:42Z hardy.ferentschik $

import static com.aplos.common.annotations.persistence.FetchType.EAGER;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;

/**
* Define a ToOne association pointing to several entity types.
* Matching the according entity type is doe through a metadata discriminator column
* This kind of mapping should be only marginal.
* 
* @author Emmanuel Bernard
*/
@java.lang.annotation.Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface Any {
	/**
	 * Metadata definition used.
	 * If defined, should point to a @AnyMetaDef name
	 * If not defined, the local (ie in the same field or property) @AnyMetaDef is used
	 */
	String metaDef() default "";

	/**
	 * Metadata discriminator column description, This column will hold the meta value corresponding to the
	 * targeted entity.
	 */
	Column metaColumn();
	/**
	 * Defines whether the value of the field or property should be lazily loaded or must be
	 * eagerly fetched. The EAGER strategy is a requirement on the persistence provider runtime
	 * that the value must be eagerly fetched. The LAZY strategy is applied when bytecode
	 * enhancement is used. If not specified, defaults to EAGER.
	 */
	FetchType fetch() default EAGER;
	/**
	 * Whether the association is optional. If set to false then a non-null relationship must always exist.
	 */
	boolean optional() default true;
}

