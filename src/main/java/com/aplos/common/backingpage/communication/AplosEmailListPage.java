package com.aplos.common.backingpage.communication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.faces.model.SelectItem;

import org.primefaces.event.SelectEvent;
import org.primefaces.model.SortOrder;

import com.aplos.common.AplosLazyDataModel;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.ListPage;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.beans.Website;
import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.beans.communication.EmailTemplate;
import com.aplos.common.beans.communication.MailServerSettings;
import com.aplos.common.comparators.SelectItemLabelComparator;
import com.aplos.common.enums.CommonEmailTemplateEnum;
import com.aplos.common.enums.EmailFilterStatus;
import com.aplos.common.enums.EmailStatus;
import com.aplos.common.enums.MessageGenerationType;
import com.aplos.common.interfaces.EmailFolder;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.persistence.ImplicitPolymorphismMatch;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.scheduledjobs.IncomingAplosEmailJob;
import com.aplos.common.templates.emailtemplates.NewsletterEmail;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

public abstract class AplosEmailListPage extends ListPage {
	private static final long serialVersionUID = 7724399000278064732L;
	private EmailFolder selectedEmailFolder;
	private List<SelectItem> emailFolderTypeSelectItems;
	private String selectedEmailFolderType;
	private Class<? extends EmailFolder> selectedEmailFolderClass;
	private List<SelectItem> updateStatusSelectItems;
	private EmailFilterStatus selectedEmailFilterStatus;
	
	@Override
	public boolean responsePageLoad() {
		boolean continueLoad = super.responsePageLoad();
		createEmailFolderTypeSelectItems();
		createUpdateStatusSelectItems();
		return continueLoad;
	}
	
	public boolean isShowingEmailFolderFilter() {
		return true;
	}
	
	public boolean isShowingMailServerSettingsFilter() {
		return true;
	}
	
	public boolean isShowingEmailTemplateFilter() {
		return true;
	}
	
	public void createUpdateStatusSelectItems() {
		setUpdateStatusSelectItems( CommonUtil.getEnumSelectItems(EmailFilterStatus.class) );
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
	
	public SelectItem[] getMailServerSettingsSelectItems() {
		BeanDao mailServerSettingsDao = new BeanDao( MailServerSettings.class );
		mailServerSettingsDao.addWhereCriteria( "bean.emailAddress != null" );
		MailServerSettings errorEmailSettings = CommonConfiguration.getCommonConfiguration().getErrorMailServerSettings();
		if( errorEmailSettings != null ) {
			mailServerSettingsDao.addWhereCriteria( "bean.id != " + errorEmailSettings.getId() );
		}
		List<MailServerSettings> mailServerSettings = mailServerSettingsDao.getAll();
		return AplosBean.getSelectItemBeansWithNotSelected( mailServerSettings );
	}
	
	public void createEmailFolderTypeSelectItems() {
		ImplicitPolymorphismMatch implicitPolymorphismMatch = ApplicationUtil.getPersistentApplication().getDynamicMetaValuesMap().get( EmailFolder.class );
		List<SelectItem> selectItems = new ArrayList<SelectItem>( implicitPolymorphismMatch.getPersistentClasses().size() );
		int count = 0;
		String tempPluralDisplayName;
		for( PersistentClass tempPersistentClass : implicitPolymorphismMatch.getPersistentClasses() ) {
			tempPluralDisplayName =  AplosAbstractBean.getPluralDisplayName( (Class<? extends AplosAbstractBean>) tempPersistentClass.getTableClass() );
			tempPluralDisplayName = CommonUtil.firstLetterToUpperCase( tempPluralDisplayName );
			selectItems.add( new SelectItem( tempPersistentClass.getTableClass().getName(), tempPluralDisplayName ) );  
		}
		if( selectItems.size() > 0 && getSelectedEmailFolderType() == null ) {
			setSelectedEmailFolderType( (String) selectItems.get( 0 ).getValue() );
			selectedEmailFolderTypeUpdated();
		}
		Collections.sort( selectItems, new SelectItemLabelComparator() );
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

	public void goToNewNewsletter() {
		AplosEmail aplosEmail = new AplosEmail().initialiseNewBean();
		aplosEmail.setEmailGenerationType( MessageGenerationType.MESSAGE_GROUPS );
		NewsletterEmail emailTemplate = Website.getCurrentWebsiteFromTabSession().loadEmailTemplate(CommonEmailTemplateEnum.NEWSLETTER, true);
		aplosEmail.updateEmailTemplate(emailTemplate);
		aplosEmail.addToScope();
		JSFUtil.redirect( AplosEmailEditPage.class );
	}
	
	public void updateSelected() {
		Object aplosEmailObjs[] = getDataTableState().getLazyDataModel().getSelectedRows();
		if( aplosEmailObjs != null ) {
			for( int i = 0, n = aplosEmailObjs.length; i < n; i++ ) {
				AplosEmail saveableBean = ((AplosEmail)aplosEmailObjs[ i ]).getSaveableBean();
				if( EmailFilterStatus.UNREAD.equals( getSelectedEmailFilterStatus() ) ) {
					saveableBean.setEmailReadDate( null );
					saveableBean.saveDetails();
				} else if( EmailFilterStatus.READ.equals( getSelectedEmailFilterStatus() ) ) {
					saveableBean.setEmailReadDate( new Date() );
					saveableBean.saveDetails();
				} else if( EmailFilterStatus.FLAGGED.equals( getSelectedEmailFilterStatus() ) ) {
					saveableBean.setEmailStatus( EmailStatus.FLAGGED );
					saveableBean.saveDetails();
				} else if( EmailFilterStatus.UNFLAGGED.equals( getSelectedEmailFilterStatus() ) ) {
					saveableBean.setEmailStatus( EmailStatus.UNFLAGGED );
					saveableBean.saveDetails();
				} else if( EmailFilterStatus.COMPLETE.equals( getSelectedEmailFilterStatus() ) ) {
					saveableBean.setEmailStatus( EmailStatus.COMPLETE );
					saveableBean.saveDetails();
				}
			}
		}
	}
	
	public boolean isShowingNewNewsletterBtn() {
		return true;
	}
	
	public void refreshEmailList() {
		refreshEmailListStatic();
	}
	
	public static void refreshEmailListStatic() {
		/*
		 * This used to hold until the mail was downloaded, however it cannot now work
		 * as the request is put into a queue so that emails are deleted before a new request
		 * is made.  I could have written code here to make sure that this compeletes first 
		 * but I think it will be better to write code to show the the user the progress of 
		 * the thread and use a push architecture to reload the page once the thread is complete.
		 */
		Future<?> future = ApplicationUtil.getJobScheduler().runScheduledJob( IncomingAplosEmailJob.class );
		try {
			future.get();
		} catch( ExecutionException exEx ) {
			ApplicationUtil.handleError( exEx );
		} catch( InterruptedException interEx ) {
			ApplicationUtil.handleError( interEx );
		}
	}
	
	public SelectItem[] getEmailTemplateSelectItems() {
		return AplosBean.getSelectItemBeansWithNotSelected(EmailTemplate.class);
	}
	
	@Override
	public AplosLazyDataModel getAplosLazyDataModel(
			DataTableState dataTableState, BeanDao aqlBeanDao) {
		return new AplosEmailLdm(dataTableState, aqlBeanDao);
	}

	public EmailFolder getSelectedEmailFolder() {
		return selectedEmailFolder;
	}

	public void setSelectedEmailFolder(EmailFolder selectedEmailFolder) {
		this.selectedEmailFolder = selectedEmailFolder;
	}

	public List<SelectItem> getEmailFolderTypeSelectItems() {
		return emailFolderTypeSelectItems;
	}

	public void setEmailFolderTypeSelectItems(
			List<SelectItem> emailFolderTypeSelectItems) {
		this.emailFolderTypeSelectItems = emailFolderTypeSelectItems;
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

	public List<SelectItem> getUpdateStatusSelectItems() {
		return updateStatusSelectItems;
	}

	public void setUpdateStatusSelectItems(List<SelectItem> updateStatusSelectItems) {
		this.updateStatusSelectItems = updateStatusSelectItems;
	}

	public EmailFilterStatus getSelectedEmailFilterStatus() {
		return selectedEmailFilterStatus;
	}

	public void setSelectedEmailFilterStatus(EmailFilterStatus selectedEmailFilterStatus) {
		this.selectedEmailFilterStatus = selectedEmailFilterStatus;
	}

	public class AplosEmailLdm extends AplosLazyDataModel {
		private static final long serialVersionUID = 7229178100132302261L;
		private EmailTemplate emailTemplate;
		private List<EmailTemplate> otherEmailTemplates = new ArrayList<EmailTemplate>();
		private Set<EmailFolder> emailFolders = new HashSet<EmailFolder>();
		private MailServerSettings mailServerSettings;

		public AplosEmailLdm(DataTableState dataTableState, BeanDao beanDao) {
			super(dataTableState, beanDao);
			beanDao.setSelectCriteria( "bean.id" );
			beanDao.setEditPageClass(AplosEmailEditPage.class);
//			setMailServerSettings(JSFUtil.determineMailServerSettings());
		}
		
		@Override
		public void addStateFields() throws NoSuchFieldException {
			addStateField( "emailFolders" );
			addStateField( "emailTemplate" );
			addStateField( "mailServerSettings" );
		}
		
		public void deleteSelectedEmails() {
			Object aplosEmailObjs[] = getSelectedRows();
			if( aplosEmailObjs != null ) {
				for( int i = 0, n = aplosEmailObjs.length; i < n; i++ ) {
					AplosBean loadedBean = (AplosBean) new BeanDao( AplosEmail.class ).get( ((AplosEmail)aplosEmailObjs[ i ]).getId() );
					if (loadedBean != null && loadedBean.checkForDelete() && loadedBean.getDeletable() && loadedBean.getEditable()) {
						setDeleteBean( loadedBean );
						if( isHardDelete() || getDataTableState().isShowingDeleted() ) {
							callBeanHardDelete(loadedBean);
						} else {
							loadedBean.delete();
						}
					} else {
						JSFUtil.addMessageForError("This item cannot be deleted, it is uneditable or undeleteable.");
					}	
				}
			} else {
				JSFUtil.addMessageForError("Please select items from the list by clicking the check box");
			}
		}
		
		public String getRowStyle() {
			AplosEmail aplosEmail = (AplosEmail) getAplosBean();
			if( aplosEmail != null ) {
				if( EmailStatus.FLAGGED.equals( aplosEmail.getEmailStatus() ) ) {
					return "background-color:#FFBBBB";
				}
			}
			return "";
		}
		
		@Override
		public List<Object> load(int first, int pageSize, String sortField,
				SortOrder sortOrder, Map<String, String> filters) {
			return load(first, pageSize, sortField, sortOrder, filters, true, true);
		}
		
		@Override
		public void goToNew() {
			super.goToNew();
			AplosEmail aplosEmail = getAssociatedBeanFromScope();
			aplosEmail.updateEmailTemplate( CommonEmailTemplateEnum.GENERAL_EMAIL );
		}
		
		public List<Object> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters, boolean clearWhereCriteria, boolean clearSelectJoin ) {
			if( clearWhereCriteria ) {
				getBeanDao().clearWhereCriteria();
			}
			if( getMailServerSettings() != null ) {
				getBeanDao().addWhereCriteria( "bean.mailServerSettings.id = " + getMailServerSettings().getId() );
			}
			if( getEmailTemplate() != null ) {
				StringBuffer emailTemplateCondition = new StringBuffer( "bean.emailTemplate.id = " ).append( getEmailTemplate().getId() );
				for( int i = 0, n = getOtherEmailTemplates().size(); i < n; i++ ) {
					emailTemplateCondition.append( " OR bean.emailTemplate.id = " ).append( getOtherEmailTemplates().get( i ).getId() );
				}
				getBeanDao().addWhereCriteria( emailTemplateCondition.toString() );
			}
			if( clearSelectJoin ) {
//				getBeanDao().setSelectJoin( "" );
			}
			if( getEmailFolders().size() > 0 ) {
				getBeanDao().addQueryTable( "efl", "bean.emailFolders");
				StringBuffer folderWhereCriteriaBuf = new StringBuffer();
				for( EmailFolder tempEmailFolder : getEmailFolders() ) {
					if( folderWhereCriteriaBuf.length() > 0 ) {
						folderWhereCriteriaBuf.append( " OR " );
					}
					folderWhereCriteriaBuf.append( "(efl.id = " );
					folderWhereCriteriaBuf.append( tempEmailFolder.getId() );
					folderWhereCriteriaBuf.append( " AND efl.class = '" );
					folderWhereCriteriaBuf.append( tempEmailFolder.getClass().getSimpleName() );
					folderWhereCriteriaBuf.append( "')" );
				}
				getBeanDao().addWhereCriteria( folderWhereCriteriaBuf.toString() );
			}
			if( filters.containsKey( "toAddress:toAddress" ) || (sortField != null && sortField.contains( "'toAddress:toAddress'" ) ) ) {
				getBeanDao().addQueryTable( "toAddress", "bean.toAddresses");
			}
			return super.load(first, pageSize, sortField, sortOrder, filters);
		}
		
		public void addEmailFolder( SelectEvent event ) {
			EmailFolder emailFolder = (EmailFolder) event.getObject();
			if( emailFolder != null ) {
				getEmailFolders().add( emailFolder );	
			} else {
				JSFUtil.addMessage( "Please select an email folder" );
			}
		}
		
		public void removeEmailFolder() {
			EmailFolder emailFolder = (EmailFolder) JSFUtil.getRequest().getAttribute( "selectedSource" );
			getEmailFolders().remove( emailFolder );
		}
		
		public List<EmailFolder> getEmailFolderList() {
			return new ArrayList<EmailFolder>( getEmailFolders() );
		}

		public Set<EmailFolder> getEmailFolders() {
			return emailFolders;
		}

		public void setEmailFolders(Set<EmailFolder> emailFolders) {
			this.emailFolders = emailFolders;
		}
		
		public EmailTemplate getEmailTemplate() {
			return emailTemplate;
		}

		public void setEmailTemplate(EmailTemplate emailTemplate) {
			this.emailTemplate = emailTemplate;
		}

		public MailServerSettings getMailServerSettings() {
			return mailServerSettings;
		}

		public void setMailServerSettings(MailServerSettings mailServerSettings) {
			this.mailServerSettings = mailServerSettings;
		}

		public List<EmailTemplate> getOtherEmailTemplates() {
			return otherEmailTemplates;
		}

		public void setOtherEmailTemplates(List<EmailTemplate> otherEmailTemplates) {
			this.otherEmailTemplates = otherEmailTemplates;
		}
	}
}
