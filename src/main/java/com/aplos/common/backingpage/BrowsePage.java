package com.aplos.common.backingpage;

import java.io.File;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import com.aplos.common.enums.CommonWorkingDirectory;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@SessionScoped
public class BrowsePage extends BackingPage {
	private static final long serialVersionUID = -6786153912837668167L;

	private File currentDirectory = new File(CommonWorkingDirectory.UPLOAD_DIR.getDirectoryPath(true));

	@Override
	public boolean requestPageLoad() {
		if( JSFUtil.getRequest().getParameter("CKEditorFuncNum") != null ) {
			JSFUtil.addToTabSession( "CKEditorFuncNum", JSFUtil.getRequest().getParameter("CKEditorFuncNum"));
		}
		return true;
	}
	public File[] getFiles() {
		return currentDirectory.listFiles();
	}

	public void open(File file) {
		if (!file.isDirectory()) { return; }
		currentDirectory = file;
	}

	public String getMediaId(File file) {
		return file.getAbsolutePath()
			.replace( new File(CommonWorkingDirectory.UPLOAD_DIR.getDirectoryPath(true)).getAbsolutePath(), "" )
			.replaceAll( "\\\\", "/" );
	}

	public boolean isRoot() {
		if( currentDirectory.equals( new File(CommonWorkingDirectory.UPLOAD_DIR.getDirectoryPath(true)) ) ) {
			return true;
		} else {
			return false;
		}
	}

	public void openParent() {
		if (currentDirectory.getAbsolutePath().equals(new File(CommonWorkingDirectory.SERVER_WORK_DIR.getDirectoryPath(false)).getAbsolutePath())) { return; }
		currentDirectory = currentDirectory.getParentFile();
	}

}
