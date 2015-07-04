package com.aplos.common.backingpage;

import com.aplos.common.TrailDisplayName;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.enums.JsfScope;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.JSFUtil;

public abstract class EditPage extends BackingPage {
	private static final long serialVersionUID = 1702263467428428442L;
	private EditPageConfig editPageConfig = new EditPageConfig();

	public EditPage() {
		super();
		if( getBeanDao() != null ) {
			init( getBeanDao() );
		}
	}
	
	public boolean requestPageLoad() {
		if( JSFUtil.isPostBack() ) {
			Long currentAssociatedBeanId = null;
			if( resolveAssociatedBean() != null ) {
				if( !JSFUtil.isAjaxRequest() && JSFUtil.getRequest().getParameterMap().containsKey( "associatedBeanId" ) ) {
					currentAssociatedBeanId = resolveAssociatedBean().getId();
					if( getAssociatedBeanId() != null && !getAssociatedBeanId().equals( getAlternateAssociatedBeanId() ) ) {
						ApplicationUtil.handleError( new Exception( "Bean id " + getAssociatedBeanId() + " and alternate bean id " + getAlternateAssociatedBeanId() + " do not match" ), false );
					}
					if( (currentAssociatedBeanId != null && (getAssociatedBeanId() == null || getAssociatedBeanId() < -1))
						|| (currentAssociatedBeanId != null && !currentAssociatedBeanId.equals( getAssociatedBeanId() )) ) {
						JSFUtil.addMessageForError( "Any changes from the last page have not been saved.");
						String associatedBeanClass = "object";
						if( getBeanDao().getBeanClass() != null ) {
							associatedBeanClass = getBeanDao().getBeanClass().getSimpleName();
						}
						StringBuffer errorMessage = new StringBuffer( "Expected " );
						errorMessage.append( associatedBeanClass ).append( " with id " );
						errorMessage.append( getAssociatedBeanId() ).append( " but found " );
						errorMessage.append( associatedBeanClass ).append( " with id " );
						errorMessage.append( currentAssociatedBeanId );
						ApplicationUtil.handleError( new Exception( errorMessage.toString() ), false );
						JSFUtil.redirect( MultipleTabWarningPage.class );
					}
				}
			}
		}
		return true;
	}
	
	@Override
	public boolean responsePageLoad() {
		/*
		 * The minus numbers here are just to test why sometimes this 
		 * mechanism doesn't seem to work and sends null in the hidden field when
		 * a bean is present.
		 */
		if( resolveAssociatedBean() != null ) {
			if( resolveAssociatedBean().getId() != null ) {
				setAssociatedBeanId( resolveAssociatedBean().getId() );
			} else {
				setAssociatedBeanId( -2l );
			}
		} else {
			setAssociatedBeanId( -1l );
		}

		AplosAbstractBean alternateBean = null;
		if( getBeanDao() != null ) {
			alternateBean = (AplosAbstractBean) JSFUtil.resolveVariable( getBeanDao().getBeanClass() );
		}
		if( alternateBean != null ) {
			if( alternateBean.getId() != null ) {
				setAlternateAssociatedBeanId( alternateBean.getId() );
			} else {
				setAlternateAssociatedBeanId( -2l );
			}
		} else {
			setAlternateAssociatedBeanId( -1l );
		}
		return super.responsePageLoad();
	}
	
	public void checkEditBean() {
		if( getBeanDao() != null && (resolveAssociatedEditBean() == null || resolveAssociatedEditBean().isReadOnly()) ) {
			AplosAbstractBean aplosAbstractBean = JSFUtil.getBeanFromScope( getBeanDao().getBeanClass() );
			if( aplosAbstractBean == null ) {
				/*
				 * The bean scope may be different to edit bean scope (currently FLASH_VIEW_CURRENT)
				 */
				aplosAbstractBean = resolveAssociatedEditBean();
			}
			if( aplosAbstractBean != null ) {
				aplosAbstractBean.getSaveableBean().addToScope( JsfScope.FLASH_VIEW_CURRENT );
			}
		}
	}

	@SuppressWarnings("unchecked")
	public <T extends AplosBean> T resolveAssociatedEditBean() {
		if (getBeanDao() != null) {
			return (T) JSFUtil.getBeanFromScope( getBeanDao().getBeanClass(), JsfScope.FLASH_VIEW_CURRENT );
		} else {
			return null;
		}
	}
	
	public boolean isShowingEditActionButtons() {
		return true;
	}
	
	public void init( BeanDao aqlBeanDao ) {
		setBeanDao( aqlBeanDao );
		if( aqlBeanDao != null ) {
			addRequiredStateBinding( aqlBeanDao.getBeanClass() );
		}
		setAssociatedBeanScope( determineScope( getClass(), getBeanDao().getBeanClass() ) );
		getEditPageConfig().setApplyBtnActionListener( new SaveBtnListener( this ) );
		getEditPageConfig().setOkBtnActionListener( new OkBtnListener( this ) );
		getEditPageConfig().setCancelBtnActionListener( new CancelBtnListener( this ) );
	}

	@Override
	public String getPageTitle() {
		return "Aplos CMS - " + getPageHeading();
	}

	@Override
	public TrailDisplayName determineTrailDisplayName(String requestUrl) {
		TrailDisplayName trailDisplayName = null;

		AplosBean associatedBean = this.resolveAssociatedBean();
		if (associatedBean != null) {
			//We only display this bean name if the edit page we have is the default for the
			//panel we are on, otherwise (for example) overview page would show bean name instead
			//of overview (and then we would have two crumbs in a row with the bean name)
			if (associatedBean.getEditPage() == null || associatedBean.getEditPage().getClass().isAssignableFrom(getClass())) {
				if (!associatedBean.isNew()){
					//Naming Priority: Bean Instance Trail Display Name (override, if not new)
					if (associatedBean.getTrailDisplayName() != null && !associatedBean.getTrailDisplayName().equals("")) {
						trailDisplayName = new TrailDisplayName(associatedBean);
						return trailDisplayName;
					}
				} else {
					//Naming Priority: New <Bean Type> (if new)
					trailDisplayName = new TrailDisplayName();
					trailDisplayName.setDynamicBundleKey( associatedBean.getClass().getName().replace( "com.aplos.", "" ) );
					return trailDisplayName;
				}
			}
		}

		if( trailDisplayName == null ) {
			trailDisplayName = getTrailDisplayNameFromMenuTab( requestUrl );
		}

		if( trailDisplayName == null ) {
			trailDisplayName = getTrailDisplayNameFromClassName();
		}

		return trailDisplayName;
	}

	@Override
	public String getPageHeading() {
		AplosBean associatedBean = resolveAssociatedBean();
		String pageHeading = null;
		if (associatedBean != null && associatedBean.isNew() ) {
			pageHeading = "Create new " + getBeanClassDisplayName();
		} else if (associatedBean != null) {
			pageHeading = "Edit " + getBeanClassDisplayName();
			if( !associatedBean.getEntityName().equals( associatedBean.getDisplayName() ) ) {
				pageHeading += " - " + associatedBean.getDisplayName();
			}

			pageHeading += " (" + associatedBean.getId() + ")";
		} else {
			if( JSFUtil.getNavigationStack().size() > 0 ) {
				pageHeading = JSFUtil.getNavigationStack().get( JSFUtil.getNavigationStack().size() - 1 ).getTrailDisplayName().determineName();
			} else {
				pageHeading = "";
			}
		}

		return pageHeading;
	}

	@Override
	public Class<? extends BackingPage> getDefaultPreviousPageClass() {
		return getBeanDao().getListPageClass();
	}

	public void okBtnAction() {
		getEditPageConfig().okBtnAction( true );
	}

	public void cancelBtnAction() {
		getEditPageConfig().cancelBtnAction( true );
	}

	public void applyBtnAction() {
		getEditPageConfig().applyBtnAction( false );
	}

	public void setEditPageConfig(EditPageConfig editPageConfig) {
		this.editPageConfig = editPageConfig;
	}
	public EditPageConfig getEditPageConfig() {
		return editPageConfig;
	}

	public String deleteBean() {
		AplosBean deleteBean = resolveAssociatedBean();

		if ( deleteBean.checkForDelete() ) {
			deleteBean.delete();
		}
		return null;
	}

	public void redirectToPage( AplosBean aplosBean ) {
		aplosBean.addToScope( BackingPage.determineScope( getClass(), aplosBean.getClass() ) );
		JSFUtil.redirect( getClass() );
	}

	public String reinstateBean() {
		resolveAssociatedBean().reinstate();
		return null;
	}

	public String getBeanClassDisplayName() {
		Class<? extends AplosAbstractBean> beanClass = null;
		if (getBeanDao() != null) {
			beanClass = getBeanDao().getBeanClass();
		} else if ( resolveAssociatedBean() != null ) {
			beanClass = resolveAssociatedBean().getClass() ;
		} 
		
		if( beanClass != null ) {
			return ApplicationUtil.getAplosContextListener().translate( beanClass.getName().replace( "com.aplos.", "" ) );
		} else {
			return "";
		}
	}
}
