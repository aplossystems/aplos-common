package com.aplos.common.interfaces;

import java.util.List;

public interface BulkEmailFinder extends BulkMessageFinderInter {
	public List<BulkEmailSource> getEmailAutoCompleteSuggestions(String searchString, Integer limit);
}
