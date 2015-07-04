package com.aplos.common.interfaces;

import com.aplos.common.beans.AplosWorkingDirectory;

public interface AplosWorkingDirectoryInter {
	public String getDirectoryPath( boolean includeServerWorkPath );
	public AplosWorkingDirectory getAplosWorkingDirectory();
	public void setAplosWorkingDirectory( AplosWorkingDirectory aplosWorkingDirectory );
	public boolean isRestricted();
	public String name();
}
