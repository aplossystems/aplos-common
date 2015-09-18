package com.aplos.common.module;

import com.aplos.common.beans.Website;
import com.aplos.common.utils.ApplicationUtil;


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
							case 1:
								upgradeTo1_10_2();
							case 2:
								upgradeTo1_10_3();
						}
				}
			}
	}
	
	private void upgradeTo1_10_3() {
		ApplicationUtil.executeSql( "UPDATE CommonConfiguration SET mainTextColour = mainHeaderColour" );
		
		setPatchVersionNumber(3);
	}
	
	private void upgradeTo1_10_2() {
		ApplicationUtil.executeSql( "UPDATE CommonConfiguration SET highlightColour = REPLACE(highlightColour, '#', '')" );
		ApplicationUtil.executeSql( "UPDATE CommonConfiguration SET mainHeaderColour = REPLACE(mainHeaderColour, '#', '')" );
		ApplicationUtil.executeSql( "UPDATE CommonConfiguration SET subHeaderColour = REPLACE(subHeaderColour, '#', '')" );
		ApplicationUtil.executeSql( "UPDATE CommonConfiguration SET subHeaderTextColour = REPLACE(subHeaderTextColour, '#', '')" );
		
		setPatchVersionNumber(2);
	}
	
	private void upgradeTo1_10_1() {
		setDefault( Website.class, "deferScriptStatus", "1" );
		setDefault( Website.class, "deferStyleStatus", "1" );
		setPatchVersionNumber(1);
	}

}




