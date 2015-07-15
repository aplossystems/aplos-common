package com.aplos.common.application;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;

import javax.faces.application.Resource;
import javax.faces.application.ResourceWrapper;

import org.apache.log4j.Logger;

import com.aplos.common.enums.CommonWorkingDirectory;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

public class MinifiedCssResource extends ResourceWrapper {
    private Resource resource;
	private Logger logger = Logger.getLogger(getClass());
	private PriorityFutureTask<File> minifyTask;

    public MinifiedCssResource(Resource resource, PriorityFutureTask<File> minifyTask) {
        this.setResource(resource);
        setMinifyTask(minifyTask);
    }
    
    @Override
    public InputStream getInputStream() throws IOException {
    	if( minifyTask != null && minifyTask.isDone()  ) {
    		try {
    			File file = minifyTask.get();
    			if( file != null ) {
    				return new FileInputStream( file );
    			}
    		} catch( ExecutionException eex ) {
    			ApplicationUtil.handleError( eex, false );
    		} catch( InterruptedException iex ) {
    			ApplicationUtil.handleError( iex, false );
    		}
    	} 
        
    	return new FileInputStream( getProcessedFile(getResourceName(), super.getInputStream()) );
    }
    
    public synchronized static File getProcessedFile( String resourceName, InputStream resourceInputStream ) throws IOException {
    	resourceName = resourceName.replace( ".css", "_processed.css" );
    	resourceName = resourceName.substring( resourceName.lastIndexOf("/") + 1 );
    	File file = new File( CommonWorkingDirectory.PROCESSED_RESOURCES_DIR.getDirectoryPath(true) + resourceName );
    	if( !file.exists() || ApplicationUtil.getAplosContextListener().isDebugMode() ) {
    		String lineSep = System.getProperty("line.separator");
    		BufferedReader br = null;
     	    br = new BufferedReader(new InputStreamReader( resourceInputStream ));
     	    
     	   String nextLine = "";
     	   StringBuffer sb = new StringBuffer();
     	   try {
     		   while ((nextLine = br.readLine()) != null) {
     			   sb.append(nextLine);
     			   sb.append(lineSep);
 			   }
     	   } catch (IOException e1) {
     		   e1.printStackTrace();
     	   }
     	   String cssContent = sb.toString();
     	   String newCssContent = CommonUtil.includeContextRootInPathsForCss(cssContent, JSFUtil.getContextPath());
     	   FileOutputStream fileOutputStream = new FileOutputStream(file);
     	   fileOutputStream.write( newCssContent.getBytes() );
     	   fileOutputStream.close();
    	}
    	return file;
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
