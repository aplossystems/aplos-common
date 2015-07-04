package com.aplos.common.backingpage;

import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.beans.Country;
import com.aplos.common.enums.VatExemption;
import com.aplos.common.utils.CommonUtil;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=Country.class)
public class CountryEditPage extends EditPage {
	private static final long serialVersionUID = 565982483406278738L;

	public List<SelectItem> getVatExemptionSelectItems() {
		return CommonUtil.getEnumSelectItems( VatExemption.class, "Not Selected" );
	}
}
