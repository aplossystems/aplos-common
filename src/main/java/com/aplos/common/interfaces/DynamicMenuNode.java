package com.aplos.common.interfaces;

import java.util.List;

import com.aplos.common.beans.AplosAbstractBean;
import com.aplos.common.beans.Website;


public interface DynamicMenuNode {
	public boolean saveDetails();
	public DynamicMenuNode getParent();
	public void replaceChildWithSaveableBean( DynamicMenuNode dynamicMenuNode );
	public <T extends AplosAbstractBean> T  getReadOnlyBean();
	public List<DynamicMenuNode> getMenuNodeChildren();
	public void addChild(Integer position, DynamicMenuNode newChild);
	public void removeChild(DynamicMenuNode child);
	public boolean isSiteRoot();
	public boolean getIsOutputLink();
	public Long getId();
	public String getDisplayName();
	public void setParent(DynamicMenuNode newParent);
	public void delete();
	public DynamicMenuNode getCopy();
	public Website determineWebsite();
	public <T extends AplosAbstractBean> T getSaveableBean();
}