package com.aplos.common.aql;

import java.io.Serializable;

public interface AqlProxy extends Serializable {
	/**
	 * Perform serialization-time write-replacement of this proxy.
	 *
	 * @return The serializable proxy replacement.
	 */
	public Object writeReplace();

	/**
	 * Get the underlying lazy initialization handler.
	 *
	 * @return The lazy initializer.
	 */
	public AbstractLazyInitializer getHibernateLazyInitializer();
}
