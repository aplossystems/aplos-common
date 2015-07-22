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

import javax.faces.application.Resource;

import org.apache.commons.io.IOUtils;

import com.aplos.common.enums.CommonWorkingDirectory;
import com.yahoo.platform.yui.compressor.CssCompressor;

public class CallableCssMinifier implements Callable<File> {
	private String resourceName;
	private File processedFile;
	
	public CallableCssMinifier( String resourceName, File processedFile ) {
		this.resourceName = resourceName;
		this.processedFile = processedFile;
	}

	@Override
	public File call() throws Exception {
	    Writer out = null;
	    File minifiedFile = null; 
	    try {
			String processedResourceName = resourceName.replace( "/", "_" );
			minifiedFile = new File(CommonWorkingDirectory.MINIFIED_CSS.getDirectoryPath(true) + processedResourceName );
			if( !minifiedFile.exists() ) {
		        CssCompressor compressor = new CssCompressor(new InputStreamReader(new FileInputStream(processedFile)));
		        out = new OutputStreamWriter(new FileOutputStream(minifiedFile));
		        compressor.compress(out, 0);
			}
	    } finally {
	        IOUtils.closeQuietly(out);
	    }
	    return minifiedFile;
	}
}
