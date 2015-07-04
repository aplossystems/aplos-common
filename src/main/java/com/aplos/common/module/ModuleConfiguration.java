package com.aplos.common.module;
import java.util.ArrayList;
import java.util.List;

import com.aplos.common.annotations.PluralDisplayName;
import com.aplos.common.annotations.persistence.MappedSuperclass;
import com.aplos.common.annotations.persistence.Transient;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.communication.MailRecipientFinder;
import com.aplos.common.listeners.AplosContextListener;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@MappedSuperclass
@PluralDisplayName(name="module configurations")
public abstract class ModuleConfiguration extends AplosBean {
	private static final long serialVersionUID = 7768524544403905068L;
	//  Singleton Class

	private int moduleVersionMajor;
	private int moduleVersionMinor;
	private int moduleVersionPatch;

	@Transient
	private List<MailRecipientFinder> mailRecipientFinders = new ArrayList<MailRecipientFinder>();

	public boolean recursiveModuleConfigurationInit(AplosContextListener aplosContextListener, int loopCount ) {
		return false;
	}

	public abstract ModuleConfiguration getModuleConfiguration();

	public static ModuleConfiguration getModuleConfiguration(Class<? extends ModuleConfiguration> moduleConfigurationClass) {
		ModuleConfiguration moduleConfiguration;
		if ( JSFUtil.getRequest() != null ) {
			moduleConfiguration = (ModuleConfiguration) JSFUtil.getBeanFromScope( moduleConfigurationClass );
		} else {
			moduleConfiguration = (ModuleConfiguration) AplosContextListener.getAplosContextListener().getContext().getAttribute( CommonUtil.getBinding( moduleConfigurationClass ) );
//			if( moduleConfiguration != null && HibernateUtil.isSessionFactoryInitialised() && HibernateUtil.getCurrentSession().getTransaction().isActive() && !HibernateUtil.getCurrentSession().contains( moduleConfiguration ) ) {
//				moduleConfiguration = null;
//			}
		}
		if( moduleConfiguration == null ) {
			moduleConfiguration = (ModuleConfiguration) new BeanDao( moduleConfigurationClass ).get( 1 );
			if ( moduleConfiguration == null ) {
				moduleConfiguration = (ModuleConfiguration) CommonUtil.getNewInstance( moduleConfigurationClass, null );
				moduleConfiguration.initialiseNewBean();
				moduleConfiguration.setModuleVersionMajor( moduleConfiguration.getMaximumModuleVersionMajor() );
				moduleConfiguration.setModuleVersionMinor( moduleConfiguration.getMaximumModuleVersionMinor() );
				moduleConfiguration.setModuleVersionPatch( moduleConfiguration.getMaximumModuleVersionPatch() );
				moduleConfiguration.saveDetails();
			}

			if( JSFUtil.getRequest() != null ) {
				moduleConfiguration.addToScope();
			} else {
				AplosContextListener.getAplosContextListener().getContext().setAttribute( CommonUtil.getBinding( moduleConfigurationClass ), moduleConfiguration );
			}
		}
		return moduleConfiguration;
	}

	public int getMaximumModuleVersionMajor() {
		return 1;
	}

	public int getMaximumModuleVersionMinor() {
		return 0;
	}

	public int getMaximumModuleVersionPatch() {
		return 0;
	}

	public boolean isVersionGreaterThanOrEqualTo( int major, int minor, int patch ) {
		if( getModuleVersionMajor() < major ) {
			return false;
		} else if( getModuleVersionMajor() == major ) {
			if( getModuleVersionMinor() < minor ) {
				return false;
			} else if( getModuleVersionMinor() == minor ) {
				if( getModuleVersionPatch() < patch ) {
					return false;
				}
			}
		}
		return true;
	}

	public void setModuleVersionMajor(int moduleVersionMajor) {
		this.moduleVersionMajor = moduleVersionMajor;
	}

	public int getModuleVersionMajor() {
		return moduleVersionMajor;
	}

	public void setModuleVersionMinor(int moduleVersionMinor) {
		this.moduleVersionMinor = moduleVersionMinor;
	}

	public int getModuleVersionMinor() {
		return moduleVersionMinor;
	}

	public void setModuleVersionPatch(int moduleVersionPatch) {
		this.moduleVersionPatch = moduleVersionPatch;
	}

	public int getModuleVersionPatch() {
		return moduleVersionPatch;
	}

	public void setMailRecipientFinders(List<MailRecipientFinder> mailRecipientFinders) {
		this.mailRecipientFinders = mailRecipientFinders;
	}

	public List<MailRecipientFinder> getMailRecipientFinders() {
		return mailRecipientFinders;
	}
}
