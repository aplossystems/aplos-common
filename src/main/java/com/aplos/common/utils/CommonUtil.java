package com.aplos.common.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.model.SelectItem;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import net.sf.javainetlocator.InetAddressLocator;
import net.sf.javainetlocator.InetAddressLocatorException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.jasypt.salt.RandomSaltGenerator;
import org.jasypt.util.password.BasicPasswordEncryptor;
import org.primefaces.model.UploadedFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xhtmlrenderer.pdf.ITextRenderer;

import cb.jdynamite.JDynamiTe;

import com.aplos.common.AplosUrl;
import com.aplos.common.BackingPageUrl;
import com.aplos.common.LabeledEnumInter;
import com.aplos.common.appconstants.AplosAppConstants;
import com.aplos.common.appconstants.AplosScopedBindings;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.BackingPage;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.Country;
import com.aplos.common.beans.Currency;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.beans.FileDetails;
import com.aplos.common.beans.ShoppingCart;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.beans.Website;
import com.aplos.common.enums.CommonWorkingDirectory;
import com.aplos.common.enums.DayOfWeek;
import com.aplos.common.enums.JsfScope;
import com.aplos.common.interfaces.DataTableStateCreator;
import com.aplos.common.interfaces.SaltHolder;
import com.aplos.common.listeners.AplosContextListener;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.persistence.PersistentBeanSaver;
import com.aplos.common.tabpanels.MenuTab;
import com.lowagie.text.DocumentException;

public class CommonUtil {
    public static Random rand = new Random();
    
    public static void changeListToSaveableBeans( List beanList ) {
    	for( int i = 0, n = beanList.size(); i < n; i++ ) {
    		if( beanList.get( i ) instanceof AplosAbstractBean ) {
    			beanList.set( i , ((AplosAbstractBean) beanList.get( i )).getSaveableBean() );
    		}
    	}
    		
    }

    public static boolean validateXss(String content) {
    	if (content != null) {
    		return !content.matches("(.*)<[^>]*script(.*)");
		}
    	return true;
	}

	public static String encodeAgainstXss(String content) {
		if (content != null) {
			return content.replaceAll("<", "&lt");
		}
		return content;
	}
    
    public static Cookie findCookie( String cookieName ) {
    	if( JSFUtil.getRequest().getCookies() != null ) {
	    	for( Cookie cookie : JSFUtil.getRequest().getCookies() ) {
	    		if( cookie.getName().equals( cookieName ) ) {
	    			return cookie;
	    		}
	    	}
    	}
    	return null;
    }
    
    public static boolean dateMatch( Calendar cal1, Calendar cal2 ) {
    	if( cal1.get( Calendar.YEAR ) == cal2.get( Calendar.YEAR ) 
    			&& cal1.get( Calendar.MONTH ) == cal2.get( Calendar.MONTH )
    			&& cal1.get( Calendar.DAY_OF_YEAR ) == cal2.get( Calendar.DAY_OF_YEAR ) ) {
			return true;
		} 
    	return false;
    }
    
    public static IOException  close(Closeable resource) {
		if (resource != null) {
			try {
				resource.close();
			}
			catch (IOException e) {
				return e;
			}
		}

		return null;
    }
    
    public static boolean isEmpty(Collection<?> collection) {
		return collection == null || collection.isEmpty();
	}
    
    public static boolean saveBean( AplosAbstractBean aplosAbstractBean ) {
		if( aplosAbstractBean.isReadOnly() ) {
			ApplicationUtil.handleError( new Exception( "Read Only" ), false );
		}
//			JSFUtil.addMessageForWarning( "This bean is read only and cannot be saved" );
//			return false;
//		} else {
			boolean wasNew = aplosAbstractBean.isNew();
			try {
				PersistentBeanSaver.saveBean( aplosAbstractBean );
			} catch( Exception ex ) {
				ApplicationUtil.handleError(ex);
				return false;
			}
			if( aplosAbstractBean.getAfterSaveListener() != null ) {
				aplosAbstractBean.getAfterSaveListener().actionPerformed(wasNew);
			}

//			generateHashcode();
			return true;
//		}
    }
	public static boolean saveBeanWithThrow( AplosAbstractBean aplosAbstractBean ) throws Exception {
		if( aplosAbstractBean.isReadOnly() ) {
			throw new Exception( "Read Only" );
		}
//			JSFUtil.addMessageForWarning( "This bean is read only and cannot be saved" );
//			return false;
//		} else {
		boolean wasNew = aplosAbstractBean.isNew();
		try {
			PersistentBeanSaver.saveBean( aplosAbstractBean );
		} catch( Exception ex ) {
			throw ex;
		}
		if( aplosAbstractBean.getAfterSaveListener() != null ) {
			aplosAbstractBean.getAfterSaveListener().actionPerformed(wasNew);
		}

//			generateHashcode();
		return true;
//		}
	}

	public static boolean isLocalHost( ServletContext servletContext ) {
		if( servletContext == null || 
				(!CommonUtil.isNullOrEmpty( servletContext.getContextPath() ) && !servletContext.getContextPath().equals( "/" )) ) {
			return true;
		} else {
			return false;
		}
	}
	
	public static void addUtf8StringToJDynamiTe( JDynamiTe jDynamiTe, String content ) throws IOException {
		InputStream inputStream = new ByteArrayInputStream(content.getBytes("UTF-8"));
		jDynamiTe.setInput(new BufferedReader(new InputStreamReader(inputStream,"UTF-8")));
	}

	public static String getFirstLine( String text ) {
		if( text != null && text.contains( "\n" ) ) {
			return text.substring( 0, text.indexOf( "\n" ) );
		} else {
			return text;
		}
	}

	public static void encryptBeanOntoUrl( AplosAbstractBean aplosAbstractBean, AplosUrl aplosUrl ) {
		encryptValueOntoUrl((SaltHolder)aplosAbstractBean, aplosUrl, String.valueOf( aplosAbstractBean.getId() ) );
	}
	
	public static void encryptValueOntoUrl( SaltHolder saltHolder, AplosUrl aplosUrl, String value ) {
		if( CommonUtil.isNullOrEmpty( saltHolder.getEncryptionSalt() ) ) {
			saltHolder = (SaltHolder) saltHolder.getSaveableBean();
			saltHolder.setEncryptionSalt( CommonUtil.getRandomSalt() );
			saltHolder.saveDetails();
		}

        String fixedSalt = Base64.encodeBase64URLSafeString(ApplicationUtil.getAplosContextListener().getFixedSalt().getBytes());
        UrlEncrypter urlEncrypter = new UrlEncrypter( fixedSalt, fixedSalt, 5 );
        
		String key1 = urlEncrypter.encrypt( value );
		String key2 = CommonUtil.stdEncrypt( value, saltHolder.getEncryptionSalt(), true );
		
		aplosUrl.addQueryParameter( "key1", key1 );
		aplosUrl.addQueryParameter( "key2", key2 );
	}
	
	public static <T extends AplosAbstractBean> T decryptValueFromUrl( Class<? extends AplosAbstractBean> beanClass ) {
		String value = decryptValueFromUrl();
		if( value != null ) {
			try {
				Long beanId = Long.parseLong( value );
				AplosAbstractBean aplosAbstractBean = new BeanDao( beanClass ).get( beanId );
				if( verifyValueFromUrl((SaltHolder)aplosAbstractBean, value) ) {
					return (T) aplosAbstractBean;
				}
			} catch( NumberFormatException nfex ) {
				JSFUtil.addMessage( "The key provided with this URL is corrupt, please contact " + CommonConfiguration.getCommonConfiguration().getDefaultCompanyDetails().getCompanyName() + " for more details" );
				JSFUtil.redirectToLoginPage();
			}
		}
		return null;
	}
	
	public static String decryptValueFromUrl() {
		String key1 = JSFUtil.getRequestParameter("key1");
		if( !CommonUtil.isNullOrEmpty( key1 ) ) {
	        String fixedSalt = Base64.encodeBase64URLSafeString(ApplicationUtil.getAplosContextListener().getFixedSalt().getBytes());
	        UrlEncrypter urlEncrypter = new UrlEncrypter( fixedSalt, fixedSalt, 5 );
			return urlEncrypter.decrypt(key1);
		} else {
			JSFUtil.addMessage( "Some or all of the keys provided with this URL are missing, please contact " + CommonConfiguration.getCommonConfiguration().getDefaultCompanyDetails().getCompanyName() + " for more details" );
		}
		return null;
	}
	
	public static boolean verifyValueFromUrl( SaltHolder saltHolder, String value ) {
		String key2 = JSFUtil.getRequestParameter("key2");
		if( !CommonUtil.isNullOrEmpty( key2 ) ) {
			if( CommonUtil.checkStdEncrypt( value, saltHolder.getEncryptionSalt(), true, key2) ) {
				return true;
			} 
		} else {
			JSFUtil.addMessage( "All or all of the keys provided with this URL is missing, please contact " + CommonConfiguration.getCommonConfiguration().getDefaultCompanyDetails().getCompanyName() + " for more details" );
			JSFUtil.redirectToLoginPage();
		}
		return false;
	}
	
	public static int getRandomInt(int min, int max) {
	    // nextInt is normally exclusive of the top value,
	    // so add 1 to make it inclusive
	    int randomNum = rand.nextInt((max - min) + 1) + min;

	    return randomNum;
	}
	
	public static void setJDynamiTeInput( JDynamiTe jDynamiTe, String content ) throws IOException {
		InputStream inputStream = new ByteArrayInputStream(content.getBytes("UTF-8"));
		jDynamiTe.setInput(new BufferedReader(new InputStreamReader(inputStream,"UTF-8")));
	}
	
	 @SuppressWarnings({ "unchecked", "rawtypes" })
	public static List<Class<?>> getClasses(String path, Class<?> classFilter, boolean includeAbstractClasses ) throws ClassNotFoundException, IOException {
   	ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
       assert classLoader != null;
       URL packageURL = classLoader.getResource(path);
       ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
       List<File> dirs = new ArrayList<File>();
       if (packageURL != null && packageURL.getProtocol().equals("jar")) {
           String jarFileName = URLDecoder.decode(packageURL.getFile(), "UTF-8");
           jarFileName = jarFileName.substring(5,jarFileName.indexOf("!"));
           JarFile jarFile = new JarFile(jarFileName);
           Enumeration<JarEntry> jarEntries = jarFile.entries();
           while (jarEntries.hasMoreElements()) {

           	String entryName = jarEntries.nextElement().getName();
               if (entryName.startsWith(path) && entryName.endsWith(".class")) {
               	entryName = entryName.substring(path.length() + 1);
                   Class _class = CommonUtil.initialiseClass(path, entryName);
	            	if (_class != null  && (classFilter == null || classFilter.isAssignableFrom(_class) ) && 
	            			(includeAbstractClasses || !Modifier.isAbstract( _class.getModifiers() )) ) {
	            		classes.add(_class);
	            	}
               }

           }

       } else {
			Enumeration<URL> resources = classLoader.getResources(path);
			while (resources.hasMoreElements()) {
			    URL resource = resources.nextElement();
			    String fileName = resource.getFile();
			    String fileNameDecoded = URLDecoder.decode(fileName, "UTF-8");
			    dirs.add(new File(fileNameDecoded));
			}
			for (File directory : dirs) {
			    classes.addAll(findClasses(directory, path.replace( "/", "." ), classFilter, includeAbstractClasses ));
			}
       }

       return classes;
   }
	 
   public static String joinIds( List<? extends AplosAbstractBean> aplosAbstractBeanList ) {
	   String ids[] = new String[ aplosAbstractBeanList.size() ];
	   for( int i = 0, n = aplosAbstractBeanList.size(); i < n; i++ ) {
		   ids[ i ] = String.valueOf( aplosAbstractBeanList.get( i ).getId() );
	   }
	   return StringUtils.join( ids, "," );
   }
	 
   public static boolean writeStringToFile( String content, File file, boolean overwriteExistingFile, boolean displayMessages ) {
	   try {
		   if( !file.exists() || overwriteExistingFile ) {
			   writeStringToFile(content, file );
			   return true;
		   } else {
			   if( displayMessages ) {
				   JSFUtil.addMessage( "File already exists" );
			   }   
		   }
	   } catch( FileNotFoundException fnfEx ) {
		   if( displayMessages ) {
			   JSFUtil.addMessage( "File could not be saved as file could not be saved" );
		   }   
	   } catch( IOException ioEx ) {
		   if( displayMessages ) {
   				JSFUtil.addMessage( "File could not be saved as io exception occured" );
		   }
	   }
	   return false;
   }
	 
   public static void writeStringToFile( String content, File file ) throws IOException, FileNotFoundException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.write(content);
		writer.close();
   }

   /**
    * Recursive method used to find all classes in a given directory and subdirs.
    *
    * @param directory   The base directory
    * @param packageName The package name for classes found inside the base directory
    * @return The classes
    * @throws ClassNotFoundException
    */
   @SuppressWarnings({ "unchecked", "rawtypes" })
	private static List<Class<?>> findClasses(File directory, String packageName, Class<?> classFilter, boolean includeAbstractClasses ) throws ClassNotFoundException {

   	List<Class<?>> classes = new ArrayList<Class<?>>();
       if (directory.exists()) {
	        File[] files = directory.listFiles();
	        for (File file : files) {
	        	String fileName = file.getName();
	            if (file.isDirectory()) {
	                assert !fileName.contains(".");
	            	classes.addAll(findClasses(file, packageName + "." + fileName, classFilter, includeAbstractClasses));
	            } else if (fileName.endsWith(".class")) {
	            	Class _class = CommonUtil.initialiseClass(packageName, fileName);
	            	if (_class != null && (classFilter == null || classFilter.isAssignableFrom(_class) ) &&
	            			(includeAbstractClasses || !Modifier.isAbstract( _class.getModifiers() ))) {
	            		classes.add(_class);
	            	}
	            }
	        }
       }
       return classes;
   }


    @SuppressWarnings({ "rawtypes", "unchecked" })
	public static Class<?> initialiseClass(String packageName, String fileName) throws ClassNotFoundException {
    	if (!fileName.contains("$")) { //$ signifies a private, anonymous or inner class
	    	Class _class = null;
	    	if (fileName.contains("/")) {
	    		packageName = packageName + "/" + fileName.substring(0,fileName.lastIndexOf("/"));
	    		fileName = fileName.substring(fileName.lastIndexOf("/")+1);
	    	}
	    	packageName = packageName.replaceAll("/", ".");
			try {
				_class = Class.forName(packageName + '.' + fileName.substring(0, fileName.length() - 6));
			} catch (ExceptionInInitializerError e) {
				_class = Class.forName(packageName + '.' + fileName.substring(0, fileName.length() - 6),false, Thread.currentThread().getContextClassLoader());
			} catch (NoClassDefFoundError er) {
				er.printStackTrace();
			}
			if (_class != null) {
				return _class;
			}
    	}
    	return null;
    }

	public static boolean isAlphabetic( String string ) {
		if( string != null ) {
			String newString = FormatUtil.stripNonAlphabetic( string );
			if( newString.length() == string.length() ) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean isAlphaNumeric( String string, boolean isOnlyAlphaNumeric ) {
		if( isOnlyAlphaNumeric ) {
			if( string != null ) {
				String newString = FormatUtil.stripNonAlphanumeric( string );
				if( newString.length() == string.length() ) {
					return true;
				}
			}
		} else {
			Pattern pattern = Pattern.compile( "[0-9]{1}");
			Matcher m = pattern.matcher( string );
			boolean containsNumbers = m.find();
			pattern = Pattern.compile( "[A-Za-z]{1}");
			m = pattern.matcher( string );
			boolean containsLetters = m.find();
			return containsNumbers && containsLetters;
		}
		return false;
	}
	
	public static DayOfWeek getDayOfWeek( Date date ) {
		Calendar cal = GregorianCalendar.getInstance(Locale.UK);
		cal.setTime(date);
		int dayOfWeek = cal.get( Calendar.DAY_OF_WEEK );
		dayOfWeek = (dayOfWeek + 5) % 7;
		return DayOfWeek.values()[ dayOfWeek ];
	}
	
	public static String appendServerWorkPath( String directoryPath ) {
		return CommonWorkingDirectory.SERVER_WORK_DIR.getDirectoryPath(false) + directoryPath;
	}
	
	public static String getPathRelativeToServerWorkDir( String directoryPath ) {
		if( !CommonUtil.isNullOrEmpty(directoryPath) ) {
			directoryPath = directoryPath.replace( CommonWorkingDirectory.SERVER_WORK_DIR.getDirectoryPath(false), "" );
			return directoryPath.replace( CommonWorkingDirectory.SERVER_WORK_DIR.getDirectoryPath(false).replace("//", "/"), "" );
		} else {
			return directoryPath;
		}
	}
	
	public static String[] sepearateEmailAddresses( String joinedEmailAddresses ) {
		String[] emailAddresses = joinedEmailAddresses.split( ",|;" );
		List<String> filteredEmailAddresses = new ArrayList<String>();
		for( int i = 0, n = emailAddresses.length; i < n; i++ ) {
			emailAddresses[ i ] = emailAddresses[ i ].trim();
			if( !CommonUtil.isNullOrEmpty( emailAddresses[ i ] ) ) {
				filteredEmailAddresses.add( emailAddresses[ i ] );
			}
		}
		return filteredEmailAddresses.toArray( new String[ 0 ] );
	}
	
	public static boolean isResourceRequest( String requestUrl ) {
		return requestUrl.contains( "/javax.faces.resource" ) || requestUrl.contains( "/media/" ) || requestUrl.startsWith( "/resources" );
	}

	public static boolean isFileUploaded( UploadedFile uploadedFile ) {
		if( uploadedFile != null && !CommonUtil.isNullOrEmpty( uploadedFile.getFileName() ) ) {
			return true;
		}
		return false;
	}

	public static DataTableState createDataTableState( DataTableStateCreator dataTableStateCreator, Class<?> parentClass ) {
		return createDataTableState( dataTableStateCreator, parentClass, null, null );
	}

	public static DataTableState createDataTableState( DataTableStateCreator dataTableStateCreator, Class<?> parentClass, BeanDao aqlBeanDao ) {
		return createDataTableState(dataTableStateCreator, parentClass, null, aqlBeanDao );
	}

	public static DataTableState createDataTableState( DataTableStateCreator dataTableStateCreator, Class<?> parentClass, String identifier, BeanDao aqlBeanDao ) {
		DataTableState dataTableState = CommonUtil.loadDataTableState( parentClass, identifier );
		if( dataTableState == null ) {
			dataTableState = dataTableStateCreator.getDefaultDataTableState( parentClass );
			dataTableState.setTableIdentifier(identifier);
			dataTableState.saveDetails();
		}
		if( aqlBeanDao != null ) {
			dataTableState.setLazyDataModel( dataTableStateCreator.getAplosLazyDataModel( dataTableState, aqlBeanDao ) );
			dataTableState.getLazyDataModel().recoverStateFieldInformation();
		}
		return dataTableState;
	}

	public static DataTableState loadDataTableState( Class<?> parentClass ) {
		return loadDataTableState( parentClass, null );
	}

	public static DataTableState loadDataTableState( Class<?> parentClass, String identifier ) {
		BeanDao dataTableStateDao = new BeanDao( DataTableState.class );
		dataTableStateDao.addWhereCriteria( "bean.parentClass = '" + parentClass.getName() + "'" );
		if (JSFUtil.getLoggedInUser() != null) {
			dataTableStateDao.addWhereCriteria( "bean.owner.id = " + JSFUtil.getLoggedInUser().getId() );
		} else {
			dataTableStateDao.addWhereCriteria( "bean.owner.id IS null" );
		}
		if( identifier == null ) {
			dataTableStateDao.addWhereCriteria( "bean.tableIdentifier IS null" );
		} else {
			dataTableStateDao.addWhereCriteria( "bean.tableIdentifier = '" + identifier + "'" );
		}
		dataTableStateDao.setMaxResults(1);
		DataTableState dataTableState = dataTableStateDao.getFirstBeanResult();
//		if( dataTableState != null ) {
//			dataTableState.hibernateInitialise(true);
//		}
		if( dataTableState != null ) {
			return (DataTableState) dataTableState.getSaveableBean();
		}
		return null;
	}

	public static boolean removeCookie(String cookieName) {
		boolean found = false;
		Cookie cookies[] = ((HttpServletRequest)JSFUtil.getFacesContext().getExternalContext().getRequest()).getCookies();
		if (cookies.length > 0) {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals(cookieName)) {
					cookie.setMaxAge(0);
					JSFUtil.getResponse().addCookie(cookie);
					found=true;
				}
			}
		}
		return found;
	}

	public static String replaceLast(String subject, String find, String replace) {
		int lastIndex = subject.lastIndexOf(find);
		if (lastIndex > -1) {
			int suffixIndex = lastIndex + find.length();
			String suffix = "";
			if (subject.length() > suffixIndex) {
				suffix = subject.substring(suffixIndex);
			}
			return subject.substring(0,lastIndex) + replace + suffix;
		}
		return subject;
	}

	public static String addParameterToUrl( String url, String key, String value ) {
		if( url.contains( "?" ) ) {
			url += "&";
		} else {
			url += "?";
		}
		url += key + "=" + value;
		return url;
	}

	public static String getRandomSalt() {
		try {
			return new String( new String( new RandomSaltGenerator().generateSalt( 10 ) ).getBytes( "latin1" ) );
		} catch (UnsupportedEncodingException e) {
			ApplicationUtil.getAplosContextListener().handleError( e );
		}
		return null;
	}

	public static SelectItem[] getSelectItemBeans( String packageName, String className ) {
		try {
			@SuppressWarnings("unchecked")
			Class<? extends AplosAbstractBean> aplosAbstractBeanClass = (Class<? extends AplosAbstractBean>) Class.forName( "com.aplos." + packageName + ".beans." + className );

			return AplosAbstractBean.getSelectItemBeans( aplosAbstractBeanClass );
		} catch( ClassNotFoundException cnfex ) {
			ApplicationUtil.getAplosContextListener().handleError( cnfex );
		}
		return null;
	}

	public static BigDecimal addPercentage( BigDecimal value, BigDecimal percentage ) {
		return value.add( getPercentageAmount( value, percentage ) );
	}

	public static BigDecimal getPercentageAmount( BigDecimal value, BigDecimal percentage ) {
		return value.multiply( percentage.divide( new BigDecimal( 100 ) ) );
	}

	public static BigDecimal getPercentageAmountAndRound( BigDecimal value, BigDecimal percentage ) {
		return value.multiply( percentage.divide( new BigDecimal( 100 ) ) ).setScale( 2, RoundingMode.HALF_DOWN );
	}

	public static BigDecimal getInclusivePercentageAmountAndRound( BigDecimal value, BigDecimal percentage ) {
		return value.setScale( 4, RoundingMode.HALF_DOWN ).divide( percentage.add( new BigDecimal( 100 ) ), RoundingMode.HALF_DOWN ).multiply( percentage ).setScale( 2, RoundingMode.HALF_DOWN );
	}

	public static double roundToLowestPenny( double value ) {
		return ((int) (value * 100))/100d;
	}

	public static double getDoubleOrZero( Double value ) {
		if( value == null ) {
			return 0;
		} else {
			return value;
		}
	}

	/**
	 * @deprecated misleading name, use {@link CommonUtil#loadPrintTemplateToString(String, String, AplosContextListener)}
	 * @param filename
	 * @param path
	 * @param aplosContextListener
	 * @return
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	@Deprecated
	public static String loadFileToString( String filename, String path, AplosContextListener aplosContextListener ) throws URISyntaxException, IOException {
		return loadPrintTemplateToString(filename,path,aplosContextListener);
	}

	public static String loadPrintTemplateToString( String filename, String path, AplosContextListener aplosContextListener ) throws URISyntaxException, IOException {
		URL url = JSFUtil.checkFileLocations(filename, path, true );
		if( url != null ) {
			return new String( readEntireFile( url.openStream() ) );
		} else {
			return null;
		}
	}

	public static JDynamiTe loadContentInfoJDynamiTe( String filename, String path, AplosContextListener aplosContextListener ) throws URISyntaxException, IOException {
		URL url = JSFUtil.checkFileLocations(filename, path, true );
		System.err.println( url.toString() );
		if( url != null ) {
			JDynamiTe jDynamiTe = new JDynamiTe();
			// This was added in as just sending the file name in the JDynamiTe constructor
			// seems to cache the file.
			InputStream inStream = url.openStream();
			jDynamiTe.setInput( inStream );
			inStream.close();
			return jDynamiTe;
		} else {
			return null;
		}
	}

	public static Locale getLocale(String languageString) {
		return new Locale(languageString.toLowerCase());
	}

	public static SelectItem copySelectItem( SelectItem selectItem ) {
		if( selectItem != null ) {
			return new SelectItem( selectItem.getValue(), selectItem.getLabel() );
		} else {
			return null;
		}
	}

	public static String translate( String key ) {
		return AplosContextListener.getAplosContextListener().translate( key );
	}

	public static String translate( String key, Locale locale ) {
		return AplosContextListener.getAplosContextListener().translate( key, locale );
	}

	public static AplosUrl getExternalPageUrl( AplosUrl aplosUrl ) {
		return getExternalPageUrl( Website.getCurrentWebsiteFromTabSession(), aplosUrl );
	}

	public static AplosUrl getExternalPageUrl( Website website, AplosUrl aplosUrl ) {
		aplosUrl.setHost( website );
		aplosUrl.setScheme( AplosUrl.Protocol.HTTP );
		return aplosUrl;
	}

	public static boolean isPostcodeRequired( Country country ) {
		if( country == null ) {
			return false;
		} else if( country.getId().equals( 10002l ) || // England
//				   country.getId().equals( 372l ) || // Ireland  Ireland does not have postcodes
				   country.getId().equals( 831l ) || // Guernsey
				   country.getId().equals( 832l ) || // Jersey
				   country.getId().equals( 833l ) || // Isle of man
				   country.getId().equals( 10006l ) || // Scotland
				   country.getId().equals( 10007l ) // Wales
				   ){
			return true;
		}
		return false;
	}

	/**
	 * @deprecated  v1.6.13
	 * Please use new BackingPageUrl(*) instead
	 * @param backingPageClass
	 * @return
	 */
	@Deprecated
	public static BackingPageUrl getBackingPageUrl( Class<? extends BackingPage> backingPageClass ) {
		return new BackingPageUrl( Website.getCurrentWebsiteFromTabSession(), backingPageClass, false );
	}


	/**
	 * @deprecated  v1.6.13
	 * Please use new BackingPageUrl(*) instead
	 * @param backingPageClass
	 * @return
	 */
	@Deprecated
	public static BackingPageUrl getBackingPageUrl( Class<? extends BackingPage> backingPageClass, boolean addExtension ) {
		return new BackingPageUrl( Website.getCurrentWebsiteFromTabSession(), backingPageClass, addExtension );
	}


	/**
	 * @deprecated  v1.6.13
	 * Please use new BackingPageUrl(*) instead
	 * @param backingPageClass
	 * @return
	 */
	@Deprecated
	public static BackingPageUrl getBackingPageUrl( Website site, Class<? extends BackingPage> backingPageClass, boolean addExtension ) {
		return new BackingPageUrl( site, backingPageClass, addExtension);
	}


	/**
	 * @deprecated  v1.6.13
	 * Please use new BackingPageUrl(*) instead
	 * @param backingPageClass
	 * @return
	 */
	@Deprecated
	public static BackingPageUrl getBackingPageUrl( String sitePackageName, Class<? extends BackingPage> backingPageClass, boolean addExtension ) {
		return new BackingPageUrl( sitePackageName, backingPageClass, addExtension );
	}

	/**
	 * Returns the currency in the session,
	 * if it is null it updates both the session and cart currencies first via AplosModuleFilterer
	 */
	public static Currency getCurrency() {
		Currency currentCurrency = JSFUtil.getBeanFromScope(Currency.class);
		if (currentCurrency == null) {
			currentCurrency = ApplicationUtil.getAplosModuleFilterer().updateSessionCurrency(JSFUtil.getRequest());
		}
		return currentCurrency;
	}

	public static void setCurrency(Currency currency) {
		currency.addToScope( JsfScope.TAB_SESSION );
		ShoppingCart cart = JSFUtil.getBeanFromScope(ShoppingCart.class);
		if (cart != null) {
			cart.setCurrency(currency);
		}
	}

	public static Country getCountryByIp(String ip) {
		String countryCode = getCountryCodeByIp(ip);
		if (countryCode != null) {
			countryCode = FormatUtil.stripNonNumeric(countryCode,".");
			if (countryCode.length() > 0 && !countryCode.equals("**")) {
				BeanDao dao = new BeanDao(Country.class);
				dao.setWhereCriteria("bean.iso2 LIKE :iso2"); //reasonably sure this will be upper anyway
				dao.setNamedParameter( "iso2", countryCode.toUpperCase() );
				dao.setMaxResults(1);
				return dao.setIsReturningActiveBeans(true).getFirstBeanResult();
			}
		}
		return null;
	}

	public static String getCountryCodeByIp(String ip) {
		return getLocaleByIp(ip).getCountry();
	}

	/** Wrapper. Takes an IP address and returns the correct locale or default (UK) if fails
	 *  uses javainetlocator : http://javainetlocator.sourceforge.net/docs/ */
	public static Locale getLocaleByIp(String ip) {
		try {
			return InetAddressLocator.getLocale(ip /*.getBytes() */);
		} catch (InetAddressLocatorException e) {
			e.printStackTrace();
			return Locale.UK;
		}
	}

//	public static String getDynamicTabRedirectUrl(String whereToNavigateTo, Long menuTabId, boolean addExtension) {
//		if (whereToNavigateTo.contains("?dmtab=")) {
//			//if our string already contained a menu tab id just replace it
//			whereToNavigateTo.replaceFirst("\\?dmtab=(.*?)(&)+?", "\\?dmtab=" + menuTabId + "$2");
//			return whereToNavigateTo;
//		}
//
//		if (addExtension && !whereToNavigateTo.endsWith(".jsf")) {
//			whereToNavigateTo += ".jsf";
//		}
//
//		if (addExtension) {
//			return whereToNavigateTo + "?dmtab=" + menuTabId + "&lang=" + CommonUtil.getContextLocale().getLanguage();
//		} else {
//			return whereToNavigateTo;
//		}
//	}

	public static Long getMenuTabId(Class<? extends BackingPage> backingPageClass) {
		//when going to dynamic menu tab edit its throwing an exception
		BeanDao dao = new BeanDao(MenuTab.class);
		dao.setSelectCriteria("bean.id");
		dao.setWhereCriteria("bean.tabActionClass.backingPageClass='" + backingPageClass.getName() + "'");
		dao.addWhereCriteria("bean.defaultTabForBackingPage=1");
		Long tabId = (Long) dao.setIsReturningActiveBeans(true).getFirstResult();
		if (tabId == null) {
			dao.clearWhereCriteria();
			dao.setWhereCriteria("bean.tabActionClass.backingPageClass='" + backingPageClass.getName() + "'");
			tabId = (Long) dao.setIsReturningActiveBeans(true).getFirstResult();
		}
		return tabId;
	}

	public static String getUnicodeEntityStr( String inputStr ) {
		StringBuffer outputStrBuf = new StringBuffer();
		for( int i = 0, n = inputStr.length(); i < n; i++ ) {
			outputStrBuf.append( "&#" + (int) inputStr.charAt( i ) + ";" );
		}
		return outputStrBuf.toString();
	}

	public static Locale getContextLocale() {
		if( JSFUtil.getFacesContext() == null ) {
			return Locale.getDefault();
		}
		else {
			Locale locale = (Locale) JSFUtil.getFromTabSession(AplosScopedBindings.CURRENT_LOCALE);
			if (locale == null) {
				locale = Locale.getDefault();
				setContextLocale(locale,JSFUtil.getSessionTemp());
			}
			return locale;
		}
	}

	public static Locale getContextLocale(HttpSession session) {
		Locale locale = (Locale) session.getAttribute(AplosScopedBindings.CURRENT_LOCALE);
		if (locale == null) {
			locale = Locale.getDefault();
			setContextLocale(locale);
		}
		return locale;

	}

	public static void setContextLocale(Locale locale) {
		setContextLocale(locale,JSFUtil.getSessionTemp());
	}

	public static void setContextLocale(Locale locale,HttpSession session) {
		JSFUtil.addToTabSession( AplosScopedBindings.CURRENT_LOCALE, locale );
	}

	public static String getFlashUrl( String flashFileUrl, String directoryPath ) {
		if( flashFileUrl == null || flashFileUrl.equals( "" ) ) {
			return "";
		} else {
			try {

				String productBrandDir = CommonUtil.getPathRelativeToServerWorkDir( directoryPath );
				String url = "/media/?" + AplosAppConstants.FILE_NAME + "=" + productBrandDir + URLEncoder.encode(flashFileUrl, "UTF-8") + "&amp;" + AplosAppConstants.TYPE + "=" + AplosAppConstants.FLASH;

				return JSFUtil.getRequest().getContextPath() + url;
			} catch( UnsupportedEncodingException usee ) {
				return "";
			}
		}
	}

	public static String uploadFile(UploadedFile upFile, String directoryPath, Long id) {
		return uploadFile(upFile, directoryPath, String.valueOf( id ), null );
	}

	public static String uploadFile(UploadedFile upFile, String directoryPath, String id) {
		return uploadFile(upFile, directoryPath, id, null );
	}

	public static String uploadFile(UploadedFile upFile, String directoryPath, Long id, String suffix ) {
		return uploadFile(upFile, directoryPath, String.valueOf( id ), suffix );
	}

	public static String uploadFile(UploadedFile upFile, String directoryPath, String prefix, String suffix ) {
		String newFilename = null;
		String safeFilename = null;
		try {
			String fileType = upFile.getFileName().substring(upFile.getFileName().lastIndexOf(".") + 1, upFile.getFileName().length());
			newFilename = prefix;
			if (newFilename != null && !newFilename.equals("") && suffix != null) {
				newFilename += "_" + suffix;
			}
			safeFilename = FormatUtil.stripToAllowedCharacters(newFilename, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_-0123456789");
			if (safeFilename.length() < 1) {
				safeFilename = String.valueOf(new Date().getTime());
			}
			if (!safeFilename.equals(newFilename)) {
				JSFUtil.addMessage("Certain characters such as any arabic cannot be used in URIs, these have been removed from the filename");
			}
			safeFilename += "." + fileType.toLowerCase();
			directoryPath = directoryPath.replace("\\", "/");
			java.io.File ioFile = new java.io.File(directoryPath);

			directoryPath = ioFile.getAbsolutePath();
//			System.out.println(directoryPath);
			ioFile.mkdirs();
				ioFile = new File(directoryPath + "/" + safeFilename);
//				if (ioFile.exists()) { // only checks if it existed already new file doesnt create a new file
					try {
						InputStream uploadInStream = upFile.getInputstream();
						FileOutputStream fOut = new FileOutputStream(ioFile);
					   int c=0;
					   while ( (c=uploadInStream.read()) != -1 ) {
					      fOut.write(c);
					   } // while
					   fOut.flush();
					   fOut.close();
					} catch (FileNotFoundException ex) {
						ex.printStackTrace();
						throw new RuntimeException("Error writing file.");
					}
//				} else {
//					throw new RuntimeException("File to upload does not Exist.");
//				}

		} catch (IOException ioEx) {
			ErrorEmailSender.sendErrorEmail(JSFUtil.getRequest(),ApplicationUtil.getAplosContextListener(), ioEx);
			ioEx.printStackTrace();
		}

		return safeFilename;
	}

	public static FileDetails saveContentAsPdf( FileDetails fileDetails, String content ) throws IOException, DocumentException {
        content = XmlEntityUtil.replaceCharactersWith(content, XmlEntityUtil.EncodingType.ENTITY);
		
		String safeFilename = fileDetails.getId() + ".pdf";
		
		OutputStream os = new FileOutputStream(fileDetails.determineFileDetailsDirectory(true) + "/" + safeFilename);
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(content);
        renderer.layout();
        renderer.createPDF(os, true);

	    os.close();

	    fileDetails.setFilename( safeFilename );
	    fileDetails.saveDetails();
	    return fileDetails;
	}

	public static byte[] readEntireFile( String filePath ) throws IOException {
		//new File( url.toURI() ).getAbsolutePath()
		File file = new File( filePath );
		return readEntireFile( file );
	}

	public static byte[] readEntireFile( File file ) throws IOException {
		return readEntireFile(new FileInputStream( file ));
	}

	public static byte[] readEntireFile( InputStream inputStream ) throws IOException {
		final BufferedInputStream bis = new BufferedInputStream( inputStream );
		final byte [] bytes = new byte[inputStream.available()];
		bis.read(bytes);
		bis.close();
		return bytes;
	}

	public static long timeTrial( String timeTrialPlace, ServletContext servletContext ) {
		if( servletContext != null ) {
			Long previousTime  = (Long) servletContext.getAttribute( AplosAppConstants.TIME_TRIAL_PREVIOUS_TIME );

			Long nowTime = timeTrial( timeTrialPlace, previousTime );

			servletContext.setAttribute( AplosAppConstants.TIME_TRIAL_PREVIOUS_TIME, nowTime );

			return nowTime;
		} else {
			return -1;
		}
	}

	public static long timeTrial( String timeTrialPlace ) {
		Long previousTime  = (Long) JSFUtil.getServletContext().getAttribute( AplosAppConstants.TIME_TRIAL_PREVIOUS_TIME );

		Long nowTime = timeTrial( timeTrialPlace, previousTime );

		JSFUtil.getServletContext().setAttribute( AplosAppConstants.TIME_TRIAL_PREVIOUS_TIME, nowTime );
		return nowTime;
	}

	public static long timeTrial( String timeTrialPlace, Long previousTime ) {
		Long nowTime;
		if( previousTime == null ) {
			previousTime  = new Date().getTime();
			nowTime = previousTime  - 1;
		} else {
			nowTime = new Date().getTime();
		}

		System.err.println( timeTrialPlace + ": " + (nowTime - previousTime) + "ms" );
		return nowTime;
	}

	public static String getModuleName( String className ) {
		return className.split( "\\." )[ 2 ];
	}


	public static String getModulePackage( String className ) {
		String[] classNameBits = className.split( "\\." );
		return classNameBits[ 0 ] + "." + classNameBits[ 1 ] + "." + classNameBits[ 2 ];
	}

	public static String getFirstElementValueOrEmpty( Element element, String elementName ) {
		NodeList nodeList = element.getElementsByTagName( elementName );
		if( nodeList.getLength() > 0 ) {
			return nodeList.item( 0 ).getTextContent();
		} else {
			return "";
		}
	}

	public static void createDirectory( String directory ) {
		File dir = new File( directory );
		if (!dir.exists()) {
			dir.mkdirs();
		}
	}

	public static String getFirstElementValueOrEmpty( Document document, String elementName ) {
		NodeList nodeList = document.getElementsByTagName( elementName );
		if( nodeList.getLength() > 0 ) {
			return nodeList.item( 0 ).getTextContent();
		} else {
			return "";
		}
	}

	/**
	 * Validates whether the subject contains only digits (no decimals or operators)
	 * @param testSubject
	 * @return
	 */
	public static boolean validatePositiveInteger(String testSubject) {
		if (testSubject == null) {
			return false;
		}
		int beginlen = testSubject.length();
		testSubject = FormatUtil.stripNonNumeric(testSubject);
		int endlen = testSubject.length();
		return !testSubject.equals("") && beginlen == endlen;
	}
	
	public static boolean validateNumeric(String testSubject) {
		if (testSubject == null) {
			return false;
		}
		if( testSubject.startsWith("-") ) {
			testSubject = testSubject.substring( 1 );
		}
		if( testSubject.contains( "." ) ) {
			testSubject.replaceFirst( "\\.", "" );
		}
		int beginlen = testSubject.length();
		testSubject = FormatUtil.stripNonNumeric(testSubject);
		int endlen = testSubject.length();
		return !testSubject.equals("") && beginlen == endlen;
	}

	public static boolean validateNumeric(String testSubject, String additionalValidDigits) {
		if (testSubject == null) {
			return false;
		}
		int beginlen = testSubject.length();
		testSubject = FormatUtil.stripNonNumeric(testSubject, additionalValidDigits);
		int endlen = testSubject.length();
		return !testSubject.equals("") && beginlen == endlen;
	}

	public static boolean validateTelephone(String telephoneNumberStr) {
		return validateTelephone(telephoneNumberStr, 12);
	}

	public static boolean validateTelephone(String telephoneNumberStr, int allowedLength) {
		//[\\+]? an optional +
		//([0-9]{6,allowedLength}) followed by 6-allowedLength numbers
		return validateStringPattern(telephoneNumberStr, Pattern.compile("^[\\+]?([0-9]{6," + allowedLength + "})$"));
	}

	public static boolean validateEmailAddressFormat(String emailAddress) {
		//[-_\\.a-zA-Z0-9]+ 		One or more letters numbers, - or _
		//@							followed by an @ symbol
		//([-a-zA-Z0-9]+\\.){1,2}	followed by one or two sequences of letters numbers and hyphens followed by a dot
		//[a-zA-Z]{2,4}				followed by an extension, 2-4 letters long
		//(\\.[a-zA-Z]{2,3})?		optionally followed by a dot and another extension 2-3 letters long

//		return validateStringPattern(emailAddress, Pattern.compile("^[-_\\.a-zA-Z0-9]+@([-a-zA-Z0-9]+\\.){1,2}([a-zA-Z]*){1}(\\.[-a-zA-Z]{2,3})?$"));

		/*
		 * This has now been replaced by the recommended version on regular-expressions.info
		 * It doesn't need the optional bit at the end as it includes the period in the middle group
		 * and it should cover more email variations.
		 */
		if (emailAddress == null || emailAddress.contains("..")) {
			return false;
		}
		emailAddress = emailAddress.trim();


		return validateStringPattern(emailAddress.toLowerCase(), Pattern.compile("^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,4}$"));
	}

	//Passing in a pattern allows us to validate specific emails for other projects,
	//For example checking they are all of the .ac.uk or .mod.uk domains
	public static boolean validateStringPattern(String subject, Pattern searchPattern) {
		if (subject != null && !subject.equals("")) {
			Matcher m = searchPattern.matcher(subject);
			if (m.find()) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	public static String createStackTraceString(Throwable t) {
		List<Throwable> throwableList = new ArrayList<Throwable>();
		throwableList.add( t );
		while( ( t = t.getCause() ) != null ) {
			throwableList.add( t );
		}

		StringBuffer stackTraceStrBuf = new StringBuffer();
		for( int i = throwableList.size() - 1; i > -1; i-- ) {
			StackTraceElement stackElements[] = throwableList.get( i ).getStackTrace();
			stackTraceStrBuf.append( StringEscapeUtils.escapeHtml( throwableList.get( i ).toString() ) + "<br/>" );
			for( int j = 0, p = stackElements.length; j < p; j++ ) {
				stackTraceStrBuf.append( StringEscapeUtils.escapeHtml( stackElements[ j ].toString() ) + "<br/>" );
			}
			stackTraceStrBuf.append( "<br/><br/>" );
		}

		return stackTraceStrBuf.toString();
	}
	
	public static String stripHtml( String htmlStr ) {
		if( htmlStr != null ) {
			return htmlStr.replaceAll("\\<.*?\\>", "");
		} else {
			return htmlStr;
		}
	}
	
	public static boolean isEmptyEditorContent( String content ) {
		if( isNullOrEmpty( content ) || content.equals( AplosAppConstants.DEFAULT_CKEDITOR_CONTENT ) ) {
			return true;
		} else {
			return false;
		}
	}

	public static String getBinding( Class<?> clazz ) {
		String binding = CommonUtil.firstLetterToLowerCase( clazz.getSimpleName() );
		binding = removeHibernateProxyTextFromClass( binding );
		return binding;
	}

	public static String removeHibernateProxyTextFromClass( String className ) {
		if( className.contains( "_$$_java" ) ) {
			className = className.substring(0,className.indexOf( "_$$_java" ) );
		}
		return className;
	}

	public static String firstLetterToLowerCase( String value ) {
		return value.substring(0,1).toLowerCase() + value.substring(1);
	}
	
	public static String firstLetterOfEachWordUpperCase( String value ) {
		return firstLetterOfEachWordUpperCase(value, false);
	}
	
	public static String firstLetterOfEachWordUpperCase( String value, boolean isSettingToLowerCaseFirst ) {
		if( value != null ) {
			if( isSettingToLowerCaseFirst ) {
				value = value.toLowerCase();
			}
			String sentenceParts[] = value.split( "\\s" );
			String partTrimmed;
			for( int i = 0, n = sentenceParts.length; i < n; i++ ) {
				partTrimmed = sentenceParts[ i ].trim();
				value = value.replace( partTrimmed, firstLetterToUpperCase(partTrimmed) );
			}
		}
		return value;
	}
	public static String firstLetterToUpperCase( String value ) {
		return firstLetterToUpperCase(value, false);
	}

	public static String firstLetterToUpperCase( String value, boolean isSettingToLowerCaseFirst ) {
		if( value != null && value.length() > 1 ) {
			if( isSettingToLowerCaseFirst ) {
				value = value.toLowerCase();
			}
			return value.substring(0,1).toUpperCase() + value.substring(1);
		} else {
			return value;
		}
	}
	
	public static Enum<?> getEnumValueOf( Class<Enum<?>> enumClass, String value ) {
		for (Enum<?> myEnum : enumClass.getEnumConstants()) {
			if( myEnum.name().equals( value ) ) {
				return myEnum;
			}
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	public static List<SelectItem> getEnumSelectItems(Class<? extends LabeledEnumInter> clazz) {
		return getEnumSelectItems(clazz, null);
	}

	@SuppressWarnings("rawtypes")
	public static List<SelectItem> getEnumSelectItems(Class<? extends LabeledEnumInter> clazz, boolean sort ) {
		return getEnumSelectItems(clazz, null, sort, false);
	}

	@SuppressWarnings("rawtypes")
	public static List<SelectItem> getEnumSelectItemsWithNotSelected(Class<? extends LabeledEnumInter> clazz) {
		return getEnumSelectItems(clazz, CommonConfiguration.getCommonConfiguration().getDefaultNotSelectedText());
	}
	@SuppressWarnings("rawtypes")
	public static List<SelectItem> getEnumSelectItems(Class<? extends LabeledEnumInter> clazz, String notSelectedStr ) {
		return getEnumSelectItems( clazz, notSelectedStr, false, false );
	}

	@SuppressWarnings("rawtypes")
	public static List<SelectItem> getEnumSelectItems(Class<? extends LabeledEnumInter> clazz, String notSelectedStr, boolean sort, boolean isUsingOrdinalValue ) {
		List<LabeledEnumInter> labeledEnumInters = new ArrayList<LabeledEnumInter>();
		for (Object e : clazz.getEnumConstants()) {
			labeledEnumInters.add( (LabeledEnumInter) e );
		}
		return getEnumSelectItems(labeledEnumInters, notSelectedStr, sort, isUsingOrdinalValue );
	}

	@SuppressWarnings("rawtypes")
	public static List<SelectItem> getEnumSelectItems(List<? extends LabeledEnumInter> labeledEnumInters, String notSelectedStr ) {
		return getEnumSelectItems(labeledEnumInters, notSelectedStr, false, false);
	}

	@SuppressWarnings("rawtypes")
	public static List<SelectItem> getEnumSelectItems(List<? extends LabeledEnumInter> labeledEnumInters, String notSelectedStr, boolean sort, boolean isUsingOrdinalValue ) {
		List<SelectItem> items = new ArrayList<SelectItem>();
		if( !CommonUtil.isNullOrEmpty( notSelectedStr ) ) {
			items.add( new SelectItem( null, notSelectedStr ) );
		}
		
		if( sort ) {
			Collections.sort( labeledEnumInters, new Comparator<LabeledEnumInter>() {
				@Override
				public int compare(LabeledEnumInter o1, LabeledEnumInter o2) {
					return o1.getLabel().compareTo(o2.getLabel());
				}
			});
		}

		for( int i = 0, n = labeledEnumInters.size(); i < n; i++ ) {
			if( isUsingOrdinalValue ) {
				items.add( new SelectItem(((Enum)labeledEnumInters.get( i )).ordinal(), labeledEnumInters.get( i ).getLabel()) );
			} else {
				items.add( new SelectItem(labeledEnumInters.get( i ), labeledEnumInters.get( i ).getLabel()) );
			}
		}
		return items;
	}
	
	public static int compare( String string1, String string2 ) {
		if( string1 == null ) {
			if( string2 == null ) {
				return 0;
			} else {
				return 1;
			}
		} else if( string2 == null ) {
			return -1;
		} else {
			return string1.compareTo( string2 );
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List subListCopy( List listToCopy, int startIdx, int finishIdx ) {
		List newList = new ArrayList();
		for( int i = startIdx, n = finishIdx; i <= n; i++ ) {
			newList.add( listToCopy.get(  i  ) );
		}

		return newList;
	}

	public static String generateLinkCode(String tableName, String codeFieldName) {
		String linkCode = generateRandomCode();
		while ( ApplicationUtil.getResults(
				"SELECT * FROM " + tableName + " where " + codeFieldName + " = '"
						+ linkCode + "'").size() > 0) {
			linkCode = generateRandomCode();
		}
		return linkCode;
	}

	public static String generateRandomCode() {
		Random r = new Random();
		return Long.toString(Math.abs(r.nextLong()), 36);
	}

	public static boolean isNullOrEmpty(String text) {
		if (text == null || text.equals("")) {
			return true;
		} else {
			return false;
		}
	}

	public static SystemUser getAdminUser() {
		return CommonConfiguration.getCommonConfiguration().getDefaultAdminUser();
	}

	public static SystemUser getCurrentUserOrAdmin() {
		SystemUser currentUser = JSFUtil.getLoggedInUser();
		if (currentUser != null) {
			return currentUser;
		} else {
			return getAdminUser();
		}
	}

	public static String stdEncrypt(String password, String salt, boolean addFixedSalt) {
		BasicPasswordEncryptor basicPasswordEncryptor = new BasicPasswordEncryptor();
		String saltedPassword;
		if( addFixedSalt ) {
			saltedPassword = salt + AplosContextListener.getAplosContextListener().getFixedSalt() + password;
		} else {
			saltedPassword = salt + password;
		}
		return basicPasswordEncryptor.encryptPassword( saltedPassword );
	}

	public static boolean checkStdEncrypt(String plainPassword, String salt, boolean addFixedSalt, String encryptedPassword ) {
		BasicPasswordEncryptor basicPasswordEncryptor = new BasicPasswordEncryptor();
		String saltedPassword;
		if( addFixedSalt ) {
			saltedPassword = salt + AplosContextListener.getAplosContextListener().getFixedSalt() + plainPassword;
		} else {
			saltedPassword = salt + plainPassword;
		}
		try {
			return basicPasswordEncryptor.checkPassword( saltedPassword, encryptedPassword );
		} catch (EncryptionOperationNotPossibleException eonpe) {
			return false;
		}
	}

	public static String md5(Date date) {
		return md5(String.valueOf(date.getTime()));
	}

	public static String md5(String password) {
		byte[] newPasswordBytes = password.getBytes();

		try {
			MessageDigest algorithm = MessageDigest.getInstance("MD5");
			algorithm.reset();
			algorithm.update(newPasswordBytes);
			byte messageDigest[] = algorithm.digest();

			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < messageDigest.length; i++) {
				hexString.append( Integer.toString( ( messageDigest[i] & 0xff ) + 0x100, 16 ).substring( 1 ) );
			}

			return hexString.toString();
		} catch (NoSuchAlgorithmException nsaEx) {
			nsaEx.printStackTrace();
		}

		return "";
	}

	public static String getStringOrEmpty(Object value) {
		if (value == null) {
			return "";
		} else {
			return String.valueOf(value);
		}
	}

	public static Object getNewInstance(String className, Logger logger) {
		try {
			Class<?> clazz = Class.forName( className );
			return getNewInstance(clazz, logger);
		} catch( ClassNotFoundException cnfex ) {
			if (logger != null) {
				logger.warn("Cannot find class : " + className, cnfex);
			} else {
				cnfex.printStackTrace();
			}
			return null;
		}
	}

	public static Object getNewInstance(Class<?> classType) {
		return getNewInstance(classType, null);
	}

	public static Object getNewInstance(Class<?> classType, Logger logger) {
		try {
			return classType.newInstance();
		} catch (IllegalAccessException iaEx) {
			if (logger != null) {
				logger.warn("Cannot instantiate Instance", iaEx);
			} else {
				iaEx.printStackTrace();
			}
		} catch (InstantiationException iEx) {
			if (logger != null) {
				logger.warn("Cannot instantiate Instance", iEx);
			} else {
				iEx.printStackTrace();
			}
		}

		return null;
	}

	public static void forwardMessage(Message forward) {

	}

	public static MimeMessage createMimeMessage(String subject, String message,
			String fromAddress, String toAddress, Session mailSession) {

		Logger logger = Logger.getLogger(CommonUtil.class.getName());
		try {
			MimeMessage mimeMessage = new MimeMessage(mailSession);
			mimeMessage.setSubject(subject,"UTF-8");
			mimeMessage.setContent(message, "text/html; charset=UTF-8");
			mimeMessage.setHeader("Content-Type", "text/html; charset=UTF-8");
			mimeMessage.addRecipient(Message.RecipientType.TO,
					new InternetAddress(toAddress));
			mimeMessage.setFrom(new InternetAddress(fromAddress));
			mimeMessage.setSentDate( new Date() );
			return mimeMessage;
		} catch (javax.mail.MessagingException mEx) {
			logger.warn("Messaging Exception", mEx);
			IOException e = new IOException("E-Mail could not be sent: "
					+ mEx.getMessage());
			e.initCause(mEx);
			return null;
		}
	}

	/**
	 * Please use getStringOrEmpty(Object value)
	 * @param value
	 * @return
	 */
	public static String emptyIfNull(String value) {
		if (value == null) {
			return "";
		} else {
			return value;
		}
	}


	/**
	 * Please use getStringOrEmpty(Object value)
	 * @param value
	 * @return
	 */
	public static String emptyIfNull(Date value) {
		if (value == null) {
			return "";
		} else {
			return value.toString();
		}
	}

	public static String join( Iterable<? extends Object> pColl ) {
		return join( pColl, ",", null );
	}
	
	public static String join( Iterable<? extends Object> pColl, String separator ) {
		return join( pColl, separator, null );
	}
	
	public static String join( Iterable<? extends Object> pColl, String separator, String textToSeparateFinalIterable ) {
		Iterator<? extends Object> oIter;
		if( pColl == null || ( !( oIter = pColl.iterator() ).hasNext() ) ) {
			return "";
		}
		StringBuilder oBuilder = new StringBuilder( String.valueOf( oIter.next() ) );
		while( oIter.hasNext() ) {
			Object thisNext = oIter.next();
			if (oIter.hasNext() || textToSeparateFinalIterable == null) {
				oBuilder.append( separator ).append( String.valueOf(thisNext) );
			} else {
				//just for the last one
				oBuilder.append( " " ).append( textToSeparateFinalIterable ).append( " " ).append( String.valueOf(thisNext) );
			}
		}
		return oBuilder.toString();
	}

	public static String getFileExtension(File file) {
		String fileName = file.getName();
		if (fileName.indexOf(".") != -1) {
			return fileName.substring(fileName.indexOf("."), fileName.length());
		} else {
			return "";
		}
	}

	public static String getFileNameWithoutExtension(File file) {
		String fileName = file.getName();
		if (fileName.indexOf(".") != -1) {
			return fileName.substring(0, fileName.indexOf("."));
		} else {
			return fileName;
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List getShallowCopy(List listToCopy) {
		List newList = new ArrayList();
		for (int i = 0, n = listToCopy.size(); i < n; i++) {
			newList.add(listToCopy.get(i));
		}

		return newList;
	}

	public static String makeSafeUrlMapping(String unsafeUrl) {
		return unsafeUrl.replace( ' ', '-' )
		  .replace( "+", "-" ) //this one is to help us map url's from the old system for SEO
		  .replace( "%2b", "-" ) //this one is to help us map url's from the old system for SEO
		  .replace( "\"", "" )
		  .replace( "/", "or" )
		  .replace( "'", "" )
		  .replace( "?", "" )
		  .replace( ":", "" )
		  .replace( "#", "" )
		  .replace( ".", "" )
		  .replace( "!", "" )
		  .replace( "�", "" )
		  .replace( "$", "" )
		  .replace( "&", "and" )
		  .replace( "%26", "and" ) //this one is to help us map url's from the old system for SEO
		  .replace( "`", "")
		  .toLowerCase();
	}

	public static void removeSelectItemFromList(List<SelectItem> items, Object toRemove) {
		for (int i=0; i < items.size(); i++) {
			if (items.get(i).getValue().equals(toRemove)) {
				items.remove(i);
				break;
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static List<? extends AplosBean> sortAlphabetically(List<? extends AplosBean> unsortedList) {
		Collections.sort(unsortedList, new AplosBeanDisplayNameComparator());
		return unsortedList;
	}

	private static class AplosBeanDisplayNameComparator implements Comparator {

		@Override
		public int compare(Object bean1, Object bean2) {
			return ((AplosBean)bean1).getDisplayName().compareTo(((AplosBean)bean2).getDisplayName());
		}

	}

	public static String includeContextRootInPaths( String content, String contextRoot, AttributePrefixFinder attributePrefixFinders[] ) {
		String newUrl;
		String oldUrl;
		StringBuffer newUrlBuf;
		StringBuffer oldUrlBuf;
		int mainGroupIdx;
		Pattern pattern;
		Matcher matcher;
		String oldContent;
		String contextRootWithoutSlash = contextRoot.replace( "\\", "" ).replace( "/", "" );
		for( int i = 0, n = attributePrefixFinders.length; i < n; i++ ) {
			pattern = Pattern.compile(attributePrefixFinders[ i ].getRegex());
			oldContent = content;
			mainGroupIdx = attributePrefixFinders[ i ].getMainGroupIdx();
			matcher = pattern.matcher( oldContent );

			while( matcher.find() && !matcher.group( mainGroupIdx - 1 ).startsWith( "http:" ) ) {
				oldUrlBuf = new StringBuffer();
				int j = 1;
				for( int p = mainGroupIdx - 2; j < p; j++ ) {
					if( matcher.group( j ) != null ) {
						oldUrlBuf.append( matcher.group( j ) );
					}
				}
				oldUrl = oldUrlBuf.toString();
				newUrlBuf = new StringBuffer( oldUrl );

				if( !oldUrl.endsWith( "\\" ) && !oldUrl.endsWith( "/" ) ) {
					newUrlBuf.append( "/" );
				}

				newUrlBuf.append( contextRootWithoutSlash ).append( "/" ).append( matcher.group( j ) ).append( matcher.group( j + 1 ) );
				oldUrlBuf.append( matcher.group( j ) ).append( matcher.group( j + 1 ) );
				oldUrl = oldUrlBuf.toString();
				newUrl = newUrlBuf.toString();
				/*
				 * We need to assess each one individually otherwise url's may get included that
				 * we don't want to update, this is why we use the whole of the url in the replace
				 * statement.
				 */
				if( !newUrl.equals( oldUrl ) ) {
					content = content.replace( oldUrl, newUrl );
				}
			}
		}

		return content;
	}

	public static String includeContextRootInPathsForXhtml( String content, String contextRoot ) {
		if( contextRoot != null && !contextRoot.equals( "" ) ) {
			String contextRootWithoutSlash = contextRoot.replace( "\\", "" ).replace( "/", "" );
			AttributePrefixFinder attributePrefixFinders[] = {
				new AttributePrefixFinder( 6, "(?<!include )(src=)(&#39;|'|\")?(/|\\\\)(?!" + contextRootWithoutSlash + ")(.*)(&#39;|'|\"| )" ),
				new AttributePrefixFinder( 5, "(href=)(&#39;|'|\")?(/|\\\\)(?!" + contextRootWithoutSlash + ")(.*)(&#39;|'|\"| )" ) };
			return includeContextRootInPaths(content, contextRootWithoutSlash, attributePrefixFinders );
		} else {
			return content;
		}
	}

	public static String includeContextRootInPathsForCss( String content, String contextRoot ) {
		if( contextRoot != null && !contextRoot.equals( "" ) ) {
			String contextRootWithoutSlash = contextRoot.replace( "\\", "" ).replace( "/", "" );
			AttributePrefixFinder attributePrefixFinders[] = { new AttributePrefixFinder( 7, "(url)(\\()(\\s)?(&#39;|'|\")?(\\\\|/)(?!" + contextRootWithoutSlash + ")(.*)(&#39;|'|\"|\\)| )" ) };
			return includeContextRootInPaths(content, contextRoot, attributePrefixFinders );
		} else {
			return content;
		}
	}

	public static String includeContextRootInPaths( String contextRoot, String content ) {
		if( contextRoot != null && !contextRoot.equals( "" ) ) {
			String contextRootWithoutSlash = contextRoot.replace( "\\", "" ).replace( "/", "" );
			content = includeContextRootInPathsForXhtml(content, contextRootWithoutSlash );
			content = includeContextRootInPathsForCss(content, contextRootWithoutSlash );
		}
		return content;
	}

	private static class AttributePrefixFinder {
		private int mainGroupIdx;
		private String regex;

		public AttributePrefixFinder( int mainGroupIdx, String regex ) {
			this.setMainGroupIdx(mainGroupIdx);
			this.setRegex(regex);
		}

		public void setRegex(String regex) {
			this.regex = regex;
		}

		public String getRegex() {
			return regex;
		}

		public int getMainGroupIdx() {
			return mainGroupIdx;
		}

		public void setMainGroupIdx(int mainGroupIdx) {
			this.mainGroupIdx = mainGroupIdx;
		}
	}

	public static List<SelectItem> sortSelectItemsByDisplayName(List<SelectItem> selectItems) {
		Collections.sort( selectItems, new Comparator<SelectItem>() {
			@Override
			public int compare(SelectItem item1, SelectItem item2) {
				if ((item1 == null || item1.getLabel() == null) && (item2 == null || item2.getLabel() == null)) {
					return 0;
				}
				if (item1 == null || item1.getLabel() == null) {
					return 1;
				}
				if (item2 == null || item2.getLabel() == null) {
					return -1;
				}
				return item1.getLabel().compareToIgnoreCase( item2.getLabel() );
			}
		});
		return selectItems;
	}

}
