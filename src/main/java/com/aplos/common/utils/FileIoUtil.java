package com.aplos.common.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Row;
import org.primefaces.model.UploadedFile;

import com.aplos.common.ImportableColumn;
import com.aplos.common.appconstants.AplosAppConstants;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.FileDetails;
import com.aplos.common.beans.Website;
import com.aplos.common.enums.DocumentType;
import com.aplos.common.persistence.ArrayRowDataReceiver;
import com.aplos.common.persistence.ListRowDataReceiver;
import com.aplos.common.servlets.MediaServlet.MediaFileType;

@ManagedBean
public class FileIoUtil {

	public FileIoUtil() {	}

	public static String generateTabDelimitedFile(String filename, String[] columns, List<String[]> rows) {
		return generateTabDelimitedFile(filename, columns, rows, false);
	}
	
	public static void redirectToFile( FileDetails fileDetails ) {
		try {
			String url = "/media/" + fileDetails.getFilename().replace(" ", "_") + "?" + AplosAppConstants.FILE_NAME + "="
					+ fileDetails.determineFileDetailsDirectory(false)
					+ URLEncoder.encode(fileDetails.getFilename(), "UTF-8")  + "&" + AplosAppConstants.TYPE + "=" + MediaFileType.PDF;
			JSFUtil.redirect( JSFUtil.getRequest().getContextPath() + url, false);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public static String generateTabDelimitedFile(String filename, String[] columns, List<String[]> rows, Boolean suppressHeaders) {
		try {
		    FileWriter writer = new FileWriter(filename);
		    if (!suppressHeaders) {
			    for (int i=0; i < columns.length; i++) {
			    	writer.append(columns[i]);
			    	if (i != columns.length-1) {
			    		writer.append("\t");
			    	}
			    }
			    writer.append("\n");
		    }
		    for (int j=0; j < rows.size(); j++) {
		    	Object[] row = rows.get(j);
		    	if (row.length != columns.length) {
		    		throw new InputMismatchException("Value count does not match header count at row " + j);
		    	}
		    	for (int k=0; k < row.length; k++) {
	    			writer.append(String.valueOf(row[k]));
			    	if (k != row.length-1) {
			    		writer.append("\t");
			    	}
			    }
		    	if (j != rows.size()-1) {
		    		writer.append("\n");
		    	}
		    }
		    writer.flush();
		    writer.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
		return filename; //so we can use it with download file below
	}

	public static String generateCsvFile(BeanDao dao) {
		StringBuffer filename = new StringBuffer(FormatUtil.breakCamelCase(dao.getBeanClass().getSimpleName()));
		filename.append("s-");
		Website site = Website.getCurrentWebsiteFromTabSession();
		if (site != null) {
			filename.append(site.getDisplayName());
			filename.append("-");
		}
		filename.append(FormatUtil.formatDateForDB(new Date()));
		filename.append(".csv");
		return generateCsvFile(filename.toString(), dao);
	}

	public static String generateCsvFile(String filename, BeanDao dao) {
		return generateCsvFile(filename, dao, true, true);
	}

	public static String generateCsvFile(String filename, BeanDao dao, boolean includeInactive, boolean makeHumanReadable) {

		//TODO: I think to avoid writing everything we want into the select each time
		//we want some helper method where we can pass the bean dao with just the bean class
		//set, then we would take all fields
		//TODO: I want to be able to automatically create the joins
		//I think all it needs to do is look for a string concatenated twice eg bee.cee.dee
		//then you know you have to join cee explicitly - does the null issue only affect lists?
		if (includeInactive) {
			dao.setIsReturningActiveBeans(null);
		} 
		HashMap<String, String> aliasMap = new HashMap<String, String>();
    	aliasMap.put( "bean", "" );
    	ProcessedBeanDao processedBeanDao = new ProcessedBeanDao( dao );
    	processedBeanDao.setGeneratingBeans(false);
    	String[] columns = ApplicationUtil.extractColumnNames( processedBeanDao.getSelectSql(), aliasMap );
    	//String[] attributeNames = HibernateUtil.getAttributeFieldNames(columns);
    	String[] resultFieldNames = ApplicationUtil.getResultFieldNames(columns);
    	List objList = dao.getBeanResults();
    	for (int i=0; i < resultFieldNames.length; i++) { //make this human readable
    		resultFieldNames[i] = FormatUtil.breakCamelCase(resultFieldNames[i]);
    	}
    	return generateCsvFile(filename, resultFieldNames, objList, makeHumanReadable);
	}

	public static String generateCsvFile(String filename, List<String[]> rows) {
		return generateCsvFile(filename, new String[rows.get(0).length], rows, true);
	}

	public static String generateCsvFile(String filename, String[] columns, List<String[]> rows) {
		return generateCsvFile(filename, columns, rows, false, false);
	}

	public static String generateCsvFile(String filename, String[] columns, List<String[]> rows, boolean makeHumanReadable) {
		return generateCsvFile(filename, columns, rows, false, makeHumanReadable);
	}

	public static String generateCsvFile(String filename, String[] columns, List<String[]> rows, Boolean suppressHeaders) {
		return generateCsvFile(filename, columns, rows, suppressHeaders, false);
	}

	public static String generateCsvFile(String filename, String[] columns, List<String[]> rows, Boolean suppressHeaders, boolean makeHumanReadable) {
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
		    	Object[] row = rows.get(j);
		    	if (row.length != columns.length) {
		    		throw new InputMismatchException("Value count does not match header count at row " + j);
		    	}
		    	for (int k=0; k < row.length; k++) {
		    		if (makeHumanReadable && row[k] instanceof Boolean) {
		    			if ((Boolean)row[k]) {
		    				writer.append("Yes");
		    			} else {
		    				writer.append("No");
		    			}
		    		} else if (row[k] instanceof Date) { 
		    			writer.append(FormatUtil.formatDateTime((Date) row[k], true));
		    		} else if (row[k] instanceof String) { 
		    			writer.append( "\"" + String.valueOf(row[k]).replace( "\"", "\"\"\"") + "\"");
		    		} else {
		    			if (makeHumanReadable && row[k] == null) {
		    				writer.append("-"); // a blank would look like our exporter messed up / an error
		    			} else {
		    				writer.append(String.valueOf(row[k]));
		    			}
		    		}

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
	
	public static List<? extends AplosAbstractBean> importIntoBean( Class<? extends AplosAbstractBean> beanClass, UploadedFile uploadedFile, List<ImportableColumn> importableColumns, boolean saveBean ) {
		Map<String,ImportableColumn> rowMap = new HashMap<String,ImportableColumn>();
		
		BeanDao beanDao = new BeanDao( beanClass );
		for( int i = 0, n = importableColumns.size(); i < n; i++ ) {
			rowMap.put( importableColumns.get( i ).getName().toLowerCase(), importableColumns.get( i ) );
		}

		List<AplosAbstractBean> beanList = new ArrayList<AplosAbstractBean>();
		if( CommonUtil.isFileUploaded( uploadedFile ) ) {
			if( uploadedFile.getFileName().endsWith( ".csv" ) ) {
				try { 
					InputStream inputStream = uploadedFile.getInputstream();
					BufferedReader reader = new BufferedReader( new InputStreamReader( inputStream ) );
					String line = reader.readLine();
					
					int duplicateCount = 0;
					int badFormatCount = 0;
					int addedCount = 0;
					ProcessedBeanDao processedBeanDao = null;

					List<ImportableColumn> usedSortedColumns = new ArrayList<ImportableColumn>();
					Connection conn = null;
					try {
						conn = ApplicationUtil.getConnection();
						AplosAbstractBean tempAplosAbstractBean;
						List<Object> processedParts;
						mainWhile : while( line != null ) {
							if( processedBeanDao != null ) {
								String[] availableParts = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", beanDao.getUnprocessedSelectCriteriaList().size() );
								processedParts = new ArrayList<Object>();
								for( int i = 0, n = availableParts.length; i < n; i++ ) {
									availableParts[ i ] = FormatUtil.removeQuotesFromCsvString( availableParts[ i ] );
									if( i == (availableParts.length - 1) && availableParts[ i ].endsWith( "," ) ) {
										availableParts[ i ] = availableParts[ i ].substring(0, availableParts[ i ].length() - 1 );
									}
								}
								
								
								for( int i = 0, n = usedSortedColumns.size(); i < n; i++ ) {
									if( CommonUtil.isNullOrEmpty( availableParts[ 0 ] ) && usedSortedColumns.get( i ).isRequired() ) {
										badFormatCount++;

										if( usedSortedColumns.get( i ).isRequired() ) {
											line = reader.readLine();
											continue mainWhile;
										} else {
											availableParts[ i ] = usedSortedColumns.get( i ).getDefaultValue();
										}
									}
									processedParts.add( usedSortedColumns.get( i ).format( availableParts[ i ] ) );
									if( !usedSortedColumns.get( i ).validate( processedParts.get( i ) ) ) {
										badFormatCount++;
										if( usedSortedColumns.get( i ).isRequired() ) {
											line = reader.readLine();
											continue mainWhile;
										} else {
											processedParts.set( i, usedSortedColumns.get( i ).getDefaultValue() );
										}
									}
									if( !usedSortedColumns.get( i ).duplicateCheck( processedParts.get( i ) ) ) {
										duplicateCount++;
										
										line = reader.readLine();
										continue mainWhile;
									}
								}
								
								tempAplosAbstractBean = processedBeanDao.populateNewBean( new ListRowDataReceiver( processedParts ), true );
								if( saveBean ) {
									tempAplosAbstractBean.saveDetails();
								}
								beanList.add( tempAplosAbstractBean );
								addedCount++;
							} else {
								String[] lineParts = line.split( "," );
								List<String> unrecognisedColumns = new ArrayList<String>();
								Map<String, ImportableColumn> unfoundTables = new HashMap<String, ImportableColumn>(rowMap); 
								for( int i = 0, n = lineParts.length; i < n; i++ ) {
									lineParts[ i ] = lineParts[ i ].trim().toLowerCase();
									if( rowMap.get( lineParts[ i ] ) == null ) {
										unrecognisedColumns.add( lineParts[ i ] );
									} else {
										usedSortedColumns.add( unfoundTables.get( lineParts[ i ] ) );
										unfoundTables.remove( lineParts[ i ] );
										
									}
								}
								
								ArrayList<String> keys = new ArrayList<String>(unfoundTables.keySet());
								for( String key : keys ) {
									if( !unfoundTables.get( key ).isRequired() ) {
										unfoundTables.remove( key );
									}
								}
								if( unfoundTables.size() == 0 && unrecognisedColumns.size() == 0 ) {
									for( int i = 0, n = usedSortedColumns.size(); i < n; i++ ) {
										beanDao.addSelectCriteria( usedSortedColumns.get( i ).getVariablePath() );
									}
									processedBeanDao = beanDao.createProcessedBeanDao();
									processedBeanDao.setGeneratingBeans(true);
									processedBeanDao.preprocessCriteria();
								} else {
									StringBuffer errorStrBuf = new StringBuffer();
									if( unfoundTables.size() > 0 ) {
										errorStrBuf.append( "Couldn't find required columns : " );
										List<String> titleNames = new ArrayList<String>();
										for( String key : unfoundTables.keySet() ) {
											titleNames.add( key );
										}
										errorStrBuf.append( CommonUtil.join( titleNames, "," ) );
									}
									if( unrecognisedColumns.size() > 0 ) {
										if( errorStrBuf.length() > 1 ) {
											errorStrBuf.append( ", " );
										}
										errorStrBuf.append( "Couldn't recognise columns : " );
										errorStrBuf.append( CommonUtil.join( unrecognisedColumns, "," ) );
									}
									JSFUtil.addMessage( errorStrBuf.toString() + " in CSV file" );
									break;
								}
							}
							line = reader.readLine();
						}	
					} catch( SQLException sqlEx ) {
						ApplicationUtil.handleError(sqlEx);
					} catch( Exception ex ) {
						ApplicationUtil.handleError(ex);
					}  finally {
						ApplicationUtil.closeConnection(conn);
					}
					reader.close();
					inputStream.close();
					JSFUtil.addMessage( addedCount + " added, Not added " + duplicateCount + " duplicates, " + badFormatCount + " bad formats" );
				} catch( IOException ioex ) {
					ApplicationUtil.getAplosContextListener().handleError( ioex );
				} 
			} else {
				JSFUtil.addMessageForWarning( "This file is not in CSV format.  If it is an Excel please open it and then go to Save As - Other formats.  A dialog will open where you should select CSV (MS-DOS) from the dropdown.  You can then click to save the file in the correct format." );
			}
		}
		return beanList;
	}
	
	public static List<? extends AplosAbstractBean> importIntoBean( BeanDao beanDao, UploadedFile uploadedFile, List<Integer> duplicateChecks ) {
		return importIntoBean(beanDao, uploadedFile, duplicateChecks, null);
	}
	
	public static List<? extends AplosAbstractBean> importIntoBean( BeanDao beanDao, UploadedFile uploadedFile, List<Integer> duplicateChecks, Object[] additionalColumns ) {

		List<AplosAbstractBean> addedBeans = new ArrayList<AplosAbstractBean>();
		if( CommonUtil.isFileUploaded( uploadedFile ) ) {
			if( uploadedFile.getFileName().endsWith( ".csv" ) ) {
				try { 
					InputStream inputStream = uploadedFile.getInputstream();
					BufferedReader reader = new BufferedReader( new InputStreamReader( inputStream ) );
					String line = reader.readLine();
					boolean dataLinesStarted = false;
					
					int duplicateCount = 0;
					ProcessedBeanDao processedBeanDao = beanDao.createProcessedBeanDao();
					processedBeanDao.setGeneratingBeans(true);
					processedBeanDao.preprocessCriteria();
					
					AplosAbstractBean tempBean;
					Connection conn = null;
					try {
						conn = ApplicationUtil.getConnection();
						while( line != null ) {
							if( dataLinesStarted ) {
								String[] availableParts = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", beanDao.getUnprocessedSelectCriteriaList().size() );
								for( int i = 0, n = availableParts.length; i < n; i++ ) {
									availableParts[ i ] = FormatUtil.removeQuotesFromCsvString( availableParts[ i ] );	
								}
								
								if( duplicateChecks.size() > 0 ) {
									for( int i = 0, n = duplicateChecks.size(); i < n; i++ ) {
										beanDao.setNamedParameter( "duplicateCheck" + i, availableParts[ duplicateChecks.get( i ) ] );
									}
									if( beanDao.getCountAll() > 0 ) {
										duplicateCount++;
										line = reader.readLine();
										continue;
									}
								}
	
	
								tempBean = processedBeanDao.populateNewBean( new ArrayRowDataReceiver( availableParts, additionalColumns ), true );
								addedBeans.add(tempBean);
							} else {
								String[] lineParts = line.split( "," );
								boolean titlesFound = true;
								List<String> titleNames = new ArrayList<String>();
								for( int i = 0, n = beanDao.getUnprocessedSelectCriteriaList().size(); i < n; i++ ) {
									titleNames.add( FormatUtil.breakCamelCase( beanDao.getUnprocessedSelectCriteriaList().get( i ).getField().getName() ) );
									if( !titleNames.get( i ).equalsIgnoreCase( lineParts[ i ].trim() ) ) {
										titlesFound = false;
									}
								}
								if( titlesFound ) {
									dataLinesStarted = true;
								} else {
									JSFUtil.addMessage( "Expecting to find titles '" + CommonUtil.join( titleNames, "," ) + "' in CSV file" );
									break;
								}
							}
							line = reader.readLine();
						}
					} catch( SQLException sqlEx ) {
						ApplicationUtil.handleError(sqlEx);
					} catch( Exception ex ) {
						ApplicationUtil.handleError(ex);
					} finally {
						ApplicationUtil.closeConnection(conn);
					}
					reader.close();
					inputStream.close();
					JSFUtil.addMessage( addedBeans.size() + " added, " + duplicateCount + " duplicates not added" );
				} catch( IOException ioex ) {
					ApplicationUtil.getAplosContextListener().handleError( ioex );
				} 
			} else {
				JSFUtil.addMessageForWarning( "This file is not in CSV format.  If it is an Excel please open it and then go to Save As - Other formats.  A dialog will open where you should select CSV (MS-DOS) from the dropdown.  You can then click to save the file in the correct format." );
			}
		}
		return addedBeans;
	}

	public static String generateXlsFile(String filename, List<String[]> rows) {
		return generateXlsFile(filename, new String[rows.get(0).length], rows, true);
	}

	public static String generateXlsFile(String filename, String[] columns, List<String[]> rows) {
		return generateXlsFile(filename, columns, rows, false);
	}

	public static String generateXlsFile(String filename, String[] columns, List<String[]> rows, Boolean suppressHeaders) {
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
		return filename; //so we can wrap it with the fileDownload method (below)

	}

	/**
	 * Imports sheet 1 of an xls workbook and returns it as a list of string arrays
	 * @param xlsFile
	 * @return List of String[], Note: The first row may be a list of titles, depending on export format
	 */
	public static List<String[]> importXlsFile(UploadedFile xlsFile) {
		if (!CommonUtil.isFileUploaded(xlsFile)) {
			JSFUtil.addMessage("No File has been selected for import.", FacesMessage.SEVERITY_ERROR);
			return null;
		} else if (!xlsFile.getFileName().toLowerCase().endsWith(".xls")) {
			JSFUtil.addMessage("Uploaded File is not suitable for import. Please supply a .xls file.", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		List<String[]> rows = new ArrayList<String[]>();
		try	{
			POIFSFileSystem fileSystem = new POIFSFileSystem(xlsFile.getInputstream());
			HSSFWorkbook workBook = new HSSFWorkbook(fileSystem);
			HSSFSheet sheet = workBook.getSheetAt(0);
			Iterator<Row> sheetrow = sheet.rowIterator();
			while (sheetrow.hasNext()) {
				HSSFRow row = (HSSFRow) sheetrow.next();
				String[] cells = new String[row.getPhysicalNumberOfCells()];
				//Iterator<Cell> sheetcells = row.cellIterator();
				for (int i=0; i < cells.length; i++) {
					HSSFCell cell = row.getCell(i);
					cells[i] = cell.getStringCellValue();
				}
				rows.add(cells);
			}
			return rows;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
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

	/**
	 * Automatically has the browser download the documents created by other methods in this class
	 * Requires a com.aplos.common.servlets.DocumentServlet servlet mapping to /documentservlet in web.xml
	 * @param filelocation
	 * @param docType
	 */
	public static void fileDownload(String filelocation, DocumentType docType) {
		String redirectUrl = getFileDownloadUrl(filelocation, docType);
		try {
			JSFUtil.getResponse().sendRedirect( redirectUrl );
		} catch( IOException ioex ) {
			ioex.printStackTrace();
		}
	}

	public static String getFileDownloadUrl(String fileLocation, DocumentType docType) {
		String redirectUrl;
		if (docType == null) {
			redirectUrl = JSFUtil.getContextPath() + "/documentservlet?type=application/octet-stream&file=" + fileLocation;
		} else {
			redirectUrl = JSFUtil.getContextPath() + "/documentservlet?type=" + docType.name() + "&file=" + fileLocation;
		}
		return redirectUrl;
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






