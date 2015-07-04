package com.aplos.common.beans;

import com.aplos.common.annotations.DynamicMetaValues;
import com.aplos.common.annotations.PluralDisplayName;
import com.aplos.common.annotations.persistence.Any;
import com.aplos.common.annotations.persistence.AnyMetaDef;
import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.JoinColumn;
import com.aplos.common.annotations.persistence.MappedSuperclass;
import com.aplos.common.utils.CommonUtil;

@MappedSuperclass
@PluralDisplayName(name="translations")
public abstract class AplosTranslation extends AplosBean {

	private static final long serialVersionUID = -2285913008242466075L;

	@Any( metaColumn = @Column( name = "aplos_bean_type" ) )
    @AnyMetaDef( idType = "long", metaType = "string", metaValues = {
    })
    @JoinColumn(name="aplos_bean_id")
	@DynamicMetaValues
	private AplosBean untranslatedBean;

	private String languageString = "en";

	public AplosTranslation() {}

	public AplosTranslation(AplosBean untranslatedBean) {
		this.untranslatedBean=untranslatedBean;
		languageString = CommonUtil.getContextLocale().getLanguage();
	}

	public void setUntranslatedBean(AplosBean untranslatedBean) {
		this.untranslatedBean = untranslatedBean;
	}

	public AplosBean getUntranslatedBean() {
		return untranslatedBean;
	}

	public void setLanguageString(String languageString) {
		this.languageString = languageString;
	}

	public String getLanguageString() {
		return languageString;
	}


}






