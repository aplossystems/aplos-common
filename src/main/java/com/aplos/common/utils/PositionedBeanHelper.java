package com.aplos.common.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.interfaces.PositionedBean;

public class PositionedBeanHelper implements Serializable {
	private static final long serialVersionUID = 450374470669239624L;
	private AplosBean parent;
	private PositionedBean selectedPositionedBean;
	private PositionedBean currentPositionedBean;
	private List<PositionedBean> positionedBeanList;
	private Class<? extends PositionedBean> positionedBeanClass;

	public PositionedBeanHelper( AplosBean parent, List<PositionedBean> positionedBeanList, Class<? extends PositionedBean> positionedBeanClass ) {
		this.setParent(parent);
		this.positionedBeanList = positionedBeanList;
		this.setPositionedBeanClass(positionedBeanClass);
		if (getPositionedBeanList().size() == 0) {
			setSelectedPositionedBean(null);
			setCurrentPositionedBean(null);
		}
		else if( !getPositionedBeanList().contains(currentPositionedBean) || selectedPositionedBean == null ) {
			setSelectedPositionedBean(getPositionedBeanList().get(0));
			setCurrentPositionedBean(getSelectedPositionedBean());
		}
	}

	public void addNewPositionedBean() {
		if (currentPositionedBean != null) {
			savePositionedBean(currentPositionedBean);
		}
		if( parent != null ) {
			parent.saveDetails();
		}
		PositionedBean positionedBean = (PositionedBean) CommonUtil.getNewInstance(positionedBeanClass, null );
		positionedBean.setPositionIdx(getPositionedBeanList().size());
		getPositionedBeanList().add(positionedBean);
		setSelectedPositionedBean( positionedBean );
		setCurrentPositionedBean( positionedBean );
	}

	public void changeCurrentPositionedBean() {
		savePositionedBean( currentPositionedBean );
		setCurrentPositionedBean( selectedPositionedBean );
	}

	public void changeCurrentPositionedBean(ValueChangeEvent event) {
		if( event.getPhaseId().equals( PhaseId.UPDATE_MODEL_VALUES ) ) {
			savePositionedBean( currentPositionedBean );
			setCurrentPositionedBean( selectedPositionedBean );
		} else {
			event.setPhaseId( PhaseId.UPDATE_MODEL_VALUES );
			event.queue();
			return;
		}
	}

	public void saveCurrentPositionedBean() {
		if( getCurrentPositionedBean() != null ) {
			savePositionedBean( getCurrentPositionedBean() );
		}
	}

	public void savePositionedBean( PositionedBean positionedBean ) {
		int oldPositionIdx;
		if( positionedBean.isNew() ) {
			oldPositionIdx = getPositionedBeanList().size();
		} else {
			oldPositionIdx = (Integer) ApplicationUtil.getFirstResult( "SELECT positionIdx FROM " + AplosBean.getTableName( positionedBeanClass ) + " WHERE id = " + positionedBean.getId() )[ 0 ];
		}
		if( positionedBean.isNew() ) {
			positionedBean.saveDetails(JSFUtil.getLoggedInUser());
		}
		if( positionedBean.getPositionIdx() == null || oldPositionIdx != positionedBean.getPositionIdx() ) {
			postionedBeanChanged( getPositionedBeanList(), positionedBean, oldPositionIdx );
		}
		positionedBean.saveDetails(JSFUtil.getLoggedInUser());
	}

	public void postionedBeanChanged( List<? extends PositionedBean> positionedBeanList, PositionedBean reOrderedPositionedBean, int oldPositionIdx ) {
		//make sure we keep all numbers consecutive - ie account for position changing in either direction
		if ( oldPositionIdx > reOrderedPositionedBean.getPositionIdx()) {
			for( PositionedBean tempPositionedBean : positionedBeanList ) {
				if( !tempPositionedBean.getId().equals( reOrderedPositionedBean.getId() ) ) {
					if( tempPositionedBean.getPositionIdx() >= reOrderedPositionedBean.getPositionIdx() && tempPositionedBean.getPositionIdx() < oldPositionIdx ) {
						tempPositionedBean.setPositionIdx( tempPositionedBean.getPositionIdx() + 1 );
					}
				}
			}
			ApplicationUtil.executeSql( "UPDATE " + AplosBean.getTableName( positionedBeanClass ) + " SET positionIdx = positionIdx + 1 where positionIdx >= " + reOrderedPositionedBean.getPositionIdx() + " AND positionIdx < " + oldPositionIdx );
		} else if( oldPositionIdx < reOrderedPositionedBean.getPositionIdx() ) {
			for( PositionedBean tempPositionedBean : positionedBeanList ) {
				if( !tempPositionedBean.getId().equals( reOrderedPositionedBean.getId() ) ) {
					if( tempPositionedBean.getPositionIdx() > oldPositionIdx && tempPositionedBean.getPositionIdx() <= reOrderedPositionedBean.getPositionIdx() ) {
						tempPositionedBean.setPositionIdx( tempPositionedBean.getPositionIdx() - 1 );
					}
				}
			}

			ApplicationUtil.executeSql( "UPDATE " + AplosBean.getTableName( positionedBeanClass ) + " SET positionIdx = positionIdx - 1 where positionIdx <= " + reOrderedPositionedBean.getPositionIdx() + " AND positionIdx > " + oldPositionIdx );
		}
	}

	public void deleteCurrentPositionedBean() {
		getPositionedBeanList().remove(currentPositionedBean);
		for( PositionedBean tempPositionedBean : getPositionedBeanList() ) {
			if( tempPositionedBean.getPositionIdx() > currentPositionedBean.getPositionIdx() ) {
				tempPositionedBean.setPositionIdx( tempPositionedBean.getPositionIdx() - 1 );
			}
		}

		if( currentPositionedBean.getId() != null ) {
			@SuppressWarnings("unchecked")
			AplosAbstractBean deleteBean = new BeanDao( (Class<? extends AplosAbstractBean>) currentPositionedBean.getClass() ).get( currentPositionedBean.getId() );
			deleteBean.hardDelete();
		}
		parent.saveDetails();

		if( getPositionedBeanList().size() > 0 ) {
			setSelectedPositionedBean(getPositionedBeanList().get(0));
			setCurrentPositionedBean(getSelectedPositionedBean());
		} else {
			setSelectedPositionedBean(null);
			setCurrentPositionedBean(null);
		}
	}

	public static  List<? extends PositionedBean> getSortedPositionedBeanList( List<PositionedBean> unsortedPositionedBeans ) {
		/*
		 * Add to new list otherwise hibernate will make the list dirty and save all the children.
		 */
		List<PositionedBean> sortedPositionBeans = new ArrayList<PositionedBean>(unsortedPositionedBeans);
		Collections.sort(sortedPositionBeans,new PositionedBeanComparator());
		return sortedPositionBeans;
	}

	public List<? extends PositionedBean> getSortedPositionedBeanList() {
		return PositionedBeanHelper.getSortedPositionedBeanList( getPositionedBeanList() );
	}

	public SelectItem[] getPositionedBeanSelectItems() {
		SelectItem[] selectItems;
		selectItems = new SelectItem[ getSortedPositionedBeanList().size() ];
		for( int i = 0, n = selectItems.length; i < n; i++ ) {
			selectItems[ i ] = new SelectItem( getSortedPositionedBeanList().get( i ), getSortedPositionedBeanList().get( i ).getDisplayName() );
		}

		return selectItems;
	}

	public SelectItem[] getPositionIdxSelectItems() {
		return getPositionIdxSelectItems(false);
	}

	public SelectItem[] getPositionIdxSelectItems(boolean titledNumeric) {

		int listSize = getPositionedBeanList().size();
		List<PositionedBean> sortedPositionedBeans = CommonUtil.getShallowCopy(getSortedPositionedBeanList());
		
		// This should just be temporary as this shouldn't happen 14 Jan 2013.
		checkPositionIdxAndAutoFix(sortedPositionedBeans);
		
		Integer currentPositionIdx = currentPositionedBean.getPositionIdx();
		// This is for when it's the bean is new
		if( currentPositionIdx == null ) {
			listSize++;
		}
		SelectItem[] selectItems = new SelectItem[listSize];
		for( int i = 0, n = listSize; i < n; i++ ) {
			if ( currentPositionIdx != null && i == currentPositionIdx && !titledNumeric) {
				selectItems[i] = new SelectItem(i,"Current position");
			} else if( i == 0  && !titledNumeric ) {
				selectItems[i] = new SelectItem(i,"Beginning of list");
			} else if( i == (listSize - 1) && !titledNumeric ) {
				selectItems[i] = new SelectItem(i,"End of list");
			} else if (!titledNumeric) {
				if( currentPositionIdx == null || i < currentPositionIdx ) {
					selectItems[i] = new SelectItem(i,"Before " + sortedPositionedBeans.get( i ).getDisplayName() );
				} else {
					selectItems[i] = new SelectItem(i,"Before " + sortedPositionedBeans.get( i + 1 ).getDisplayName() );
				}
			} else {
				selectItems[i] = new SelectItem(i, FormatUtil.getTitledPosition( i + 1 ) );
			}
		}
		return selectItems;
	}
	
	public void checkPositionIdxAndAutoFix( List<PositionedBean> sortedPositionedBeans ) {
		boolean autoFixed = false;
		for( int i = 0, n = sortedPositionedBeans.size(); i < n; i++ ) {
			if( i != sortedPositionedBeans.get( i ).getPositionIdx() ) {
				sortedPositionedBeans.get( i ).setPositionIdx( i );
				sortedPositionedBeans.get( i ).saveDetails(JSFUtil.getLoggedInUser());
				autoFixed = true;
			}
		}
		
		if( autoFixed ) {
			ApplicationUtil.getAplosContextListener().handleError( new Exception( "PositionedBeanIdx out of sync" ), false );
		}
	}


	public void setSelectedPositionedBean(PositionedBean selectedPositionedBean) {
		this.selectedPositionedBean = selectedPositionedBean;
	}
	public PositionedBean getSelectedPositionedBean() {
		return selectedPositionedBean;
	}
	public void setCurrentPositionedBean(PositionedBean currentPositionedBean) {
		this.currentPositionedBean = currentPositionedBean;
	}
	public PositionedBean getCurrentPositionedBean() {
		return currentPositionedBean;
	}
	public void setPositionedBeanList(List<PositionedBean> positionedBeanList) {
		this.positionedBeanList = positionedBeanList;
	}
	public List<PositionedBean> getPositionedBeanList() {
		return positionedBeanList;
	}

	public void setParent(AplosBean parent) {
		this.parent = parent;
	}

	public AplosBean getParent() {
		return parent;
	}

	public void setPositionedBeanClass(Class<? extends PositionedBean> positionedBeanClass) {
		this.positionedBeanClass = positionedBeanClass;
	}

	public Class<? extends PositionedBean> getPositionedBeanClass() {
		return positionedBeanClass;
	}

	public static class PositionedBeanComparator implements Comparator<PositionedBean> {
		@Override
		public int compare(PositionedBean positionBean1, PositionedBean positionBean2) {
			return positionBean1.getPositionIdx().compareTo( positionBean2.getPositionIdx() );
		}
	}
}
