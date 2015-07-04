package com.aplos.common.persistence.fieldinfo;

import java.lang.reflect.Field;
import java.util.List;

import com.aplos.common.persistence.PersistableTable;
import com.aplos.common.persistence.PersistentClass;
import com.aplos.common.persistence.metadata.ColumnIndex;

public class EmbeddedFieldInfo extends PersistentClassFieldInfo {
	
	public EmbeddedFieldInfo( PersistentClass persistentClass, PersistentClass parentPersistentClass, Field field ) {
		super( persistentClass, parentPersistentClass, field );
	}
	
	@Override
	public void addForeignKeysAndIndexes(List<ForeignKeyFieldInfo> foreignKeys,
			List<ColumnIndex> columnIndexes) {
	}
	
	@Override
	public void addAdditionalFieldInfos(PersistableTable persistableTable) {
		if( !getPersistentClass().isTypesLoaded() ) {
			getPersistentClass().loadTypes();
		}
		persistableTable.getEmbeddedFieldInfos().addAll( getPersistentClass().getFieldInfos() );
		persistableTable.getFieldInfos().addAll( getPersistentClass().getFieldInfos() );
	}

}
