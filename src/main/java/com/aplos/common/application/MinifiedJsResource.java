package com.aplos.common.application;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import javax.faces.application.Resource;
import javax.faces.application.ResourceWrapper;

import org.apache.log4j.Logger;

import com.aplos.common.utils.ApplicationUtil;

public class MinifiedJsResource extends ResourceWrapper {
    private Resource resource;
	private Logger logger = Logger.getLogger(getClass());
	private PriorityFutureTask<File> minifyTask;

    public MinifiedJsResource(Resource resource, PriorityFutureTask<File> minifyTask) {
        this.setResource(resource);
        setMinifyTask(minifyTask);
    }
    
    @Override
    public InputStream getInputStream() throws IOException {
    	if( minifyTask != null && minifyTask.isDone() ) {
    		try {
    			File file = minifyTask.get();
    			if( file != null && file.exists() ) {
    				return new FileInputStream( file );
    			}
    		} catch( ExecutionException eex ) {
    			ApplicationUtil.handleError( eex, false );
    		} catch( InterruptedException iex ) {
    			ApplicationUtil.handleError( iex, false );
    		}
    	}
    	return super.getInputStream();
    }

    @Override
    public Resource getWrapped() {
        return this.getResource();
    }

    @Override
    public String getContentType() {
        return getWrapped().getContentType();
    }

    @Override
    public String getLibraryName() {
        return getWrapped().getLibraryName();
    }

    @Override
    public String getResourceName() {
        return getWrapped().getResourceName();
    }

    @Override
    public void setContentType(String contentType) {
        getWrapped().setContentType(contentType);
    }

    @Override
    public void setLibraryName(String libraryName) {
        getWrapped().setLibraryName(libraryName);
    }

    @Override
    public void setResourceName(String resourceName) {
        getWrapped().setResourceName(resourceName);
    }

    @Override
    public String toString() {
        return getWrapped().toString();
    }

	public Resource getResource() {
		return resource;
	}

	public void setResource(Resource resource) {
		this.resource = resource;
	}

	private PriorityFutureTask<File> getMinifyTask() {
		return minifyTask;
	}

	private void setMinifyTask(PriorityFutureTask<File> minifyTask) {
		this.minifyTask = minifyTask;
	}

}
