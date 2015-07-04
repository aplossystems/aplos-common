package com.aplos.common.backingpage.marketing;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.EditPage;
import com.aplos.common.beans.marketing.PotentialCompanyInteraction;
import com.aplos.common.beans.marketing.PotentialCompanyInteraction.InteractionMethod;
import com.aplos.common.utils.FormatUtil;

@ManagedBean
@ViewScoped
public class InteractionStatisticsPage extends EditPage {
	private static final long serialVersionUID = -255241093432280765L;
	
	private Date startDate;
	private Date finishDate;
	private List<PotentialCompanyInteraction> interactionList;
	
	private Map<InteractionMethod, Integer> interactionMethodCounts = new HashMap<InteractionMethod, Integer>();

	public InteractionStatisticsPage() {
		Calendar cal = new GregorianCalendar();
		cal.setTime( new Date() );
		FormatUtil.resetTime( cal );
		setFinishDate( cal.getTime() );
		
		cal.add( Calendar.DAY_OF_YEAR, -1 );
		setStartDate( cal.getTime() );
	}
	
	@Override
	public boolean responsePageLoad() {
		BeanDao aqlBeanDao = new BeanDao( PotentialCompanyInteraction.class );
		aqlBeanDao.addWhereCriteria( "bean.dateCreated > " + FormatUtil.formatDateForDB( getStartDate(), true ) );
		aqlBeanDao.addWhereCriteria( "bean.dateCreated < " + FormatUtil.formatDateForDB( getFinishDate(), true ) );
		interactionList = aqlBeanDao.getAll();
		for( InteractionMethod interactionMethod : InteractionMethod.values() ) {
			getInteractionMethodCounts().put( interactionMethod, 0 );
		}
		InteractionMethod tempInteractionMethod;
		for( int i = 0, n = interactionList.size(); i < n; i++ ) {
			tempInteractionMethod = interactionList.get( i ).getMethod();
			getInteractionMethodCounts().put( tempInteractionMethod, getInteractionMethodCounts().get( tempInteractionMethod ) + 1 );
		}

		return super.responsePageLoad();
	}
	
	public List<InteractionMethod> getInteractionMethodKeyList() {
		return new ArrayList<InteractionMethod>( interactionMethodCounts.keySet() );
	}
	
	public int getInteractionCount() {
		return interactionList.size();
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getFinishDate() {
		return finishDate;
	}

	public void setFinishDate(Date finishDate) {
		this.finishDate = finishDate;
	}

	public Map<InteractionMethod, Integer> getInteractionMethodCounts() {
		return interactionMethodCounts;
	}

	public void setInteractionMethodCounts(Map<InteractionMethod, Integer> interactionMethodCounts) {
		this.interactionMethodCounts = interactionMethodCounts;
	}
}
