package com.aplos.common.utils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.aplos.common.module.CommonConfiguration;
import com.sun.faces.facelets.tag.AbstractTagLibrary;

public final class UserLevelUtilLibrary extends AbstractTagLibrary {

    public static final String NAMESPACE = "http://www.aplossystems.co.uk/userLevelUtil";

    /**  Current instance of library. */
    public static final UserLevelUtilLibrary INSTANCE = new UserLevelUtilLibrary();
    private boolean libraryCreated = false;

    public UserLevelUtilLibrary() {
        super(NAMESPACE);
    	if( !libraryCreated ) {
            try {
                Method[] methods = UserLevelUtil.class.getMethods();

                for (int i = 0; i < methods.length; i++) {
                    if (Modifier.isStatic(methods[i].getModifiers())) {
                        this.addFunction(methods[i].getName(), methods[i]);
                    }
                }
                libraryCreated = true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
    	}
    }

    @Override
    public Method createFunction(String ns, String name) {
    	if( !libraryCreated ) {
            try {
                Method[] methods = CommonConfiguration.retrieveUserLevelUtil().getClass().getMethods();

                for (int i = 0; i < methods.length; i++) {
                    if (Modifier.isStatic(methods[i].getModifiers())) {
                        this.addFunction(methods[i].getName(), methods[i]);
                    }
                }
                libraryCreated = true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
    	}
    	return super.createFunction(ns, name);
    }
}