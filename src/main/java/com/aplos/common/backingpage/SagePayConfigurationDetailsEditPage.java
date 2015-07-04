package com.aplos.common.backingpage;

import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.beans.cardsave.CardSaveConfigurationDetails;
import com.aplos.common.beans.sagepay.SagePayConfigurationDetails;
import com.aplos.common.utils.CommonUtil;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=SagePayConfigurationDetails.class)
public class SagePayConfigurationDetailsEditPage extends EditPage {
	private static final long serialVersionUID = 6866709793406266979L;

	public List<SelectItem> getConnectToTypeSelectItems() {
		return CommonUtil.getEnumSelectItems( CardSaveConfigurationDetails.ConnectToType.class );
	}
	
}
