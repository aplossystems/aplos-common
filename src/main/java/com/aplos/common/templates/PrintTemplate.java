package com.aplos.common.templates;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.xhtmlrenderer.pdf.ITextRenderer;

import com.aplos.common.AplosUrl;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.MappedSuperclass;
import com.aplos.common.annotations.persistence.Transient;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.CreatedPrintTemplate;
import com.aplos.common.enums.CommonWorkingDirectory;
import com.aplos.common.interfaces.AplosWorkingDirectoryInter;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.FormatUtil;
import com.aplos.common.utils.JSFUtil;
import com.aplos.common.utils.XmlEntityUtil;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.BaseFont;

@MappedSuperclass
public abstract class PrintTemplate extends AplosBean {
	private static final long serialVersionUID = -2720509030225899613L;
	@Transient
	private HttpServletResponse response;
	@Transient
	private HttpServletRequest request;

	public static String printTemplatePath = "resources/templates/printtemplates/";

	public String getTemplateContent() {
		return null;
	}
	
	public boolean isReadyToPrint() {
		return true;
	}
	
	public void initialise( HttpServletResponse response, HttpServletRequest request ) {
		Map<String, String[]> paramMap = request.getParameterMap();
		Iterator<String> keyIter = paramMap.keySet().iterator();
		String tempKey;
		String[] tempEntry;
		try {
			while( keyIter.hasNext() ) {
				tempKey = keyIter.next();
				tempEntry = paramMap.get( tempKey );
				for( int i = 0, n = tempEntry.length; i < n; i++ ) {
					tempEntry[ i ] = URLDecoder.decode( tempEntry[ i ], "UTF-8" );
				}
			}
		} catch( UnsupportedEncodingException unex ) {
			ApplicationUtil.getAplosContextListener().handleError(unex);
		}
		initialise( request.getParameterMap() );
		setResponse( response );
		setRequest( request );
	}
	
	public void initialise( Map<String,String[]> params ) {}
	
	public abstract String getName();
	
	public abstract AplosWorkingDirectoryInter getAplosWorkingDirectoryInter();

	public static AplosUrl getBaseTemplateUrl( Class<? extends PrintTemplate> printTemplateClass ) {
		AplosUrl aplosUrl = new AplosUrl( "/templateServlet/template.html" );
		aplosUrl.addQueryParameter( "templateClass", printTemplateClass.getName() );
		aplosUrl.addWindowId();
		return aplosUrl;
	}
	
	public static CreatedPrintTemplate generateAndSavePDFFile( PrintTemplate printTemplate ) {
		CreatedPrintTemplate createdPrintTemplate = new CreatedPrintTemplate();
		createdPrintTemplate.setPrintTemplate( printTemplate );
		createdPrintTemplate.setFileDetailsOwner(printTemplate.getAplosWorkingDirectoryInter().getAplosWorkingDirectory());
		createdPrintTemplate.generateAndSavePDFFile();
		return createdPrintTemplate;
	}
	
	public static void addFont( String fontFilename, ITextRenderer renderer ) throws IOException, DocumentException {
		File tmpFontFile = new File( CommonWorkingDirectory.PROCESSED_RESOURCES_DIR.getDirectoryPath(true) + "fonts/" + fontFilename ); //TODO: probably the wrong path
	    if (!tmpFontFile.exists()) {
	    	
	    	//this way works, only if we put the directory in the resources dir where faceletviews is
	    	//if we  use src/main/webapp/resources none of the paths work
	    	InputStream fontIs = JSFUtil.checkFileLocations(fontFilename, "resources/fonts/", true ).openStream();
//	    	InputStream fontIs = PrintTemplate.class.getResourceAsStream( "../resources/fonts/" + fontFilename   );   
	    	
	    	if (fontIs != null) {
		    	File tmpDirFile = new File( CommonWorkingDirectory.PROCESSED_RESOURCES_DIR.getDirectoryPath(true) + "fonts" );
		    	tmpDirFile.mkdirs();
		    	tmpFontFile.createNewFile();
		        OutputStream fontOs = new FileOutputStream( tmpFontFile );
		        byte buf[] = new byte[1024];
		        int len;
		        while ( ( len = fontIs.read( buf ) ) > 0 ) {
		    	   fontOs.write(buf,0,len);
		        }
		        fontOs.close();
		        fontIs.close();
	    	}
	    }
	    if (tmpFontFile.exists()) {
	    	renderer.getFontResolver().addFont(tmpFontFile.getAbsolutePath(), BaseFont.IDENTITY_H ,BaseFont.EMBEDDED);
	    }
	}

	public HttpServletResponse getResponse() {
		return response;
	}

	public void setResponse(HttpServletResponse response) {
		this.response = response;
	}

	public HttpServletRequest getRequest() {
		return request;
	}

	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}
}
