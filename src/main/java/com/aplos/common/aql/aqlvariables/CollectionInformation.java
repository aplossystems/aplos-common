package com.aplos.common.aql.aqlvariables;

import com.aplos.common.aql.aqltables.AqlTable;
import com.aplos.common.persistence.fieldinfo.CollectionFieldInfo;

public class CollectionInformation {
	private AqlTable intermediateTable;
	private AqlTable collectionTable;
	private AqlTable mapKeyTable;
	private boolean isPolymorphic = false;
	private CollectionFieldInfo collectionFieldInfo;
	
	public CollectionInformation( CollectionFieldInfo collectionFieldInfo ) {
		setCollectionFieldInfo( collectionFieldInfo );
	}

	public AqlTable getCollectionTable() {
		return collectionTable;
	}

	public void setCollectionTable(AqlTable collectionTable) {
		this.collectionTable = collectionTable;
	}

	public AqlTable getIntermediateTable() {
		return intermediateTable;
	}

	public void setIntermediateTable(AqlTable intermediateTable) {
		this.intermediateTable = intermediateTable;
	}

	public AqlTable getMapKeyTable() {
		return mapKeyTable;
	}

	public void setMapKeyTable(AqlTable mapKeyTable) {
		this.mapKeyTable = mapKeyTable;
	}

	public boolean isPolymorphic() {
		return isPolymorphic;
	}

	public void setPolymorphic(boolean isPolymorphic) {
		this.isPolymorphic = isPolymorphic;
	}

	public CollectionFieldInfo getCollectionFieldInfo() {
		return collectionFieldInfo;
	}

	public void setCollectionFieldInfo(CollectionFieldInfo collectionFieldInfo) {
		this.collectionFieldInfo = collectionFieldInfo;
	}
}
