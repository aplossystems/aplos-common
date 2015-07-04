package com.aplos.common.interfaces;

import java.util.List;


public interface BulkGroupedMessageSource extends BulkMessageSource {
	public List<BulkMessageSource> getBulkMessageSources();
}
