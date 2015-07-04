package com.aplos.common.beans.communication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.beans.AplosBean;
import com.aplos.common.comparators.AplosEmailComparator;
import com.aplos.common.enums.EmailActionType;
import com.aplos.common.interfaces.EmailFolder;

@Entity
public class BasicEmailFolder extends AplosBean implements EmailFolder {
	private static final long serialVersionUID = -387509349459175889L;

	private String name;
	
	public BasicEmailFolder() {
	}
	
	public BasicEmailFolder( String name ) {
		setName( name );
	}
	
	public static List<AplosEmail> getEmailListFromFolder( EmailFolder emailFolder, boolean sort ) {
		BeanDao aplosEmailDao = new BeanDao( AplosEmail.class );
		aplosEmailDao.addQueryTable( "efl", "bean.emailFolders" );
		aplosEmailDao.addWhereCriteria( "efl.id = " + emailFolder.getId() );
		aplosEmailDao.addWhereCriteria( "efl.class = '" + emailFolder.getClass().getSimpleName() + "'" );
		
		List<AplosEmail> sortedAplosEmailList = new ArrayList<AplosEmail>(aplosEmailDao.getAll());
		if( sort ) {
			Collections.sort( sortedAplosEmailList, new AplosEmailComparator() );
		}
		return sortedAplosEmailList;
	}
	
	@Override
	public void aplosEmailAction(EmailActionType emailActionType, AplosEmail aplosEmail) {	
	}
	
	@Override
	public String getEmailFolderSearchCriteria() {
		return "bean.name LIKE :searchStr";
	}
	
	@Override
	public String getDisplayName() {
		return getName();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
