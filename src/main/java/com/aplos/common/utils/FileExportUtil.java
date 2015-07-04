package com.aplos.common.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.InputMismatchException;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import com.aplos.common.enums.DocumentType;

@ManagedBean
public class FileExportUtil {

	public FileExportUtil() {	}

	public static String generateCsvFile(String filename, List<String[]> rows) {
		return generateCsvFile(filename, new String[rows.get(0).length], rows, true);
	}

	public static String generateCsvFile(String filename, String[] columns, List<String[]> rows) {
		return generateCsvFile(filename, columns, rows, false);
	}

	public static String generateCsvFile(String filename, String[] columns, List<String[]> rows, Boolean suppressHeaders) {
		try {
		    FileWriter writer = new FileWriter(filename);
		    if (!suppressHeaders) {
			    for (int i=0; i < columns.length; i++) {
			    	writer.append(columns[i]);
			    	if (i != columns.length-1) {
			    		writer.append(',');
			    	}
			    }
			    writer.append('\n');
		    }
		    for (int j=0; j < rows.size(); j++) {
		    	String[] row = rows.get(j);
		    	if (row.length != columns.length) {
		    		throw new InputMismatchException("Value count does not match header count at row " + j);
		    	}
		    	for (int k=0; k < row.length; k++) {
			    	writer.append(row[k]);
			    	if (k != row.length-1) {
			    		writer.append(',');
			    	}
			    }
		    	if (j != rows.size()-1) {
		    		writer.append('\n');
		    	}
		    }
		    writer.flush();
		    writer.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
		return filename; //so we can use it with download file below
	}

	public static String generateXlsFile(String filename, List<String[]> rows) {
		return generateXlsFile(filename, new String[rows.get(0).length], rows, true);
	}

	public static String generateXlsFile(String filename, String[] columns, List<String[]> rows) {
		return generateXlsFile(filename, columns, rows, false);
	}

	public static String generateXlsFile(String filename, String[] columns, List<String[]> rows, Boolean suppressHeaders) {
		//this method doesnt appear to let use output in Arabic even if we wrap it with a buffered reader and
		//outputstreamwriter which let us specify charset encoding
		try {

			HSSFWorkbook ss = new HSSFWorkbook();
			HSSFSheet sheet = ss.createSheet("Exported Data");
			FileOutputStream fos =  new FileOutputStream(filename);
			int diff=0;
			if (!suppressHeaders) {
				diff=1;
				HSSFRow row = sheet.createRow(0);
			    for (int i=0; i < columns.length; i++) {
			    	row.createCell(i).setCellValue(columns[i]);
			    }
		    }
			for (int j=0; j < rows.size(); j++) {
				HSSFRow row = sheet.createRow(j + diff);
		    	String[] rowData = rows.get(j);
//		    	if (rowData.length != columns.length) {
//		    		throw new InputMismatchException("Value count does not match header count at row " + j);
//		    	}
		    	for (int k=0; k < rowData.length; k++) {
		    		row.createCell(k).setCellValue(rowData[k]);
			    }
		    }
			ss.write(fos);
		} catch(IOException e) {
			e.printStackTrace();
		}
		return filename; //so we can use it with download file below

	}

	//this was used in xsl originally but may have other applications
	public static String generateHtmlTableFile(String filename, String[] columns, List<String[]> rows, Boolean suppressHeaders) {

		try {
		    OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(filename),"UTF8");
		    writer.append("<table>");
		    if (!suppressHeaders) {
		    	writer.append("<tr>");
			    for (int i=0; i < columns.length; i++) {
			    	writer.append("<td>");
			    	writer.append(columns[i]);
			    	writer.append("</td>");
			    }
			    writer.append("</tr>");
		    }
		    for (int j=0; j < rows.size(); j++) {
		    	String[] row = rows.get(j);
		    	if (row.length != columns.length) {
		    		throw new InputMismatchException("Value count does not match header count at row " + j);
		    	}
		    	writer.append("<tr>");
		    	for (int k=0; k < row.length; k++) {
		    		writer.append("<td>");
			    	writer.append(row[k]);
			    	writer.append("</td>");
			    }
		    	writer.append("</tr>");
		    }
		    writer.append("</table>");
		    writer.flush();
		    writer.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
		return filename; //so we can use it with download file below

	}

	public static void fileDownload(String filelocation, DocumentType docType) {
		String redirectUrl;
		if (docType == null) {
			redirectUrl = JSFUtil.getContextPath() + "/documentservlet?type=application/octet-stream&file=" + filelocation;
		} else {
			redirectUrl = JSFUtil.getContextPath() + "/documentservlet?type=" + docType.name() + "&file=" + filelocation;
		}
		try {
			JSFUtil.getResponse().sendRedirect( redirectUrl );
		} catch( IOException ioex ) {
			ioex.printStackTrace();
		}
	}

	public static void createDirectoryStructure(String path) {
		while (path.startsWith("/")) {
			path = path.substring(1);
		}
		String[] layers = path.split("/");
		String prefix = "/";
		for (int i=0; i < layers.length; i++) {
			File currentLayer = new File(prefix + layers[i]);
			if (!currentLayer.exists()) {
				currentLayer.mkdir();
			} else if (!currentLayer.isDirectory()) {
				JSFUtil.addMessage("Directory structure could not be created - " + currentLayer + " already exists as a file (not a directory)", FacesMessage.SEVERITY_ERROR);
				break;
			}
			prefix = currentLayer + "/";
		}
	}

}






