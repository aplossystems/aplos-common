package com.aplos.common.push;

import static org.atmosphere.annotation.AnnotationUtil.broadcaster;

import java.util.ArrayList;

import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereHandler;
import org.atmosphere.cpr.AtmosphereInterceptor;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.util.IOUtils;

import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.JSFUtil;

public class AtmosphereFrameworkUtil {

    private static AtmosphereFrameworkUtil atmosphereFrameworkUtil = null;
    private AtmosphereFramework atmosphereFramework;

    public AtmosphereFrameworkUtil( AtmosphereFramework atmosphereFramework ) {
    	atmosphereFrameworkUtil = this;
    	this.atmosphereFramework = atmosphereFramework;
    }
    
    public static AtmosphereFrameworkUtil getDefault() {
    	return atmosphereFrameworkUtil;
    }
    
    public void addHandler( String mapping, AtmosphereHandler atmosphereHandler ) {
    	mapping = "/aplospush" + mapping;
    	if( atmosphereFramework.getAtmosphereHandlers().get( mapping ) == null ) {
	        try {
		        Class<? extends Broadcaster> b = (Class<? extends Broadcaster>) IOUtils.loadClass(this.getClass(), atmosphereFramework.getDefaultBroadcasterClassName());
	//	        mapping = "/aplospush/calendar";
		        atmosphereFramework.addAtmosphereHandler(mapping, atmosphereHandler, broadcaster(atmosphereFramework, b, mapping), new ArrayList<AtmosphereInterceptor>());
	        } catch( Exception ex ) {
	        	//ApplicationUtil.handleError( ex );
	        }
    	}
    }
}
