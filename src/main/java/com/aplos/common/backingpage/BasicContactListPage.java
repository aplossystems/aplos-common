package com.aplos.common.backingpage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.model.SortOrder;
import org.primefaces.model.UploadedFile;

import com.aplos.common.AplosLazyDataModel;
import com.aplos.common.AplosUrl;
import com.aplos.common.BeanMenuHelper;
import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.appconstants.AplosAppConstants;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.communication.AplosEmailEditPage;
import com.aplos.common.beans.BasicContact;
import com.aplos.common.beans.BasicContactTag;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.beans.FileDetails;
import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.beans.communication.KeyedBulkSmsSource;
import com.aplos.common.beans.communication.SmsMessage;
import com.aplos.common.enums.CommonEmailTemplateEnum;
import com.aplos.common.enums.CommonWorkingDirectory;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.FileIoUtil;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=BasicContact.class)
public class BasicContactListPage extends ListPage {
	private static final long serialVersionUID = -2238823584799953044L;
	
	private UploadedFile uploadedFile;
	private BasicContactTag selectedBasicContactTagFilter;
	private BasicContactTag selectedImportBasicContactTag;
	private List<BasicContactTag> importBasicContactTags = new ArrayList<BasicContactTag>();
	
	public BeanMenuHelper getImportBasicContactTagBmh() {
		BeanMenuHelper beanMenuHelper = new BeanMenuHelper( BasicContactTag.class );
		beanMenuHelper.setWithNotSelected(false);
		return beanMenuHelper;
	}
	
	public void addImportBasicContactTag() {
		importBasicContactTags.add( getSelectedImportBasicContactTag() );
	}
	
	public void removeImportBasicContactTag() {
		BasicContactTag basicContactTag = JSFUtil.getBeanFromRequest( "tableBean" );
		importBasicContactTags.remove( basicContactTag );
	}

	@Override
	public AplosLazyDataModel getAplosLazyDataModel(
			DataTableState dataTableState, BeanDao aqlBeanDao) {
		return new BasicContactLdm(dataTableState, aqlBeanDao);
	}
	
	public void exportBasicContactsToCsv() {
		FileDetails fileDetails = createBasicContactsCsv();	    
	    AplosUrl aplosUrl = fileDetails.getAplosUrl();
	    aplosUrl.addQueryParameter( AplosAppConstants.ATTACHMENT, "true" );
	    JSFUtil.redirect( aplosUrl );
	}
	
	public BeanMenuHelper getBasicContactTagFilterBmh() {
		BeanMenuHelper beanMenuHelper = new BeanMenuHelper( BasicContactTag.class );
		beanMenuHelper.setShowingNewBtn(false);
		return beanMenuHelper;
	}

	public void importBasicContacts() {
		BeanDao beanDao = new BeanDao( BasicContact.class );
		beanDao.addSelectCriteria( new String[]{ "bean.address.contactFirstName",
				"bean.address.contactSurname",
				"bean.address.companyName",
				"bean.address.line1",
				"bean.address.line2",
				"bean.address.line3",
				"bean.address.city",
				"bean.address.state",
				"bean.address.postcode",
				"bean.address.subscriber.emailAddress",
				"bean.address.phone",
		});
		beanDao.addWhereCriteria( "bean.address.subscriber.emailAddress = :duplicateCheck0" );
		List<Integer> duplicateChecks = new ArrayList<Integer>();
		duplicateChecks.add( 9 );
		List<BasicContact> basicContacts = (List<BasicContact>) FileIoUtil.importIntoBean( beanDao, getUploadedFile(), duplicateChecks );
		if( getImportBasicContactTags().size() > 0 ) {
			for( int i = 0, n = basicContacts.size(); i < n; i++ ) {
				for( BasicContactTag tempBasicContactTag : getImportBasicContactTags() ) {
					basicContacts.get( i ).getBasicContactTags().add( tempBasicContactTag );
				}
				basicContacts.get( i ).saveDetails();
			}
		}
	}  
	
	public static FileDetails createBasicContactsCsv() {
		List<BasicContact> basicContactList = new BeanDao( BasicContact.class ).getAll();
		StringBuffer strBuf = new StringBuffer();
		for( int i = 0, n = basicContactList.size(); i < n; i++ ) {
			strBuf.append( "\"" ).append( CommonUtil.getStringOrEmpty( basicContactList.get( i ).getAddress().getContactFirstName() ) ).append( "\"," );
			strBuf.append( "\"" ).append( CommonUtil.getStringOrEmpty( basicContactList.get( i ).getAddress().getContactSurname() ) ).append( "\"," );
			strBuf.append( "\"" ).append( CommonUtil.getStringOrEmpty( basicContactList.get( i ).getAddress().getCompanyName() ) ).append( "\"," );
			strBuf.append( "\"" ).append( CommonUtil.getStringOrEmpty( basicContactList.get( i ).getAddress().getEmailAddress() ) ).append( "\"," );
			strBuf.append( "\"" ).append( CommonUtil.getStringOrEmpty( basicContactList.get( i ).getAddress().getPhone() ) ).append( "\"," );
			strBuf.append( "\"" ).append( CommonUtil.getStringOrEmpty( basicContactList.get( i ).getAddress().getMobile() ) ).append( "\"" );
			strBuf.append( "\n" );
		}

	    FileDetails fileDetails = new FileDetails();
	    fileDetails.setFileDetailsOwner( CommonWorkingDirectory.BASIC_CONTACT_DETAILS_CSV.getAplosWorkingDirectory() );
	    fileDetails.setName("basicContacts.csv");
	    fileDetails.saveDetails();
	    fileDetails.setFilename( fileDetails.getId() + ".csv");
	    fileDetails.saveDetails();
	    fileDetails.writeStringToFile( strBuf.toString() );
	    return fileDetails;
	}
	
	public UploadedFile getUploadedFile() {
		return uploadedFile;
	}

	public void setUploadedFile(UploadedFile uploadedFile) {
		this.uploadedFile = uploadedFile;
	}

	public BasicContactTag getSelectedBasicContactTagFilter() {
		return selectedBasicContactTagFilter;
	}

	public void setSelectedBasicContactTagFilter(BasicContactTag selectedBasicContactTag) {
		this.selectedBasicContactTagFilter = selectedBasicContactTag;
	}

	public List<BasicContactTag> getImportBasicContactTags() {
		return importBasicContactTags;
	}

	public void setImportBasicContactTags(List<BasicContactTag> importBasicContactTags) {
		this.importBasicContactTags = importBasicContactTags;
	}

	public BasicContactTag getSelectedImportBasicContactTag() {
		return selectedImportBasicContactTag;
	}

	public void setSelectedImportBasicContactTag(BasicContactTag selectedImportBasicContactTag) {
		this.selectedImportBasicContactTag = selectedImportBasicContactTag;
	};

	public class BasicContactLdm extends AplosLazyDataModel {
		private static final long serialVersionUID = -5039016274818022398L;

		public BasicContactLdm(DataTableState dataTableState, BeanDao aqlBeanDao) {
			super(dataTableState, aqlBeanDao);
			getBeanDao().setSelectCriteria( "DISTINCT(bean)" );
			getBeanDao().setCountFields( "DISTINCT bean.id" );
			getBeanDao().addQueryTable( "basicContactTag", "bean.basicContactTags" );
		}
		
		@Override
		public List<Object> load(int first, int pageSize, String sortField,
				SortOrder sortOrder, Map<String, String> filters) {
			return load(first, pageSize, sortField, sortOrder, filters, true);
		}

		public List<Object> load(int first, int pageSize, String sortField,
				SortOrder sortOrder, Map<String, String> filters, boolean clearWhereCriteria ) {
			if( clearWhereCriteria ) {
				getBeanDao().clearWhereCriteria();
			}
			if( getSelectedBasicContactTagFilter() != null ) {
				getBeanDao().addWhereCriteria( "basicContactTag.id = " + getSelectedBasicContactTagFilter().getId() );
			}
			return super.load(first, pageSize, sortField, sortOrder, filters);
		}

		public void emailBasicContact() {
			BasicContact basicContact = (BasicContact) getAplosBean();
			AplosEmail aplosEmail = new AplosEmail( CommonEmailTemplateEnum.BASIC_CONTACT, basicContact, basicContact );
			aplosEmail.addEmailFolder(basicContact);
			aplosEmail.addToScope();
			JSFUtil.redirect(AplosEmailEditPage.class);
		}
		
		public boolean isDisablingEmail() {
			return false;
		}
		
		public boolean isDisablingSms() {
			return false;
		}
		
		public void smsBasicContact() {
			BasicContact basicContact = (BasicContact) getAplosBean();
			SmsMessage smsMessage;
			if( !CommonUtil.isNullOrEmpty( basicContact.getAddress().getMobile() ) ) {
				smsMessage = new SmsMessage( new KeyedBulkSmsSource( basicContact, basicContact.getAddress().getMobile() ) );
				smsMessage.setSmsMessageOwner(basicContact);
				smsMessage.redirectToEditPage();
			} else {
				JSFUtil.addMessage( "Sms cannot be created as client does not have a mobile number" );
			}
		}	
	}
}
