package com.aplos.common.backingpage;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.BeanMenuHelper;
import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.BasicContact;
import com.aplos.common.beans.BasicContactTag;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=BasicContact.class)
public class BasicContactEditPage extends EditPage {
	private static final long serialVersionUID = 4112898492425670546L;
	private BasicContactTag selectedBasicContactTag;
	
	public boolean saveBean() {
		BasicContact basicContact = resolveAssociatedBean();
		if( !CommonUtil.isNullOrEmpty( basicContact.getEmailAddress() ) ) {
			BeanDao basicContactDao = new BeanDao(BasicContact.class);
			basicContactDao.addWhereCriteria( "bean.address.subscriber.emailAddress LIKE :emailAddress" );
			if( !basicContact.isNew() ) {
				basicContactDao.addWhereCriteria( "bean.id != " + basicContact.getId() );
			}
			basicContactDao.setNamedParameter( "emailAddress", basicContact.getEmailAddress() );
			if( basicContactDao.getCountAll() > 0 ) {
				JSFUtil.addMessageForWarning( "The email address entered is already in use in another basic contact please enter another email address" );
				return false;
			}
		}
		return super.saveBean();
	}
	
	public BeanMenuHelper getBasicContactTagBmh() {
		BeanMenuHelper beanMenuHelper = new BeanMenuHelper( BasicContactTag.class );
		beanMenuHelper.setWithNotSelected(false);
		return beanMenuHelper;
	}
	
	public void addBasicContactTag() {
		BasicContact basicContact = resolveAssociatedBean();
		basicContact.getBasicContactTags().add( getSelectedBasicContactTag() );
		basicContact.saveDetails();
	}
	
	public void removeBasicContactTag() {
		BasicContact basicContact = resolveAssociatedBean();
		BasicContactTag basicContactTag = JSFUtil.getBeanFromRequest( "tableBean" );
		basicContact.getBasicContactTags().remove( basicContactTag );
		basicContact.saveDetails();
	}

	public BasicContactTag getSelectedBasicContactTag() {
		return selectedBasicContactTag;
	}

	public void setSelectedBasicContactTag(BasicContactTag selectedBasicContactTag) {
		this.selectedBasicContactTag = selectedBasicContactTag;
	};
	
}