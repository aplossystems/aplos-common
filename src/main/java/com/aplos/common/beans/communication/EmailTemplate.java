package com.aplos.common.beans.communication;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.context.FacesContext;

import cb.jdynamite.JDynamiTe;

import com.aplos.common.AplosUrl;
import com.aplos.common.AplosUrl.Protocol;
import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.DiscriminatorColumn;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.Inheritance;
import com.aplos.common.annotations.persistence.InheritanceType;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.annotations.persistence.Transient;
import com.aplos.common.appconstants.AplosAppConstants;
import com.aplos.common.appconstants.AplosScopedBindings;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.CreatedPrintTemplate;
import com.aplos.common.beans.FileDetails;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.enums.EmailFromAddressType;
import com.aplos.common.enums.EmailTemplateEnum;
import com.aplos.common.enums.JsfScope;
import com.aplos.common.interfaces.BulkEmailFinder;
import com.aplos.common.interfaces.BulkEmailSource;
import com.aplos.common.interfaces.EmailGenerator;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;
import com.aplos.common.utils.UrlEncrypter;

@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(length=50)
public abstract class EmailTemplate<EMAIL_SOURCE extends BulkEmailSource, CONTENT_SOURCE extends EmailGenerator> extends AplosBean {
	private static final long serialVersionUID = -1254296044339717425L;

	private String name;
	@Column(columnDefinition="LONGTEXT")
	private String content;
	private String subject;
	@Column(columnDefinition="LONGTEXT")
	private String plainText;
	
	@ManyToOne
	private EmailFrame emailFrame;
	
	@ManyToOne
	private EmailFrame outerEmailFrame;
	
	private EmailFromAddressType emailFromAddressType = EmailFromAddressType.CURRENT_USER;
	private String otherFromAddress;
	
	private boolean isUsingDefaultContent = true;
	@Transient
	private boolean isValidated = false;
	
	private int cachedVersionNumber;
	
	
	public EmailTemplate() {
		this.setDeletable(false);
	}
	
	public void singleMessageSourceSet( AplosEmail aplosEmail, EMAIL_SOURCE bulkEmailSource ) {
		
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
	
	public EmailFromAddressType getDefaultEmailFromAddressType() {
		return EmailFromAddressType.SYSTEM_DEFAULT;
	}
	
	public String getDefaultFromAddress() {
		if( EmailFromAddressType.CURRENT_USER.equals( getEmailFromAddressType() ) ) {
			SystemUser currentUser = JSFUtil.getLoggedInUser();
			if( currentUser != null && !CommonUtil.isNullOrEmpty( currentUser.getEmailAddress() ) ) {
				return currentUser.getEmailAddress();
			}
		} else if( EmailFromAddressType.OTHER.equals( getEmailFromAddressType() ) && !CommonUtil.isNullOrEmpty( getOtherFromAddress() ) ) {
			return getOtherFromAddress();
		} 

		// EmailFromAddressType.SYSTEM_DEFAULT 
		return CommonConfiguration.getCommonConfiguration().getDefaultAdminUser().getEmailAddress();
	}
	
	public boolean removeUnsubscribedByDefault() {
		return false;
	}
	
	public String getEmailAddress( EMAIL_SOURCE bulkEmailSource ) {
		if( bulkEmailSource != null ) {
			return bulkEmailSource.getEmailAddress();
		} else {
			return null;
		}
	}
	
	public int getVersionNumber() {
		return 1;
	}
	
	public List<BulkEmailFinder> getBulkEmailFinders() {
		return null;
	}
	
	public BulkMessageSourceGroup getBulkMessageSourceGroup() {
		return null;
	}
	
	public List<FileDetails> getAttachments( CONTENT_SOURCE emailGenerator ) {
		return new ArrayList<FileDetails>();
	}
	
	public FileDetails generateAttachment(CreatedPrintTemplate createdPrintTemplate, SingleEmailRecord singleEmailRecord,
			 EMAIL_SOURCE bulkEmailSource, CONTENT_SOURCE emailGenerator) {
		return createdPrintTemplate;
	}
	
	public List<FileDetails> getAvailableAttachments( EMAIL_SOURCE bulkEmailSource, CONTENT_SOURCE emailGenerator ) {
		return new ArrayList<FileDetails>();
	}

	public abstract EmailTemplateEnum getEmailTemplateEnum();

	/**
	 * Specify whether we send to admin automatically, when we are not in debug mode
	 * @return
	 */
	public boolean isSendToAdminAlsoByDefault( EMAIL_SOURCE bulkEmailRecipient ) {
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
	public final Class<EMAIL_SOURCE> getRequiredSourceClassForCompile() {
		Class<?> emailClass = getClass();
		ParameterizedType parametizedSuperclass = null;
		while( parametizedSuperclass == null ) {
			if( emailClass.getGenericSuperclass() instanceof ParameterizedType ) {
				parametizedSuperclass = (ParameterizedType) emailClass.getGenericSuperclass();
			} else {
				emailClass = emailClass.getSuperclass();
			}
		}

        return (Class<EMAIL_SOURCE>) parametizedSuperclass.getActualTypeArguments()[0]; 
	}
	
	public String postProcessSubject(EMAIL_SOURCE bulkEmailRecipient, CONTENT_SOURCE emailGenerator, String subject) {
		return subject;
	}
	/**
	 * This method will parse the subject string provided, with jDynamite and populate the tags using
	 * the bulkeEmail Recipient. {@link #getRequiredSourceClass()#cast(bulkEmailRecipient)} can be used
	 * to get the object as the type you require to parse the content.
	 * @param bulkEmailRecipient
	 * @param subject
	 * @param subscriberIndex
	 * @return
	 * @throws ClassCastException
	 * @throws IOException
	 */
	public String compileSubject(EMAIL_SOURCE bulkEmailRecipient, CONTENT_SOURCE emailGenerator, String subject) throws ClassCastException,IOException {
		JDynamiTe dynamiTe = new JDynamiTe();
		dynamiTe.setInput(new ByteArrayInputStream(subject.getBytes()));
		addSubjectJDynamiTeValues( dynamiTe, bulkEmailRecipient, emailGenerator); 
		dynamiTe.parse();
		return postProcessSubject( bulkEmailRecipient, emailGenerator, dynamiTe.toString() );
	}
	/**
	 * This method will parse the content string provided, with jDynamite and populate the tags using
	 * the bulkeEmail Recipient. {@link #getRequiredSourceClass()#cast(bulkEmailRecipient)} can be used
	 * to get the object as the type you require to parse the content.
	 * @param bulkEmailRecipient
	 * @param content
	 * @param subscriberIndex - this is used if you need to know which subscriber to get from the soruce if it holds multiple
	 * @return
	 * @throws ClassCastException
	 * @throws IOException
	 */
	public String compileContent(EMAIL_SOURCE bulkEmailRecipient, CONTENT_SOURCE emailGenerator, String content, SingleEmailRecord singleEmailRecord) throws ClassCastException,IOException {
		JDynamiTe dynamiTe = new JDynamiTe();
		dynamiTe.setInput(new ByteArrayInputStream(content.getBytes()));
		addContentJDynamiTeValues( dynamiTe, bulkEmailRecipient, emailGenerator, singleEmailRecord); 
		dynamiTe.parse();
		return dynamiTe.toString();
	}
	
	public void addContentJDynamiTeValues(JDynamiTe jDynamiTe, EMAIL_SOURCE bulkEmailRecipient, CONTENT_SOURCE emailGenerator, SingleEmailRecord singleEmailRecord) {
		addCommonJDynamiTeValues(jDynamiTe, singleEmailRecord);
	}
	
	public void addSubjectJDynamiTeValues(JDynamiTe jDynamiTe, EMAIL_SOURCE bulkEmailRecipient, CONTENT_SOURCE emailGenerator ) {
		addCommonJDynamiTeValues(jDynamiTe, null);
	}
	
	public static void addCommonJDynamiTeValues(JDynamiTe jDynamiTe, SingleEmailRecord singleEmailRecord ) {
		Set keySet = new HashSet();
		Enumeration variableKeys = jDynamiTe.getVariableKeys();
		while( variableKeys.hasMoreElements() ) {
			keySet.add( variableKeys.nextElement() );
		}
		
		addCommonJDynamiTeValues(jDynamiTe, singleEmailRecord, keySet);
	}
	
	public static void addCommonJDynamiTeValues(JDynamiTe jDynamiTe, SingleEmailRecord singleEmailRecord, Set keySet ) {
		if( keySet.contains( "CURRENT_USER_SIGNATURE" ) ) {
			if( JSFUtil.getLoggedInUser() != null ) {
				if( CommonUtil.isNullOrEmpty( JSFUtil.getLoggedInUser().getEmailSignature() ) ) {
					jDynamiTe.setVariable("CURRENT_USER_SIGNATURE", CommonUtil.getStringOrEmpty( JSFUtil.getLoggedInUser().getFirstName() ) );
				} else {
					jDynamiTe.setVariable("CURRENT_USER_SIGNATURE", CommonUtil.getStringOrEmpty( JSFUtil.getLoggedInUser().getEmailSignature() ) );
				}
			}
		}

		if( keySet.contains( "COMPANY_NAME" ) ) {
			String companyName = CommonConfiguration.getCommonConfiguration().getDefaultCompanyDetails().getCompanyName();
			if (companyName == null) {
				companyName = ApplicationUtil.getAplosContextListener().getImplementationModule().getPackageDisplayName();
			}
			jDynamiTe.setVariable( "COMPANY_NAME", companyName );
		}
		
		if( keySet.contains( "TRACKING_ACTION" ) && singleEmailRecord != null ) {
			if( singleEmailRecord.isNew() ) {
				singleEmailRecord.saveDetails();
			}
    		UrlEncrypter urlEncrypter = singleEmailRecord.getAplosEmail().getOrCreateUrlEncrypter();
    		StringBuffer queryStrBuf = new StringBuffer();
    		queryStrBuf.append( AplosAppConstants.EMAIL_TRACKING ).append( "=" );
    		queryStrBuf.append( urlEncrypter.encrypt(singleEmailRecord.getId().toString()) );
    		queryStrBuf.append( "&" ).append( AplosScopedBindings.ID ).append( "=" );
    		queryStrBuf.append( singleEmailRecord.getAplosEmail().getId().toString() ).append( "&" );
    		queryStrBuf.append( AplosAppConstants.TRACKING_TYPE ).append( "=" ).append( "a" );
			jDynamiTe.setVariable( "TRACKING_ACTION", queryStrBuf.toString() );
		}

		if( keySet.contains( "EMAIL_TRACKER" ) && singleEmailRecord != null ) {
			AplosUrl aplosUrl = new AplosUrl( "images/blank.gif" );
			if( singleEmailRecord.isNew() ) {
				singleEmailRecord.saveDetails();
			}
			aplosUrl.setHost(singleEmailRecord.getAplosEmail().getParentWebsite());
			aplosUrl.setScheme(Protocol.HTTPS);
    		UrlEncrypter urlEncrypter = singleEmailRecord.getAplosEmail().getOrCreateUrlEncrypter();
			aplosUrl.addQueryParameter( AplosAppConstants.EMAIL_TRACKING, urlEncrypter.encrypt(singleEmailRecord.getId().toString()) );
			aplosUrl.addQueryParameter( AplosScopedBindings.ID, singleEmailRecord.getAplosEmail().getId().toString() );
			aplosUrl.addQueryParameter( AplosAppConstants.TRACKING_TYPE, "o" );
			jDynamiTe.setVariable( "EMAIL_TRACKER", "<img src='" + aplosUrl.toString() + "' />" );
		}
	}

	public String compileSubject(EMAIL_SOURCE bulkEmailRecipient, CONTENT_SOURCE emailGenerator) throws ClassCastException,IOException {
		return compileSubject(bulkEmailRecipient, emailGenerator, determineSubject());
	}

	public String compileContent(EMAIL_SOURCE bulkEmailRecipient, CONTENT_SOURCE emailGenerator, SingleEmailRecord singleEmailRecord) throws ClassCastException,IOException {
		return compileContent(bulkEmailRecipient, emailGenerator, determineContent(), singleEmailRecord);
	}

	public void loadDefaultValues() {
		setSubject( getDefaultSubject() );
		setContent( getDefaultContent() );
		setPlainText( getDefaultPlainText() );
		setName( getDefaultName() );
		setEmailFromAddressType(getDefaultEmailFromAddressType());
	}

	public void loadDefaultValuesAndSave() {
		loadDefaultValues();
		setOuterEmailFrame(CommonConfiguration.getCommonConfiguration().getOuterEmailFrame());
		saveDetails( JSFUtil.getLoggedInUser() );
	}

	public String determineSubject() {
		if( getSubject() == null ) {
			return getDefaultSubject();
		} else {
			return getSubject();
		}
	}

	public String determineContent() {
		if( getContent() == null || getContent().equals( AplosAppConstants.DEFAULT_CKEDITOR_CONTENT ) ) {
			return getDefaultContent();
		} else {
			return getContent();
		}
	}

	public String getDefaultSubject() {
		return getDefaultName();
	}

	public String getDefaultContent() {
		return null;
	}

	public String getDefaultPlainText() {
		return null;
	}
	
	public abstract String getDefaultName();

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

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getSubject() {
		return subject;
	}

	public String loadBodyFromFile( String templateFileName ) {
		URL url = JSFUtil.checkFileLocations(templateFileName, "resources/templates/emailtemplates/", true );
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
		addToScope(getBinding(EmailTemplate.class), this, associatedBeanScope);
	}

	public EmailFrame getEmailFrame() {
		return emailFrame;
	}

	public void setEmailFrame(EmailFrame emailFrame) {
		this.emailFrame = emailFrame;
	}

	public EmailFrame getOuterEmailFrame() {
		return outerEmailFrame;
	}

	public void setOuterEmailFrame(EmailFrame outerEmailFrame) {
		this.outerEmailFrame = outerEmailFrame;
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

	public EmailFromAddressType getEmailFromAddressType() {
		return emailFromAddressType;
	}

	public void setEmailFromAddressType(EmailFromAddressType emailFromAddressType) {
		this.emailFromAddressType = emailFromAddressType;
	}

	public String getOtherFromAddress() {
		return otherFromAddress;
	}

	public void setOtherFromAddress(String otherFromAddress) {
		this.otherFromAddress = otherFromAddress;
	}

	public String getPlainText() {
		return plainText;
	}

	public void setPlainText(String plainText) {
		this.plainText = plainText;
	}

	/**
	 * @return the isValidated
	 */
	public boolean isValidated() {
		return isValidated;
	}

	/**
	 * @param isValidated the isValidated to set
	 */
	public void setValidated(boolean isValidated) {
		this.isValidated = isValidated;
	}
}
