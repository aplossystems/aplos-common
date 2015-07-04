package com.aplos.common.backingpage;

import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.beans.paypal.PayPalConfigurationDetails;
import com.aplos.common.beans.paypal.PayPalConfigurationDetails.PayPalServerType;
import com.aplos.common.utils.CommonUtil;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=PayPalConfigurationDetails.class)
public class PayPalConfigurationDetailsEditPage extends EditPage {
	private static final long serialVersionUID = -8381703986509141748L;

	public List<SelectItem> getPayPalServerTypeSelectItems() {
		return CommonUtil.getEnumSelectItems( PayPalServerType.class );
	}
}
