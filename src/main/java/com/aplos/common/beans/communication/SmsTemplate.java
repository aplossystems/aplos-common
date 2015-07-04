package com.aplos.common.beans.communication;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.net.URL;
import java.util.List;

import javax.faces.context.FacesContext;

import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.DiscriminatorColumn;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.Inheritance;
import com.aplos.common.annotations.persistence.InheritanceType;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.InternationalNumber;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.enums.JsfScope;
import com.aplos.common.interfaces.BulkEmailSource;
import com.aplos.common.interfaces.BulkSmsFinder;
import com.aplos.common.interfaces.BulkSmsSource;
import com.aplos.common.interfaces.SmsGenerator;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(length=50)
public abstract class SmsTemplate<SMS_SOURCE extends BulkSmsSource, CONTENT_SOURCE extends SmsGenerator> extends AplosBean {
	private static final long serialVersionUID = -1254296044339717425L;

	private String name;
	@Column(columnDefinition="LONGTEXT")
	private String content;
	private Class<? extends SmsGenerator> smsGeneratorClass;
	
	private boolean isUsingDefaultContent = true;
	
	private int cachedVersionNumber;
	
	
	public SmsTemplate() {
		this.setDeletable(false);
	}
	
	public void checkVersion( boolean forceUpdate ) {
		if( (isUsingDefaultContent() && (forceUpdate || getVersionNumber() != getCachedVersionNumber())) ) {
			SmsTemplate saveableTemplate = this.getSaveableBean();
			saveableTemplate.setContent(getDefaultContent());
			saveableTemplate.setCachedVersionNumber(getVersionNumber());
			saveableTemplate.saveDetails();
		}
	}
	
	@Override
	public String getDisplayName() {
		return getName();
	}
	
	public String getEditorCssUrl( AplosEmail aplosEmail ) {
		return null;
	}

	/**
	 * Safely makes the current user available to email templates which are being
	 * executed outside of the JSF lifecycle (Scheduled Jobs, Print Templates, etc.)
	 * @return
	 */
	public SystemUser getCurrentUser() {
		if ( FacesContext.getCurrentInstance() == null || JSFUtil.getLoggedInUser() == null ) {
			return CommonConfiguration.getCommonConfiguration().getDefaultAdminUser();
		} else {
			return JSFUtil.getLoggedInUser();
		}
	}
	
	public String getDefaultFromAddress() {
		return CommonConfiguration.getCommonConfiguration().getDefaultAdminUser().getEmailAddress();
	}
	
	public boolean removeUnsubscribedByDefault() {
		return false;
	}
	
	public InternationalNumber getMobileNumber( SMS_SOURCE smsSource ) {
		return smsSource.getInternationalNumber();
	}
	
	public int getVersionNumber() {
		return 1;
	}
	
	public List<BulkSmsFinder> getSmsFinders() {
		return null;
	}
	
	public BulkMessageSourceGroup getBulkMessageSourceGroup() {
		return null;
	}
	
	public boolean isContentSourceARecipient() {
		return true;
	}

	/**
	 * Specify whether we send to admin automatically, when we are not in debug mode
	 * @return
	 */
	public boolean isSendToAdminAlsoByDefault( SMS_SOURCE smsRecipient ) {
		return false;
	}
	/**
	 * This method will return an object of the class specified by {@link #getRequiredSourceClassForCompile()}
	 * will its fields populated by test data so it is suitable for use with {@link #compileContent(BulkMessageSource_OLD_REMOVE, String, int)}
	 * This object will be used when sending a test email to the admin user. As such the subscriber email
	 * should be populated with the adminUser's email address. You can use adminUser.getSourceSubscribers().get(0) for this.
	 * @param adminUser
	 * @return
	 */
	public BulkEmailSource getTestSource(SystemUser adminUser) {
		//Implement this in individual templates to allow access to the 'send tester' feature on the send bulk email page
		return null;
	}
	/**
	 * This method will return the class of BulkEmailSource expected by {@link #compileContent(BulkMessageSource_OLD_REMOVE, String, int)}
	 * @return
	 */
	public final Class<SMS_SOURCE> getRequiredSourceClassForCompile() {
		Class<?> emailClass = getClass();
		ParameterizedType parametizedSuperclass = null;
		while( parametizedSuperclass == null ) {
			if( emailClass.getGenericSuperclass() instanceof ParameterizedType ) {
				parametizedSuperclass = (ParameterizedType) emailClass.getGenericSuperclass();
			} else {
				emailClass = emailClass.getSuperclass();
			}
		}

        return (Class<SMS_SOURCE>) parametizedSuperclass.getActualTypeArguments()[0]; 
	}
	/**
	 * This method will parse the content string provided, with jDynamite and populate the tags using
	 * the bulkeEmail Recipient. {@link #getRequiredSourceClass()#cast(smsRecipient)} can be used
	 * to get the object as the type you require to parse the content.
	 * @param smsRecipient
	 * @param content
	 * @param subscriberIndex - this is used if you need to know which subscriber to get from the soruce if it holds multiple
	 * @return
	 * @throws ClassCastException
	 * @throws IOException
	 */
	public abstract String compileContent(SMS_SOURCE smsRecipient, CONTENT_SOURCE smsGenerator, String content) throws ClassCastException,IOException;

	public String compileContent(SMS_SOURCE smsRecipient, CONTENT_SOURCE smsGenerator) throws ClassCastException,IOException {
		return compileContent(smsRecipient, smsGenerator, determineContent());
	}

	public void loadDefaultValues() {
		setContent( getDefaultContent() );
		setName( getDefaultName() );
	}

	public void loadDefaultValuesAndSave() {
		loadDefaultValues();
		saveDetails( JSFUtil.getLoggedInUser() );
	}

	public String determineContent() {
		if( getContent() == null ) {
			return getDefaultContent();
		} else {
			return getContent();
		}
	}

	public String getDefaultSubject() {
		return null;
	}

	public String getDefaultContent() {
		return null;
	}
	
	public String getDefaultName() {
		return null;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String loadBodyFromFile( String templateFileName ) {
		URL url = JSFUtil.checkFileLocations(templateFileName, "resources/templates/smstemplates/", true );
		if (url != null) {
			try {
				String templateContent = new String(CommonUtil.readEntireFile( url.openStream() ));
				return templateContent;
			} catch (IOException e) {
				ApplicationUtil.getAplosContextListener().handleError( e );
			}
		}
		return null;
	}
	
	@Override
	public void addToScope(JsfScope associatedBeanScope) {
		super.addToScope(associatedBeanScope);
		addToScope(getBinding(SmsTemplate.class), this, associatedBeanScope);
	}

	public boolean isUsingDefaultContent() {
		return isUsingDefaultContent;
	}

	public void setUsingDefaultContent(boolean isUsingDefaultContent) {
		this.isUsingDefaultContent = isUsingDefaultContent;
	}

	public int getCachedVersionNumber() {
		return cachedVersionNumber;
	}

	public void setCachedVersionNumber(int cachedVersionNumber) {
		this.cachedVersionNumber = cachedVersionNumber;
	}

	public Class<? extends SmsGenerator> getSmsGeneratorClass() {
		return smsGeneratorClass;
	}

	public void setSmsGeneratorClass(Class<? extends SmsGenerator> smsGeneratorClass) {
		this.smsGeneratorClass = smsGeneratorClass;
	}
}
