package com.aplos.common.backingpage.communication;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;
import org.primefaces.model.SortOrder;

import com.aplos.common.AplosLazyDataModel;
import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.beans.communication.AplosEmail;
import com.aplos.common.beans.communication.DeletableEmailAddress;
import com.aplos.common.enums.EmailFilterStatus;
import com.aplos.common.enums.EmailType;
import com.aplos.common.utils.CommonUtil;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=AplosEmail.class)
public class IncomingEmailListPage extends AplosEmailListPage {
	private static final long serialVersionUID = 728872837137805406L;
	private DeletableEmailAddress selectedDeletableAddress;
	
	
	@Override
	public AplosLazyDataModel getAplosLazyDataModel(
			DataTableState dataTableState, BeanDao aqlBeanDao) {
		return new IncomingAplosEmailLdm(dataTableState, aqlBeanDao);
	}
	
	public List<SelectItem> getEmailFilterStatusSelectItemBeans() {
		return CommonUtil.getEnumSelectItems(EmailFilterStatus.class);
	}
	
	public List<DeletableEmailAddress> getDeletableEmailAddresses() {
		return new BeanDao( DeletableEmailAddress.class ).getAll();
	}

	@Override
	public String getPageHeading() {
		return "View incoming emails";
	}
	
	public DeletableEmailAddress getSelectedDeletableAddress() {
		return selectedDeletableAddress;
	}

	public void setSelectedDeletableAddress(DeletableEmailAddress selectedDeletableAddress) {
		this.selectedDeletableAddress = selectedDeletableAddress;
	}


	public class IncomingAplosEmailLdm extends AplosEmailLdm {
		private static final long serialVersionUID = 8682282018989288781L;
		private List<EmailFilterStatus> emailFilterStatusesToShow = new ArrayList<EmailFilterStatus>();

		public IncomingAplosEmailLdm(DataTableState dataTableState, BeanDao aqlBeanDao) {
			super(dataTableState, aqlBeanDao);
		}
		
		@Override
		public void addStateFields() throws NoSuchFieldException {
			super.addStateFields();
			addStateField( "emailFilterStatusesToShow" );
		}
		
		@Override
		
		
		public void selectBean() {
			super.selectBean();
			AplosEmail aplosEmail = getAssociatedBeanFromScope();
			if( aplosEmail.getEmailReadDate() == null ) {
				aplosEmail.setEmailReadDate( new Date() );
				aplosEmail.saveDetails();
			}
		}
		
		@Override
		public List<Object> load(int first, int pageSize, String sortField,
				SortOrder sortOrder, Map<String, String> filters) {
			getBeanDao().clearWhereCriteria();
			getBeanDao().addWhereCriteria( "bean.emailType = " + EmailType.INCOMING.ordinal() );
			
			if( getEmailFilterStatusesToShow().size() > 0 ) {
				String[] statusWhereCrierias = new String[ getEmailFilterStatusesToShow().size() ]; 
				for( int i = 0, n = getEmailFilterStatusesToShow().size(); i < n; i++ ) {
					statusWhereCrierias[ i ] = getEmailFilterStatusesToShow().get( i ).getWhereCritieria();
				}
				getBeanDao().addWhereCriteria( StringUtils.join( statusWhereCrierias, " OR " ) );
			}
			return super.load(first, pageSize, sortField, sortOrder, filters, false, true);
		}

		public List<EmailFilterStatus> getEmailFilterStatusesToShow() {
			return emailFilterStatusesToShow;
		}

		public void setEmailFilterStatusesToShow(List<EmailFilterStatus> emailFilterStatusesToShow) {
			this.emailFilterStatusesToShow = emailFilterStatusesToShow;
		}
		
		
	}
}
