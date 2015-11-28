package com.aplos.common.beans;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Date;

import org.xhtmlrenderer.pdf.ITextRenderer;

import antlr.StringUtils;

import com.aplos.common.annotations.DynamicMetaValues;
import com.aplos.common.annotations.PluralDisplayName;
import com.aplos.common.annotations.persistence.Any;
import com.aplos.common.annotations.persistence.AnyMetaDef;
import com.aplos.common.annotations.persistence.Cascade;
import com.aplos.common.annotations.persistence.CascadeType;
import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.JoinColumn;
import com.aplos.common.templates.PrintTemplate;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.FormatUtil;
import com.aplos.common.utils.XmlEntityUtil;
import com.aplos.common.utils.XmlEntityUtil.EncodingType;

@Entity
@PluralDisplayName(name="created templates")
public class CreatedPrintTemplate extends FileDetails {
	private static final long serialVersionUID = -4487615806016031773L;

	@Any( metaColumn = @Column( name = "printTemplate_type" ) )
    @AnyMetaDef( idType = "long", metaType = "string", metaValues = {
    		/* Meta Values added in at run-time */
    } )
    @JoinColumn(name="printTemplate_id")
	@DynamicMetaValues
	@Cascade({CascadeType.ALL})
	private PrintTemplate printTemplate;
	
	public CreatedPrintTemplate() {
	}
	
	public CreatedPrintTemplate( Class<? extends PrintTemplate> templateClass ) {
		PrintTemplate printTemplate = ((PrintTemplate) CommonUtil.getNewInstance( templateClass, null ));
		init( printTemplate );
	}
	
	public CreatedPrintTemplate( PrintTemplate printTemplate ) {
		init( printTemplate );
	}
	
	public void init(PrintTemplate printTemplate ) {
		setPrintTemplate( printTemplate );
		setName( getPrintTemplate().getName() ); 
		setFileDetailsOwner( getPrintTemplate().getAplosWorkingDirectoryInter().getAplosWorkingDirectory() );
	}
	
	public CreatedPrintTemplate generateAndSavePDFFile() {
		try {
			String directoryPath = determineFileDetailsDirectory(true); 
			String templateContent = getPrintTemplate().getTemplateContent();
			String outputFile = (directoryPath + getPrintTemplate().getName()).trim() + " - ";
			String nameEnd = FormatUtil.formatDate( new Date() );
			if (new File(outputFile + nameEnd + ".pdf" ).exists()) {
				int i = 2;
				while( new File(outputFile + nameEnd + "_" + i + ".pdf" ).exists()) {
					i++;
				}
				outputFile += nameEnd + "_" + i + ".pdf";
			}
			else {
				outputFile += nameEnd + ".pdf";
			}
		
			new File(directoryPath).mkdirs();
			OutputStream os = new FileOutputStream(outputFile);
			templateContent = XmlEntityUtil.replaceCharactersWith(templateContent, XmlEntityUtil.EncodingType.ENTITY);
	        ITextRenderer renderer = new ITextRenderer();

	        ApplicationUtil.getAplosContextListener().addFonts( renderer );
	        
	        renderer.setDocumentFromString(templateContent);
	        renderer.layout();
	        renderer.createPDF(os, true);
	        os.close();
	        File pdfFile = new File(outputFile);
			setFilename( pdfFile.getName() );
			saveDetails();
			return this;
		} catch (Exception e) {
			ApplicationUtil.getAplosContextListener().handleError( e );
		}
		return null;
	}

	public PrintTemplate getPrintTemplate() {
		return printTemplate;
	}

	public void setPrintTemplate(PrintTemplate printTemplate) {
		this.printTemplate = printTemplate;
	}
}
