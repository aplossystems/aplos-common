package com.aplos.common.application;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.application.Resource;

import com.aplos.common.enums.CommonWorkingDirectory;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

public class AplosCssResource extends AplosVersionedResource {

    public AplosCssResource(Resource resource, String version ) {
        super( resource, version );
    }
    
    @Override
    public InputStream getInputStream() throws IOException {
    	String resourceName = getResourceName().replace( ".css", "_processed.css" );
    	resourceName = resourceName.substring( resourceName.lastIndexOf("/") + 1 );
    	File file = new File( CommonWorkingDirectory.PROCESSED_RESOURCES_DIR.getDirectoryPath(true) + resourceName );
    	if( !file.exists() || ApplicationUtil.getAplosContextListener().isDebugMode() ) {
    		String lineSep = System.getProperty("line.separator");
    		BufferedReader br = null;
     	    try {
     	    	br = new BufferedReader(new InputStreamReader( super.getInputStream() ));
     	    } catch (FileNotFoundException e2) {
     	    	e2.printStackTrace();
     	    } catch( IOException ioEx ) {
     	    	ioEx.printStackTrace();
     	    }
     	    
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
    	return new FileInputStream( file );
    }

    @Override
    public Resource getWrapped() {
        return getResource();
    }

    @Override
    public String getRequestPath() {
        return super.getRequestPath();
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

}
