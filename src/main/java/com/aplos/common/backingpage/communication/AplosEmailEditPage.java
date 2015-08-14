package com.aplos.common.backingpage.communication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.UploadedFile;

import cb.jdynamite.JDynamiTe;

import com.aplos.common.AplosDataListModel;
import com.aplos.common.AplosLazyDataModel;
import com.aplos.common.BackingPageUrl;
import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.EditPage;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.BasicContact;
import com.aplos.common.beans.CreatedPrintTemplate;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.beans.FileDetails;
import com.aplos.common.beans.Website;
import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.beans.communication.BasicBulkMessageFinder;
import com.aplos.common.beans.communication.BulkMessageSourceGroup;
import com.aplos.common.beans.communication.EmailFrame;
import com.aplos.common.beans.communication.SingleEmailRecord;
import com.aplos.common.enums.CommonWorkingDirectory;
import com.aplos.common.enums.EmailStatus;
import com.aplos.common.enums.EmailType;
import com.aplos.common.enums.MessageGenerationType;
import com.aplos.common.interfaces.BulkEmailFinder;
import com.aplos.common.interfaces.BulkEmailSource;
import com.aplos.common.interfaces.BulkMessageSource;
import com.aplos.common.interfaces.BulkSubscriberSource;
import com.aplos.common.interfaces.DataTableStateCreator;
import com.aplos.common.interfaces.EmailFolder;
import com.aplos.common.interfaces.EmailGenerator;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.persistence.ImplicitPolymorphismMatch;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=AplosEmail.class)
public class AplosEmailEditPage extends EditPage implements DataTableStateCreator {
	private static final long serialVersionUID = -3061479138263431639L;
	private AplosEmail aplosEmail;
	private String toAddressesStr;
	private String ccAddressesStr;
	private String bccAddressesStr;
	private String fromAddressStr;
	private List<SelectItem> attachmentSelectItems;
	private List<SelectItem> emailFrameSelectItems;
	private List<SelectItem> bulkMessageSourceGroupSelectItems;
	private SelectItem[] emailFolderTypeSelectItems;
	private List<BulkEmailFinder> bulkEmailFinders = new ArrayList<BulkEmailFinder>();
	private List<FileDetails> additionalAttachments = new ArrayList<FileDetails>();
	private BulkEmailSource temporarySourceSelection = null;
	private UploadedFile attachmentUploadedFile;
	private BulkMessageSourceGroup selectedBulkMessageSourceGroup;
	private String selectedEmailFolderType;
	private Class<? extends EmailFolder> selectedEmailFolderClass;
	private EmailFolder selectedEmailFolder;
	private List<SelectItem> bulkMessageSourceGroupFilterSelectItems;
	private DataTableState filteredSourcesDataTableState;
	private DataTableState removedSourcesDataTableState;
	private DataTableState recipientDataTableState;
	
	//this field is used with the autocomplete
	private BulkEmailSource recipientSelection = null;
	private BasicBulkMessageFinder selectedEmailFinder = null;
	private boolean isAllowingNewEmailAddresses = true;
	private boolean isShowingRecipientType = true;
	private boolean isInPreviewMode = false;
	private String subjectPreview;
	private String htmlPreview;
	
	private List<BulkEmailSource> selectedEmailSources = new ArrayList<BulkEmailSource>();
	private SingleEmailRecord selectedSingleEmailRecord;
	
	
	public AplosEmailEditPage() {
		createBulkMessageSourceGroupFilterSelectItems();
	}
	
	@Override
	public boolean responsePageLoad() {
		boolean continueLoad = super.responsePageLoad();
		if( getAplosEmail() == null ) {
			setAplosEmail((AplosEmail)JSFUtil.getBeanFromScope( AplosEmail.class ));
			if( getAplosEmail() != null ) {
				updateAplosEmailVariables();
				getAplosEmail().filterMessageSourceList();
				if( MessageGenerationType.SINGLE_SOURCE.equals( getAplosEmail().getEmailGenerationType() ) ) {
					if( getAplosEmail().getFirstEmailSource() != null ) {
						setSelectedSingleEmailRecord(getAplosEmail().getSingleEmailRecord(getAplosEmail().getFirstEmailSource(), true));
					}
				}
			}
			if( CommonUtil.isNullOrEmpty( getAplosEmail().getFromAddress() ) ) {
				setFromAddressStr( getAplosEmail().determineDefaultFromAddress() );
			}
		}
		
		createAttachmentSelectItems();
		createEmailFrameSelectItems();
		createBulkMessageSourceGroupSelectItems();
		createEmailFolderTypeSelectItems();
		if( getFilteredSourcesDataTableState() == null ) {
			setFilteredSourcesDataTableState(CommonUtil.createDataTableState( this, getClass() ));
			getFilteredSourcesDataTableState().setShowingIdColumn(false);
			getFilteredSourcesDataTableState().setShowingNewBtn(false);
			getFilteredSourcesDataTableState().setShowingShowDeletedBtn(false);
			getFilteredSourcesDataTableState().setShowingResetBtn(false);
			getFilteredSourcesDataTableState().setShowingDeleteColumn(false);
			getFilteredSourcesDataTableState().setDataListModel( new FilteredSourcesDlm( new ArrayList<BulkEmailSource>(aplosEmail.filterMessageSourceList()) ) );
		}
		if( getRemovedSourcesDataTableState() == null ) {
			setRemovedSourcesDataTableState(CommonUtil.createDataTableState( this, getClass() ));
			getRemovedSourcesDataTableState().setShowingIdColumn(false);
			getRemovedSourcesDataTableState().setShowingNewBtn(false);
			getRemovedSourcesDataTableState().setShowingShowDeletedBtn(false);
			getRemovedSourcesDataTableState().setShowingResetBtn(false);
			getRemovedSourcesDataTableState().setShowingDeleteColumn(false);
			getRemovedSourcesDataTableState().setShowingRowHighlight(false);
			getRemovedSourcesDataTableState().setDataListModel( new RemovedSourcesDlm( new ArrayList<BulkEmailSource>(aplosEmail.getRemovedSourceSet()) ) );
		}
		if( getRecipientDataTableState() == null && getAplosEmail().getEmailSentDate() != null ) {
			BeanDao singleEmailRecordDao = new BeanDao( SingleEmailRecord.class );
			singleEmailRecordDao.addWhereCriteria( "bean.aplosEmail.id = " + getAplosEmail().getId() );
			setRecipientDataTableState(CommonUtil.createDataTableState( this, getClass(), "recipients", singleEmailRecordDao ));
			getRecipientDataTableState().setShowingIdColumn(false);
			getRecipientDataTableState().setShowingNewBtn(false);
			getRecipientDataTableState().setShowingResetBtn(false);
			getRecipientDataTableState().setShowingShowDeletedBtn(false);
		}
		return continueLoad;
	}
	
	public List<FileDetails> getSaveableAttachments() {
		if( getSelectedSingleEmailRecord() != null ) {
			return getSelectedSingleEmailRecord().getSaveableAttachments();
		} else {
			return getAplosEmail().getSaveableAttachments();
		}
	}
	
	@Override
	public AplosLazyDataModel getAplosLazyDataModel(
			DataTableState dataTableState, BeanDao aqlBeanDao) {
		if( "recipients".equals( dataTableState.getTableIdentifier() ) ) {
			return new RecipientLdm( dataTableState, aqlBeanDao );
		}
		return super.getAplosLazyDataModel(dataTableState, aqlBeanDao);
	}
	
	public void updatePreviewText() {
		if( getSelectedSingleEmailRecord() != null ) {
			EmailGenerator emailGenerator = getAplosEmail().determineEmailGenerator(getSelectedSingleEmailRecord());
			try {
				String processedSubject = getAplosEmail().processSubjectDynamicValues( getSelectedSingleEmailRecord().getBulkEmailSource(), emailGenerator );
				String processedHtml = getAplosEmail().generateHtmlBody( getSelectedSingleEmailRecord(), emailGenerator, processedSubject);
				setSubjectPreview(processedSubject);
				setHtmlPreview(processedHtml);
			} catch( IOException ioex ) {
				ApplicationUtil.handleError( ioex );
			}
		}
	}
	
	public boolean isReadyToPrint( FileDetails saveableAttachment ) {
		if( saveableAttachment instanceof CreatedPrintTemplate ) {
			CreatedPrintTemplate createdPrintTemplate = (CreatedPrintTemplate) saveableAttachment;
			if( createdPrintTemplate.getPrintTemplate() != null ) {
				return createdPrintTemplate.getPrintTemplate().isReadyToPrint();
			}
		} 
		return true;
	}
	
	public boolean isShowingEmailFolders() {
		return CommonConfiguration.getCommonConfiguration().isShowingEmailFolders();
	}
	
	public boolean isShowingAttachments() {
		return true;
	}
	
	public boolean isShowingAdvancedOptions() {
		return true;
	}
	
	public String getEmailContentUrl() {
		BackingPageUrl backingPageUrl = new BackingPageUrl( AplosEmailContentPage.class );
		backingPageUrl.addContextPath();
		return backingPageUrl.toString();
	}
	
	public String getHtmlPreviewUrl() {
		BackingPageUrl backingPageUrl = new BackingPageUrl( AplosEmailHtmlPreviewPage.class );
		backingPageUrl.addContextPath();
		return backingPageUrl.toString();
	}
	
	public String getEmailPrintContentUrl() {
		BackingPageUrl backingPageUrl = new BackingPageUrl( AplosEmailPrintContentPage.class );
		backingPageUrl.addContextPath();
		return backingPageUrl.toString();
	}
	
	public int getRecipientReadCount() {
		BeanDao singleEmailRecordDao = new BeanDao( SingleEmailRecord.class );
		singleEmailRecordDao.addWhereCriteria( "bean.openedDate != null" );
		singleEmailRecordDao.addWhereCriteria( "bean.aplosEmail.id = " + getAplosEmail().getId() );
		return singleEmailRecordDao.getCountAll();
	}
	
	public int getRecipientActionedCount() {
		BeanDao singleEmailRecordDao = new BeanDao( SingleEmailRecord.class );
		singleEmailRecordDao.addWhereCriteria( "bean.actionedDate != null" );
		singleEmailRecordDao.addWhereCriteria( "bean.aplosEmail.id = " + getAplosEmail().getId() );
		return singleEmailRecordDao.getCountAll();
	}

	public void createBulkMessageSourceGroupFilterSelectItems() {
		List<SelectItem> selectItems = new ArrayList<SelectItem>();
		selectItems.add(new SelectItem(null, "Please Select"));
		selectItems.addAll( Website.getCurrentWebsiteFromTabSession().getBulkEmailFinderSelectItems() );
		setBulkMessageSourceGroupFilterSelectItems(selectItems);
	}
	
	public List<SelectItem> getEmailStatusSelectItems() {
		return CommonUtil.getEnumSelectItems(EmailStatus.class);
	}
	
	public boolean isShowingPreviewButton() {
		return selectedSingleEmailRecord != null && !MessageGenerationType.SINGLE_SOURCE.equals( getAplosEmail().getEmailGenerationType() );
	}
	
	public String getAssociatedEmailDisplayName( AplosEmail aplosEmail ) {
		StringBuffer displayNameBuf = new StringBuffer( String.valueOf( aplosEmail.getId() ) );
		if( aplosEmail.getEmailSentDate() != null ) {
			displayNameBuf.append( " Sent " + aplosEmail.getEmailSentDateTimeStr() );
		} else {
			displayNameBuf.append( " Not sent" );
		}
		return displayNameBuf.toString();
	}
	
	public String getEditorCssUrl() {
		AplosEmail aplosEmail = getAplosEmail();
		if( aplosEmail != null && aplosEmail.getEmailTemplate() != null ) {
			String editorCssUrl = aplosEmail.getEmailTemplate().getEditorCssUrl( aplosEmail );
			if( !CommonUtil.isNullOrEmpty( editorCssUrl ) ) {
				return "CKEDITOR.config.contentsCss = " + editorCssUrl; 
			}
		}
		return "";
	}
	
	public boolean isAssociatedWithEditPage( BulkEmailSource bulkEmailSource ) {
		if( bulkEmailSource instanceof AplosBean && ((AplosBean) bulkEmailSource).getEditPageClass() != null 
				&& JSFUtil.getLoggedInUser().getUserLevel().checkAccess( new BackingPageUrl( ((AplosBean) bulkEmailSource).getEditPageClass() ).toString() ) ) {
			return true;
		}
		return false;
	}
	
	public boolean isAssociatedWithEditPage( EmailFolder emailFolder ) {
		if( emailFolder instanceof AplosBean && ((AplosBean) emailFolder).getEditPageClass() != null 
				&& JSFUtil.getLoggedInUser().getUserLevel().checkAccess( new BackingPageUrl( ((AplosBean) emailFolder).getEditPageClass() ).toString() ) ) {
			return true;
		}
		return false;
	}
	
	public void addRecipient( SelectEvent event ) {
		BulkEmailSource selectedEmailRecipient = (BulkEmailSource) event.getObject();
		if (selectedEmailRecipient != null) {
			if( selectedEmailRecipient instanceof BasicContact && ((BasicContact)selectedEmailRecipient).isNew() ) {
				if( CommonUtil.validateEmailAddressFormat( selectedEmailRecipient.getEmailAddress() ) ) {
					BasicContact basicContact = (BasicContact)selectedEmailRecipient;
					BeanDao basicContactDao = new BeanDao( BasicContact.class );
					basicContactDao.addWhereCriteria( "bean.address.subscriber.emailAddress LIKE :emailAddress" );
					basicContactDao.setNamedParameter( "emailAddress", basicContact.getAddress().getEmailAddress() );
					BasicContact duplicateBasicContact = basicContactDao.getFirstBeanResult();
					if( duplicateBasicContact != null ) {
						selectedEmailRecipient = duplicateBasicContact;
//						HibernateUtil.initialise(duplicateBasicContact, true);
					} else {
						basicContact.saveDetails();
						setRecipientSelection(null);
					}
				} else {
					JSFUtil.addMessageForError( "This is not a valid email address" );
					return;
				}
			}
			getSelectedEmailSources().add( selectedEmailRecipient );
			
		}
		getAplosEmail().filterMessageSourceList();
		setRecipientSelection(null);
	}

	public List<BulkEmailSource> suggestRecipients(String searchStr) {
		List<BulkEmailSource> suggestions = new ArrayList<BulkEmailSource>();
		List<BulkEmailSource> tempSuggestions = new ArrayList<BulkEmailSource>();
		int invalidCounter=0;

		boolean alreadyInList = false;
		if (getSelectedEmailFinder() != null) {
			BulkEmailFinder bulkEmailFinder = getSelectedEmailFinder().getBulkEmailFinderClassInstance();
			List<BulkEmailSource> sourceTypedSuggestions = bulkEmailFinder.getEmailAutoCompleteSuggestions(searchStr, 15);
			if (sourceTypedSuggestions != null) {
				for (BulkEmailSource tempBulkEmailSource : sourceTypedSuggestions) { 
					if( CommonUtil.validateEmailAddressFormat( tempBulkEmailSource.getEmailAddress() ) ) {
						tempSuggestions.add( tempBulkEmailSource );
						if (tempBulkEmailSource != null 
								&& tempBulkEmailSource.getEmailAddress() != null
								&& tempBulkEmailSource.getEmailAddress().equals(searchStr)) {
							alreadyInList = true;
						}
					} else {
						invalidCounter++;
					}
				}
			}
			if (ApplicationUtil.getAplosContextListener().isDebugMode() && invalidCounter > 0) {
				JSFUtil.addMessage("Debug: " + invalidCounter + " matches were discarded because they did not have valid mobile numbers");
			}
		} else {
			JSFUtil.addMessageForError("Please select the type to search for.");
		}

		if (!alreadyInList && isAllowingNewEmailAddresses()) {
			BasicContact basicContact = new BasicContact();
			basicContact.getAddress().setEmailAddress( searchStr );
			tempSuggestions.add(basicContact);
		}
		for (BulkEmailSource bulkEmailSource : tempSuggestions) {
			if (!getAplosEmail().getMessageSourceList().contains(bulkEmailSource)) {
				suggestions.add(bulkEmailSource);
			}
		}
		return sortByFullDetails(suggestions);
	}

	public static List<BulkEmailSource> sortByFullDetails( List<BulkEmailSource> bulkEmailSources ) {
		Collections.sort( bulkEmailSources, new Comparator<BulkEmailSource>() {
			@Override
			public int compare(BulkEmailSource endpoint1, BulkEmailSource endpoint2) {
				return endpoint1.getSourceUniqueDisplayName().toLowerCase().compareTo( endpoint2.getSourceUniqueDisplayName().toLowerCase() );
			}
		});
		return bulkEmailSources;
	}
		
	public String removeRecipient(AjaxBehaviorEvent event) {
		BulkEmailSource recipient = (BulkEmailSource) JSFUtil.getRequest().getAttribute("recipient");
		getSelectedEmailSources().remove( recipient );
		return null; //to refresh the ui repeat
	}
	
	public void createEmailFolderTypeSelectItems() {
		ImplicitPolymorphismMatch implicitPolymorphismMatch = ApplicationUtil.getPersistentApplication().getDynamicMetaValuesMap().get( EmailFolder.class );
		SelectItem selectItems[] = new SelectItem[ implicitPolymorphismMatch.getPersistentClasses().size() ];
		int count = 0;
		String tempPluralDisplayName;
		for( PersistentClass tempPersistentClass : implicitPolymorphismMatch.getPersistentClasses() ) {
			tempPluralDisplayName =  AplosAbstractBean.getPluralDisplayName( (Class<? extends AplosAbstractBean>) tempPersistentClass.getTableClass() );
			tempPluralDisplayName = CommonUtil.firstLetterToUpperCase( tempPluralDisplayName );
			selectItems[ count++ ] = new SelectItem( tempPersistentClass.getTableClass().getName(), tempPluralDisplayName );  
		}
		if( selectItems.length > 0 && getSelectedEmailFolderType() == null ) {
			setSelectedEmailFolderType( (String) selectItems[ 0 ].getValue() );
			selectedEmailFolderTypeUpdated();
		}
		setEmailFolderTypeSelectItems(selectItems);
	}
	
	public void selectedEmailFolderTypeUpdated() {
		try {
			Class<? extends EmailFolder> emailFolderClass = (Class<? extends EmailFolder>) Class.forName( getSelectedEmailFolderType() );
			setSelectedEmailFolderClass(emailFolderClass);
		} catch (ClassNotFoundException e) {
			ApplicationUtil.handleError(e);
		}
	}

	public List<EmailFolder> suggestEmailFolders(String searchStr) {
		if( getSelectedEmailFolderType() != null ) {
			EmailFolder emailFolder = (EmailFolder) CommonUtil.getNewInstance( getSelectedEmailFolderClass() );
			BeanDao emailFolderDao = new BeanDao( (Class<? extends AplosAbstractBean>) getSelectedEmailFolderClass() );
			emailFolderDao.addWhereCriteria( emailFolder.getEmailFolderSearchCriteria() );
			emailFolderDao.setNamedParameter( "searchStr", "%" + searchStr + "%" );
			emailFolderDao.setMaxResults(15);
			return emailFolderDao.getAll();
		} else {
			return new ArrayList<EmailFolder>();
		}
	}
	
	public void addEmailFolder( SelectEvent event ) {
		EmailFolder emailFolder = (EmailFolder) event.getObject();
		if( emailFolder != null ) {
			getAplosEmail().addEmailFolder( emailFolder );	
		} else {
			JSFUtil.addMessage( "Please select an email folder" );
		}
	}
	
	public void removeEmailFolder() {
		EmailFolder emailFolder = (EmailFolder) JSFUtil.getRequest().getAttribute( "selectedSource" );
		removeEmailFolder(emailFolder);
	}
	
	public void removeEmailFolder(EmailFolder emailFolder) {
		getAplosEmail().getEmailFolders().remove( emailFolder );
	}

	public void createBulkMessageSourceGroupSelectItems() {
		setBulkMessageSourceGroupSelectItems( new ArrayList<SelectItem>() );
		
		/*
		 * These are already saved in the database as a bulkMessageSourceGroup anyway so there's no need to
		 * add them again.
		 */
//		for (BasicBulkMessageFinder bulkMessageFinder : Website.getCurrentWebsiteFromTabSession().getBulkMessageFinderMap().values() ) {
//			bulkMessageSourceGroupSelectItems.add(new SelectItem(bulkMessageFinder, bulkMessageFinder.getName()));
//		}
		BeanDao bulkMessageSourceGroupDao = new BeanDao( BulkMessageSourceGroup.class ).setIsReturningActiveBeans( true );
		bulkMessageSourceGroupDao.addWhereCriteria( "bean.isVisibleInSearches = true" );
		List<BulkMessageSourceGroup> bulkMessageSourceGroups = bulkMessageSourceGroupDao.getAll();
		for( BulkMessageSourceGroup tempBulkMessageSourceGroup : bulkMessageSourceGroups ) {
			if( tempBulkMessageSourceGroup.getSourceType() != null && BulkSubscriberSource.class.isAssignableFrom( tempBulkMessageSourceGroup.getSourceType() ) ) {
				bulkMessageSourceGroupSelectItems.add(new SelectItem(tempBulkMessageSourceGroup, tempBulkMessageSourceGroup.getName() ));
			}
		}
	}

	public void addSelectedBulkMessageSourceGroup() {
		AplosEmail aplosEmail = resolveAssociatedBean();
		if( getSelectedBulkMessageSourceGroup().getId() == null ) {
			getSelectedBulkMessageSourceGroup().saveDetails();
		}
		aplosEmail.getMessageSourceList().add( getSelectedBulkMessageSourceGroup() );
		getFilteredSourcesDataTableState().setDataListModel( new AplosDataListModel( new ArrayList<BulkEmailSource>(aplosEmail.filterMessageSourceList()) ) );
	}
	
	public void createAttachmentSelectItems() {
		//all system-wide documents need to be available
		
		if( getAplosEmail() != null && getAplosEmail().getEmailTemplate() != null ) {
			BulkEmailSource bulkEmailSource = null;
			if( getAplosEmail().getEmailGenerationType().equals( MessageGenerationType.SINGLE_SOURCE ) ) {
				bulkEmailSource = getAplosEmail().getFirstEmailSource();
			}
			List<FileDetails> availableAttachments = getAplosEmail().getEmailTemplate().getAvailableAttachments(bulkEmailSource, aplosEmail.getEmailGenerator());
					
			//turn each document into a select item
			attachmentSelectItems = new ArrayList<SelectItem>();
			for (FileDetails document : availableAttachments) {
				attachmentSelectItems.add( new SelectItem(document, document.getName() + " (" + document.getExtension() + ")") );
			}
		}
	}

	public void addAttachment() {
		if (CommonUtil.isFileUploaded(attachmentUploadedFile)) {
			AplosEmail aplosEmail = (AplosEmail) resolveAssociatedBean();
			
			FileDetails fileDetails = new FileDetails();
			fileDetails.setFileDetailsOwner(CommonWorkingDirectory.APLOS_EMAIL_FILE_DIR.getAplosWorkingDirectory());
			fileDetails.updateFile(attachmentUploadedFile);
			aplosEmail.addSaveableAttachment( fileDetails );
			
			for( SingleEmailRecord tempSingleEmailRecord : aplosEmail.getSingleEmailRecordMap().values() ) {
				tempSingleEmailRecord.getSaveableAttachments().add( fileDetails );
			}
		}
	}
	
	public void forwardEmail() {
		AplosEmail loadedEmail = getAplosEmail();
		AplosEmail newEmail = commonCopyEmail(loadedEmail, true);
		/*
		 * Otherwise the email won't send.
		 */
		if( MessageGenerationType.SINGLE_SOURCE.equals( newEmail.getEmailGenerationType() ) ) {
			newEmail.setEmailGenerationType(MessageGenerationType.NONE);
		}
		newEmail.getToAddresses().clear();
		newEmail.getMessageSourceList().clear();
		newEmail.setEmailType(EmailType.FORWARD);
		updateAplosEmailVariables();
		JSFUtil.addMessage( "The email has been duplicated and is ready to be forwarded" );
	}
	
	public void duplicateEmail() {
		AplosEmail loadedEmail = getAplosEmail();
		AplosEmail newEmail = commonCopyEmail(loadedEmail, false);
		newEmail.setMessageSourceList( new ArrayList<BulkMessageSource>( loadedEmail.getMessageSourceList() ) );
		newEmail.setEmailType(EmailType.OUTGOING);
		updateAplosEmailVariables();
		JSFUtil.addMessage( "The email has been duplicated" );
	}
	
	public static void switchInboxToOutbox( AplosEmail aplosEmail ) {
		aplosEmail.getEmailFolders().remove( CommonConfiguration.getCommonConfiguration().getInboxEmailFolder() );
		aplosEmail.addEmailFolder( CommonConfiguration.getCommonConfiguration().getSentEmailFolder() );
	}
	
	public void replyToEmail() {
		AplosEmail loadedEmail = getAplosEmail();
		AplosEmail newEmail = commonCopyEmail(loadedEmail, true);
		newEmail.setToAddress( loadedEmail.getFromAddress() );
		newEmail.getSaveableAttachments().clear();
		newEmail.setEmailType(EmailType.REPLY);
		updateAplosEmailVariables();
	}

	public boolean isInDebugMode() {
		return ApplicationUtil.getAplosContextListener().isDebugMode();
	}
	
	public AplosEmail commonCopyEmail( AplosEmail srcEmail, boolean insertHeaders ) {
		AplosEmail newEmail = new AplosEmail();
		newEmail.copy( srcEmail );
		newEmail.setFromAddress(srcEmail.determineDefaultFromAddress());
		if( newEmail.getOriginalEmail() == null ) {
			newEmail.setOriginalEmail(getAplosEmail());
		}
		if( insertHeaders && !CommonUtil.isNullOrEmpty(newEmail.getHtmlBody()) ) {
			JDynamiTe jDynamiTe = new JDynamiTe();
			try {
				CommonUtil.addUtf8StringToJDynamiTe(jDynamiTe, AplosEmail.REPLY_HEADER);
				jDynamiTe.setVariable( "FROM", CommonUtil.getStringOrEmpty( srcEmail.getFromAddress() ) );
				jDynamiTe.setVariable( "SENT", srcEmail.getEmailSentDateTimeStr() );
				if( srcEmail.getToAddresses().size() > 0 ) {
					jDynamiTe.setVariable( "TO", "<b>To: </b>" + StringUtils.join( srcEmail.getToAddresses(), "," ) + "<br/>" );
				}
				if( srcEmail.getCcAddresses().size() > 0 ) {
					jDynamiTe.setVariable( "CC", "<b>Cc: </b>" + StringUtils.join( srcEmail.getCcAddresses(), "," ) + "<br/>" );
				}
				if( srcEmail.getBccAddresses().size() > 0 ) {
					jDynamiTe.setVariable( "BCC", "<b>Bcc: </b>" + StringUtils.join( srcEmail.getBccAddresses(), "," ) + "<br/>" );
				}
				jDynamiTe.setVariable( "SUBJECT", CommonUtil.getStringOrEmpty( srcEmail.getSubject() ) );
				jDynamiTe.parse();
				newEmail.setHtmlBody( jDynamiTe.toString() + CommonUtil.getStringOrEmpty( newEmail.getHtmlBody() ) );
			} catch( IOException ioex ) {
				ApplicationUtil.handleError( ioex );
			}
	        if( CommonConfiguration.getCommonConfiguration().isUsingEmailBodyDivider() ) {
				newEmail.setHtmlBody( "<p></p>" + AplosEmail.EMAIL_BODY_DIVIDER + newEmail.getHtmlBody() );
	        }
		}
		switchInboxToOutbox( newEmail );
		newEmail.setEmailReadDate(null);
		newEmail.setEmailSentDate(null);
		newEmail.setSendingPlainText(false);
		newEmail.addToScope();
		setAplosEmail( (AplosEmail) newEmail.getSaveableBean() );
		
		return aplosEmail;
	}
	
	public void createEmailFrameSelectItems() {
		List<EmailFrame> emailFrameList = new BeanDao( EmailFrame.class ).getAll();
		setEmailFrameSelectItems(Arrays.asList( AplosBean.getSelectItemBeansWithNotSelected(emailFrameList) ));
	}
	
	// Overridable
	public boolean isShowingEmailFrameDdl() {
		return true;
	}
	
	public void sendEmail() {
		String[] fromAddresses = CommonUtil.sepearateEmailAddresses( getFromAddressStr() );
		if( fromAddresses.length > 1 ) {
			JSFUtil.addMessage( "You can only enter one from address" );
		} else if( fromAddresses.length < 1 ) {
			JSFUtil.addMessage( "You must enter at least one address" );
		} else {
			updateVariablesInAplosEmail();
			if( getAplosEmail().sendAplosEmailToQueue() != null ) {
				/*
				 * As the message gets saved you need to make sure references are reloaded
				 * to avoid any error messages in the fileDetails which do checks on the objects
				 * before saving.
				 */
				setAplosEmail( (AplosEmail) getAplosEmail() );
				JSFUtil.addMessage( "Your message has been sent" );	
			} else {
				JSFUtil.addMessage( "Your message could not be sent" );
			}
		}
	}
	
	public void checkForFurtherDetails() {
		AplosEmail aplosEmail = resolveAssociatedBean();
		aplosEmail.setIncomingReadRetryCount( 0 );
		AplosEmailListPage.refreshEmailListStatic();
	}
	
	public void updateAplosEmailVariables() {
		setFromAddressStr( getAplosEmail().getFromAddress() );
		setToAddressesStr( StringUtils.join( getAplosEmail().getToAddresses(), ";" ) );
		setCcAddressesStr( StringUtils.join( getAplosEmail().getCcAddresses(), ";" ) );
		setBccAddressesStr( StringUtils.join( getAplosEmail().getBccAddresses(), ";" ) );
		if( getAplosEmail().getEmailTemplate() != null ) {
			List<BulkEmailFinder> bulkEmailFinders = getAplosEmail().getEmailTemplate().getBulkEmailFinders();
			if( bulkEmailFinders != null ) {
				setBulkEmailFinders(bulkEmailFinders);
			}
		}
	}
	
	public void updateVariablesInAplosEmail() {
		String[] fromAddresses = CommonUtil.sepearateEmailAddresses( getFromAddressStr() );
		getAplosEmail().setFromAddress( fromAddresses[ 0 ] );
//		for( int i = 0, n = getSelectedEmailSources().size(); i < n; i++ ) {
//			getAplosEmail().addToAddress( getSelectedEmailSources().get( i ).getEmailAddress() );
//		}
		getAplosEmail().setToAddresses( CommonUtil.sepearateEmailAddresses( getToAddressesStr() ) );
		getAplosEmail().setCcAddresses( CommonUtil.sepearateEmailAddresses( getCcAddressesStr() ) );
		getAplosEmail().setBccAddresses( CommonUtil.sepearateEmailAddresses( getBccAddressesStr() ) );
	}
	
	public void triggerAttach() {
		AplosEmail aplosEmail = resolveAssociatedBean();
		for (int i = getAdditionalAttachments().size() -1; i >= 0; i--) {
			if (!aplosEmail.getSaveableAttachments().contains(getAdditionalAttachments().get(i))) {
				aplosEmail.addSaveableAttachment( getAdditionalAttachments().get(i) );
			}
			getAdditionalAttachments().remove(i);
		}
	}
	
	@Override
	public void applyBtnAction() {
		saveAction();
	}
	
	@Override
	public void okBtnAction() {
		saveAction();
		navigateToPreviousPage();
	}
	
	public void saveAction() {
		AplosEmail aplosEmail = resolveAssociatedBean(); 
		updateVariablesInAplosEmail();
		aplosEmail.saveDetails();
	}

	public void removeBulkMessageSource() {
		BulkMessageSource bulkMessageSource = (BulkMessageSource) JSFUtil.getRequest().getAttribute( "selectedSource" );
		getAplosEmail().getMessageSourceList().remove( bulkMessageSource );
	}

	public void removeBulkEmailSource(BulkEmailSource bulkEmailSource) {
		getAplosEmail().getMessageSourceList().remove(bulkEmailSource);
	}

	public boolean isEmailUpdateAllowed() {
		return getAplosEmail().getEmailSentDate() == null;
	}

	public String getToAddressesStr() {
		return toAddressesStr;
	}

	public void setToAddressesStr(String toAddressesStr) {
		this.toAddressesStr = toAddressesStr;
	}

	public String getCcAddressesStr() {
		return ccAddressesStr;
	}

	public void setCcAddressesStr(String ccAddressesStr) {
		this.ccAddressesStr = ccAddressesStr;
	}

	public List<SelectItem> getAttachmentSelectItems() {
		return attachmentSelectItems;
	}

	public void setAttachmentSelectItems(List<SelectItem> attachmentSelectItems) {
		this.attachmentSelectItems = attachmentSelectItems;
	}

	public String getFromAddressStr() {
		return fromAddressStr;
	}

	public void setFromAddressStr(String fromAddressStr) {
		this.fromAddressStr = fromAddressStr;
	}

	public List<SelectItem> getEmailFrameSelectItems() {
		return emailFrameSelectItems;
	}

	public void setEmailFrameSelectItems(List<SelectItem> emailFrameSelectItems) {
		this.emailFrameSelectItems = emailFrameSelectItems;
	}

	private AplosEmail getAplosEmail() {
		return aplosEmail;
	}

	private void setAplosEmail(AplosEmail aplosEmail) {
		this.aplosEmail = aplosEmail;
	}

	private List<BulkEmailFinder> getBulkEmailFinders() {
		return bulkEmailFinders;
	}

	private void setBulkEmailFinders(List<BulkEmailFinder> bulkEmailFinders) {
		this.bulkEmailFinders = bulkEmailFinders;
	}

	public BulkEmailSource getTemporarySourceSelection() {
		return temporarySourceSelection;
	}

	public void setTemporarySourceSelection(BulkEmailSource temporarySourceSelection) {
		this.temporarySourceSelection = temporarySourceSelection;
	}

	public List<FileDetails> getAdditionalAttachments() {
		return additionalAttachments;
	}

	public void setAdditionalAttachments(List<FileDetails> additionalAttachments) {
		this.additionalAttachments = additionalAttachments;
	}

	public UploadedFile getAttachmentUploadedFile() {
		return attachmentUploadedFile;
	}

	public void setAttachmentUploadedFile(UploadedFile attachmentUploadedFile) {
		this.attachmentUploadedFile = attachmentUploadedFile;
	}

	public BulkMessageSourceGroup getSelectedBulkMessageSourceGroup() {
		return selectedBulkMessageSourceGroup;
	}

	public void setSelectedBulkMessageSourceGroup(
			BulkMessageSourceGroup selectedBulkMessageSourceGroup) {
		this.selectedBulkMessageSourceGroup = selectedBulkMessageSourceGroup;
	}

	public List<SelectItem> getBulkMessageSourceGroupSelectItems() {
		return bulkMessageSourceGroupSelectItems;
	}

	public void setBulkMessageSourceGroupSelectItems(
			List<SelectItem> bulkMessageSourceGroupSelectItems) {
		this.bulkMessageSourceGroupSelectItems = bulkMessageSourceGroupSelectItems;
	}

	public String getBccAddressesStr() {
		return bccAddressesStr;
	}

	public void setBccAddressesStr(String bccAddressesStr) {
		this.bccAddressesStr = bccAddressesStr;
	}

	public EmailFolder getSelectedEmailFolder() {
		return selectedEmailFolder;
	}

	public void setSelectedEmailFolder(EmailFolder selectedEmailFolder) {
		this.selectedEmailFolder = selectedEmailFolder;
	}

	public String getSelectedEmailFolderType() {
		return selectedEmailFolderType;
	}

	public void setSelectedEmailFolderType(String selectedEmailFolderType) {
		this.selectedEmailFolderType = selectedEmailFolderType;
	}

	public Class<? extends EmailFolder> getSelectedEmailFolderClass() {
		return selectedEmailFolderClass;
	}

	public void setSelectedEmailFolderClass(Class<? extends EmailFolder> selectedEmailFolderClass) {
		this.selectedEmailFolderClass = selectedEmailFolderClass;
	}

	public SelectItem[] getEmailFolderTypeSelectItems() {
		return emailFolderTypeSelectItems;
	}

	public void setEmailFolderTypeSelectItems(
			SelectItem[] emailFolderTypeSelectItems) {
		this.emailFolderTypeSelectItems = emailFolderTypeSelectItems;
	}

	public List<SelectItem> getBulkMessageSourceGroupFilterSelectItems() {
		return bulkMessageSourceGroupFilterSelectItems;
	}

	public void setBulkMessageSourceGroupFilterSelectItems(
			List<SelectItem> bulkMessageSourceGroupFilterSelectItems) {
		this.bulkMessageSourceGroupFilterSelectItems = bulkMessageSourceGroupFilterSelectItems;
	}

	public BulkEmailSource getRecipientSelection() {
		return recipientSelection;
	}

	public void setRecipientSelection(BulkEmailSource recipientSelection) {
		this.recipientSelection = recipientSelection;
	}

	public BasicBulkMessageFinder getSelectedEmailFinder() {
		return selectedEmailFinder;
	}

	public void setSelectedEmailFinder(BasicBulkMessageFinder selectedEmailFinder) {
		this.selectedEmailFinder = selectedEmailFinder;
	}

	public boolean isAllowingNewEmailAddresses() {
		return isAllowingNewEmailAddresses;
	}

	public void setAllowingNewEmailAddresses(boolean isAllowingNewEmailAddresses) {
		this.isAllowingNewEmailAddresses = isAllowingNewEmailAddresses;
	}

	public boolean isShowingRecipientType() {
		return isShowingRecipientType;
	}

	public void setShowingRecipientType(boolean isShowingRecipientType) {
		this.isShowingRecipientType = isShowingRecipientType;
	}

	public List<BulkEmailSource> getSelectedEmailSources() {
		return selectedEmailSources;
	}

	public void setSelectedEmailSources(List<BulkEmailSource> selectedEmailSources) {
		this.selectedEmailSources = selectedEmailSources;
	}

	public DataTableState getFilteredSourcesDataTableState() {
		return filteredSourcesDataTableState;
	}

	public void setFilteredSourcesDataTableState(DataTableState filteredSourcesDataTableState) {
		this.filteredSourcesDataTableState = filteredSourcesDataTableState;
	}

	public DataTableState getRecipientDataTableState() {
		return recipientDataTableState;
	}

	public void setRecipientDataTableState(DataTableState recipientDataTableState) {
		this.recipientDataTableState = recipientDataTableState;
	}

	public SingleEmailRecord getSelectedSingleEmailRecord() {
		return selectedSingleEmailRecord;
	}

	public void setSelectedSingleEmailRecord(SingleEmailRecord selectedSingleEmailRecord) {
		this.selectedSingleEmailRecord = selectedSingleEmailRecord;
	}
	
	public boolean isInPreviewMode() {
		return isInPreviewMode;
	}

	public void setInPreviewMode(boolean isInPreviewMode) {
		this.isInPreviewMode = isInPreviewMode;
	}

	public String getSubjectPreview() {
		return subjectPreview;
	}

	public void setSubjectPreview(String subjectPreview) {
		this.subjectPreview = subjectPreview;
	}

	public String getHtmlPreview() {
		return htmlPreview;
	}

	public void setHtmlPreview(String htmlPreview) {
		this.htmlPreview = htmlPreview;
	}

	public DataTableState getRemovedSourcesDataTableState() {
		return removedSourcesDataTableState;
	}

	public void setRemovedSourcesDataTableState(
			DataTableState removedSourcesDataTableState) {
		this.removedSourcesDataTableState = removedSourcesDataTableState;
	}

	public class FilteredSourcesDlm extends AplosDataListModel {
		private static final long serialVersionUID = -1537643904193847236L;
		private List<BulkEmailSource> filteredSourceList;
		
		public FilteredSourcesDlm( List<BulkEmailSource> dataList ) {
			super(dataList);
			filteredSourceList = dataList;
		}
		
		@Override
		public void selectBean() {
			setSelectedSingleEmailRecord( getAplosEmail().getSingleEmailRecord( (BulkEmailSource) getAplosBean(), true ) );
			setInPreviewMode(true);
			updatePreviewText();
		}
		
		public void deleteBeanAction() {
			BulkEmailSource bulkEmailSource = (BulkEmailSource) JSFUtil.getRequest().getAttribute( "tableBean" );
			getFilteredSourceList().remove( bulkEmailSource );
			getAplosEmail().getRemovedSourceSet().add( bulkEmailSource );
			((RemovedSourcesDlm) getRemovedSourcesDataTableState().getDataListModel()).getRemovedSourceList().add( bulkEmailSource );
		}

		public List<BulkEmailSource> getFilteredSourceList() {
			return filteredSourceList;
		}
	}

	public class RemovedSourcesDlm extends AplosDataListModel {
		private static final long serialVersionUID = -1537643904193847236L;
		
		private List<BulkEmailSource> removedSourceList;
		
		public RemovedSourcesDlm( List dataList ) {
			super(dataList);
			removedSourceList = dataList;
		}
		
		public void reinstateBeanAction() {
			BulkEmailSource bulkEmailSource = (BulkEmailSource) JSFUtil.getRequest().getAttribute( "tableBean" );
			getAplosEmail().getRemovedSourceSet().remove( bulkEmailSource );
			removedSourceList.remove( bulkEmailSource );
			((FilteredSourcesDlm) getFilteredSourcesDataTableState().getDataListModel()).getFilteredSourceList().add( bulkEmailSource );
		}
		
		public List<BulkEmailSource> getRemovedSourceList() {
			return removedSourceList;
		}
	}
	
	public class RecipientLdm extends AplosLazyDataModel {

		
		public RecipientLdm( DataTableState dataTableState, BeanDao aqlBeanDao ) {
			super(dataTableState, aqlBeanDao);
		}
		
		@Override
		public AplosBean selectBean(boolean redirect) {
			SingleEmailRecord singleEmailRecord = (SingleEmailRecord) JSFUtil.getRequest().getAttribute( "tableBean" );
			if( singleEmailRecord.getBulkEmailSource() instanceof AplosBean ) {
				((AplosBean) singleEmailRecord.getBulkEmailSource()).redirectToEditPage();
			} else {
				JSFUtil.addMessage("Cannot redirect to edit page");
			}
			return null;
		}
	}
}
