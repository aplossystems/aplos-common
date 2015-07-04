package com.aplos.common.backingpage.marketing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;
import org.primefaces.model.SortOrder;
import org.primefaces.model.UploadedFile;

import com.aplos.common.AplosLazyDataModel;
import com.aplos.common.AplosUrl;
import com.aplos.common.ImportableColumn;
import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.appconstants.AplosAppConstants;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.ListPage;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.beans.FileDetails;
import com.aplos.common.beans.communication.EmailTemplate;
import com.aplos.common.beans.listbeans.PotentialCompanyListBean;
import com.aplos.common.beans.marketing.PotentialCompany;
import com.aplos.common.beans.marketing.PotentialCompanyCategory;
import com.aplos.common.beans.marketing.PotentialCompanyInteraction;
import com.aplos.common.beans.marketing.PotentialCompanyInteraction.InteractionMethod;
import com.aplos.common.beans.marketing.SalesCallTask;
import com.aplos.common.enums.CommonWorkingDirectory;
import com.aplos.common.enums.PotentialCompanyStatus;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.FileIoUtil;
import com.aplos.common.utils.JSFUtil;

@AssociatedBean(beanClass=PotentialCompany.class)
@ManagedBean
@ViewScoped
public class PotentialCompanyListPage extends ListPage {
	private static final long serialVersionUID = -5949632093062753181L;
	
	private PotentialCompanyStatus selectedPotentialCompanyStatus;
	private Long firstId;
	private Long lastId;
	private List<SelectItem> newsletterTemplateSelectItems;
	private EmailTemplate selectedEmailTemplate;

	private List<PotentialCompanyCategory> potentialCompanyCategoryList;
	private UploadedFile uploadedFile;
	
	private static final String CONTACT_FIRST_NAME = "Contact first name";
	private static final String CONTACT_SURNAME = "Contact surname";
	private static final String COMPANY_NAME = "Company name";
	private static final String ADDRESS_LINE_1 = "Address Line 1";
	private static final String ADDRESS_LINE_2 = "Address Line 2";
	private static final String ADDRESS_LINE_3 = "Address Line 3";
	private static final String CITY = "City";
	private static final String COUNTY = "County";
	private static final String POSTCODE = "Postcode";
	private static final String JOB_TITLE = "Job Title";
	private static final String EMAIL_ADDRESS = "Email";
	private static final String PHONE = "Phone";
	
	
	@Override
	public boolean responsePageLoad() {
		if( potentialCompanyCategoryList == null ) {
			potentialCompanyCategoryList = new BeanDao( PotentialCompanyCategory.class ).getAll();
		}
		if( getNewsletterTemplateSelectItems() == null ) {
			createNewsletterTemplateSelectItems();
		}
		boolean continueLoad = super.responsePageLoad();
		return continueLoad;
	}
	
	public void createTask( List<PotentialCompany> potentialCompanyList ) {
		SalesCallTask salesCallTask = new SalesCallTask();
		salesCallTask.initialiseNewBean();
		salesCallTask.setUnfinishedPotentialCompanyList(potentialCompanyList);
		salesCallTask.saveDetails();
		salesCallTask.redirectToEditPage();
	}
	
	public void createNewsletterTemplateSelectItems() {
	}
	
	public boolean isShowingTaskPnl() {
		return true;
	}
	
	public boolean isShowingStatusPnl() {
		return true;
	}
	
	public boolean isShowingExportPnl() {
		return true;
	}
	
	public boolean isShowingEmailPnl() {
		return true;
	}
	
	public boolean isShowingImportPnl() {
		return true;
	}
	
	@Override
	public BeanDao getListBeanDao() {
		BeanDao potentialCompanyDao = new BeanDao( PotentialCompany.class );
		potentialCompanyDao.setSelectCriteria( "bean.id, bean.address.companyName, bean.mainCategory, bean.address, bean.potentialCompanyStatus, bean.lastContactedDate, bean.reminderDate, (SELECT count(*) FROM PotentialCompanyInteraction pci WHERE pci.potentialCompany_id = bean.id) as numberOfInteractions" );
		potentialCompanyDao.setListBeanClass(PotentialCompanyListBean.class);
		return potentialCompanyDao;
	}
	
	public List<PotentialCompany> getSelectedPotentialCompanies() {
		Object[] rows = getDataTableState().getLazyDataModel().getSelectedRows();
		List<PotentialCompany> potentialCompanyList = new ArrayList<PotentialCompany>();
		if( rows != null ) {
			for( int i = 0, n = rows.length; i < n; i++ ) {
				potentialCompanyList.add( ((PotentialCompany) rows[ i ] ) );
			}
		}
		return potentialCompanyList;
	}
	
	public List<PotentialCompany> getFilteredPotentialCompanies() {
		PotentialCompanyLdm potentialCompanyLdm = (PotentialCompanyLdm) getDataTableState().getLazyDataModel();
		BeanDao aqlBeanDao = potentialCompanyLdm.getBeanDao();
		getDataTableState().getLazyDataModel().addSearchParameters(aqlBeanDao, getDataTableState().getColumnFilters());
		aqlBeanDao.setMaxResults( -1 );
		List<PotentialCompany> potentialCompanyList = aqlBeanDao.getAll();
		return potentialCompanyList;
	}
	
	public void sendToPotentialCompanies( List<PotentialCompany> potentialCompanyList ) {
//		if (potentialCompanyList.size() > 0) {
//			BulkMessageSourceGroup besGroup = new BulkMessageSourceGroup();
//			besGroup.setName("Selected donors");
//			besGroup.setSourceType(PotentialCompany.class);
//			
//			for (int i=0; i < potentialCompanyList.size(); i++) {
//				besGroup.getBulkMessageSources().add(potentialCompanyList.get( i ));
//			}
//
//			besGroup.aqlSaveDetails();
//			AplosEmail aplosEmail = new AplosEmail( AltruiEmailTemplateEnum.GENERAL_DONOR_EMAIL, besGroup );
//			aplosEmail.setUsingEmailSourceAsOwner(true);
//			aplosEmail.redirectToEditPage( false );
//		} else {
//			JSFUtil.addMessageForError("Please select the donor(s) you wish to message.");
//		}
	}

	@Override
	public boolean emailBeans(List<AplosBean> beanList) {
		if( getSelectedEmailTemplate() != null ) {
			return super.emailBeans(beanList, getSelectedEmailTemplate().getEmailTemplateEnum() );
		} else {
			return super.emailBeans(beanList);
		}
	}

	
	public void changeStatus( List<PotentialCompanyListBean> potentialCompanyList ) {
		String potentialCompanyIds = CommonUtil.joinIds(potentialCompanyList);
		StringBuffer hql = new StringBuffer( "UPDATE " ).append( AplosBean.getTableName( PotentialCompany.class ) );
		hql.append( " SET potentialCompanyStatus = " ).append( getSelectedPotentialCompanyStatus().ordinal() );
		hql.append( " WHERE id IN (" ).append( potentialCompanyIds ).append( ")" );
		ApplicationUtil.executeSql( hql.toString() );
	}
	
	@Override
	public AplosLazyDataModel getAplosLazyDataModel(
			DataTableState dataTableState, BeanDao aqlBeanDao) {
		return new PotentialCompanyLdm(dataTableState, aqlBeanDao);
	}

	public void importPotentialCompanies() {
		List<ImportableColumn> importableColumns = new ArrayList<ImportableColumn>();
		importableColumns.add( new ImportableColumn( CONTACT_FIRST_NAME, "bean.address.contactFirstName" ) );
		importableColumns.add( new ImportableColumn( CONTACT_SURNAME, "bean.address.contactSurname" ) );
		importableColumns.add( new ImportableColumn( COMPANY_NAME, "bean.address.companyName" ) );
		importableColumns.add( new ImportableColumn( ADDRESS_LINE_1, "bean.address.line1" ) );
		importableColumns.add( new ImportableColumn( ADDRESS_LINE_2, "bean.address.line2" ) );
		importableColumns.add( new ImportableColumn( ADDRESS_LINE_3, "bean.address.line3" ) );
		importableColumns.add( new ImportableColumn( CITY, "bean.address.city" ) );
		importableColumns.add( new ImportableColumn( COUNTY, "bean.address.state" ) );
		importableColumns.add( new ImportableColumn( POSTCODE, "bean.address.postcode" ) );
		importableColumns.add( new ImportableColumn( JOB_TITLE, "bean.contactPosition" ) );
		importableColumns.add( new ImportableColumn( EMAIL_ADDRESS, "bean.address.subscriber.emailAddress" ) );
		importableColumns.add( new TitleImportableColumn() );
		importableColumns.add( new ImportableColumn( PHONE, "bean.address.phone" ) {
			private BeanDao duplicationDao;
			
			@Override
			public void init() {
				duplicationDao = new BeanDao( PotentialCompany.class );
				duplicationDao.addWhereCriteria( "bean.address.phone = :phone" );
			}
			
			@Override
			public Object format(String value) {
				if( CommonUtil.isNullOrEmpty(value) ) {
					return value;
				}
				if( !value.startsWith( "0" ) ) {
					value = "0" + value; 
				}

				value = value.replaceAll( "\\s", "" );
				value = value.trim();
				return value;
			}

			@Override
			public boolean validate(Object value) {
				String strValue = (String) value;
				if( CommonUtil.isNullOrEmpty(strValue) ) {
					return true;
				}
				if( strValue.length() < 10
						|| !CommonUtil.validatePositiveInteger( strValue ) ) {
					return false;
				}
				return true;
			}
			
			@Override
			public boolean duplicateCheck(Object value) {
				String strValue = (String) value;
				if( !CommonUtil.isNullOrEmpty(strValue) ) {
					duplicationDao.setNamedParameter( "phone", strValue );
					if( duplicationDao.getCountAll() > 0 ) {
						return false;
					}
				}
				return super.duplicateCheck(value);
			}
		});
		importableColumns.add( new ImportableColumn( "Email", "bean.address.subscriber.emailAddress" ) );

		List<PotentialCompany> potentialCompanyList = (List<PotentialCompany>) FileIoUtil.importIntoBean( PotentialCompany.class, getUploadedFile(), importableColumns, false );
		for( int i = 0, n = potentialCompanyList.size(); i < n; i++ ) {
			if( !CommonUtil.isNullOrEmpty( potentialCompanyList.get( i ).getAddress().getSubscriber().getEmailAddress() ) ) {
				potentialCompanyList.get( i ).getAddress().getSubscriber().setFirstName( potentialCompanyList.get( i ).getAddress().getContactFirstName() );
				potentialCompanyList.get( i ).getAddress().getSubscriber().setSurname( potentialCompanyList.get( i ).getAddress().getContactSurname() );
			}
			potentialCompanyList.get( i ).saveDetails();
		}
	} 
	
	public void exportCompaniesToCsv( List<PotentialCompanyListBean> potentialCompanyList ) {
		StringBuffer strBuf = new StringBuffer();
		BeanDao potentialCompanyDao = new BeanDao( PotentialCompany.class );
		PotentialCompany tempPotentialCompany;
		
		strBuf.append( "\"" ).append( "ID" ).append( "\"," );
		strBuf.append( "\"" ).append( CONTACT_FIRST_NAME ).append( "\"," );
		strBuf.append( "\"" ).append( CONTACT_SURNAME ).append( "\"," );
		strBuf.append( "\"" ).append( COMPANY_NAME ).append( "\"," );
		strBuf.append( "\"" ).append( ADDRESS_LINE_1 ).append( "\"," );
		strBuf.append( "\"" ).append( ADDRESS_LINE_2 ).append( "\"," );
		strBuf.append( "\"" ).append( ADDRESS_LINE_3 ).append( "\"," );
		strBuf.append( "\"" ).append( CITY ).append( "\"," );
		strBuf.append( "\"" ).append( COUNTY ).append( "\"," );
		strBuf.append( "\"" ).append( POSTCODE ).append( "\"," );
		strBuf.append( "\"" ).append( JOB_TITLE ).append( "\"," );
		strBuf.append( "\"" ).append( EMAIL_ADDRESS ).append( "\"," );
		strBuf.append( "\"" ).append( PHONE ).append( "\"" );
		strBuf.append( "\n" );
		
		
		for( int i = 0, n = potentialCompanyList.size(); i < n; i++ ) {
			tempPotentialCompany = potentialCompanyDao.get( potentialCompanyList.get( i ).getId() );
			strBuf.append( "\"" ).append( CommonUtil.getStringOrEmpty( tempPotentialCompany.getId() ) ).append( "\"," );
			strBuf.append( "\"" ).append( CommonUtil.getStringOrEmpty( tempPotentialCompany.getAddress().getContactFirstName() ) ).append( "\"," );
			strBuf.append( "\"" ).append( CommonUtil.getStringOrEmpty( tempPotentialCompany.getAddress().getContactSurname() ) ).append( "\"," );
			strBuf.append( "\"" ).append( CommonUtil.getStringOrEmpty( tempPotentialCompany.getAddress().getCompanyName() ) ).append( "\"," );
			strBuf.append( "\"" ).append( CommonUtil.getStringOrEmpty( tempPotentialCompany.getAddress().getLine1() ) ).append( "\"," );
			strBuf.append( "\"" ).append( CommonUtil.getStringOrEmpty( tempPotentialCompany.getAddress().getLine2() ) ).append( "\"," );
			strBuf.append( "\"" ).append( CommonUtil.getStringOrEmpty( tempPotentialCompany.getAddress().getLine3() ) ).append( "\"," );
			strBuf.append( "\"" ).append( CommonUtil.getStringOrEmpty( tempPotentialCompany.getAddress().getCity() ) ).append( "\"," );
			strBuf.append( "\"" ).append( CommonUtil.getStringOrEmpty( tempPotentialCompany.getAddress().getState() ) ).append( "\"," );
			strBuf.append( "\"" ).append( CommonUtil.getStringOrEmpty( tempPotentialCompany.getAddress().getPostcode() ) ).append( "\"," );
			strBuf.append( "\"" ).append( CommonUtil.getStringOrEmpty( tempPotentialCompany.getContactPosition() ) ).append( "\"," );
			strBuf.append( "\"" ).append( CommonUtil.getStringOrEmpty( tempPotentialCompany.getAddress().getEmailAddress() ) ).append( "\"," );
			strBuf.append( "\"" ).append( CommonUtil.getStringOrEmpty( tempPotentialCompany.getAddress().getPhone() ) ).append( "\"" );
			strBuf.append( "\n" );
			
			PotentialCompanyInteraction potentialCompanyInteraction = new PotentialCompanyInteraction();
			potentialCompanyInteraction.setPotentialCompany(tempPotentialCompany);
			potentialCompanyInteraction.setMethod(InteractionMethod.SYSTEM);
			potentialCompanyInteraction.setPotentialCompanyStatus(null);
			potentialCompanyInteraction.setNotes( "Added from export" );
			potentialCompanyInteraction.saveDetails();
		}

	    try {
		    FileDetails fileDetails = new FileDetails();
		    fileDetails.setFileDetailsOwner( CommonWorkingDirectory.POTENTIAL_COMPANY_CSV.getAplosWorkingDirectory() );
		    fileDetails.setName("addressDetails.csv");
		    fileDetails.saveDetails();
		    fileDetails.setFilename( fileDetails.getId() + ".csv");
		    fileDetails.saveDetails();
		    
		    // Write the output to a file
		    File file = fileDetails.getFile();
		    file.createNewFile();
		    FileOutputStream fileOut = new FileOutputStream(file);
		    fileOut.write( strBuf.toString().getBytes() );
		    fileOut.close();

		    AplosUrl aplosUrl = fileDetails.getAplosUrl();
		    aplosUrl.addQueryParameter( AplosAppConstants.ATTACHMENT, "true" );
		    JSFUtil.redirect( aplosUrl );
	    } catch( FileNotFoundException fnfEx ) {
	    	ApplicationUtil.getAplosContextListener().handleError( fnfEx );
	    } catch( IOException ioEx ) {
	    	ApplicationUtil.getAplosContextListener().handleError( ioEx );
	    }
	}
	
	public SelectItem[] getCategorySelectItems() {
		return AplosBean.getSelectItemBeans( potentialCompanyCategoryList );
	}

	public PotentialCompanyStatus getSelectedPotentialCompanyStatus() {
		return selectedPotentialCompanyStatus;
	}

	public void setSelectedPotentialCompanyStatus(
			PotentialCompanyStatus selectedPotentialCompanyStatus) {
		this.selectedPotentialCompanyStatus = selectedPotentialCompanyStatus;
	}

	public UploadedFile getUploadedFile() {
		return uploadedFile;
	}

	public void setUploadedFile(UploadedFile uploadedFile) {
		this.uploadedFile = uploadedFile;
	}

	public Long getFirstId() {
		return firstId;
	}

	public void setFirstId(Long firstId) {
		this.firstId = firstId;
	}

	public Long getLastId() {
		return lastId;
	}

	public void setLastId(Long lastId) {
		this.lastId = lastId;
	}
	
	public List<SelectItem> getNewsletterTemplateSelectItems() {
		return newsletterTemplateSelectItems;
	}

	public void setNewsletterTemplateSelectItems(
			List<SelectItem> newsletterTemplateSelectItems) {
		this.newsletterTemplateSelectItems = newsletterTemplateSelectItems;
	}

	public EmailTemplate getSelectedEmailTemplate() {
		return selectedEmailTemplate;
	}

	public void setSelectedEmailTemplate(EmailTemplate selectedEmailTemplate) {
		this.selectedEmailTemplate = selectedEmailTemplate;
	}

	public class PotentialCompanyLdm extends AplosLazyDataModel {
		private static final long serialVersionUID = 3427450636008250211L; 
		private List<PotentialCompanyStatus> selectedFilterStatuses = new ArrayList<PotentialCompanyStatus>();
		private List<PotentialCompanyCategory> selectedFilterCategories = new ArrayList<PotentialCompanyCategory>();
		private Boolean isOnlyViewedEmail;
		private Boolean isOnlyActionedEmail;
		private Boolean isSubscribed;
		private Boolean isWithEmailAddress;
		private Boolean isWithWebsite;
		private Boolean isCallingAllowed;
		private List<SelectItem> yesNoSelectItems;
		
		public PotentialCompanyLdm( DataTableState dataTableState, BeanDao aqlBeanDao ) {
			super(dataTableState, aqlBeanDao);
			setYesNoSelectItems(createYesNoSelectItems());
		}
		
		@Override
		public void addStateFields() throws NoSuchFieldException {
			addStateField( "selectedFilterStatuses" );
			addStateField( "selectedFilterCategories" );
		}
		
		public void redirectToInteractions() {
			PotentialCompany potentialCompany = (PotentialCompany) getRealBean();
			potentialCompany.addToScope();
			JSFUtil.redirect( PotentialCompanyInteractionListPage.class );
		}
		
		public void newCopiedInteraction() {
			PotentialCompany potentialCompany = (PotentialCompany) getRealBean();
			potentialCompany.addToScope();

			PotentialCompanyInteraction oldInteraction = JSFUtil.getBeanFromScope( PotentialCompanyInteraction.class );	
			PotentialCompanyInteraction potentialCompanyInteraction = new PotentialCompanyInteraction();
			potentialCompanyInteraction.setPotentialCompany( potentialCompany );
			if( oldInteraction != null ) {
				oldInteraction.copyFieldsIntoNewInteraction( potentialCompanyInteraction );
			} else {
				potentialCompanyInteraction.setPotentialCompanyStatus( potentialCompany.getPotentialCompanyStatus() );
			}
			potentialCompany.addToScope();
			potentialCompanyInteraction.addToScope();
			JSFUtil.redirect( PotentialCompanyInteractionEditPage.class );
		}
		
		public void addSpecificSearchCriteria() {
			getBeanDao().clearWhereCriteria();
			getBeanDao().removeQueryTable( "pci" );
			getBeanDao().removeQueryTable( "ser" );

			if( getFirstId() != null ) {
				getBeanDao().addWhereCriteria( "bean.id >= " + getFirstId() );
			}
			if( getLastId() != null ) {
				getBeanDao().addWhereCriteria( "bean.id <= " + getLastId() );
			}
			
			if( getIsOnlyViewedEmail() != null ) {
				getBeanDao().setCountFields( "DISTINCT(bean.id)");
				getBeanDao().setSelectCriteria( "DISTINCT(bean)" );
				getBeanDao().addReverseJoinTable( PotentialCompanyInteraction.class, "pci.potentialCompany", "bean" );
				getBeanDao().addQueryTable( "ser", "pci.singleEmailRecord" );
				if( getIsOnlyViewedEmail() ) {
					getBeanDao().addWhereCriteria( "ser.openedDate != null" );
				} else {
					getBeanDao().addWhereCriteria( "ser.openedDate = null" );
				}
			}
			
			if( getIsOnlyActionedEmail() != null ) {
				getBeanDao().setCountFields( "DISTINCT(bean.id)");
				getBeanDao().setSelectCriteria( "DISTINCT(bean)" );
				getBeanDao().addReverseJoinTable( PotentialCompanyInteraction.class, "pci.potentialCompany", "bean" );
				getBeanDao().addQueryTable( "ser", "pci.singleEmailRecord" );
				if( getIsOnlyActionedEmail()  ) {
					getBeanDao().addWhereCriteria( "ser.actionedDate != null" );
				} else {
					getBeanDao().addWhereCriteria( "ser.actionedDate = null" );
				}
			}
			
			if( getIsSubscribed() != null ) {
				if( getIsSubscribed() ) {
					getBeanDao().addWhereCriteria( "bean.address.subscriber.isSubscribed = true" );
				} else {
					getBeanDao().addWhereCriteria( "bean.address.subscriber.isSubscribed = false" );
				}
			}
			
			if( getIsWithEmailAddress() != null ) {
				if( getIsWithEmailAddress() ) {
					getBeanDao().addWhereCriteria( "bean.address.subscriber.emailAddress != null" );
				} else {
					getBeanDao().addWhereCriteria( "bean.address.subscriber.emailAddress = null" );
				}
			}
			
			if( getIsWithWebsite() != null ) {
				if( getIsWithWebsite() ) {
					getBeanDao().addWhereCriteria( "bean.webAddress != null" );
				} else {
					getBeanDao().addWhereCriteria( "bean.webAddress = null" );
				}
			}
			
			if( getIsCallingAllowed() != null ) {
				if( getIsCallingAllowed() ) {
					getBeanDao().addWhereCriteria( "bean.isCallingAllowed = true" );
				} else {
					getBeanDao().addWhereCriteria( "bean.isCallingAllowed = false" );
				}
			}

			if( getSelectedFilterStatuses().size() > 0 ) {
				String statusWhereCriteria[] = new String[ getSelectedFilterStatuses().size() ];
				int count = 0;
				for( PotentialCompanyStatus tempPotentialCompanyStatus : getSelectedFilterStatuses() ) {
					statusWhereCriteria[ count++ ] = "bean.potentialCompanyStatus = " + String.valueOf( tempPotentialCompanyStatus.ordinal() );
				}
				getBeanDao().addWhereCriteria( StringUtils.join( statusWhereCriteria, " OR " ) );
			}

			if( getSelectedFilterCategories().size() > 0 ) {
				String categoryWhereCriteria[] = new String[ getSelectedFilterCategories().size() ];
				int count = 0;
				for( PotentialCompanyCategory tempPotentialCompanyCategory : getSelectedFilterCategories() ) {
					categoryWhereCriteria[ count++ ] = "bean.mainCategory.id = " + tempPotentialCompanyCategory.getId();
				}
				getBeanDao().addWhereCriteria( StringUtils.join( categoryWhereCriteria, " OR " ) );
			}
		}
		
		@Override
		public List<Object> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
			addSpecificSearchCriteria();
			List<Object> potentialCompanyListBeans = super.load(first, pageSize, sortField, sortOrder, filters);
			return potentialCompanyListBeans;
		}

		public List<PotentialCompanyStatus> getSelectedFilterStatuses() {
			return selectedFilterStatuses;
		}

		public void setSelectedFilterStatuses(List<PotentialCompanyStatus> selectedFilterStatuses) {
			this.selectedFilterStatuses = selectedFilterStatuses;
		}

		public List<PotentialCompanyCategory> getSelectedFilterCategories() {
			return selectedFilterCategories;
		}

		public void setSelectedFilterCategories(List<PotentialCompanyCategory> selectedFilterCategories) {
			this.selectedFilterCategories = selectedFilterCategories;
		}
		
		public List<SelectItem> createYesNoSelectItems() {
			List<SelectItem> selectItems = new ArrayList<SelectItem>();
			selectItems.add( new SelectItem( null, "Either" ) );
			selectItems.add( new SelectItem( true, "Yes" ) );
			selectItems.add( new SelectItem( false, "No" ) );
			return selectItems;
		}

		
		public List<SelectItem> getYesNoSelectItems() {
			return yesNoSelectItems;
		}

		public Boolean getIsOnlyViewedEmail() {
			return isOnlyViewedEmail;
		}

		public void setIsOnlyViewedEmail(Boolean isOnlyViewedEmail) {
			this.isOnlyViewedEmail = isOnlyViewedEmail;
		}

		public Boolean getIsOnlyActionedEmail() {
			return isOnlyActionedEmail;
		}

		public void setIsOnlyActionedEmail(Boolean isOnlyActionedEmail) {
			this.isOnlyActionedEmail = isOnlyActionedEmail;
		}

		public Boolean getIsSubscribed() {
			return isSubscribed;
		}

		public void setIsSubscribed(Boolean isSubscribed) {
			this.isSubscribed = isSubscribed;
		}

		public Boolean getIsWithEmailAddress() {
			return isWithEmailAddress;
		}

		public void setIsWithEmailAddress(Boolean isWithEmailAddress) {
			this.isWithEmailAddress = isWithEmailAddress;
		}

		public Boolean getIsWithWebsite() {
			return isWithWebsite;
		}

		public void setIsWithWebsite(Boolean isWithWebsite) {
			this.isWithWebsite = isWithWebsite;
		}

		public Boolean getIsCallingAllowed() {
			return isCallingAllowed;
		}

		public void setIsCallingAllowed(Boolean isCallingAllowed) {
			this.isCallingAllowed = isCallingAllowed;
		}

		public void setYesNoSelectItems(List<SelectItem> yesNoSelectItems) {
			this.yesNoSelectItems = yesNoSelectItems;
		}

	}
}
