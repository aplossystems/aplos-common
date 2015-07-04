package com.aplos.common.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

import org.w3c.dom.Document;

import com.aplos.common.listeners.AplosContextListener;

@ManagedBean
@ApplicationScoped
public class FormatUtilJsfHelper {

	public String breakCamelCase(String camelCase) {
		return FormatUtil.breakCamelCase(camelCase);
	}

	public String stripFirstAndLastSlashes( String text ) {
		return FormatUtil.stripFirstAndLastSlashes(text);
	}

	public String stdConvertDocumentToString( Document document, AplosContextListener aplosContextListener ) {
		return FormatUtil.stdConvertDocumentToString(document, aplosContextListener);
	}

	/**
	 * @return String containing only digits
	 */
	public String stripNonNumeric(String subject) {
		return FormatUtil.stripNonNumeric(subject);
	}

	/**
	 * @return String containing only digits and anything included in the second parameter
	 * @param additionalCharactersToAllow - E.g. +/-/.
	 */
	public String stripNonNumeric(String subject, String additionalCharactersToAllow) {
		return stripToAllowedCharacters(subject, "0123456789" + additionalCharactersToAllow);
	}

	/**
	 * @return String containing only characters found in the second parameter
	 */
	public String stripToAllowedCharacters(String subject, String allowedCharacters) {
		return FormatUtil.stripToAllowedCharacters(subject, allowedCharacters);
	}

	public SimpleDateFormat getStdSimpleDateFormat() {
		return FormatUtil.getStdSimpleDateFormat();
	}
	public SimpleDateFormat getStdHourMinuteFormat() {
		return FormatUtil.getStdHourMinuteFormat();
	}

	public SimpleDateFormat getEngSlashSimpleDateFormat() {
		return FormatUtil.getEngSlashSimpleDateFormat();
	}

	public SimpleDateFormat getDBSimpleDateFormat() {
		return FormatUtil.getDBSimpleDateFormat();
	}

	public String formatDate(Date date) {
		return FormatUtil.formatDate(date);
	}

	public String formatTime(Date date) {
		return FormatUtil.formatTime(date);
	}

	public String formatPhoneNumber( String phoneNumber ) {
		return FormatUtil.formatPhoneNumber(phoneNumber);
	}

	public Date parseDate(String dateStr) {
		return FormatUtil.parseDate(dateStr);
	}

	public String formatDate(SimpleDateFormat simpleDataFormat, Date date) {
		return FormatUtil.formatDate(simpleDataFormat, date);
	}

	public Date parseDate(SimpleDateFormat simpleDateFormat,
			String dateStr) {
		return FormatUtil.parseDate(simpleDateFormat, dateStr);
	}

	public String formatUkCurrency(double value) {
		return FormatUtil.formatUkCurrency(value);
	}

	public String formatTwoDigit(double value) {
		return FormatUtil.formatTwoDigit(value);
	}

	public String formatDateForDB(Date date) {
		return FormatUtil.formatDateForDB(date);
	}

	public Date removeDatesTime(Date date) {
		return FormatUtil.removeDatesTime(date);
	}

	public String getMonthNameFromDate(Date dateIn) {
		return FormatUtil.getMonthNameFromDate(dateIn);
	}

	public String replaceNewLineCharForHtml( String orignalStr ) {
		return FormatUtil.replaceNewLineCharForHtml(orignalStr);
	}

	public String formatNumber( int number ) {
		return FormatUtil.formatNumber(number);
	}

}
