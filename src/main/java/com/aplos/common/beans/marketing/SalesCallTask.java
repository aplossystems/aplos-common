package com.aplos.common.beans.marketing;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.aplos.common.annotations.persistence.CollectionOfElements;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.ManyToMany;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.backingpage.marketing.SalesCallTaskListPage;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.beans.SystemUser;
import com.aplos.common.enums.PotentialCompanyStatus;
import com.aplos.common.utils.JSFUtil;

@Entity
public class SalesCallTask extends AplosBean {
	private static final long serialVersionUID = -9019051913597742399L;

	private Date startedDate;
	private Date completedDate;
	private Date cancelledDate;
	@ManyToOne
	private SystemUser systemUser;
	@ManyToMany
	private List<PotentialCompany> unfinishedPotentialCompanyList;
	@ManyToMany
	private List<PotentialCompany> finishedPotentialCompanyList;
	private int currentIdx;
	private String name;
	@CollectionOfElements
	private List<PotentialCompanyStatus> exitStatuses;
	
	@Override
	public <T> T initialiseNewBean() {
		loadDefaultExitStrategies();
		return super.initialiseNewBean();
	}
	
	public void loadDefaultExitStrategies() {
		exitStatuses = new ArrayList<PotentialCompanyStatus>();
		exitStatuses.add( PotentialCompanyStatus.COMPANY_CLOSED );
		exitStatuses.add( PotentialCompanyStatus.CONVERTED );
		exitStatuses.add( PotentialCompanyStatus.NOT_APPLICABLE );
		exitStatuses.add( PotentialCompanyStatus.NOT_CURRENTLY_APPLICABLE );
		exitStatuses.add( PotentialCompanyStatus.NOT_INTERESTED );
		exitStatuses.add( PotentialCompanyStatus.ON_SYSTEM );
		exitStatuses.add( PotentialCompanyStatus.PHONE_NUMBER_NOT_WORKING );
		exitStatuses.add( PotentialCompanyStatus.NOT_RESPONDING );
	}
	
	public void reassessLists() { 
		for( int i = unfinishedPotentialCompanyList.size() - 1; i > -1; i-- ) {
			if( isCompleted( unfinishedPotentialCompanyList.get( i ) ) ) {
				finishedPotentialCompanyList.add( unfinishedPotentialCompanyList.get( i ) );
				unfinishedPotentialCompanyList.remove( i );
			}
			if( i < getCurrentIdx() ) {
				setCurrentIdx( getCurrentIdx() - 1 );
			}
		}
		for( int i = finishedPotentialCompanyList.size() - 1; i > -1; i-- ) {
			if( !isCompleted( finishedPotentialCompanyList.get( i ) ) ) {
				unfinishedPotentialCompanyList.add( finishedPotentialCompanyList.get( i ) );
				finishedPotentialCompanyList.remove( i );
			}
		}
	}
	
	public void startTask() {
		SalesCallTask saveableSalesCallTask = getSaveableBean();
		saveableSalesCallTask.setStartedDate( new Date() );
		saveableSalesCallTask.loadCurrentPotentialCompany(true);
		saveableSalesCallTask.saveDetails();
		addToScope();
	}
	
	public void continueTask() {
		SalesCallTask saveableSalesCallTask = getSaveableBean();
		saveableSalesCallTask.loadCurrentPotentialCompany(true);
		saveableSalesCallTask.saveDetails();
		addToScope();
	}

	public void previousTask() {
		SalesCallTask saveableSalesCallTask = getSaveableBean();
		if( saveableSalesCallTask.verifyCurrentPotentialCompanyFinished() == false ) {
			if( getCurrentIdx() == 0 ) {
				saveableSalesCallTask.setCurrentIdx( saveableSalesCallTask.getUnfinishedPotentialCompanyList().size() - 1 );
			} else {
				saveableSalesCallTask.setCurrentIdx( saveableSalesCallTask.getCurrentIdx() - 1 );
			}
		} 
			
		while( saveableSalesCallTask.verifyCurrentPotentialCompanyFinished() ) {
			if( getCurrentIdx() == 0 ) {
				saveableSalesCallTask.setCurrentIdx( saveableSalesCallTask.getUnfinishedPotentialCompanyList().size() - 1 );
			} else {
				saveableSalesCallTask.setCurrentIdx( saveableSalesCallTask.getCurrentIdx() - 1 );
			}
		}
		saveableSalesCallTask.loadCurrentPotentialCompany(true);
		saveableSalesCallTask.saveDetails();
		addToScope();
	}
	
	public void nextTask() {
		boolean isIncrementingIdx = true;
		SalesCallTask saveableSalesCallTask = getSaveableBean();
		while( saveableSalesCallTask.verifyCurrentPotentialCompanyFinished() ) {
			isIncrementingIdx = false;
		}
		if( isIncrementingIdx ) {
			saveableSalesCallTask.setCurrentIdx(saveableSalesCallTask.getCurrentIdx() + 1); 
		}
		saveableSalesCallTask.loadCurrentPotentialCompany(true);
		saveableSalesCallTask.saveDetails();
		addToScope();
	}
	
	public boolean verifyCurrentPotentialCompanyFinished() {
		if( isCompleted( getUnfinishedPotentialCompanyList().get( getCurrentIdx() ) ) ) {
			getFinishedPotentialCompanyList().add(getUnfinishedPotentialCompanyList().get( getCurrentIdx() ));
			getUnfinishedPotentialCompanyList().remove(getCurrentIdx());
			return true;
		}
		return false;
	}
	
	public PotentialCompany loadCurrentPotentialCompany( boolean redirectIfFound ) {
		if( getCurrentIdx() < getUnfinishedPotentialCompanyList().size() ) {
			while( verifyCurrentPotentialCompanyFinished() ) {}
			PotentialCompany potentialCompany = getUnfinishedPotentialCompanyList().get( getCurrentIdx() );
			if( redirectIfFound ) {
				potentialCompany.addToScope();
				potentialCompany.redirectToEditPage();
			}
			return potentialCompany;
		} else {
			reassessLists();
			if( getUnfinishedPotentialCompanyList().size() == 0 ) {
				setCompletedDate( new Date() );
				JSFUtil.addMessage( "Congratulations your task has been completed" );
				JSFUtil.redirect( SalesCallTaskListPage.class );
			} else {
				setCurrentIdx( 0 );
				JSFUtil.addMessage( "You have cycled through the task list and are back to the first one." );
				while( verifyCurrentPotentialCompanyFinished() ) {}
				PotentialCompany potentialCompany = getUnfinishedPotentialCompanyList().get( getCurrentIdx() );
				if( redirectIfFound ) {
					potentialCompany.addToScope();
					potentialCompany.redirectToEditPage();
				}
				return potentialCompany;
			}
		}
		return null;
	}
	
	public boolean isCompleted( PotentialCompany potentialCompany ) {
		return getExitStatuses().contains( potentialCompany.getPotentialCompanyStatus() );
	}
	
	public SystemUser getSystemUser() {
		return systemUser;
	}
	public void setSystemUser(SystemUser systemUser) {
		this.systemUser = systemUser;
	}
	public int getCurrentIdx() {
		return currentIdx;
	}
	public void setCurrentIdx(int currentIdx) {
		this.currentIdx = currentIdx;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Date getCompletedDate() {
		return completedDate;
	}
	public void setCompletedDate(Date completedDate) {
		this.completedDate = completedDate;
	}
	public Date getCancelledDate() {
		return cancelledDate;
	}
	public void setCancelledDate(Date cancelledDate) {
		this.cancelledDate = cancelledDate;
	}
	public Date getStartedDate() {
		return startedDate;
	}
	public void setStartedDate(Date startedDate) {
		this.startedDate = startedDate;
	}

	public List<PotentialCompany> getFinishedPotentialCompanyList() {
		return finishedPotentialCompanyList;
	}

	public void setFinishedPotentialCompanyList(
			List<PotentialCompany> finishedPotentialCompanyList) {
		this.finishedPotentialCompanyList = finishedPotentialCompanyList;
	}

	public List<PotentialCompanyStatus> getExitStatuses() {
		return exitStatuses;
	}

	public void setExitStatuses(List<PotentialCompanyStatus> exitStatuses) {
		this.exitStatuses = exitStatuses;
	}

	public List<PotentialCompany> getUnfinishedPotentialCompanyList() {
		return unfinishedPotentialCompanyList;
	}

	public void setUnfinishedPotentialCompanyList(
			List<PotentialCompany> unfinishedPotentialCompanyList) {
		this.unfinishedPotentialCompanyList = unfinishedPotentialCompanyList;
	}
}
