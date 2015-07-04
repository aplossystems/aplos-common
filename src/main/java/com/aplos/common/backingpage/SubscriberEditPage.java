package com.aplos.common.backingpage;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.beans.Subscriber;
import com.aplos.common.enums.CommonBundleKey;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=Subscriber.class)
public class SubscriberEditPage extends EditPage {

	private static final long serialVersionUID = -8135957599154220601L;

	public SubscriberEditPage() {
		getEditPageConfig().setApplyBtnActionListener(new SaveBtnListener( this ) {

			private static final long serialVersionUID = -6018054862441708059L;

			@Override
			public void actionPerformed( boolean redirect ) {
				saveSubscriber( this, null, redirect );
			}
		});
		getEditPageConfig().setOkBtnActionListener(new OkBtnListener( this ) {

			private static final long serialVersionUID = 7772501598224891886L;

			@Override
			public void actionPerformed( boolean redirect ) {
				saveSubscriber( this, getBackingPage().getBeanDao().getListPageClass(), redirect );
			}
		});
	}

	public void saveSubscriber( AplosEventListener aplosEventListener, Class<? extends BackingPage> returnClass, boolean redirect ) {
		Subscriber subscriber = (Subscriber) aplosEventListener.getBackingPage().resolveAssociatedBean();
		if (CommonUtil.validateEmailAddressFormat(subscriber.getEmailAddress())) {
			if( ApplicationUtil.checkUniqueValue( subscriber, "emailAddress" ) ) {
				subscriber.saveDetails();
				JSFUtil.addMessage(ApplicationUtil.getAplosContextListener().translateByKey( CommonBundleKey.SAVED_SUCCESSFULLY ),FacesMessage.SEVERITY_INFO);
				if( redirect && returnClass != null ) {
					JSFUtil.redirect( returnClass );
				}
			} else {
				JSFUtil.addMessage( "Email address already exists for a subscriber" );
			}
		} else {
			JSFUtil.addMessage( "Email address format is invalid" );
		}
	}

}
