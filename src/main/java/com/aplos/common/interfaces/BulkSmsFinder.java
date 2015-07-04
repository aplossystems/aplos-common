package com.aplos.common.interfaces;

import java.util.List;

public interface BulkSmsFinder extends BulkMessageFinderInter {
	public List<BulkSmsSource> getSmsAutoCompleteSuggestions(String searchString, Integer limit);
}
