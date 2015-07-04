package com.aplos.common.application;

import javax.faces.application.Resource;
import javax.faces.application.ResourceWrapper;

import org.apache.log4j.Logger;

public class AplosVersionedResource extends ResourceWrapper {

    private Resource resource;
	private Logger logger = Logger.getLogger(getClass());
	private String version;

    public AplosVersionedResource(Resource resource, String version) {
        this.setResource(resource);
        setVersion( version );
    }

    @Override
    public Resource getWrapped() {
        return this.getResource();
    }

    @Override
    public String getRequestPath() {
		 if( getVersion() != null ) {
			 StringBuffer strBuf = new StringBuffer( super.getRequestPath() );
			 strBuf.append( "&amp;v=" ).append( getVersion() );
			 return strBuf.toString();
		 } else {
			 return super.getRequestPath();
		 }
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

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Resource getResource() {
		return resource;
	}

	public void setResource(Resource resource) {
		this.resource = resource;
	}

}
