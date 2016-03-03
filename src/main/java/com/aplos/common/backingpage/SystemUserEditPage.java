package com.aplos.common.backingpage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.beans.Website;
import com.aplos.common.beans.communication.MailServerSettings;
import com.aplos.common.beans.lookups.UserLevel;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.FormatUtil;
import com.aplos.common.utils.JSFUtil;
import com.google.common.io.BaseEncoding;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=SystemUser.class)
public class SystemUserEditPage extends EditPage {
	private static final long serialVersionUID = -2983262147901951318L;
	private List<Website> fullWebsites = new ArrayList<Website>();
	private List<Website> selectedRestrictedWebsites = new ArrayList<Website>();
	private List<Website> selectedAllowedWebsites = new ArrayList<Website>();
	private String newIpAddress;
	private UserLevel selectedAdditionalUserLevel; 
	private String password;
	private String googleSecretKey;
	private boolean isShowingSecondAuthentication;

	public SystemUserEditPage() {
		getBeanDao().setListPageClass( SystemUserListPage.class );
		fullWebsites = new ArrayList<Website>( ApplicationUtil.getAplosContextListener().getWebsiteList() );
	}
	
	public void generateNewSecretKey() {
		int secretSize = 10;
		byte[] buffer = new byte[secretSize];
		new Random().nextBytes(buffer);

		byte[] secretKey = Arrays.copyOf(buffer, secretSize);
		googleSecretKey = BaseEncoding.base32().encode(secretKey);
		SystemUser systemUser = JSFUtil.getBeanFromScope(SystemUser.class);
		systemUser.setGoogleSecretKey(googleSecretKey);
		saveBeanWithWrap();
	}
	
	public void clearSecretKey() {
		SystemUser systemUser = JSFUtil.getBeanFromScope(SystemUser.class);
		systemUser.setGoogleSecretKey(null);
		saveBeanWithWrap();
	}
	
	public String getGoogleSecretKey() {
		return googleSecretKey;
	}

	public SelectItem[] getUserLevelSelectItems() {
		BeanDao userLevelDao = new BeanDao( UserLevel.class );
		userLevelDao.addWhereCriteria( "bean.clearanceLevel >= " + JSFUtil.getLoggedInUser().getUserLevel().getClearanceLevel() );  
		return AplosBean.getSelectItemBeans(userLevelDao.getAll());
	}

	public SelectItem[] getAdditionalUserLevelSelectItems() {
		BeanDao userLevelDao = new BeanDao( UserLevel.class );
		userLevelDao.addWhereCriteria( "bean.clearanceLevel >= " + JSFUtil.getLoggedInUser().getUserLevel().getClearanceLevel() );
		List<UserLevel> additionalUserLevels = userLevelDao.getAll();
		SystemUser systemUser = resolveAssociatedBean();
		additionalUserLevels.remove( systemUser.getUserLevel() );
		for( UserLevel additionalUserLevel : systemUser.getAdditionalUserLevels() ) {
			additionalUserLevels.remove( additionalUserLevel );
		}
		return AplosBean.getSelectItemBeans(additionalUserLevels);
	}
	
	public void usingOwnMailServerSettingsUpdated() {
		SystemUser systemUser = resolveAssociatedBean();
		if( systemUser.getMailServerSettings() == null ) {
			systemUser.setMailServerSettings( new MailServerSettings() );
		}
	}
	
	public void addAdditionalUserLevel() {
		if( getSelectedAdditionalUserLevel() != null ) {
			SystemUser systemUser = resolveAssociatedBean();
			systemUser.getAdditionalUserLevels().add( getSelectedAdditionalUserLevel() );
		} else {
			JSFUtil.addMessage( "Please select an additional user level from the drop down" );
		}
	}
	
	public void removeAdditionalUserLevel() {
		if( getSelectedAdditionalUserLevel() != null ) {
			SystemUser systemUser = resolveAssociatedBean();
			systemUser.getAdditionalUserLevels().remove( getSelectedAdditionalUserLevel() );
		} else {
			JSFUtil.addMessage( "Please select an additional user level from the drop down" );
		}
	}
	
	public List<UserLevel> getAdditionalUserLevelList() {
		SystemUser systemUser = resolveAssociatedBean();
		return new ArrayList<UserLevel>( systemUser.getAdditionalUserLevels() );
	}

	public SelectItem[] getAllowedWebsiteSelectItems() {
		SystemUser systemUser = resolveAssociatedBean();
		return AplosBean.getSelectItemBeans(systemUser.getVisibleWebsites());
	}

	public SelectItem[] getRestrictedWebsiteSelectItems() {
		SystemUser systemUser = resolveAssociatedBean();
		return AplosBean.getSelectItemBeans(getRestrictedWebsites());
	}

	public String addIpAddress() {
		Pattern pattern = Pattern.compile("[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}");
		Matcher matcher = pattern.matcher(getNewIpAddress());
		if( matcher.find() ) {
			SystemUser systemUser = JSFUtil.getBeanFromScope( SystemUser.class );
			systemUser.getAllowedIpAddresses().add(FormatUtil.padIpAddress(matcher.group(0)));
		} else {
			JSFUtil.addMessageForError("Please enter valid text for IP Address in the form 000.000.000.000");
		}
		return null;
	}

	public String removeIpAddress() {
		String ipAddress = (String) JSFUtil.getRequest().getAttribute("tableBean");
		SystemUser systemUser = JSFUtil.getBeanFromScope( SystemUser.class );
		systemUser.getAllowedIpAddresses().remove( ipAddress );
		return null;
	}
	
	@Override
	public boolean saveBean() {
		SystemUser systemUser = resolveAssociatedBean();
		SystemUser duplicateUser = SystemUser.getSystemUserByUsername( systemUser.getUsername() );
		if( duplicateUser != null && !duplicateUser.getId().equals( systemUser.getId() ) ) {
			JSFUtil.addMessageForWarning( "This username is already taken, please try another" );
		} else {
			if( CommonUtil.isNullOrEmpty( getPassword() ) && CommonUtil.isNullOrEmpty( systemUser.getPassword() ) ) {
				JSFUtil.addMessageForWarning( "Please set a password for this user" );
			} else if( !CommonUtil.isNullOrEmpty( getPassword() ) ) {
				systemUser.updatePassword(getPassword());
			}
			return super.saveBean();
		}
		return false;
	}

	private List<Website> getRestrictedWebsites() {
		SystemUser systemUser = resolveAssociatedBean();
		List<Website> restrictedWebsites = new ArrayList<Website>( fullWebsites );
		restrictedWebsites.removeAll(systemUser.getVisibleWebsites());
		return restrictedWebsites;
	}

	public String addWebsiteAccess() {
		if (getSelectedRestrictedWebsites().size() > 0) {
			SystemUser systemUser = resolveAssociatedBean();
			for (Website website : getSelectedRestrictedWebsites()) {
				systemUser.getVisibleWebsites().add(website);
			}
		} else {
			JSFUtil.addMessageForError("No websites have been selected for access");
		}
		return null;
	}

	public String removeWebsiteAccess() {
		if (getSelectedAllowedWebsites().size() > 0) {
			SystemUser systemUser = resolveAssociatedBean();
			for (Website website : getSelectedAllowedWebsites()) {
				systemUser.getVisibleWebsites().remove(website);
			}
		} else {
			JSFUtil.addMessageForError("No websites have been selected for restriction");
		}
		return null;
	}

	public void setSelectedRestrictedWebsites(
			List<Website> selectedRestrictedWebsites) {
		this.selectedRestrictedWebsites = selectedRestrictedWebsites;
	}

	public List<Website> getSelectedRestrictedWebsites() {
		return selectedRestrictedWebsites;
	}

	public void setSelectedAllowedWebsites(List<Website> selectedAllowedWebsites) {
		this.selectedAllowedWebsites = selectedAllowedWebsites;
	}

	public List<Website> getSelectedAllowedWebsites() {
		return selectedAllowedWebsites;
	}

	public String getNewIpAddress() {
		return newIpAddress;
	}

	public void setNewIpAddress(String newIpAddress) {
		this.newIpAddress = newIpAddress;
	}

	public UserLevel getSelectedAdditionalUserLevel() {
		return selectedAdditionalUserLevel;
	}

	public void setSelectedAdditionalUserLevel(
			UserLevel selectedAdditionalUserLevel) {
		this.selectedAdditionalUserLevel = selectedAdditionalUserLevel;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isShowingSecondAuthentication() {
		return isShowingSecondAuthentication;
	}

	public void setShowingSecondAuthentication(boolean isShowingSecondAuthentication) {
		this.isShowingSecondAuthentication = isShowingSecondAuthentication;
	}


}
