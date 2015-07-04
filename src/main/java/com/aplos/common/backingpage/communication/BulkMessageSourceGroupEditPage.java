package com.aplos.common.backingpage.communication;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.model.SelectItem;

import org.primefaces.model.DualListModel;
import org.primefaces.model.SortOrder;

import com.aplos.common.AplosLazyDataModel;
import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.backingpage.LazyDataModelEditPage;
import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.DataTableState;
import com.aplos.common.beans.Subscriber;
import com.aplos.common.beans.Website;
import com.aplos.common.beans.communication.BasicBulkMessageFinder;
import com.aplos.common.beans.communication.BulkMessageSourceGroup;
import com.aplos.common.interfaces.BulkEmailFinder;
import com.aplos.common.interfaces.BulkEmailSource;
import com.aplos.common.interfaces.BulkMessageFinderInter;
import com.aplos.common.interfaces.BulkMessageSource;
import com.aplos.common.interfaces.BulkSmsSource;
import com.aplos.common.utils.FormatUtil;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=BulkMessageSourceGroup.class)
public class BulkMessageSourceGroupEditPage extends LazyDataModelEditPage {
	private static final long serialVersionUID = -8934819587053896798L;

	private DualListModel<BulkMessageSource> pickListModel = new DualListModel<BulkMessageSource>();
	private BasicBulkMessageFinder selectedBulkMessageFinder;
	private List<SelectItem> bulkMessageFinderSelectItems;

	public BulkMessageSourceGroupEditPage() {
		super();
		BulkMessageSourceGroup bulkEmailSourceGroup = resolveAssociatedBean();
		pickListModel.setTarget(bulkEmailSourceGroup.getBulkMessageSources());
		createBulkMessageFinderSelectItems();
		updateLists(null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean responsePageLoad() {
		boolean returnValue = super.responsePageLoad();
		if (selectedBulkMessageFinder != null) {
			/*
			 * This should probably be changed so that the AqlBeanDao is retrieved from the finder as at the moment
			 * this only supports simple finding, it does not support cases where joins are involved in the finder.
			 */
			BulkMessageFinderInter bulkMessageFinderInter = selectedBulkMessageFinder.getBulkEmailFinderClassInstance();
			getDataTableState().getLazyDataModel().getBeanDao().setBeanClass((Class<? extends AplosAbstractBean>) selectedBulkMessageFinder.getBulkMessageFinderClass(), null);
			getDataTableState().getLazyDataModel().setSearchCriteria( bulkMessageFinderInter.getFinderSearchCriteria() ); 
			getDataTableState().getLazyDataModel().getBeanDao().setOrderBy( bulkMessageFinderInter.getAlphabeticalSortByCriteria() );
		}
		return returnValue;
	}
	
	@SuppressWarnings("unchecked")
	public void createBulkMessageFinderSelectItems() {
		BulkMessageSourceGroup bulkEmailSourceGroup = resolveAssociatedBean();
		List<SelectItem> selectItems = new ArrayList<SelectItem>();
		if (!bulkEmailSourceGroup.isSmsRequired() && !bulkEmailSourceGroup.isEmailRequired()) {
			selectItems.add(new SelectItem(null,"You must select at least one option (SMS / Email"));
			setBulkMessageFinderSelectItems( selectItems );
			return;
		}
		List<BasicBulkMessageFinder> bulkMessageFinders = null;
		
		if( bulkEmailSourceGroup.isSmsRequired() ) {
			bulkMessageFinders = Website.getCurrentWebsiteFromTabSession().getBulkSmsFinders();
		} else {
			bulkMessageFinders = Website.getCurrentWebsiteFromTabSession().getBulkEmailFinders();	
		} 
		
		setSelectedBulkMessageFinder( null );
		if( bulkMessageFinders != null ) {
			for( int i = 0, n = bulkMessageFinders.size(); i < n; i++ ) {
				if( bulkMessageFinders.get( i ).getSourceType().equals( bulkEmailSourceGroup.getSourceType() ) ) {
					setSelectedBulkMessageFinder( bulkMessageFinders.get( i ) );
				}
				selectItems.add( new SelectItem( bulkMessageFinders.get( i ), FormatUtil.breakCamelCase( bulkMessageFinders.get( i ).getBulkMessageFinderClass().getSimpleName() ) ) );
			}
		}

		//if we no longer support the selected source type, change the selection
		if (bulkMessageFinders.size() > 0 && selectedBulkMessageFinder == null) {
			setSelectedBulkMessageFinder( bulkMessageFinders.get( 0 ) );
			bulkEmailSourceGroup.setSourceType( selectedBulkMessageFinder.getSourceType() );
		}
		setBulkMessageFinderSelectItems( selectItems );
	}

	@Override
	public void okBtnAction() {
		BulkMessageSourceGroup bulkEmailSourceGroup = resolveAssociatedBean();
		if (!bulkEmailSourceGroup.isSmsRequired() && !bulkEmailSourceGroup.isEmailRequired()) {
			JSFUtil.addMessageForError("A group must support at least one of the options (SMS / Email)");
		} else {
			//bulkEmailSourceGroup.setBulkMessageSources(pickListModel.getTarget());
			bulkEmailSourceGroup.saveDetails();
			JSFUtil.redirect(getBeanDao().getListPageClass());
		}
	}

	@Override
	public void applyBtnAction() {
		BulkMessageSourceGroup bulkEmailSourceGroup = resolveAssociatedBean();
		if (!bulkEmailSourceGroup.isSmsRequired() && !bulkEmailSourceGroup.isEmailRequired()) {
			JSFUtil.addMessageForError("A group must support at least one of the options (SMS / Email)");
		} else {
			//bulkEmailSourceGroup.setBulkMessageSources(pickListModel.getTarget());
			bulkEmailSourceGroup.saveDetails();
		}
	}

	@SuppressWarnings("unchecked")
	public String updateLists(AjaxBehaviorEvent event) {
		BulkMessageSourceGroup bulkEmailSourceGroup = resolveAssociatedBean();
		List<BulkMessageSource> tempAvailableSources = new ArrayList<BulkMessageSource>();

		//##### Remove anything from our object which no longer fits #######
		
		//if we only want a subscriber, any class is valid
		if (bulkEmailSourceGroup.getSourceType() != null && bulkEmailSourceGroup.getSourceType().isAssignableFrom(Subscriber.class)) {
			for (BulkMessageSource source : bulkEmailSourceGroup.getBulkMessageSources()) { //pickListModel.getTarget()) {
				if (bulkEmailSourceGroup.isSmsRequired() && !(source instanceof BulkSmsSource)) {
					continue;
				}
				if (bulkEmailSourceGroup.isEmailRequired() && !(source instanceof BulkEmailSource)) {
					continue;
				}
				tempAvailableSources.add(source);
			}
		} else if (bulkEmailSourceGroup.getSourceType() != null) {
			//otherwise make sure the sources already assigned to our group are valid
			for (BulkMessageSource source : bulkEmailSourceGroup.getBulkMessageSources()) { //pickListModel.getTarget()) {
				if (bulkEmailSourceGroup.isSmsRequired() && !(source instanceof BulkSmsSource)) {
					continue;
				}
				if (bulkEmailSourceGroup.isEmailRequired() && !(source instanceof BulkEmailSource)) {
					continue;
				}
				if (source instanceof BulkMessageSourceGroup) {
					if (((BulkMessageSourceGroup)source).containsRequiredClass(bulkEmailSourceGroup.getSourceType())) {
						tempAvailableSources.add(source);
					}
				} else if (source.getClass().isAssignableFrom(bulkEmailSourceGroup.getSourceType())) {
					tempAvailableSources.add(source);
				}
			}
		}
		
		if( selectedBulkMessageFinder != null ) {
			bulkEmailSourceGroup.setSourceType( selectedBulkMessageFinder.getSourceType() );
		}
		bulkEmailSourceGroup.setBulkMessageSources(tempAvailableSources);
		createBulkMessageFinderSelectItems();

		return null;
	}
	
	public void addToBulkMessageSourceGroup() {
		Object[] selectedRows = getDataTableState().getLazyDataModel().getSelectedRows();
		if (selectedRows != null && selectedRows.length > 0) {
			BulkMessageSourceGroup bulkEmailSourceGroup = JSFUtil.getBeanFromScope(BulkMessageSourceGroup.class);
			for (Object object : selectedRows) {
				BulkMessageSource bulkMessageSource = (BulkMessageSource) object;
				if (bulkMessageSource instanceof BulkEmailFinder) {
					if (!bulkEmailSourceGroup.getBulkMessageSources().contains( bulkMessageSource ) ) {
						bulkEmailSourceGroup.getBulkMessageSources().add( bulkMessageSource );
					}
				} else {
					if (!bulkEmailSourceGroup.getBulkMessageSources().contains( bulkMessageSource ) ) {
						bulkEmailSourceGroup.getBulkMessageSources().add( bulkMessageSource );
					}
				}
			}
		}
		getDataTableState().getLazyDataModel().setSelectedRows(new Object[0]);
	}

	public void removeFromBulkMessageSourceGroup() {
		BulkMessageSourceGroup bulkEmailSourceGroup = resolveAssociatedBean();
		BulkMessageSource selectedRow = (BulkMessageSource) JSFUtil.getRequest().getAttribute("selectedRow");
		bulkEmailSourceGroup.getBulkMessageSources().remove(selectedRow);
	}

	@Override
	public BeanDao getListBeanDao() {
		return new BeanDao(Subscriber.class);
	}

	@Override
	public AplosLazyDataModel getAplosLazyDataModel(DataTableState dataTableState, BeanDao aqlBeanDao) {
		return new AvailableSubscribersLazyDataModel(dataTableState, aqlBeanDao);
	}

	public BasicBulkMessageFinder getSelectedBulkMessageFinder() {
		return selectedBulkMessageFinder;
	}

	public void setSelectedBulkMessageFinder(BasicBulkMessageFinder selectedBulkMessageFinder) {
		this.selectedBulkMessageFinder = selectedBulkMessageFinder;
	}

	public List<SelectItem> getBulkMessageFinderSelectItems() {
		return bulkMessageFinderSelectItems;
	}

	public void setBulkMessageFinderSelectItems(
			List<SelectItem> bulkMessageFinderSelectItems) {
		this.bulkMessageFinderSelectItems = bulkMessageFinderSelectItems;
	}

	public class AvailableSubscribersLazyDataModel extends AplosLazyDataModel {

		private static final long serialVersionUID = 3073754970678486068L;

		public AvailableSubscribersLazyDataModel( DataTableState dataTableState, BeanDao aqlBeanDao ) {
			super(dataTableState, aqlBeanDao);
			getDataTableState().setShowingNewBtn(false);
			getDataTableState().setShowingDeleteColumn(false);
			getDataTableState().setShowingEditPencilColumn(false);
			getDataTableState().setShowingIdColumn(true); 
			getDataTableState().setShowingIdColumnFilter(false);
			getDataTableState().setShowingQuickView(true);
			//TODO: add showingtoggleDeleted to dts
		}

		@Override
		public List<Object> load(int first, int pageSize, String sortField,	SortOrder sortOrder, Map<String, String> filters) {
			//getBeanDao().setWhereCriteria("bean.id NOT IN ()"); //can be mixed classes so this wont work
			return super.load(first, pageSize, sortField, sortOrder, filters);
		}

	}
}
