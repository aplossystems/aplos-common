package com.aplos.common;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import com.aplos.common.beans.AplosBean;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.JSFUtil;

@ManagedBean
@SessionScoped
public class ThemeManager {
	private String theme;
	private boolean soundEnabled = true;
	private boolean nonFlashSoundEnabled = false;
	
	public static final String MODERN = "modern";
	public static final String RESPONSIVE = "responsive";

	public ThemeManager() {
		theme = ApplicationUtil.getAplosContextListener().getDefaultTheme();
	}
	
	public static ThemeManager getThemeManager() {
		return (ThemeManager) JSFUtil.resolveVariable( AplosBean.getBinding( ThemeManager.class ) );
	}

	public String getResponsiveEditTemplatePath() {
		return "/common/templates/" + theme + "/responsiveEdit.xhtml";
	}

	public String getListTemplatePath() {
		return "/common/templates/" + theme + "/list.xhtml";
	}

	public String getEditTemplatePath() {
		return "/common/templates/" + theme + "/edit.xhtml";
	}

	public String getBackendTemplatePath() {
		return "/common/templates/" + theme + "/backend.xhtml";
	}

	public String getBasicTemplatePath() {
		return "/common/templates/" + theme + "/basic.xhtml";
	}

	public String getFrontendListTemplatePath() {
		return "/common/templates/" + theme + "/frontendList.xhtml";
	}

	public String getFrontendEditTemplatePath() {
		return "/common/templates/" + theme + "/frontendEdit.xhtml";
	}

	public void setTheme(String theme) {
		this.theme = theme;
	}

	public String getTheme() {
		return theme;
	}

	public void setSoundEnabled(Boolean soundEnabled) {
		this.soundEnabled = soundEnabled;
	}

	public boolean isSoundEnabled() {
		return soundEnabled;
	}

	public void reverseSoundEnabledValue() {
		if ( soundEnabled == true ) {
			soundEnabled = false;
		}
		else {
			soundEnabled = true;
		}
	}

	public String getSoundState() {
		if (soundEnabled) {
			return "on";
		}
		else {
			return "off";
		}
	}

	public String getSoundManagerLine(String id, String filename) {
		return getSoundManagerLine(id, filename, "" );
	}


	public String getSoundManagerLine(String id, String filename, String extraOptions ) {
		if (soundEnabled && nonFlashSoundEnabled) {
			String url = "";
			if (!filename.startsWith(JSFUtil.getContextPath())) {
				url += JSFUtil.getContextPath();
			}

			StringBuffer strBuf = new StringBuffer( "requestSoundManagerPlay('" + id + "', \"{url:'" + url + filename + "'" );
			if( extraOptions != null && !extraOptions.equals( "" ) ) {
				strBuf.append( ", " + extraOptions );
			}
			strBuf.append( " }\" );" );
			return   strBuf.toString();
		}
		else {
			return "";
		}
	}

	public String getSoundManagerLine(Integer id, String filename) {
		return getSoundManagerLine(id.toString(), filename);
	}
}
