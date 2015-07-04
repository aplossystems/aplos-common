package com.aplos.common.annotations.persistence;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * Parameter (basically key/value pattern)
 *
 * @author Emmanuel Bernard
 */
@Target({})
@Retention(RUNTIME)
public @interface Parameter {
	String name();

	String value();
}

