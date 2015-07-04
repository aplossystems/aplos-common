package com.aplos.common.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.aplos.common.enums.JsfScope;

@Target({TYPE})
@Retention(RUNTIME)
public @interface BeanScope {
	public JsfScope scope() default JsfScope.NOT_SELECTED;
}
