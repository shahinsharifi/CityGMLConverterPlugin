/*
 * This file is part of the 3D City Database Importer/Exporter.
 * Copyright (c) 2007 - 2013
 * Institute for Geodesy and Geoinformation Science
 * Technische Universitaet Berlin, Germany
 * http://www.gis.tu-berlin.de/
 * 
 * The 3D City Database Importer/Exporter program is free software:
 * you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program. If not, see 
 * <http://www.gnu.org/licenses/>.
 * 
 * The development of the 3D City Database Importer/Exporter has 
 * been financially supported by the following cooperation partners:
 * 
 * Business Location Center, Berlin <http://www.businesslocationcenter.de/>
 * virtualcitySYSTEMS GmbH, Berlin <http://www.virtualcitysystems.de/>
 * Berlin Senate of Business, Technology and Women <http://www.berlin.de/sen/wtf/>
 */
package org.citydb.plugins.CityGMLConverter.gui.view;

import java.awt.Component;

import javax.swing.Icon;
import javax.xml.bind.JAXBContext;

import org.citydb.api.plugin.extension.view.View;
import org.citydb.plugins.CityGMLConverter.CityKMLExportPlugin;
import org.citydb.plugins.CityGMLConverter.util.Util;
import org.citygml4j.builder.jaxb.JAXBBuilder;




public class CityKMLExportView extends View {
	private final CityKmlExportPanel component;
	
	/**
	 * @throws Exception 
	 * @wbp.parser.entryPoint
	 */
	public CityKMLExportView(CityKMLExportPlugin plugin) throws Exception {
		component = new CityKmlExportPanel(plugin);
	}
	
	@Override
	public String getLocalizedTitle() {
		return Util.I18N.getString("main.tabbedPane.CityKmlExport");
	}

	@Override
	public Component getViewComponent() {
		return component;
	}

	@Override
	public String getToolTip() {
		return null;
	}

	@Override
	public Icon getIcon() {
		return null;
	}
	
	public void loadSettings() {
		component.loadSettings();
	}
	
	public void setSettings() {
		component.setSettings();
	}
	
	public void doTranslation() {
		component.doTranslation();
	}
	

}
