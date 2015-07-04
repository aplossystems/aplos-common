package com.aplos.common.application;

import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.application.ResourceHandlerWrapper;


public class AplosResourceHandler extends ResourceHandlerWrapper {

	// Constants ------------------------------------------------------------------------------------------------------

	/** The default library name of a combined resource. Make sure that this is never used for other libraries. */
	public static final String LIBRARY_NAME = "aplos.combinedresources";


	private static final String TARGET_HEAD = "head";
	private static final String TARGET_BODY = "body";

	// Properties -----------------------------------------------------------------------------------------------------

	private ResourceHandler wrapped;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Creates a new instance of this combined resource handler which wraps the given resource handler. This will also
	 * register this resource handler as a pre render view event listener, so that it can do the job of removing the
	 * CSS/JS resources and adding combined ones.
	 * @param wrapped The resource handler to be wrapped.
	 */
	public AplosResourceHandler(ResourceHandler wrapped) {
		this.wrapped = wrapped;
	}

    @Override
    public Resource createResource(String resourceName, String libraryName) {
        Resource resource = super.createResource(resourceName, libraryName);

        if(resource != null && libraryName != null ) {
        	if( libraryName.equalsIgnoreCase("styles") ) {
        		if( resourceName.equalsIgnoreCase("common.css") ) {
            		return new AplosCssResource(resource, "1");
        		} else if( resourceName.equalsIgnoreCase("modern.css") ) {
            		return new AplosCssResource(resource, "1");
        		} else {
        			return new AplosCssResource(resource, null);
        		}
        	} else if( resourceName.equalsIgnoreCase("aploscommon.js") ) {
        		return new AplosVersionedResource(resource, "1" );
        	} else if( resourceName.equalsIgnoreCase("ckeditor/aplosckeditor.js") ) {
        		return new AplosVersionedResource(resource, "1" );
        	} else if( resourceName.equalsIgnoreCase("components.js") ) {
        		return new AplosVersionedResource(resource, "1" );
        	} else if( resourceName.equalsIgnoreCase("prettyphoto/css/prettyPhoto.css") ) {
        		return new AplosCssResource(resource, null );
        	}
        } 
        
        return resource;
    }

    @Override
    public ResourceHandler getWrapped() {
        return this.wrapped;
    }

}
