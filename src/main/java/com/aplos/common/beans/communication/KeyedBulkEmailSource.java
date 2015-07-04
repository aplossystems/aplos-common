package com.aplos.common.beans.communication;

import com.aplos.common.annotations.DynamicMetaValues;
import com.aplos.common.annotations.persistence.Any;
import com.aplos.common.annotations.persistence.AnyMetaDef;
import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.JoinColumn;
import com.aplos.common.aql.BeanMap;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.interfaces.BulkEmailSource;
import com.aplos.common.interfaces.KeyedBulkEmailSourceInter;
import com.aplos.common.persistence.PersistentBeanSaver;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;
import com.aplos.common.utils.JSFUtil;

@Entity
public class KeyedBulkEmailSource extends AplosBean implements BulkEmailSource {
	private static final long serialVersionUID = 6742582719829874630L;
	
	@Any( metaColumn = @Column( name = "keyedBulkEmailSource_type" ) )
    @AnyMetaDef( idType = "long", metaType = "string", metaValues = {
    		/* Meta Values added in at run-time */ } )
    @JoinColumn( name = "keyedBulkEmailSource_id" )
	@DynamicMetaValues
	private KeyedBulkEmailSourceInter keyedBulkEmailSource;
    private int messageSourceKey;
    
    public KeyedBulkEmailSource() {
	}
    
	
	@Override
	public String getJDynamiTeValue(String variableKey, AplosEmail aplosEmail) {
		return keyedBulkEmailSource.getJDynamiTeValue(getMessageSourceKey(),variableKey);
	}
    
    public KeyedBulkEmailSource( KeyedBulkEmailSourceInter keyedBulkEmailSourceInter, int messageSourceKey ) {
    	setKeyedBulkEmailSource(keyedBulkEmailSourceInter);
    	setMessageSourceKey(messageSourceKey);
	}

	public int getMessageSourceKey() {
		return messageSourceKey;
	}

	public void setMessageSourceKey(int messageSourceKey) {
		this.messageSourceKey = messageSourceKey;
	}

	public boolean isNew() {
		return getId() == null;
	}
	
	@Override
	public Long getMessageSourceId() {
		return getKeyedBulkEmailSource().getMessageSourceId();
	}
	
//	@Override
//	public void hibernateInitialiseAfterCheck(boolean fullInitialisation) {
//		HibernateUtil.initialise(getKeyedBulkEmailSource(), fullInitialisation);
//	}
	
	@Override
	public String getSourceUniqueDisplayName() {
		return getKeyedBulkEmailSource() + " " + getKeyedBulkEmailSource().getKeyLabel( messageSourceKey );
	}
	
	@Override
	public String getFirstName() {
		return getKeyedBulkEmailSource().getFirstName( messageSourceKey );
	}
	
	@Override
	public String getSurname() {
		return getKeyedBulkEmailSource().getSurname( messageSourceKey );
	}
	
	@Override
	public String getEmailAddress() {
		return getKeyedBulkEmailSource().getEmailAddress( messageSourceKey );
	}

	public KeyedBulkEmailSourceInter getKeyedBulkEmailSource() {
		return keyedBulkEmailSource;
	}

	public void setKeyedBulkEmailSource(KeyedBulkEmailSourceInter keyedBulkEmailSource) {
		this.keyedBulkEmailSource = keyedBulkEmailSource;
	}
}
