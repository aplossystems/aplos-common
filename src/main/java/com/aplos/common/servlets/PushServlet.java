package com.aplos.common.servlets;

import static org.atmosphere.annotation.AnnotationUtil.broadcaster;

import java.util.ArrayList;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.atmosphere.client.TrackMessageSizeInterceptor;
import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereInterceptor;
import org.atmosphere.cpr.AtmosphereServlet;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.MetaBroadcaster;
import org.atmosphere.interceptor.AtmosphereResourceLifecycleInterceptor;
import org.atmosphere.util.IOUtils;

import com.aplos.common.push.AtmosphereFrameworkUtil;
import com.aplos.common.push.CalendarTest;
import com.aplos.common.push.EventBusFactory;
import com.aplos.common.utils.ApplicationUtil;

public class PushServlet extends AtmosphereServlet {
    private final Logger logger = Logger.getLogger(PushServlet.class.getName());

    /**
     * Create an Atmosphere Servlet.
     */
    public PushServlet() {
        this(false);
    }

    /**
     * Create an Atmosphere Servlet.
     *
     * @param isFilter true if this instance is used as an {@link org.atmosphere.cpr.AtmosphereFilter}
     */
    public PushServlet(boolean isFilter) {
        super(isFilter, true);
    }

    /**
     * Create an Atmosphere Servlet.
     *
     * @param isFilter           true if this instance is used as an {@link org.atmosphere.cpr.AtmosphereFilter}
     * @param autoDetectHandlers
     */
    public PushServlet(boolean isFilter, boolean autoDetectHandlers) {
        super(isFilter, autoDetectHandlers);
    }


    protected PushServlet configureFramework(ServletConfig sc) throws ServletException {
        super.configureFramework(sc, false);
        framework().interceptor(new AtmosphereResourceLifecycleInterceptor())
                .interceptor(new TrackMessageSizeInterceptor());
        

        framework().getAtmosphereConfig().startupHook(new AtmosphereConfig.StartupHook() {
            public void started(AtmosphereFramework framework) {
                configureMetaBroadcasterCache(framework);
            }
        });

        framework().init(sc);
        
        new AtmosphereFrameworkUtil( framework() );
        
        EventBusFactory f = new EventBusFactory(framework().getAtmosphereConfig().metaBroadcaster());
        framework().getAtmosphereConfig().properties().put("eventBus", f.getDefault());


        if (framework().getAtmosphereHandlers().size() == 0) {
            ApplicationUtil.handleError( new Exception("No Annotated class using @PushEndpoint found. Push will not work."), false );
        }
        return this;
    }

    protected void configureMetaBroadcasterCache(AtmosphereFramework framework){
        MetaBroadcaster m = framework.metaBroadcaster();
        m.cache(new MetaBroadcaster.ThirtySecondsCache(m, framework.getAtmosphereConfig()));
    }

}
