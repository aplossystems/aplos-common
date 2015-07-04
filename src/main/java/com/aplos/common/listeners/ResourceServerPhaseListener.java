package com.aplos.common.listeners;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.net.URL;
import java.nio.charset.Charset;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.ClientAbortException;
import org.apache.log4j.Logger;

import com.aplos.common.filters.ResourceFilter;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

public class ResourceServerPhaseListener implements PhaseListener {
	private static final long serialVersionUID = 1484645282270137671L;
	public static final String RESOURCES_PATH = "resources";
	private Logger logger = Logger.getLogger(getClass());

    public ResourceServerPhaseListener() {
        super();
    }

    @Override
	public PhaseId getPhaseId() {
        return PhaseId.RESTORE_VIEW;
    }

    @Override
	public void afterPhase(PhaseEvent event) {

        // If this is restoreView phase
        if (PhaseId.RESTORE_VIEW == event.getPhaseId()) {
        	if (event.getFacesContext().getViewRoot() == null) {
				return;
			}

        	String viewId = event.getFacesContext().getViewRoot().getViewId();
        	if( !viewId.startsWith( "/" ) ) {
        		viewId = "/" + viewId;
        	}
        	if ( viewId.toLowerCase().endsWith(".jpg") || viewId.toLowerCase().endsWith(".gif") || viewId.toLowerCase().endsWith(".png") ||
        			viewId.toLowerCase().endsWith( ".js" ) || viewId.toLowerCase().endsWith( ".css" ) || viewId.toLowerCase().endsWith( ".jar" ) || viewId.toLowerCase().endsWith( ".xml" )) {
              // write the file

              ServletContext servletContext = (ServletContext)FacesContext.getCurrentInstance().getExternalContext().getContext();
              HttpServletResponse response = (HttpServletResponse)event.getFacesContext().getExternalContext().getResponse();
              HttpServletRequest request = (HttpServletRequest)event.getFacesContext().getExternalContext().getRequest();
      		  String contextPath = request.getContextPath();
              ResourceFilter.writeFile(response, viewId, contextPath, logger);
              event.getFacesContext().responseComplete();
            }
        }
    }

    @Override
	public void beforePhase(PhaseEvent event) {
    }

}

