package com.aplos.common.beans.communication;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;

import com.aplos.common.annotations.DynamicMetaValues;
import com.aplos.common.annotations.PluralDisplayName;
import com.aplos.common.annotations.persistence.Any;
import com.aplos.common.annotations.persistence.AnyMetaDef;
import com.aplos.common.annotations.persistence.Cascade;
import com.aplos.common.annotations.persistence.CascadeType;
import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.FetchType;
import com.aplos.common.annotations.persistence.JoinColumn;
import com.aplos.common.annotations.persistence.JoinTable;
import com.aplos.common.annotations.persistence.ManyToAny;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.annotations.persistence.OneToMany;
import com.aplos.common.annotations.persistence.Transient;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.beans.Website;
import com.aplos.common.enums.CommonEmailTemplateEnum;
import com.aplos.common.enums.JsfScope;
import com.aplos.common.enums.MessageGenerationType;
import com.aplos.common.enums.SmsStatus;
import com.aplos.common.interfaces.BulkMessageSource;
import com.aplos.common.interfaces.BulkSmsSource;
import com.aplos.common.interfaces.EmailGenerator;
import com.aplos.common.interfaces.SmsGenerator;
import com.aplos.common.interfaces.SmsMessageOwner;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.threads.BulkSmsSender;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.FormatUtil;
import com.aplos.common.utils.JSFUtil;
import com.nexmo.client.NexmoClient;
import com.nexmo.client.auth.AuthMethod;
import com.nexmo.client.auth.TokenAuthMethod;
import com.nexmo.client.sms.SmsSubmissionResult;
import com.nexmo.client.sms.messages.TextMessage;

@Entity
@PluralDisplayName(name="SMS messages")
public class SmsMessage extends AplosBean implements EmailGenerator {
	private static final long serialVersionUID = -4632770220618449407L;

    @ManyToAny( metaColumn = @Column( name = "message_source_type" ), fetch=FetchType.LAZY )
    @AnyMetaDef( idType = "long", metaType = "string", metaValues = { /* Meta Values added in at run-time */ } )
    @JoinTable( inverseJoinColumns = @JoinColumn( name = "messageSources_id" ) )
    @DynamicMetaValues
	private List<BulkMessageSource> messageSourceList = new ArrayList<BulkMessageSource>();
    @Any( metaColumn = @Column( name = "sms_generator_type" ) )
    @AnyMetaDef( idType = "long", metaType = "string", metaValues = { /* Meta Values added in at run-time */ } )
    @JoinColumn( name = "sms_generator_id" )
    @DynamicMetaValues
    private SmsGenerator smsGenerator;

	@Any( metaColumn = @Column( name = "smsMessageOwner_type" ) )
    @AnyMetaDef( idType = "long", metaType = "string", metaValues = {
    		/* Meta Values added in at run-time */ } )
    @JoinColumn( name = "smsMessageOwner_id" )
	@DynamicMetaValues
	private SmsMessageOwner smsMessageOwner;

	@ManyToOne
	private SmsAccount smsAccount;

	private String name;
	private String sourceNumber;
	@Column(columnDefinition="LONGTEXT") 
	private String content;
	private String messageId;
	private int creditsUsed;
	private int recipientCount;
	private int retryAttempts = 0;
	private SmsStatus smsStatus = SmsStatus.UNSENT;
	@OneToMany(fetch=FetchType.LAZY)
	@Cascade({CascadeType.ALL})
	private List<SmsMessagePart> messageParts = new ArrayList<SmsMessagePart>(); 
	private MessageGenerationType smsGenerationType = MessageGenerationType.NONE;
	@ManyToOne(fetch=FetchType.LAZY)
	private SmsTemplate smsTemplate; 
	private boolean isRemovingDuplicateNumbers = true;
	private boolean isUsingSmsSourceAsOwner = true;
	private Date smsSentDate;
	
	@Transient 
	private String saveableContent;
	@Transient
	private Set<BulkSmsSource> filteredSourceSet;
	
	
	public SmsMessage() {
	}
	
	public SmsMessage( BulkMessageSource bulkMessageSource ) {
		init();
		getMessageSourceList().clear();
		getMessageSourceList().add( bulkMessageSource );
		if( bulkMessageSource instanceof BulkMessageSourceGroup ) {
			setSmsGenerationType(MessageGenerationType.MESSAGE_GROUPS);
		} else {
			setSmsGenerationType(MessageGenerationType.SINGLE_SOURCE);
		}
	} 
	
	public SmsMessage( Class<? extends SmsTemplate> smsTemplateClass, BulkMessageSourceGroup bulkMessageSourceGroup) {
		init();
		getMessageSourceList().clear();
		updateSmsTemplate(smsTemplateClass);
		getMessageSourceList().add( bulkMessageSourceGroup );
		setSmsGenerationType(MessageGenerationType.MESSAGE_GROUPS);
	}
	
	public void init() {
		CommonConfiguration commonConfiguration = CommonConfiguration.getCommonConfiguration();
		if( commonConfiguration != null ) {
			updateSmsAccount( commonConfiguration.getDefaultSmsAccount() );
		}
	}
	
	public int determineCharsPerMessage() {
        return determineCharsPerMessage( getContent() );
	}
	
	public static int determineCharsPerMessage( String content ) {
        Charset asciiCharset = Charset.forName("UTF-8");
		CharsetEncoder asciiEncoder = asciiCharset.newEncoder();
        if( asciiEncoder.canEncode( content ) ) {
        	return 160;
        } else {
        	return 70;
        }
	}
	
	public void updateSmsAccount( SmsAccount smsAccount ) {
		setSmsAccount(smsAccount);
		if( getSmsAccount() != null ) {
			setSourceNumber( getSmsAccount().determineSourceNumber() );
		}
	}
	
	@Override
	public <T> T initialiseNewBean() {
		return super.initialiseNewBean();
	}
	
	public String getFirstSmsSourceFullName() {
		BulkSmsSource firstSmsSource = getFirstSmsSource();
		if( firstSmsSource != null ) {
			return FormatUtil.getFullName( firstSmsSource.getFirstName(), firstSmsSource.getSurname() );	
		} else {
			return null;
		}
	}
	
	@Override
	public void addToScope(JsfScope associatedBeanScope) {
		super.addToScope(associatedBeanScope);
		if( !getClass().equals( SmsMessage.class ) ) {
			addToScope( JSFUtil.getBinding( SmsMessage.class ), this, associatedBeanScope );
		}
	}
	
	public BulkSmsSource getFirstSmsSource() {
		for( int i = 0, n = getMessageSourceList().size(); i < n; i++ ) {
			if( getMessageSourceList().get( 0 ) instanceof BulkSmsSource ) {
				return (BulkSmsSource) getMessageSourceList().get( 0 );
			}
		} 
		return null; 
	}
	
//	@Override
//	public void hibernateInitialiseAfterCheck(boolean fullInitialisation) {
//		super.hibernateInitialiseAfterCheck(fullInitialisation);
//		HibernateUtil.initialiseList(getMessageParts(), fullInitialisation);
//		HibernateUtil.initialise(getSmsTemplate(), fullInitialisation);
//
//		if( HibernateUtil.isInstanceOf( getSmsGenerator(), HibernateInitialisable.class ) ) {
//			HibernateUtil.initialise((HibernateInitialisable)getSmsGenerator(),false);
//		}
//		if( fullInitialisation ) {
//			HibernateUtil.initialiseList(getMessageSourceList(),false);
//		}
//	}
	
	public void updateSmsTemplate( Class<? extends SmsTemplate> smsTemplateClass ) {
		SmsTemplate smsTemplate = Website.getCurrentWebsiteFromTabSession().loadSmsTemplate(smsTemplateClass, true);
		updateSmsTemplate( smsTemplate );
	}
	
	public void updateSmsTemplate( SmsTemplate smsTemplate ) {
		if( smsTemplate != null ) {
			smsTemplate.checkVersion( ApplicationUtil.getAplosContextListener().isDebugMode() );
			setContent(smsTemplate.getContent());
		} 
		setSmsTemplate( smsTemplate );
	}
	
	public void addSmsMessagePart(SmsMessagePart smsMessagePart) {
		getMessageParts().add(smsMessagePart);
	}
	
	@Override
	public void saveBean(SystemUser currentUser) {
		if( getSaveableContent() != null ) {
			setContent( getSaveableContent() );
		}
		for( int i = 0, n = getMessageSourceList().size(); i < n; i++ ) {
			if( getMessageSourceList().get( i ) instanceof KeyedBulkSmsSource 
					&& ((KeyedBulkSmsSource)getMessageSourceList().get( i )).isNew() ) {
				((KeyedBulkSmsSource) getMessageSourceList().get( i )).saveDetails();
			}
		}
		super.saveBean(currentUser);
	}
	
	public int determineRecipientCount() {
		if( getFilteredSourceSet() == null ) {
			filterMessageSourceList();
		}
		return getFilteredSourceSet().size();
	}
	
	public void filterMessageSourceList() {
		Set<BulkSmsSource> resultsToReturn = new HashSet<BulkSmsSource>();
		boolean removeUnsubscribed = false;
		if( getSmsTemplate() != null && !MessageGenerationType.SINGLE_SOURCE.equals( getSmsGenerationType() ) ) {
			removeUnsubscribed = getSmsTemplate().removeUnsubscribedByDefault();
		}
		BulkMessageSourceGroup.filterBulkSmsSources( this, getMessageSourceList(), resultsToReturn, removeUnsubscribed);
		setFilteredSourceSet( resultsToReturn );
	}

	public boolean sendSmsMessageToQueue() {
		return sendSmsMessageToQueue(true);
	}

	public boolean sendSmsMessageToQueue( boolean saveSms ) {
		if( getSmsSentDate() == null ) {
			setSmsStatus(SmsStatus.SENDING);
			setSmsSentDate(new Date());
			if ( CommonUtil.isNullOrEmpty( getSourceNumber() ) ) {
				JSFUtil.addMessage( "A source number was not set for the SMS message" );
				return false;
			}
			if( getFilteredSourceSet() == null ) {
				filterMessageSourceList();
			}
			if( CommonUtil.isNullOrEmpty( getSourceNumber() ) && getSmsAccount() != null ) {
				getSmsAccount().determineSourceNumber();
			}
			Set<BulkSmsSource> smsSourceSet = getFilteredSourceSet();
	
			Logger logger = Logger.getLogger(CommonUtil.class.getName());
			
			try {
		        if( saveSms ) {
		        	String tempSaveableContent = getSaveableContent();
		        	String tempCurrentContent = getContent();
		        	saveDetails();
		        	setContent( tempCurrentContent );
		        	setSaveableContent(tempSaveableContent);
		        }
				
			   BulkSmsSender bulkSmsSender = new BulkSmsSender( this, smsSourceSet );
			   bulkSmsSender.startSendingSmsMessages();
		        
		        if( saveSms ) {
					saveDetails();
		        }
	
		        updateSmsMessageOwner( smsSourceSet, true );

				if( !ApplicationUtil.getAplosContextListener().isDebugMode() ) {
			        return true;
				} else {
					JSFUtil.addMessage( "Sms messages were not sent as you are in debug mode." );
				}
			} catch (javax.mail.MessagingException mEx) {
				ApplicationUtil.getAplosContextListener().handleError( mEx );
			} catch (IOException ioEx) {
				ApplicationUtil.getAplosContextListener().handleError( ioEx );
			}
		} else {
			JSFUtil.addMessage( "This SMS message has already been sent" );
		}
		return false;
	}

	
	public int sendSms( List<String> processedToAddresses, BulkSmsSource bulkSmsSource ) {
		if( true ) {
			try {
				return sendNexemo( processedToAddresses, bulkSmsSource );
			} catch( IOException ioex ) {
				ApplicationUtil.handleError(ioex);
			}
		}
		return 0;
	}
	
	public int sendNexemo( List<String> processedToAddresses, BulkSmsSource bulkSmsSource ) throws IOException {
		NexmoClient client = null;

        try {
        	CommonConfiguration commonConfiguration = CommonConfiguration.getCommonConfiguration();
        	AuthMethod auth = new TokenAuthMethod(commonConfiguration.getNexemoKey(), commonConfiguration.getNexemoSecretKey());
        	client = new NexmoClient(auth);
        } catch (Exception e) {
        	ApplicationUtil.handleError(e);
        	return 0;
        }
        
        boolean isUnicode = true;
        Charset asciiCharset = Charset.forName("UTF-8");
		CharsetEncoder asciiEncoder = asciiCharset.newEncoder();
		
		SmsGenerator determinedSmsGenerator = getSmsGenerator();
		if( determinedSmsGenerator == null ) {
			determinedSmsGenerator = bulkSmsSource;
		}

		int creditsUsed = 0;
		SmsStatus firstStatus = null;
		for( String destinationNumber : processedToAddresses ) {
			StringBuffer parsedContent  = null;
			if( getSmsTemplate() != null && bulkSmsSource != null ) {
				BulkSmsSource compileSource = bulkSmsSource;
				SmsGenerator compileGenerator = determinedSmsGenerator;
				if( compileSource instanceof KeyedBulkSmsSource ) {
					compileSource = ((KeyedBulkSmsSource) compileSource).getBulkSmsSource();
				}
				if( compileGenerator instanceof KeyedBulkSmsSource ) {
					compileGenerator = ((KeyedBulkSmsSource) compileGenerator).getBulkSmsSource();
				}
				parsedContent =  new StringBuffer( getSmsTemplate().compileContent( compileSource, compileGenerator, getContent() ) );
			} else {
				parsedContent = new StringBuffer( getContent() );
			}
			
	        if( asciiEncoder.canEncode( parsedContent ) ) {
	        	isUnicode = false;
	        } else {
	        	isUnicode = true;
	        }
			
	        String parsedSourceNumber = getSourceNumber().replaceAll( "\\s", "" );
			if( parsedSourceNumber.startsWith( "+" ) ) {
				parsedSourceNumber = parsedSourceNumber.substring( 1 );
			}
	        if( !NumberUtils.isNumber(parsedSourceNumber) ) {
	        	if( destinationNumber.startsWith( "1" ) && destinationNumber.length() == 11 ) {
	        		parsedSourceNumber = "19786745635";
	        	} else {
	        		parsedSourceNumber = getSourceNumber();
	        	}
	        } else if( parsedSourceNumber.startsWith( "00" ) ) {
				parsedSourceNumber = parsedSourceNumber.substring( 2 );
	        }
	        if( parsedSourceNumber.startsWith( "0" ) ) {
	        	parsedSourceNumber = "44" + parsedSourceNumber.substring( 1 );
	        }
	        
	        TextMessage tempNexemoMesssage = new TextMessage(parsedSourceNumber, destinationNumber, parsedContent.toString(), isUnicode);
			
	        // Use the Nexmo client to submit the Text Message ...
	
	        SmsSubmissionResult[] results = null;
	        try {
	        	SSLConnectionSocketFactory sslsf = new
						SSLConnectionSocketFactory(SSLContexts.createDefault(),
						new String[] { "TLSv1.2" }, null,
						SSLConnectionSocketFactory.getDefaultHostnameVerifier());
				
				HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
				client.setHttpClient(httpClient);
	            results = client.getSmsClient().submitMessage(tempNexemoMesssage);
	        } catch (Exception e) {
	        	ApplicationUtil.handleError(e);
	        	return 0;
	        }
	
	        // Evaluate the results of the submission attempt ...
	        for (int i=0;i<results.length;i++) {
	        	if( firstStatus == null ) {
		            if (results[i].getStatus() == SmsSubmissionResult.STATUS_OK) {
		                firstStatus = SmsStatus.SENT_SUCCESFULLY;
		            } else if (results[i].getTemporaryError()) {
		            	firstStatus = SmsStatus.RETRYING;
		            } else {
		            	firstStatus = SmsStatus.CANCELLED;
		            }
	        	}    
	        	
	        	if (results[i].getStatus() == SmsSubmissionResult.STATUS_OK) {
	                creditsUsed++;
	        	}
	
	        	SmsMessagePart smsMessagePart = new SmsMessagePart();
	            smsMessagePart.setStatusCode(results[i].getStatus());
	            smsMessagePart.setUnicode(isUnicode);
	            smsMessagePart.setMessageId(results[i].getMessageId());
	            smsMessagePart.setErrorText(results[i].getErrorText());
	            smsMessagePart.setDestinationNumber(destinationNumber);
	
	            if (results[i].getMessagePrice() != null) {
	            	smsMessagePart.setMessagePrice(results[i].getMessagePrice());
	            }
	            addSmsMessagePart(smsMessagePart);
	        }
			
			SingleSmsRecord singleSmsRecord = new SingleSmsRecord( this, destinationNumber, bulkSmsSource );
			singleSmsRecord.setSmsSentDate( new Date() );
			singleSmsRecord.setStatus( firstStatus );
			singleSmsRecord.saveDetails();
			
	        if( firstStatus != null ) {
	        	setSmsStatus( firstStatus );
	        }
		}
		
		return creditsUsed;
	}

	public void updateSmsQuota(int messagesToDebit) {
		if (messagesToDebit > 0) {
			int currentSmsLimit = getSmsAccount().getCachedSmsCredits();
			if( getSmsAccount().isAutoRebuySmsBundle() &&
					getSmsAccount().getAutoRebuyThreshold() > currentSmsLimit ) {
				currentSmsLimit += getSmsAccount().getAutoRebuyBundleSize();
				SmsBundlePurchase smsBundlePurchase = new SmsBundlePurchase( getSmsAccount(), getSmsAccount().getAutoRebuyBundleSize() );
				smsBundlePurchase.saveDetails();
				SmsAccount saveableSmsAccount = getSmsAccount().getSaveableBean(); 
				saveableSmsAccount.addActiveBundlePurchase( smsBundlePurchase );
				saveableSmsAccount.saveDetails();
				AplosEmail aplosEmail = new AplosEmail( CommonEmailTemplateEnum.SMS_BUNDLE_PURCHASE, CommonUtil.getAdminUser(), smsBundlePurchase );
				aplosEmail.sendAplosEmailToQueue();
			}
			getSmsAccount().removeSmsCredits(messagesToDebit);
		}
	}
	
	public void updateSmsMessageOwner( Set<BulkSmsSource> smsSourceSet, boolean smsSent ) {
        if( getSmsMessageOwner() != null ) {
        	updateSmsMessageOwner( getSmsMessageOwner(), smsSent );
        } else if( isUsingSmsSourceAsOwner() ) {
        	for( BulkSmsSource smsSource : smsSourceSet ) {
        		if( smsSource instanceof SmsMessageOwner ) {
        			updateSmsMessageOwner( (SmsMessageOwner) smsSource, smsSent );
        		}
        	}
        }
	}
	
	public void updateSmsMessageOwner( SmsMessageOwner smsMessageOwner, boolean emailSent ) {
        if( smsMessageOwner != null ) {
        	boolean smsMessageOwnerUpdated = false;
        	if( emailSent ) {
        		smsMessageOwner = (SmsMessageOwner) smsMessageOwner.getSaveableBean();
        		smsMessageOwner.smsMessageSent(this);
        		smsMessageOwnerUpdated = true;
        	}
        	if( smsMessageOwnerUpdated ) {
        		smsMessageOwner.saveDetails(JSFUtil.getLoggedInUser() );
        	}
        }
	}
	
	public String getSentDateStdStr() {
		return FormatUtil.formatDate(getSmsSentDate());
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getSourceNumber() {
		return sourceNumber;
	}

	public void setSourceNumber(String sourceNumber) {
		this.sourceNumber = sourceNumber;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public String getSaveableContent() {
		return saveableContent;
	}

	public void setSaveableContent(String saveableContent) {
		this.saveableContent = saveableContent;
	}

	public int getCreditsUsed() {
		return creditsUsed;
	}

	public void setCreditsUsed(int creditsUsed) {
		this.creditsUsed = creditsUsed;
	}

	public int getRetryAttempts() {
		return retryAttempts;
	}

	public void setRetryAttempts(int retryAttempts) {
		this.retryAttempts = retryAttempts;
	}

	public SmsStatus getSmsStatus() {
		return smsStatus;
	}

	public void setSmsStatus(SmsStatus smsStatus) {
		this.smsStatus = smsStatus;
	}

	public List<SmsMessagePart> getMessageParts() {
		return messageParts;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getRecipientCount() {
		return recipientCount;
	}

	public void setRecipientCount(int recipientCount) {
		this.recipientCount = recipientCount;
	}

	public List<BulkMessageSource> getMessageSourceList() {
		return messageSourceList;
	}

	public MessageGenerationType getSmsGenerationType() {
		return smsGenerationType;
	}

	public void setSmsGenerationType(MessageGenerationType smsGenerationType) {
		this.smsGenerationType = smsGenerationType;
	}

	public SmsTemplate getSmsTemplate() {
		return smsTemplate;
	}

	public void setSmsTemplate(SmsTemplate smsTemplate) {
		this.smsTemplate = smsTemplate;
	}

	public Date getSmsSentDate() {
		return smsSentDate;
	}

	public void setSmsSentDate(Date smsSentDate) {
		this.smsSentDate = smsSentDate;
	}

	public SmsGenerator getSmsGenerator() {
		return smsGenerator;
	}

	public void setSmsGenerator(SmsGenerator smsGenerator) {
		this.smsGenerator = smsGenerator;
	}

	public Set<BulkSmsSource> getFilteredSourceSet() {
		return filteredSourceSet;
	}

	public void setFilteredSourceSet(Set<BulkSmsSource> filteredSourceSet) {
		this.filteredSourceSet = filteredSourceSet;
	}

	public boolean isRemovingDuplicateNumbers() {
		return isRemovingDuplicateNumbers;
	}

	public void setRemovingDuplicateNumbers(boolean isRemovingDuplicateNumbers) {
		this.isRemovingDuplicateNumbers = isRemovingDuplicateNumbers;
	}

	public SmsMessageOwner getSmsMessageOwner() {
		return smsMessageOwner;
	}

	public void setSmsMessageOwner(SmsMessageOwner smsMessageOwner) {
		this.smsMessageOwner = smsMessageOwner;
	}

	public boolean isUsingSmsSourceAsOwner() {
		return isUsingSmsSourceAsOwner;
	}

	public void setUsingSmsSourceAsOwner(boolean isUsingSmsSourceAsOwner) {
		this.isUsingSmsSourceAsOwner = isUsingSmsSourceAsOwner;
	}

	public SmsAccount getSmsAccount() {
		return smsAccount;
	}

	public void setSmsAccount(SmsAccount smsAccount) {
		this.smsAccount = smsAccount;
	}


}
