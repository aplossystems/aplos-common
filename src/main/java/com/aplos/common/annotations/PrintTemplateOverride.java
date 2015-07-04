package com.aplos.common.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.aplos.common.templates.PrintTemplate;

@Target({TYPE})
@Retention(RUNTIME)
public @interface PrintTemplateOverride {
	public Class<? extends PrintTemplate> templateClass();
}
