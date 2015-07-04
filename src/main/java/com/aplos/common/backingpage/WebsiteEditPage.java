package com.aplos.common.backingpage;

import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import com.aplos.common.ScheduledJob;
import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.VatType;
import com.aplos.common.beans.Website;
import com.aplos.common.enums.CombinedResourceStatus;
import com.aplos.common.enums.SslProtocolEnum;
import com.aplos.common.threads.AplosThread;
import com.aplos.common.threads.JobScheduler;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=Website.class)
public class WebsiteEditPage extends EditPage {

	private static final long serialVersionUID = 5460450499505410630L;

	public WebsiteEditPage() {
		getEditPageConfig().setOkBtnActionListener( new OkBtnListener( this ) {

			private static final long serialVersionUID = -8150788281650865165L;

			@Override
			public void actionPerformed(boolean redirect) {
				Website website = getBackingPage().resolveAssociatedBean();
				boolean wasNew = website.isNew();
				website.saveDetails();
				if( wasNew ) {
					website.createDefaultWebsiteObjects(ApplicationUtil.getAplosContextListener());
				}
				ApplicationUtil.startNewTransaction();
				//return getBackingPage().getBeanDao().getListPageNavigation();
				JSFUtil.redirect("common", SiteStructurePage.class);
			}
		});
		getEditPageConfig().setCancelBtnActionListener( new CancelBtnListener( this ) {

			private static final long serialVersionUID = 6705986682954954696L;

			@Override
			public void actionPerformed(boolean redirect) {
				JSFUtil.redirect("common", SiteStructurePage.class);
			}
		});
		getRequiredStateBindings().clear();
		if( resolveAssociatedBean() == null ) {
			Website.getCurrentWebsiteFromTabSession().addToScope();
		}
		getRequiredStateBindings().add( CommonUtil.getBinding( Website.class ) );

	}
	
	public List<SelectItem> getCombinedResourceStatusSelectItems() {
		return CommonUtil.getEnumSelectItems( CombinedResourceStatus.class );
	}
	
	public String getPauseResumeBtnText() {
		ScheduledJob scheduledJob = JSFUtil.getBeanFromRequest( "scheduledJob" );
		if( scheduledJob.isRunning() ) {
			return "Pause";
		} else {
			return "Resume";
		}
	}
	
	public void toggleThreadRun() {
		ScheduledJob scheduledJob = JSFUtil.getBeanFromRequest( "scheduledJob" );
		if( scheduledJob.isRunning() ) {
			scheduledJob.setRunning( false );
		} else {
			scheduledJob.setRunning( true );
		}
	}
	
	public List<SelectItem> getSslProtocolSelectItems() {
		return CommonUtil.getEnumSelectItems( SslProtocolEnum.class );
	}

	public SelectItem[] getVatTypeSelectItems() {
		return AplosBean.getSelectItemBeans( VatType.class );
	}
	
	public List<AplosThread> getAplosThreadList() {
		return new ArrayList<AplosThread>(ApplicationUtil.getAplosContextListener().getAplosThreads());
	}
	
	public List<ScheduledJob<?>> getScheduledJobs() {
		return ApplicationUtil.getJobScheduler().getScheduledJobs();
	}
	
	public JobScheduler getJobScheduler() {
		return ApplicationUtil.getJobScheduler();
	}
	
	public void executeScheduledJob() {
		ScheduledJob<?> scheduledJob = (ScheduledJob<?>) JSFUtil.getRequest().getAttribute( "scheduledJob" );
		ApplicationUtil.getJobScheduler().runScheduledJob( scheduledJob.getClass() );
	}

	@Override
	public boolean responsePageLoad() {
		super.responsePageLoad();
		return true;
	}
}
