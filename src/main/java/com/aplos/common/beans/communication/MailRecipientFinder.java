package com.aplos.common.beans.communication;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.aplos.common.annotations.persistence.CollectionOfElements;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.interfaces.MailRecipient;

@Entity
public class MailRecipientFinder extends AplosBean {
	private static final long serialVersionUID = 1156456680331426695L;

	private String name;

	@CollectionOfElements
	private List<String> hqlStatements = new ArrayList<String>();

	public MailRecipientFinder() {}

	public MailRecipientFinder(String name, String hqlStatement) {
		this.name = name;
		this.hqlStatements.add( hqlStatement );
	}

	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}

	public List<String> getHqlStatements() {
		return hqlStatements;
	}

	public void setHqlStatements(List<String> hqlStatements) {
		this.hqlStatements = hqlStatements;
	}

	public List<MailRecipient> getMailRecipientsAsList() {
		return new ArrayList<MailRecipient>( getMailRecipients() );
	}

	public Set<MailRecipient> getMailRecipients() {
		Set<MailRecipient> mailRecipients = new HashSet<MailRecipient>();
		for (String hqlStatement : getHqlStatements()) {
			BeanDao aqlBeanDao = new BeanDao(hqlStatement);
			mailRecipients.addAll(aqlBeanDao.getAll());
		}
//		for (MailRecipient mailRecipient : mailRecipients) {
//			if( mailRecipient.getMailRecipientAddress() != null ) {
//				HibernateUtil.initialise( mailRecipient.getMailRecipientAddress(), true );
//			}
//		}

		return mailRecipients;
	}

}
