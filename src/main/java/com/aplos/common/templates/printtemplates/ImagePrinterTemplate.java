package com.aplos.common.templates.printtemplates;

import java.util.Map;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.enums.CommonWorkingDirectory;
import com.aplos.common.interfaces.AplosWorkingDirectoryInter;
import com.aplos.common.interfaces.ImagePrinter;
import com.aplos.common.templates.PrintTemplate;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.JSFUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

public class ImagePrinterTemplate extends PrintTemplate {
	private static final long serialVersionUID = 2068365414744587282L;
	private ImagePrinter imagePrinter;
	
	@Override
	public String getName() {
		return "Image Printer";
	}

	@Override
	public void initialise( Map<String,String[]> params ) {
		String imagePrinterClass = params.get( "imagePrinterClass" )[ 0 ];
		int imagePrinterId = Integer.valueOf( params.get( "imagePrinterId" )[ 0 ] );
		try {
			setImagePrinter( (ImagePrinter) new BeanDao( (Class<? extends AplosAbstractBean>) Class.forName( imagePrinterClass ) ).get( imagePrinterId ) );
		} catch( ClassNotFoundException cnfex ) {
			ApplicationUtil.getAplosContextListener().handleError( cnfex );
		}
	}

	public String getTemplateContent() {
		try {
	        Document document = new Document( PageSize.A4, 0, 0, 0, 0);

            PdfWriter writer = PdfWriter.getInstance(document, getResponse().getOutputStream());
            document.open();
            PdfContentByte cb = writer.getDirectContent();


    		String serverUrl = ApplicationUtil.getAplosContextListener().getServerUrl();
    		if( serverUrl.endsWith( "/" ) ) {
    			serverUrl = serverUrl.substring( 0, serverUrl.length() - 1 );
    		}
    		serverUrl = serverUrl + JSFUtil.getContextPath();
            
            Image image = Image.getInstance(getImagePrinter().getImageForPrinting( serverUrl, "rotate=-90&maxWidth=842" ), null);
            image.setAbsolutePosition( 0, 0 );
			cb.addImage(image);

	        document.close();

			return null;
		}
		catch( Exception e ) {
			ApplicationUtil.getAplosContextListener().handleError( e );
		}
		return null;
	}
	
	@Override
	public AplosWorkingDirectoryInter getAplosWorkingDirectoryInter() {
		return CommonWorkingDirectory.IMAGE_PRINTER;
	}

	public ImagePrinter getImagePrinter() {
		return imagePrinter;
	}

	public void setImagePrinter(ImagePrinter imagePrinter) {
		this.imagePrinter = imagePrinter;
	}
}
