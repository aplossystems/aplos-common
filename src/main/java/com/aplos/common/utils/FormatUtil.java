package com.aplos.common.utils;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;

import com.aplos.common.beans.Currency;
import com.aplos.common.enums.ContactTitle;
import com.aplos.common.listeners.AplosContextListener;

@ManagedBean
@SessionScoped
public class FormatUtil {
	public static String[] positionTitles = { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };

	public static String getTitledPosition( int position ) {
		int titleIdx = position;
		if(titleIdx >= 10) {
			titleIdx = titleIdx % 10;
		}
		return position + positionTitles[ titleIdx ];
	}
	
	public static String getVerticalHtmlText( String originalText ) {
		StringBuffer strBuf = new StringBuffer();
		for( int i = 0; i < originalText.length(); i++ ) {
			strBuf.append( originalText.charAt( i ) );
			strBuf.append( "<br/>" );
		}
		return strBuf.toString();
	}
	
	public static String getBooleanText( boolean value ) {
		if( value ) {
			return "Yes";
		} else {
			return "No";
		}
	}
	
	public static String removeExtension( String stringWithExtension ) {
		return stringWithExtension.substring(0, stringWithExtension.lastIndexOf("."));
	}
	
	public static String removeQuotesFromCsvString( String csvString ) {
		if( csvString.startsWith( "\"" ) && csvString.endsWith( "\"" ) ) {
			csvString = csvString.substring( 1, csvString.length() - 1 );
			csvString = csvString.replace( "\"\"", "\"" );
		}
		return csvString;
	}
	
	public static String padIpAddress( String ipAddress ) {
		String[] ipAddressParts = ipAddress.split( "\\." );
		DecimalFormat threeDigitFormat = new DecimalFormat( "000" );
		for( int i = 0, n = ipAddressParts.length; i < n; i++ ) {
			ipAddressParts[ i ] = StringUtils.leftPad( ipAddressParts[ i ], 3, "0" );
		}
		return StringUtils.join( ipAddressParts, "." );
	}
	
	public static String getFullName( ContactTitle contactTitle, String firstName, String surname ) {
		StringBuffer strBuf = new StringBuffer();
		if( contactTitle != null ) {
			strBuf.append( contactTitle.getLabel() );
		}
		if( !CommonUtil.isNullOrEmpty(firstName) ) {
			if( strBuf.length() > 0 ) {
				strBuf.append( " " );
			}
			strBuf.append( firstName );
		}
		if( !CommonUtil.isNullOrEmpty(surname) ) {
			if( strBuf.length() > 0 ) {
				strBuf.append( " " );
			}
			strBuf.append( surname );
		}
		return strBuf.toString();
	}
	
	public static String getFullName( String firstName, String surname ) {
		StringBuffer strBuf = new StringBuffer();
		if( !CommonUtil.isNullOrEmpty(firstName) ) {
			strBuf.append( firstName );
		}
		if( !CommonUtil.isNullOrEmpty(surname) ) {
			if( strBuf.length() > 0 ) {
				strBuf.append( " " );
			}
			strBuf.append( surname );
		}
		return strBuf.toString();
	}

	public static String breakCamelCase(String camelCase) {
		char[] chars = CommonUtil.firstLetterToUpperCase(camelCase).toCharArray();
		int length = camelCase.length();
		StringBuffer buf= new StringBuffer(length + 16);
		for (int i=0; i<length; i++) {
			char current= chars[i];
			if (i!= 0 && Character.isUpperCase(current)) {
				buf.append(' ');
			}
			buf.append(current);
		}
		return buf.toString();
	}

	/**
	 * converts CONSTANT_TOKEN into ConstantToken or constantToken
	 */
	public static String camelToken(String tokenCase) { return camelToken( tokenCase, false); }

	public static void resetTime( Calendar cal ) {
		cal.set( Calendar.HOUR_OF_DAY, 0 );
		cal.set( Calendar.MINUTE, 0 );
		cal.set( Calendar.SECOND, 0 );
		cal.set( Calendar.MILLISECOND, 0 );
	}

	public static String camelToken(String tokenCase, boolean firstToLower) {
		tokenCase = untoken(tokenCase);
		if (!firstToLower) {
			tokenCase = CommonUtil.firstLetterToUpperCase(tokenCase);
		}
		char[] chars = tokenCase.toCharArray();
		int length = tokenCase.length();
		StringBuffer buf= new StringBuffer(length + 16);
		boolean convertUpper = false;
		for (int i=0; i<length; i++) {
			char current= chars[i];
			if (convertUpper) {
				buf.append( String.valueOf(current).toUpperCase() );
				convertUpper = false;
			} else {
				if (i != 0 && ' ' == current) {
					convertUpper = true;
					//continue;
				}
				buf.append(current);
			}
		}
		return buf.toString();
	}

	public static String untoken(String token) {
		token = token.toLowerCase();
		return token.replaceAll("_", " ");
	}

	public static String stripFirstAndLastSlashes( String text ) {
		if( text.startsWith( "/" ) || text.startsWith( "\\" ) ) {
			text = text.substring( 1 );
		}
		if( text.endsWith( "/" ) || text.endsWith( "\\" ) ) {
			text = text.substring( 0, text.length() - 1 );
		}

		return text;
	}

	public static String stdConvertDocumentToString( Document document, AplosContextListener aplosContextListener ) {
		try {
			//set up a transformer
	        TransformerFactory transfac = TransformerFactory.newInstance();
	        Transformer trans = transfac.newTransformer();
	        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

	        //create string from xml tree
	        StringWriter sw = new StringWriter();
	        StreamResult result = new StreamResult(sw);
	        DOMSource source = new DOMSource(document);
	        trans.transform(source, result);
	        return sw.toString();
		} catch ( TransformerConfigurationException tcex ) {
	    	ErrorEmailSender.sendErrorEmail(null,aplosContextListener, tcex );
	    	return "";
	    } catch (TransformerException tex ) {
	    	ErrorEmailSender.sendErrorEmail(null,aplosContextListener, tex );
	    	return "";
		}
	}

	/**
	 * @return String containing only digits
	 */
	public static String stripNonNumeric(String subject) {
		return stripToAllowedCharacters(subject, "0123456789");
	}
	
	public static String stripNonAlphanumeric(String subject) {
		return stripToAllowedCharacters(subject, "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
	}
	
	public static String stripNonAlphabetic(String subject) {
		return stripToAllowedCharacters(subject, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
	}

	/**
	 * @return String containing only digits and anything included in the second parameter
	 * @param additionalCharactersToAllow - E.g. +/-/.
	 */
	public static String stripNonNumeric(String subject, String additionalCharactersToAllow) {
		return stripToAllowedCharacters(subject, "0123456789" + additionalCharactersToAllow);
	}

	/**
	 * @return String containing only characters found in the second parameter
	 */
	public static String stripToAllowedCharacters(String subject, String allowedCharacters) {
		StringBuffer strBuff = new StringBuffer();
		char[] chars = subject.toCharArray();
		for (int i=0; i < chars.length; i++) {
			if (allowedCharacters.contains(String.valueOf(chars[i]))) {
				strBuff.append(chars[i]);
			}
		}
		return strBuff.toString();
	}


	public static SimpleDateFormat getStdSimpleDateFormat() {
		return getStdSimpleDateFormat( CommonUtil.getContextLocale() );
	}

	public static SimpleDateFormat getStdSimpleDateFormat(Locale locale) {
		return new SimpleDateFormat("dd MMM yyyy",locale);
	}
	public static SimpleDateFormat getStdHourMinuteFormat() {
		return new SimpleDateFormat("HH:mm");
	}

	public static SimpleDateFormat getEngSlashSimpleDateFormat() {
		return new SimpleDateFormat("dd/MM/yyyy");
	}

	public static SimpleDateFormat getEngSlashSimpleDateTimeFormat() {
		return new SimpleDateFormat("HH:mm dd/MM/yyyy");
	}

	public static SimpleDateFormat getDBSimpleDateFormat() {
		return new SimpleDateFormat("yyyy-MM-dd");
	}
	
	public static SimpleDateFormat getDBSimpleDateTimeFormat() {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	}

	public static String formatDate(Date date) {
		if( date != null ) {
			return formatDate(getStdSimpleDateFormat(), date);
		} else {
			return "";
		}
	}
	
	public static String formatDateAsAge(Date date) {
		return formatDateAsAge(date, false, false);
	}
	
	public static String formatDateAsAge(Date date, boolean includeMonthsValue, boolean includeYearText) {
		int[] results = convertDateToYearsAndMonths(date);
		String returnVal = null;
		if (includeYearText) {
			returnVal = results[0] + " years";
		} else {
			returnVal = results[0] + "";
		}
		if (includeMonthsValue) {
			returnVal += ", " + results[1];
			if (includeYearText) {
				returnVal += " months";
			}
		}	
		return returnVal;
	}
	
	public static int[] convertDateToYearsAndMonths(Date date) {
		if (date == null) {
			return new int[] {0,0};
		}
		Calendar now = Calendar.getInstance();
		Calendar dob = Calendar.getInstance();
		dob.setTime(date);
		int year1 = now.get(Calendar.YEAR);
		int year2 = dob.get(Calendar.YEAR);
		int age = year1 - year2;
		int nowMonth = now.get(Calendar.MONTH);
		int dobMonth = dob.get(Calendar.MONTH);
		int monthsDiff = 0;
		if (dobMonth > nowMonth) {
		  age--;
		  monthsDiff = 12 - (dobMonth - nowMonth);
		} else if (nowMonth == dobMonth) {
		  int day1 = now.get(Calendar.DAY_OF_MONTH);
		  int day2 = dob.get(Calendar.DAY_OF_MONTH);
		  if (day2 > day1) {
		    age--;
		  }
		} else {
			monthsDiff = nowMonth - dobMonth;
		}
		int[] results = { age, monthsDiff };
		return results;
	}

	public static String formatDateTime(Date date, boolean dateFirst) {
		return formatDateTime(date, dateFirst, true);
	}
	
	public static String formatDateTime(Date date, boolean dateFirst, boolean includeYear) {
		if( date != null ) {
			if (dateFirst) {
				if (includeYear) {
					return formatDate(new SimpleDateFormat("dd MMM yyyy 'at' HH:mm"), date);
				} else {
					return formatDate(new SimpleDateFormat("dd MMM 'at' HH:mm"), date);
				}
			} else {
				if (includeYear) {
					return formatDate(new SimpleDateFormat("HH:mm 'on' dd MMM yyyy"), date);
				} else {
					return formatDate(new SimpleDateFormat("HH:mm 'on' dd MMM"), date);
				}
			}
		} else {
			return "";
		}
	}

	public static String formatTime(Date date) {
		return formatTime(date, true);
	}

	public static String formatTime(Date date, boolean includeSeconds) {
		if( date != null ) {
			if (includeSeconds) {
				return formatDate(new SimpleDateFormat("HH:mm:ss"), date);
			} else {
				return formatDate(new SimpleDateFormat("HH:mm"), date);
			}
		} else {
			return "";
		}
	}

	public static String formatPhoneNumber( String phoneNumber ) {
		if( phoneNumber != null && phoneNumber.length() > 5 ) {
			return phoneNumber.substring( 0, 5 ) + " " + phoneNumber.substring( 5, phoneNumber.length() );
		} else {
			return CommonUtil.getStringOrEmpty( phoneNumber );
		}
	}

	public static Date parseDate(String dateStr) {
		return parseDate(getStdSimpleDateFormat(CommonUtil.getContextLocale()), dateStr);
	}

	public static String formatDate(SimpleDateFormat simpleDateFormat, Date date) {
		if( date != null ) {
			return simpleDateFormat.format(date);
		} else {
			return null;
		}
	}

	public static Date parseDate(SimpleDateFormat simpleDateFormat,
			String dateStr) {
		try {
			return removeDatesTime(simpleDateFormat.parse(dateStr));
		} catch (ParseException parseEx) {
			return null;
		}
	}

	/**
	 * @deprecated  As of AplosCommon V1.0.2, replaced by {@link formatUkCurrency(double value)}
	 */
	@Deprecated
	public static String formatCurrency(double value) {
		return formatUkCurrency(value);
	}

	public static String formatUkCurrency(double value) {
		return "\u00A3" + formatTwoDigit(value);
	}
	
	public static String formatUkCurrency(BigDecimal value) {
		return "\u00A3" + formatTwoDigit(value);
	}

	public static String formatCurrentCurrency(double value) {
		Currency curr = JSFUtil.getBeanFromScope(Currency.class);
		if (curr == null) {
			return formatTwoDigit(value);
		}
		return curr.getPrefix() + formatTwoDigit(value) + curr.getSuffix();
	}

	public static String formatCurrentCurrency(BigDecimal value) {
		return formatCurrentCurrency(value.doubleValue());
	}

	public static String formatTwoDigit( double value, boolean includeCommas ) {
		DecimalFormat twoDig;
		if( includeCommas ) {
			twoDig = new DecimalFormat();
			twoDig.setMinimumFractionDigits(2);
			twoDig.setMaximumFractionDigits(2);
		} else {
			twoDig = new DecimalFormat( "0.00" );
		}
		return twoDig.format(value);
	}
	
	public static String formatHuge( double value ) {
		return formatHuge( value,  false );
	}
	
	public static String formatHuge( double value, boolean decimalPlaces ) {
		DecimalFormat twoDig;
		twoDig = new DecimalFormat();
		if (decimalPlaces) {
			twoDig.setMinimumFractionDigits(2);
			twoDig.setMaximumFractionDigits(2);
		} else {
			twoDig.setMinimumFractionDigits(0);
			twoDig.setMaximumFractionDigits(0);
		}
		return twoDig.format(value);
	}

	public static String formatOneDigit(double value) {
		DecimalFormat oneDig = new DecimalFormat( "0.0" );
		return oneDig.format(value);
	}
	
	public static String formatTwoDigit(double value) {
		return formatTwoDigit(value, true );
	}

	public static String formatTwoDigit(BigDecimal value, BigDecimal defaultIfNull ) {
		if( value == null ) {
			return formatTwoDigit( defaultIfNull.doubleValue(), true );
		} else {
			return formatTwoDigit(value.doubleValue(), true);
		}
	}

	public static BigDecimal twoDigitRoundHalfDown(BigDecimal value) {
		if( value != null ) {
			return value.setScale(2,RoundingMode.HALF_DOWN);
		} else {
			return null;
		}
	}

	public static String formatTwoDigit(BigDecimal value) {
		return formatTwoDigit(value.doubleValue(), true);
	}

	public static String formatTwoDigit(BigDecimal value, boolean includeCommas ) {
		return formatTwoDigit(value.doubleValue(), includeCommas);
	}

	public static String formatDateForDB(Date date) {
		return getDBSimpleDateFormat().format(date);
	}

	public static String formatDateForDB(Date date, boolean addApostrophes ) {
		StringBuffer strBuf = new StringBuffer();
		if( addApostrophes ) {
			strBuf.append("'");
		}
		strBuf.append( getDBSimpleDateFormat().format(date) );
		if( addApostrophes ) {
			strBuf.append("'");
		}
		return strBuf.toString();
	}

	public static String formatDateTimeForDB(Date date) {
		return formatDateTimeForDB(date, false);
	}

	public static String formatDateTimeForDB(Date date, boolean addApostrophes, boolean resetTime ) {
		StringBuffer strBuf = new StringBuffer();
		if( addApostrophes ) {
			strBuf.append("'");
		}
		strBuf.append( formatDateTimeForDB(date, resetTime) );
		if( addApostrophes ) {
			strBuf.append("'");
		}
		return strBuf.toString();
	}
	
	public static String formatDateTimeForDB(Date date, boolean resetTime) {
		if( date != null ) {
			if (resetTime) {
				return getDBSimpleDateTimeFormat().format( removeDatesTime(date) );
			} else {
				return getDBSimpleDateTimeFormat().format( date );
			}
		} else {
			return "";
		}
	}

	public static Date removeDatesTime(Date date) {
		Calendar calendar = Calendar.getInstance();

		// Make sure the calendar will not perform automatic correction.
		calendar.setLenient(false);

		// Set the time of the calendar to the given date.
		calendar.setTime(date);

		// Remove the hours, minutes, seconds and milliseconds.
		resetTime( calendar );

		// Return the date again.
		return calendar.getTime();
	}

	public static String[] getMonthNames() {
		/*
		 *  This is because of YouStudy and should be changed to something more generic
		 *  perhaps adding a boolean to the arguments.
		 */

		String months[];
		if (CommonUtil.getContextLocale().getLanguage().equals("ar")) {
			months = new DateFormatSymbols(CommonUtil.getContextLocale()).getMonths();
		} else {
			months = new DateFormatSymbols(CommonUtil.getContextLocale()).getShortMonths();
		}

		/*
		 *  For some reason the function puts an extra null month at the end so we have to it.
		 */
		if( months.length == 13 ) {
			String newMonths[] = new String[12];
			System.arraycopy( months, 0, newMonths, 0, 12 );
			months = newMonths;
		}

		return months;
	}

	public static String getMonthNameFromDate(Date dateIn) {
		return getMonthNames()[dateIn.getMonth()];
	}

	public static String replaceNewLineCharForHtml( String orignalStr ) {
		return orignalStr.replaceAll( "\n", "<br/>" );
	}

	public static String formatNumber( double number ) {
		// TODO This should be replaced with the DecimalFormat method
		double decimalValue = number % 1;
		int intPart = (int) number / 1;
		StringBuffer strBuf = new StringBuffer();
		int divisor = 1000;
		int remainder = 0;
		boolean firstLoop = true;
		while( intPart > 0 ) {
			if( !firstLoop ) {
				strBuf.insert( 0, new DecimalFormat( "000" ).format( remainder ) );
			}
			remainder = intPart % divisor;
			intPart -= remainder;
			intPart /= 1000;
			if( strBuf.length() != 0 ) {
				strBuf.insert( 0, "," );
			}
			firstLoop = false;
		}
		strBuf.insert( 0, remainder );
		if( decimalValue != 0 ) {
			strBuf.append( "." + decimalValue );
		}
		return strBuf.toString();
	}

}
