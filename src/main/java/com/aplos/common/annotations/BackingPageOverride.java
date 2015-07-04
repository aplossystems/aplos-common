package com.aplos.common.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.aplos.common.backingpage.BackingPage;

@Target({TYPE})
@Retention(RUNTIME)
public @interface BackingPageOverride {
	public Class<? extends BackingPage> backingPageClass();
}
