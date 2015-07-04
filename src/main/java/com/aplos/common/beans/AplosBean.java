package com.aplos.common.beans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.faces.context.FacesContext;

import com.aplos.common.BackingPageUrl;
import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.FetchType;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.annotations.persistence.MappedSuperclass;
import com.aplos.common.annotations.persistence.Transient;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.BackingPage;
import com.aplos.common.backingpage.EditPage;
import com.aplos.common.backingpage.ListPage;
import com.aplos.common.listeners.AplosContextListener;
import com.aplos.common.module.CommonConfiguration;
import com.aplos.common.persistence.AqlException;
import com.aplos.common.persistence.ArchivableTable;
import com.aplos.common.persistence.PersistentBeanSaver;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.FormatUtil;
import com.aplos.common.utils.JSFUtil;

/**
 * @author dominic.valentyne
 * Parent class for all data beans. Holds auditing
 *         data fields common to all beans.
 */
@MappedSuperclass
public abstract class AplosBean extends AplosAbstractBean {

	private static final long serialVersionUID = -6864099336227263486L;

	private Date dateCreated;
	private Long userIdCreated;
	private Date dateLastModified;
	private Long userIdLastModified;
	private Date dateInactivated;
	private Long userIdInactivated;
	private Long displayId;
	@Column(defaultBoolean=false)
	private boolean isArchived = false;
	@ManyToOne(fetch=FetchType.LAZY)
	private SystemUser owner;

	@Transient
	private final List<AplosTranslation> currentTranslationList = new ArrayList<AplosTranslation>();

	public AplosBean() {}
	
	@Override
	public void clearFieldsAfterCopy() {
		super.clearFieldsAfterCopy();
		setDateCreated(null);
		setUserIdCreated(null);
		setDateLastModified(null);
		setDateInactivated(null);
		setUserIdInactivated(null);
		setDisplayId(null);
	}

	public void copy( AplosAbstractBean srcBean ) {
		super.copy( srcBean );
		AplosBean aplosBean = (AplosBean) srcBean;
		setDateCreated( aplosBean.getDateCreated() );
		setUserIdCreated( aplosBean.getUserIdCreated() );
		setDateLastModified( aplosBean.getDateLastModified() );
		setUserIdLastModified( aplosBean.getUserIdLastModified() );
		setDateInactivated( aplosBean.getDateInactivated() );
		setUserIdInactivated( aplosBean.getUserIdInactivated() );
		setOwner( aplosBean.getOwner() );
	}

	//don't make this static or we can't use it from a view
	public String getActiveLanguage() {
		return CommonUtil.getContextLocale().getLanguage();
	}

	public void redirectToEditPage() {
		AplosBean aplosBean = this.getSaveableBean();
		if( aplosBean.isArchived() ) {
			aplosBean.unarchive();
		}
		aplosBean.addToScope( BackingPage.determineScope( getEditPageClass(), getClass() ) );
		JSFUtil.redirect( getEditPageClass() );
	}

	public BackingPageUrl redirectToEditPage( boolean isRedirectingNow ) {
		AplosBean aplosBean = this.getSaveableBean();
		if( aplosBean.isArchived() ) {
			aplosBean.unarchive();
		}
		aplosBean.addToScope( BackingPage.determineScope( getEditPageClass(), getClass() ) );
		BackingPageUrl backingPageUrl = new BackingPageUrl( getEditPageClass() );
		if( isRedirectingNow ) {
			JSFUtil.redirect( backingPageUrl );
		}
		return backingPageUrl;
	}
	
	public void ajaxRedirectToEditPage() {
		AplosBean loadedAplosBean = new BeanDao( getClass() ).get( getId() );
//		HibernateUtil.initialise( loadedAplosBean, true );
		loadedAplosBean.addToScope( BackingPage.determineScope( getEditPageClass(), getClass() ) );
		JSFUtil.ajaxRedirect( getEditPageClass() );
	}

	public <T extends AplosTranslation> T getAplosTranslation() {
		return getAplosTranslation(CommonUtil.getContextLocale().getLanguage());
	}

	@SuppressWarnings("unchecked")
	public <T extends AplosTranslation> T getAplosTranslation(String translateToLanguage) {
		CommonConfiguration commonConfiguration = CommonConfiguration.getCommonConfiguration();
		// CommonConfiguration is sometimes null if objects are created at startup.
		if( commonConfiguration == null || !commonConfiguration.getIsInternationalizedApplication() || translateToLanguage.equals( commonConfiguration.getDefaultLanguageStr() ) ) {
			return null;
		} else {
			// First check to see if translation object is already in cache list
			for( int i = 0, n = currentTranslationList.size(); i < n; i++ ) {
				if( translateToLanguage.equals( currentTranslationList.get( i ).getLanguageString() ) ) {
					return (T) currentTranslationList.get( i );
				}
			}

			AplosTranslation aplosTranslation = null;
			// If not try to load it from the database
			if( getId() != null ) {
				try {
					BeanDao translationDao = new BeanDao( (Class<? extends AplosAbstractBean>) Class.forName( getTranslationClassName() ) )
						.addWhereCriteria( "untranslatedBean.id = " + getId() )
						.addWhereCriteria( "languageString = :translateToLanguage" );
					translationDao.setNamedParameter( "translateToLanguage", translateToLanguage );
					List<AplosTranslation> translationList = translationDao.getAll();
					if( translationList.size() > 0 ) {
						aplosTranslation = translationList.get( 0 );
						currentTranslationList.add( aplosTranslation );
					}
				} catch( ClassNotFoundException cnfex ) {
					ApplicationUtil.getAplosContextListener().handleError( cnfex );
				}
			}

			// If it's still null then create a new one
			if( aplosTranslation == null ) {
				aplosTranslation = getEmptyAplosTranslation(translateToLanguage);
				currentTranslationList.add( aplosTranslation );
			}

			return (T) aplosTranslation;
		}
	}

	public String getTranslationClassName() {
		StringBuffer fullClassNameBuf = new StringBuffer( getClass().getName() );
		fullClassNameBuf.insert( fullClassNameBuf.lastIndexOf( "." ), ".translations" ).toString();
		return fullClassNameBuf.toString() + "Translation";
	}

	@SuppressWarnings("unchecked")
	public <T extends AplosTranslation> T getEmptyAplosTranslation(String languageString) {
		AplosTranslation aplosTranslation = (AplosTranslation) CommonUtil.getNewInstance(getTranslationClassName(), null);
		aplosTranslation.setUntranslatedBean( this );
		aplosTranslation.setLanguageString(languageString);
		return (T) aplosTranslation;
	}
	
	public void unarchive() {
		String tableName = getPersistentClass().getDbPersistentClass().getTableClass().getSimpleName().toLowerCase();
		ArchivableTable archivableTable = ApplicationUtil.getPersistentApplication().getArchivableTableMap().get( tableName );
		archivableTable.unarchive( this );
	}

	/**
	 * @deprecated see {@link #redirectToEditPage()}
	 */
	public void goToEditPage() {
		//addToTabSession();
		//JSFUtil.redirect( getEditPageClass() );
		redirectToEditPage();
	}

	public void goToListPage() {
		JSFUtil.redirect( getListPageClass() );
	}

	public Class<? extends BackingPage> getEditPageClass() {
		AplosContextListener aplosContextListener = ApplicationUtil.getAplosContextListener();
		if( aplosContextListener != null ) {
			return JSFUtil.getEditPageClass( getClass() );
		} else {
			return null;
		}
	}

	public Class<? extends BackingPage> getListPageClass() {
		AplosContextListener aplosContextListener = ApplicationUtil.getAplosContextListener();
		if( aplosContextListener != null ) {
			return aplosContextListener.getListPageClasses().get( getClass().getSimpleName() );
		} else {
			return null;
		}
	}

	public Date getDateCreated() {
		return dateCreated;
	}
	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}
	public Long getUserIdCreated() {
		return userIdCreated;
	}
	public void setUserIdCreated(Long userIdCreated) {
		this.userIdCreated = userIdCreated;
	}
	public Date getDateLastModified() {
		return dateLastModified;
	}
	public void setDateLastModified(Date dateLastModified) {
		this.dateLastModified = dateLastModified;
	}
	public Long getUserIdLastModified() {
		return userIdLastModified;
	}
	public void setUserIdLastModified(Long userIdLastModified) {
		this.userIdLastModified = userIdLastModified;
	}
	public Date getDateInactivated() {
		return dateInactivated;
	}
	public void setDateInactivated( Date dateInactivated ) {
		this.dateInactivated = dateInactivated;
	}
	public Long getUserIdInactivated() {
		return userIdInactivated;
	}
	public void setUserIdInactivated( Long userIdInactivated ) {
		this.userIdInactivated = userIdInactivated;
	}

	public String getDateCreatedStdStr() {
		if (dateCreated == null) {
			return "---";
		}
		return FormatUtil.formatDate( dateCreated );
	}
	
	public String getDateTimeCreatedStdStr() {
		if (dateCreated == null) {
			return "---";
		}
		return FormatUtil.formatDateTime( dateCreated, true );
	}
	
	public String getDateModifiedStdStr() {
		if (dateLastModified == null) {
			return "---";
		}
		return FormatUtil.formatDate( dateLastModified );
	}
	
	public String getDateTimeModifiedStdStr() {
		if (dateLastModified == null) {
			return "---";
		}
		return FormatUtil.formatDateTime( dateLastModified, true );
	}

	private void setAuditingDetails( SystemUser currentUser ) {
		setDateLastModified( new Date() );
		if( currentUser != null ) {
			setUserIdLastModified( currentUser.getId() );
		}

		if(userIdCreated == null) {
			if( getOwner() == null ) {
				setOwner( currentUser );
			}
			if( currentUser != null ) {
				setUserIdCreated( currentUser.getId() );
			} else {
				/*
				 *  Make sure that the system doesn't set the userIdCreated on the next
				 *  save as the object is already created then. 
				 */
				setUserIdCreated( -1l );
			}
		}
		if( getDateCreated() == null ) {
			setDateCreated( new Date() );
		}
	}
	

	@Override
	public final boolean saveDetails() {
		return saveDetails( JSFUtil.getLoggedInUser() );
	}

	public final boolean saveDetails( SystemUser currentUser ) {
		if( isValidForSave() ) {
			boolean wasNew = isNew();
			boolean saveDetails = preSaveDetails( currentUser );

			if( saveDetails ) {
				saveBean( currentUser );
			
				postSaveDetails( currentUser, wasNew );
			}
			return true;
		}
		return false;
	}
	
	public boolean isValidForSave() {
		if( isReadOnly() ) {
			ApplicationUtil.handleError( new Exception( "Read Only" ), false );
//			JSFUtil.addMessageForWarning( "This bean is read only and cannot be saved" );
		}
		if( isLazilyLoaded() ) {
			throw new AqlException( "Lazily loaded" );
		}
//			return false;
//		} else {
			return true;
//		}
	}
	
	public final boolean preSaveDetails( SystemUser currentUser ) {
		setAuditingDetails( currentUser );
		return true;
	}
	
	public void saveBean( SystemUser currentUser ) {
		PersistentBeanSaver.saveBean( this );
	}
	
	public final void postSaveDetails(  SystemUser currentUser, boolean wasNew ) {
		if( getAfterSaveListener() != null ) {
			getAfterSaveListener().actionPerformed(wasNew);
		}

		generateHashcode();
		for( int i = 0, n = currentTranslationList.size(); i < n; i++ ) {
			currentTranslationList.get( i ).saveDetails();
		}
	}

	public String getEditHeader() {
		if (isNew()) {
			return createFrameHeader("Create New " + getClass().getSimpleName()
					.replaceAll("([a-z])([A-Z])", "$1 $2"), isActive());
		} else {
			return createFrameHeader(getDisplayName(), isActive());
		}
	}

	public String createFrameHeader(String title, boolean active) {
		StringBuffer header = new StringBuffer();
		header.append("<h1 class='frameHeader'>");

		if( isNew() ) {
			header.append("<span class='active'>");
		} else if (!active) {
			header.append("<span class='inactive'>View ");
		} else {
			header.append("<span class='active'>Edit ");
		}

		header.append(title);
		header.append("</span></h1><br />");
		return header.toString();
	}

	public boolean checkForDelete() {
		return true;
	}

	public void delete() {
		AplosBean saveableBean = getSaveableBean();
		saveableBean.setDateInactivated( new Date() );
		SystemUser currentUser = JSFUtil.getLoggedInUser();
		if( currentUser != null ) {
			saveableBean.setUserIdInactivated( JSFUtil.getLoggedInUser().getId() );
		} else {
			saveableBean.setUserIdInactivated( -1l );
		}

		saveableBean.setActive(false);
		saveableBean.saveDetails();
	}

	public void reinstate() {
		reinstate( true );
	}

	public void reinstate( boolean save ) {
		setActive( true );
		if( save ) {
			saveDetails();
		}
	}

	public AplosBean getFromSessionBinding() {
		return (AplosBean) JSFUtil.getBean( getBinding() );
	}

	public EditPage getEditPage() {
		String binding = getBinding() + "EditPage";
		FacesContext context = FacesContext.getCurrentInstance();
		return (EditPage) context.getApplication().evaluateExpressionGet( context, "#{" + binding + "}", BackingPage.class );
	}

	public ListPage getListPage() {
		String binding = getBinding() + "ListPage";
		FacesContext context = FacesContext.getCurrentInstance();
		return (ListPage) context.getApplication().evaluateExpressionGet( context, "#{" + binding + "}", BackingPage.class );
	}

	public SystemUser getOwner() {
		return owner;
	}

	public void setOwner(SystemUser owner) {
		this.owner = owner;
	}

	public void setDisplayId(Long displayId) {
		this.displayId = displayId;
	}

	public Long getDisplayId() {
		return displayId;
	}

	public static List<? extends AplosAbstractBean> sortByDateCreated( List<? extends AplosAbstractBean> lookupBeanList ) {
		return sortByDateCreated(lookupBeanList, false);
	}

	public static List<? extends AplosAbstractBean> sortByDateCreated( List<? extends AplosAbstractBean> lookupBeanList, final Boolean reverseOrder ) {
		Collections.sort( lookupBeanList, new Comparator<AplosAbstractBean>() {
			@Override
			public int compare(AplosAbstractBean aplosBean1, AplosAbstractBean aplosBean2) {
				if ( (aplosBean1 == null || ((AplosBean)aplosBean1).getDateCreated() == null )
						&& (aplosBean2 == null || ((AplosBean)aplosBean2).getDateCreated() == null)) {
					return 0;
				}
				if (aplosBean1 == null || ((AplosBean)aplosBean1).getDateCreated() == null) {
					return ((reverseOrder)?-1:1);
				}
				if (aplosBean2 == null || ((AplosBean)aplosBean2).getDateCreated() == null) {
					return ((reverseOrder)?1:-1);
				}
				if (reverseOrder) {
					return ((AplosBean)aplosBean1).getDateCreated().compareTo( ((AplosBean)aplosBean2).getDateCreated() );
				} else {
					return ((AplosBean)aplosBean2).getDateCreated().compareTo( ((AplosBean)aplosBean1).getDateCreated() );
				}
			}
		});
		return lookupBeanList;
	}
	
	// The enclosed code below is used on the info/quick view popups
	
	public String getInfoDialogCreatedByText() {
		Long userId = getUserIdCreated();
		if (userId != null) {
			BeanDao userDao = new BeanDao(SystemUser.class);
			SystemUser user = userDao.get(userId);
			if (user != null) {
				return user.getDisplayName();
			} else {
				return "Unknown";
			}
		} else {
			return "Not Recorded";
		}
	}
	
	public SystemUser getUserCreated() {
		Long userId = getUserIdCreated();
		if (userId != null) {
			BeanDao userDao = new BeanDao(SystemUser.class);
			SystemUser user = userDao.get(userId);
			return user;
		} else {
			return null;
		}
	}
	
	public String getInfoDialogModifiedByText() {
		Long userId = getUserIdLastModified();
		if (userId != null) {
			BeanDao userDao = new BeanDao(SystemUser.class);
			SystemUser user = userDao.get(userId);
			if (user != null) {
				return user.getDisplayName();
			} else {
				return "Unknown";
			}
		} else {
			return "Not Recorded";
		}
	}
	
	public String getInfoDialogDeletedByText() {
		Long userId = getUserIdInactivated();
		if (userId != null) {
			BeanDao userDao = new BeanDao(SystemUser.class);
			SystemUser user = userDao.get(userId);
			if (user != null) {
				return user.getDisplayName();
			} else {
				return "Unknown";
			}
		} else if (getDateInactivated() == null) {
			return "---";
		} else {
			return "Not Recorded";
		}
	}
	
	public String getInfoDialogDeletedOnText() {
		if (getDateInactivated() == null) {
			return "Never";
		}
		return FormatUtil.formatDate( getDateInactivated() );
	}
	
	public String getInfoDialogEditableYesNoText() {
		if (getEditable()) {
			return "Yes";
		}
		return "No";
	}
	
	public String getInfoDialogDeletableYesNoText() {
		if (getDeletable()) {
			return "Yes";
		}
		return "No";
	}
	
	public String getInfoDialogIdText() {
		if (isNew()) {
			return "New bean, No id assigned yet";
		}
		return String.valueOf(getId());
	}
	
	public String getInfoDialogActiveText() {
		if (isActive()) {
			return "Active";
		}
		return "Deleted";
	}

	public boolean isArchived() {
		return isArchived;
	}

	public void setArchived(boolean isArchived) {
		this.isArchived = isArchived;
	}
	
	// The enclosed code above is used on the info/quick view popups
}
