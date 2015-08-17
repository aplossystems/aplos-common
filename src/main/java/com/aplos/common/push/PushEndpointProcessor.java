package com.aplos.common.push;

import java.util.ArrayList;
import java.util.List;

import org.atmosphere.annotation.Processor;
import org.atmosphere.config.AtmosphereAnnotation;
import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereHandler;
import org.atmosphere.cpr.AtmosphereInterceptor;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.util.IOUtils;

import com.aplos.common.utils.ApplicationUtil;

//@AtmosphereAnnotation(PushEndpoint.class)
public class PushEndpointProcessor implements Processor<Object> {

    //@Override
    public void handle(AtmosphereFramework framework, Class<Object> annotatedClass) {
//        try {
//            Class<?> aClass = annotatedClass;
//            PushEndpoint a = aClass.getAnnotation(PushEndpoint.class);
//            List<AtmosphereInterceptor> l = new ArrayList<AtmosphereInterceptor>();
//
//            Object c = framework.newClassInstance(Object.class, aClass);
//            AtmosphereHandler handler = framework.newClassInstance(PushEndpointHandlerProxy.class, PushEndpointHandlerProxy.class).configure(framework.getAtmosphereConfig(), c);
//            l.add(framework.newClassInstance(AtmosphereInterceptor.class, PushEndpointInterceptor.class));
//
//            Class<? extends Broadcaster> b = (Class<? extends Broadcaster>) IOUtils.loadClass(this.getClass(), framework.getDefaultBroadcasterClassName());
//
//            framework.addAtmosphereHandler(a.value(), handler, broadcaster(framework, b, a.value()), l);
//        } catch (Throwable e) {
//            ApplicationUtil.handleError(e);
//        }
    }

}
