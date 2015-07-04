package com.aplos.common.backingpage.communication;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.model.SelectItem;

import org.primefaces.event.SelectEvent;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.EditPage;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.InternationalNumber;
import com.aplos.common.beans.Website;
import com.aplos.common.beans.communication.BasicBulkMessageFinder;
import com.aplos.common.beans.communication.BulkMessageSourceGroup;
import com.aplos.common.beans.communication.KeyedBulkSmsSource;
import com.aplos.common.beans.communication.SmsMessage;
import com.aplos.common.beans.communication.SmsTemplate;
import com.aplos.common.interfaces.BulkMessageSource;
import com.aplos.common.interfaces.BulkSmsFinder;
import com.aplos.common.interfaces.BulkSmsSource;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=SmsMessage.class)
public class SmsMessageEditPage extends EditPage {
	private static final long serialVersionUID = -8964101139370700913L;

	public static final int CHARS_PER_MESSAGE = 160;
	private BasicBulkMessageFinder selectedSmsFinder = null;
	
	private String flashedSourceDisplayName = null;
	private List<SelectItem> bulkMessageSourceGroupSelectItems;
	private List<SelectItem> bulkMessageSourceGroupFilterSelectItems;
	
	//this field is used with the autocomplete
	private BulkSmsSource recipientSelection = null;
	private int charsPerMessage = 160;
	private SmsMessage smsMessage;
	private BulkMessageSourceGroup selectedBulkMessageSourceGroup;
	
	private SmsTemplate selectedSmsTemplate;
	private SmsTemplate currentSmsTemplate;
	private boolean isShowingLimitRecipientSearch = true;
	private boolean isTemplateSelected = false;
	private boolean isAllowingNewNumbers = true;
	private String prefix;
	private String suffix;
	
	private SelectItem[] smsTemplateSelectItems;
	
	public SmsMessageEditPage() {
		createBulkMessageSourceGroupFilterSelectItems();
		createBulkMessageSourceGroupSelectItems();
	}

	@Override
	public boolean responsePageLoad() {
		if( getSmsMessage() == null ) {
			setSmsMessage((SmsMessage)JSFUtil.getBeanFromScope( SmsMessage.class ));
			if( !smsMessage.isNew() || smsMessage.getSmsTemplate() != null ) {
				setTemplateSelected(true);
			}
		}
		
		if( !isTemplateSelected() ) {
			createSmsTemplateSelectItems();
			if( getSmsTemplateSelectItems().length == 1 ) {
				setTemplateSelected(true);
			}
		}
		
		return super.responsePageLoad();
	}
	
	public boolean isMessageSent() {
		return getSmsMessage().getSmsSentDate() != null;
	}

	public boolean isSmsUpdateAllowed() {
		return getSmsMessage().getSmsSentDate() == null;
	}
	
	public void checkEncoding() {
        setCharsPerMessage( getSmsMessage().determineCharsPerMessage() );
	}

	public void createBulkMessageSourceGroupSelectItems() {
		bulkMessageSourceGroupSelectItems = new ArrayList<SelectItem>();
		
		BeanDao bulkMessageSourceGroupDao = new BeanDao( BulkMessageSourceGroup.class );
		bulkMessageSourceGroupDao.addWhereCriteria( "bean.isVisibleInSearches = true" );
		List<BulkMessageSourceGroup> bulkMessageSourceGroups = bulkMessageSourceGroupDao.getAll();
		for( BulkMessageSourceGroup tempBulkMessageSourceGroup : bulkMessageSourceGroups ) {
			if( tempBulkMessageSourceGroup.getSourceType() != null && BulkSmsSource.class.isAssignableFrom( tempBulkMessageSourceGroup.getSourceType() ) ) {
				bulkMessageSourceGroupSelectItems.add(new SelectItem(tempBulkMessageSourceGroup, tempBulkMessageSourceGroup.getName() ));
			}
		}
	}

	public void addSelectedBulkMessageSourceGroup() {
		SmsMessage smsMessage = resolveAssociatedBean();
		if( getSelectedBulkMessageSourceGroup().getId() == null ) {
			getSelectedBulkMessageSourceGroup().saveDetails();
		}
		
		smsMessage.getMessageSourceList().add( getSelectedBulkMessageSourceGroup() );
		smsMessage.filterMessageSourceList();
	}

	public void removeBulkMessageSourceGroup() {
		BulkMessageSource bulkMessageSource = (BulkMessageSource) JSFUtil.getRequest().getAttribute( "selectedSource" );
		getSmsMessage().getMessageSourceList().remove( bulkMessageSource );
		smsMessage.filterMessageSourceList();
	}

	public void removeBulkMessageSourceGroup(BulkSmsSource bulkSmsSource) {
		getSmsMessage().getMessageSourceList().remove(bulkSmsSource);
		smsMessage.filterMessageSourceList();
	}

	public void createBulkMessageSourceGroupFilterSelectItems() {
		List<SelectItem> selectItems = new ArrayList<SelectItem>();
		selectItems.add(new SelectItem(null, "Please Select"));
		selectItems.addAll( Website.getCurrentWebsiteFromTabSession().getBulkSmsFinderSelectItems() );
		setBulkMessageSourceGroupFilterSelectItems(selectItems);
	}

	private boolean isMobileNumber(String number) {
		if (number == null) {
			return false;
		}
		return (number.startsWith("07"));
	}

	public void resetForm() {
		setSmsMessage(new SmsMessage());
		getSmsMessage().init();
		getSmsMessage().addToScope();
	}

	public void sendSMS() {
		if (getSmsMessage().determineRecipientCount() < 1) {
			JSFUtil.addMessageForError("You have not selected recipients to send this message to.");
			return;
		}
		if (getSmsMessage().getContent().length() < 1 || getSmsMessage().getContent().trim().equals("")) {
			JSFUtil.addMessageForError("The message is empty.");
			return;
		}
		
		if( getSmsMessage().sendSmsMessageToQueue() ) {
			JSFUtil.addMessage( "Your SMS message has been sent" );
		}
	}

	public static String getDivertSMSAddress() {
		try {
			if( InetAddress.getLocalHost().getHostName().equalsIgnoreCase( "targaryan-PC" ) ) {
				return "07725742734";
			} else {
				return "07727230047";
			}
		} catch( UnknownHostException unEx ) {
			return "07727230047";
		}
	}
	
	public void addRecipient( SelectEvent event ) {
		BulkSmsSource selectedSmsRecipient = (BulkSmsSource) event.getObject();
		if (selectedSmsRecipient != null) {
			if( selectedSmsRecipient instanceof KeyedBulkSmsSource && ((KeyedBulkSmsSource)selectedSmsRecipient).isNew() ) {
				((KeyedBulkSmsSource) selectedSmsRecipient).saveDetails();
			}
			getSmsMessage().getMessageSourceList().add( selectedSmsRecipient );
		}
		getSmsMessage().filterMessageSourceList();
		recipientSelection = null;
	}

	public List<BulkSmsSource> suggestRecipients(String searchStr) {
		InternationalNumber internationalNumber = InternationalNumber.parseMobileNumberStr( searchStr );
		String parsedSearchStr = internationalNumber.getSafeFullNumber();
		List<BulkSmsSource> suggestions = new ArrayList<BulkSmsSource>();
		List<BulkSmsSource> tempSuggestions = new ArrayList<BulkSmsSource>();
		int invalidCounter=0;

		boolean alreadyInList = false;
		if (getSelectedSmsFinder() != null) {
			BulkSmsFinder bulkSmsFinder = getSelectedSmsFinder().getBulkSmsFinderClassInstance();
			List<BulkSmsSource> sourceTypedSuggestions = bulkSmsFinder.getSmsAutoCompleteSuggestions(searchStr, 15);
			if (sourceTypedSuggestions != null) {
				for (BulkSmsSource tempBulkSmsSource : sourceTypedSuggestions) { 
					if( tempBulkSmsSource.getInternationalNumber() != null && CommonUtil.validateTelephone(tempBulkSmsSource.getInternationalNumber().getSafeFullNumber())) {
						tempSuggestions.add( tempBulkSmsSource );
						if (tempBulkSmsSource != null && tempBulkSmsSource.getInternationalNumber().getSafeFullNumber().equals(searchStr)) {
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

		if (!alreadyInList && isAllowingNewNumbers()) {
			tempSuggestions.add(new KeyedBulkSmsSource( null, internationalNumber ) );
		}
		for (BulkSmsSource bulkSmsSource : tempSuggestions) {
			if (!getSmsMessage().getMessageSourceList().contains(bulkSmsSource)) {
				suggestions.add(bulkSmsSource);
			}
		}
		return sortByFullDetails(suggestions);
	}

		
	public String removeSmsRecipient(AjaxBehaviorEvent event) {
		BulkSmsSource recipient = (BulkSmsSource) JSFUtil.getRequest().getAttribute("recipient");
		getSmsMessage().getMessageSourceList().remove( recipient );
		return null; //to refresh the ui repeat
	}
	
	public void createSmsTemplateSelectItems() {
		BeanDao smsTemplateDao = new BeanDao( SmsTemplate.class );
		smsTemplateSelectItems = AplosBean.getSelectItemBeansWithNotSelected( smsTemplateDao.getAll() );
	}
	
	public SelectItem[] getSmsTemplateSelectItems() {
		return smsTemplateSelectItems;
	}

//	public List<String> getSmsRecipientEndpointList() {
//		ArrayList<String> smsRecipientList = new ArrayList<String>();
//		Iterator<TelephoneEndpoint> iter = smsRecipients.iterator();
//		while( iter.hasNext() ) {
//			smsRecipientList.add( iter.next().getEndpoint() );
//		}
//		return smsRecipientList;
//	}

	public BulkSmsSource getRecipientSelection() {
		return recipientSelection;
	}

	public void setRecipientSelection(BulkSmsSource recipientSelection) {
		this.recipientSelection = recipientSelection;
	}

	public void setSmsRecipientsSize( int smsRecipientsSize ) {
		//dummy method for jsf
	}

	/**
	 * This returns the limit this user/company/application has on SMS messages.
	 */
	public int getSmsQuota() {
		return getSmsMessage().getSmsAccount().getCachedSmsCredits();
	}

	public static List<BulkSmsSource> sortByFullDetails( List<BulkSmsSource> bulkSmsSources ) {
		Collections.sort( bulkSmsSources, new Comparator<BulkSmsSource>() {
			@Override
			public int compare(BulkSmsSource endpoint1, BulkSmsSource endpoint2) {
				return endpoint1.getSourceUniqueDisplayName().toLowerCase().compareTo( endpoint2.getSourceUniqueDisplayName().toLowerCase() );
			}
		});
		return bulkSmsSources;
	}
	
	public void smsTemplateSelected() {
		setTemplateSelected(true);
		getSmsMessage().updateSmsTemplate(getSmsMessage().getSmsTemplate());
	}
	
	public String getFlashedSourceDisplayName() {
		return flashedSourceDisplayName;
	}

	public void setFlashedSourceDisplayName(String flashedSourceDisplayName) {
		this.flashedSourceDisplayName = flashedSourceDisplayName;
	}

	public BasicBulkMessageFinder getSelectedSmsFinder() {
		return selectedSmsFinder;
	}

	public void setSelectedSmsFinder(BasicBulkMessageFinder selectedSmsFinder) {
		this.selectedSmsFinder = selectedSmsFinder;
	}

	public List<SelectItem> getBulkMessageSourceGroupSelectItems() {
		return bulkMessageSourceGroupSelectItems;
	}

	public void setBulkMessageSourceGroupSelectItems(
			List<SelectItem> bulkMessageSourceGroupSelectItems) {
		this.bulkMessageSourceGroupSelectItems = bulkMessageSourceGroupSelectItems;
	}

	public void setCharsPerMessage(int charsPerMessage) {
		this.charsPerMessage = charsPerMessage;
	}

	public int getCharsPerMessage() {
		return charsPerMessage;
	}

	public SmsMessage getSmsMessage() {
		return smsMessage;
	}

	public void setSmsMessage(SmsMessage smsMessage) {
		this.smsMessage = smsMessage;
	}

	public BulkMessageSourceGroup getSelectedBulkMessageSourceGroup() {
		return selectedBulkMessageSourceGroup;
	}

	public void setSelectedBulkMessageSourceGroup(
			BulkMessageSourceGroup selectedBulkMessageSourceGroup) {
		this.selectedBulkMessageSourceGroup = selectedBulkMessageSourceGroup;
	}

	public List<SelectItem> getBulkMessageSourceGroupFilterSelectItems() {
		return bulkMessageSourceGroupFilterSelectItems;
	}

	public void setBulkMessageSourceGroupFilterSelectItems(
			List<SelectItem> bulkMessageSourceGroupFilterSelectItems) {
		this.bulkMessageSourceGroupFilterSelectItems = bulkMessageSourceGroupFilterSelectItems;
	}


	public SmsTemplate getSelectedSmsTemplate() {
		return selectedSmsTemplate;
	}


	public void setSelectedSmsTemplate(SmsTemplate selectedSmsTemplate) {
		this.selectedSmsTemplate = selectedSmsTemplate;
	}


	public SmsTemplate getCurrentSmsTemplate() {
		return currentSmsTemplate;
	}


	public void setCurrentSmsTemplate(SmsTemplate currentSmsTemplate) {
		this.currentSmsTemplate = currentSmsTemplate;
	}

	public boolean isShowingLimitRecipientSearch() {
		return isShowingLimitRecipientSearch;
	}

	public void setShowingLimitRecipientSearch(boolean isShowingLimitRecipientSearch) {
		this.isShowingLimitRecipientSearch = isShowingLimitRecipientSearch;
	}

	public boolean isTemplateSelected() {
		return isTemplateSelected;
	}

	public void setTemplateSelected(boolean isTemplateSelected) {
		this.isTemplateSelected = isTemplateSelected;
	}

	public boolean isAllowingNewNumbers() {
		return isAllowingNewNumbers;
	}

	public void setAllowingNewNumbers(boolean isAllowingNewNumbers) {
		this.isAllowingNewNumbers = isAllowingNewNumbers;
	}

	public void setSmsTemplateSelectItems(SelectItem[] smsTemplateSelectItems) {
		this.smsTemplateSelectItems = smsTemplateSelectItems;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

}
