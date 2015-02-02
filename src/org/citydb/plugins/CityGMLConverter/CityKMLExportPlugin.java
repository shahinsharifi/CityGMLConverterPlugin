package org.citydb.plugins.CityGMLConverter;

import java.util.Locale;
import java.util.ResourceBundle;

import org.citydb.api.controller.ApplicationStarter;
import org.citydb.api.plugin.Plugin;
import org.citydb.api.plugin.extension.config.ConfigExtension;
import org.citydb.api.plugin.extension.config.PluginConfigEvent;
import org.citydb.api.plugin.extension.preferences.Preferences;
import org.citydb.api.plugin.extension.preferences.PreferencesExtension;
import org.citydb.api.plugin.extension.view.View;
import org.citydb.api.plugin.extension.view.ViewExtension;
import org.citydb.plugins.CityGMLConverter.config.ConfigImpl;
import org.citydb.plugins.CityGMLConverter.gui.preferences.CityKMLExportPreferences;
import org.citydb.plugins.CityGMLConverter.gui.view.CityKMLExportView;
import org.citydb.plugins.CityGMLConverter.util.Util;


public class CityKMLExportPlugin implements Plugin, ViewExtension, PreferencesExtension , ConfigExtension<ConfigImpl>{
	
	private ConfigImpl config;
	private CityKMLExportView view;
	private CityKMLExportPreferences preferences;
	
	private Locale currentLocale;

	public static void main(String[] args) {
		// test run
		ApplicationStarter starter = new ApplicationStarter();
		starter.run(args, new CityKMLExportPlugin());
	}

		
	@Override
	public void init(Locale locale){
		try {
			view = new CityKMLExportView(this);
			preferences = new CityKMLExportPreferences(this);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		loadSettings();	
		switchLocale(locale);
	}

	@Override
	public void shutdown() {
		setSettings();
	}

	@Override
	public void switchLocale(Locale locale) {
		if (locale.equals(currentLocale))
			return;
		
		Util.I18N = ResourceBundle.getBundle("org.citydb.plugins.CityGMLConverter.gui.Label", locale);
		currentLocale = locale;
		
		view.doTranslation();
		preferences.doTranslation();
		
	}

	@Override
	public Preferences getPreferences() {
		return preferences;
	}

	@Override
	public View getView() {
		return view;
	}
	
	public void loadSettings() {
		view.loadSettings();
		preferences.loadSettings();
	}

	public void setSettings() {
		view.setSettings();
		preferences.setSettings();
	}
	
	@Override
	public void handleEvent(PluginConfigEvent event) {
		switch (event) {
		case RESET_DEFAULT_CONFIG:
			this.config = new ConfigImpl();
			loadSettings();
			break;
		case PRE_SAVE_CONFIG:
			setSettings();
			break;
		}
	}
	
	@Override
	public ConfigImpl getConfig() {
		return config;
	}

	public void setConfig(ConfigImpl config) {
		this.config = config;
	}
	
	@Override
	public void configLoaded(ConfigImpl config) {
		boolean reload = this.config != null;		
		setConfig(config);
		
		if (reload)
			loadSettings();
	}
	
}