package com.aplos.common.annotations.persistence;

//$Id: Type.java 14736 2008-06-04 14:23:42Z hardy.ferentschik $

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
* hibernate type
*
* @author Emmanuel Bernard
*/
@Target({FIELD, METHOD})
@Retention(RUNTIME)
public @interface Type {
	String type();

	Parameter[] parameters() default {};
}
