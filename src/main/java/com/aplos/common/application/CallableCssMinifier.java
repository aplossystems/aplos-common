package com.aplos.common.application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;

import com.aplos.common.enums.CommonWorkingDirectory;
import com.yahoo.platform.yui.compressor.CssCompressor;

public class CallableCssMinifier implements Callable<File> {
	private String resourceName;
	private InputStream resourceInputStream;
	
	public CallableCssMinifier( String resourceName, InputStream resourceInputStream ) {
		this.resourceName = resourceName;
		this.resourceInputStream = resourceInputStream;
	}

	@Override
	public File call() throws Exception {
		Reader in = null;
	    Writer out = null;
	    File minifiedFile = null; 
	    try {
	    	File processedFile = MinifiedCssResource.getProcessedFile(resourceName, resourceInputStream);
	        CssCompressor compressor = new CssCompressor(new InputStreamReader(new FileInputStream(processedFile)));

			resourceName = resourceName.replace( "/", "_" );
			minifiedFile = new File(CommonWorkingDirectory.MINIFIED_CSS.getDirectoryPath(true) + resourceName );
	        out = new OutputStreamWriter(new FileOutputStream(minifiedFile));
	        compressor.compress(out, 0);
	    } finally {
	        IOUtils.closeQuietly(resourceInputStream);
	        IOUtils.closeQuietly(out);
	    }
	    return minifiedFile;
	}
}
