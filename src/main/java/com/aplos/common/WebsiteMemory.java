package com.aplos.common;

import java.util.HashMap;
import java.util.Map;

import com.aplos.common.beans.DynamicBundle;
import com.aplos.common.beans.communication.BasicBulkMessageFinder;
import com.aplos.common.beans.communication.EmailTemplate;
import com.aplos.common.beans.communication.SmsTemplate;
import com.aplos.common.enums.BulkMessageFinderEnum;
import com.aplos.common.enums.EmailTemplateEnum;
import com.aplos.common.templates.PrintTemplate;

public class WebsiteMemory {
	private DynamicBundle mainDynamicBundle;
	private Map<EmailTemplateEnum, EmailTemplate> emailTemplateMap = new HashMap<EmailTemplateEnum, EmailTemplate>();
	private Map<Class<? extends SmsTemplate>, SmsTemplate> smsTemplateMap = new HashMap<Class<? extends SmsTemplate>, SmsTemplate>();
	private Map<Class<? extends PrintTemplate>, PrintTemplate> printTemplateMap = new HashMap<Class<? extends PrintTemplate>, PrintTemplate>();
	private Map<BulkMessageFinderEnum, BasicBulkMessageFinder> bulkMessageFinderMap = new HashMap<BulkMessageFinderEnum, BasicBulkMessageFinder>();

	public DynamicBundle getMainDynamicBundle() {
		return mainDynamicBundle;
	}
	public void setMainDynamicBundle(DynamicBundle mainDynamicBundle) {
		this.mainDynamicBundle = mainDynamicBundle;
	}
	public Map<EmailTemplateEnum, EmailTemplate> getEmailTemplateMap() {
		return emailTemplateMap;
	}
	public void setEmailTemplateMap(Map<EmailTemplateEnum, EmailTemplate> emailTemplateMap) {
		this.emailTemplateMap = emailTemplateMap;
	}
	public Map<BulkMessageFinderEnum, BasicBulkMessageFinder> getBulkMessageFinderMap() {
		return bulkMessageFinderMap;
	}
	public void setBulkMessageFinderMap(Map<BulkMessageFinderEnum, BasicBulkMessageFinder> bulkMessageFinderMap) {
		this.bulkMessageFinderMap = bulkMessageFinderMap;
	}
	public Map<Class<? extends PrintTemplate>, PrintTemplate> getPrintTemplateMap() {
		return printTemplateMap;
	}
	public void setPrintTemplateMap(Map<Class<? extends PrintTemplate>, PrintTemplate> printTemplateMap) {
		this.printTemplateMap = printTemplateMap;
	}
	public Map<Class<? extends SmsTemplate>, SmsTemplate> getSmsTemplateMap() {
		return smsTemplateMap;
	}
	public void setSmsTemplateMap(Map<Class<? extends SmsTemplate>, SmsTemplate> smsTemplateMap) {
		this.smsTemplateMap = smsTemplateMap;
	}
}
