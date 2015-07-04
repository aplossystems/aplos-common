package com.aplos.common.module;


public class CommonModuleUpgrader extends ModuleUpgrader {

	public CommonModuleUpgrader(AplosModuleImpl aplosModule) {
		super(aplosModule,CommonConfiguration.class);
	}

	@Override
	protected void upgradeModule() {
		//don't use break, allow the rules to cascade
		switch (getMajorVersionNumber()) {
			case 1:
				switch (getMinorVersionNumber()) {
					case 10:
						switch (getPatchVersionNumber()) {
						}
				}
			}
	}

}




