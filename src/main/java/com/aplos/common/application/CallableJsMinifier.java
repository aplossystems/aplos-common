package com.aplos.common.application;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;

import com.aplos.common.compression.uglify.UglifyJs;
import com.aplos.common.enums.CommonWorkingDirectory;
import com.aplos.common.utils.CommonUtil;

public class CallableJsMinifier implements Callable<File> {
	private String resourceName;
	private InputStream resourceInputStream;
	
	public CallableJsMinifier( String resourceName, InputStream resourceInputStream ) {
		this.resourceName = resourceName;
		this.resourceInputStream = resourceInputStream;
	}

	@Override
	public File call() throws Exception {
		resourceName = resourceName.replace( "/", "_" );
		File minifiedFile = new File(CommonWorkingDirectory.MINIFIED_JS.getDirectoryPath(true) + resourceName );
		if( !minifiedFile.exists() ) {
			try {
				FileOutputStream fileOutputStream = new FileOutputStream(minifiedFile);
				IOUtils.copy(resourceInputStream, fileOutputStream);
				resourceInputStream.close();
				fileOutputStream.close();
				UglifyJs uglifyJs = new UglifyJs();
				String[] args = new String[] { "-nc", minifiedFile.getAbsolutePath() };
				String result = uglifyJs.uglify( args );
				CommonUtil.writeStringToFile( result, minifiedFile, true, false );
			} catch( Exception ex ) {
				throw ex;
			} finally {
				minifiedFile.delete();
			}
		}
		return minifiedFile;
	}
}
