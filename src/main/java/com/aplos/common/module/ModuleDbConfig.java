package com.aplos.common.module;

import com.aplos.common.persistence.PersistentApplication;

public abstract class ModuleDbConfig {
	private  AplosModule aplosModule;

	public ModuleDbConfig( AplosModule aplosModule ) {
		setAplosModule(aplosModule);
	}

	public abstract void addAnnotatedClass(PersistentApplication persistentApplication);

	public void setAplosModule(AplosModule aplosModule) {
		this.aplosModule = aplosModule;
	}

	public AplosModule getAplosModule() {
		return aplosModule;
	}
}
