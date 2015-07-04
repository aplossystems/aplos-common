package com.aplos.common;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.aql.ProcessedBeanDao;
import com.aplos.common.beans.AplosSiteBean;
import com.aplos.common.beans.Website;
import com.aplos.common.listeners.AplosContextListener;
import com.aplos.common.utils.ApplicationUtil;

public class SiteBeanDao extends BeanDao {
	private static final long serialVersionUID = -5815685975930841016L;

	private Long websiteId;

	// Special case for startup where FacesContext is not yet available
	public SiteBeanDao( AplosContextListener aplosContextListener, Website website,
			Class<? extends AplosSiteBean> beanClass) {
		super( beanClass );
		if( !aplosContextListener.isAplosSiteBeanDisabled( beanClass ) ) {
			if( website != null ) {
				this.setWebsiteId(website.getId());
			}
		}
	}

	public SiteBeanDao(Website website, Class<? extends AplosSiteBean> beanClass) {
		super( beanClass );
		if( !ApplicationUtil.getAplosContextListener().isAplosSiteBeanDisabled( beanClass ) ) {
			if( website != null ) {
				this.setWebsiteId(website.getId());
			}
		}
	}

	public SiteBeanDao(Class<? extends AplosSiteBean> beanClass) {
		super( beanClass );
		if( !ApplicationUtil.getAplosContextListener().isAplosSiteBeanDisabled( beanClass ) ) {
			if( Website.getCurrentWebsiteFromTabSession() != null ) {
				setWebsiteId(Website.getCurrentWebsiteFromTabSession().getId());
			}
		}
	}
	
	@Override
	public void preCriteriaEvaluation(ProcessedBeanDao processedBeanDao) {
		super.preCriteriaEvaluation(processedBeanDao);
		processWhereCriteria( "bean.parentWebsite.id = " + getWebsiteId(), true, processedBeanDao.getProcessedWhereConditionGroup() );
	}
	public Long getWebsiteId() {
		return websiteId;
	}

	public void setWebsiteId(Long websiteId) {
		this.websiteId = websiteId;
	}
}
