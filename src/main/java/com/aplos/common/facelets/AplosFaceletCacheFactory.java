package com.aplos.common.facelets;

import javax.faces.view.facelets.FaceletCache;

import com.aplos.common.utils.ApplicationUtil;
import com.sun.faces.config.WebConfiguration;
import com.sun.faces.facelets.impl.FaceletCacheFactoryImpl;

public class AplosFaceletCacheFactory extends FaceletCacheFactoryImpl {
	
	public AplosFaceletCacheFactory() {
		super();
		String refreshPeriod = String.valueOf( ApplicationUtil.getAplosContextListener().getFaceletRefreshPeriod() );
		WebConfiguration.getInstance().overrideContextInitParameter(WebConfiguration.WebContextInitParameter.FaceletsDefaultRefreshPeriod, refreshPeriod);
	}


    @Override
    public FaceletCache getFaceletCache() {
        WebConfiguration webConfig = WebConfiguration.getInstance();
        String refreshPeriod = webConfig.getOptionValue(WebConfiguration.WebContextInitParameter.FaceletsDefaultRefreshPeriod);
        long period = Long.parseLong(refreshPeriod) * 1000;
        AplosFaceletCache aplosFaceletCache = new AplosFaceletCache(period);
        ApplicationUtil.getAplosContextListener().setAplosFaceletCache( aplosFaceletCache );
        return aplosFaceletCache;

    }
}
