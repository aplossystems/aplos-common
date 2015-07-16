package com.aplos.common.module;

import com.aplos.common.beans.Website;


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
							case 0 :
								upgradeTo1_10_1();
						}
				}
			}
	}
	
	private void upgradeTo1_10_1() {
		setDefault( Website.class, "deferScripts", "1" );
		setPatchVersionNumber(1);
	}

}




