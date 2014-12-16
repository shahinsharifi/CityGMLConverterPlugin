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
package org.citydb.plugins.CityGMLConverter.gui.preferences;

import org.citydb.plugins.CityGMLConverter.CityKMLExportPlugin;




public class CityKMLExportPreferences extends AbstractPreferences {
	
	public CityKMLExportPreferences(CityKMLExportPlugin plugin) {
		super(new CityKMLExportEntry());
		
		DefaultPreferencesEntry renderingNode = new RenderingPanel();
		renderingNode.addChildEntry(new DefaultPreferencesEntry(new BuildingRenderingPanel(plugin)));
		renderingNode.addChildEntry(new DefaultPreferencesEntry(new WaterBodyRenderingPanel(plugin)));
		renderingNode.addChildEntry(new DefaultPreferencesEntry(new LandUseRenderingPanel(plugin)));
		renderingNode.addChildEntry(new DefaultPreferencesEntry(new VegetationRenderingPanel(plugin)));
		renderingNode.addChildEntry(new DefaultPreferencesEntry(new TransportationRenderingPanel(plugin)));
		renderingNode.addChildEntry(new DefaultPreferencesEntry(new ReliefRenderingPanel(plugin)));
		renderingNode.addChildEntry(new DefaultPreferencesEntry(new CityFurnitureRenderingPanel(plugin)));
		DefaultPreferencesEntry genericCityObjectRenderingNode = new GenericCityObjectBalloonPanel();
	//	genericCityObjectRenderingNode.addChildEntry(new DefaultPreferencesEntry(new ThreeDRenderingPanel(plugin)));
	//	genericCityObjectRenderingNode.addChildEntry(new DefaultPreferencesEntry(new PointAndCurveRenderingPanel(plugin)));
		renderingNode.addChildEntry(genericCityObjectRenderingNode);		
		renderingNode.addChildEntry(new DefaultPreferencesEntry(new CityObjectGroupRenderingPanel(plugin)));

		DefaultPreferencesEntry balloonNode = new BalloonPanel();
		balloonNode.addChildEntry(new DefaultPreferencesEntry(new BuildingBalloonPanel(plugin)));
		balloonNode.addChildEntry(new DefaultPreferencesEntry(new WaterBodyBalloonPanel(plugin)));
		balloonNode.addChildEntry(new DefaultPreferencesEntry(new LandUseBalloonPanel(plugin)));
		balloonNode.addChildEntry(new DefaultPreferencesEntry(new VegetationBalloonPanel(plugin)));
		balloonNode.addChildEntry(new DefaultPreferencesEntry(new TransportationBalloonPanel(plugin)));
		balloonNode.addChildEntry(new DefaultPreferencesEntry(new ReliefBalloonPanel(plugin)));
		balloonNode.addChildEntry(new DefaultPreferencesEntry(new CityFurnitureBalloonPanel(plugin)));
		DefaultPreferencesEntry genericCityObjectBalloonNode = new GenericCityObjectBalloonPanel();
	//	genericCityObjectBalloonNode.addChildEntry(new DefaultPreferencesEntry(new ThreeDBalloonPanel(plugin)));
	//	genericCityObjectBalloonNode.addChildEntry(new DefaultPreferencesEntry(new PointAndCurveBalloonPanel(plugin)));
		balloonNode.addChildEntry(genericCityObjectBalloonNode);
		balloonNode.addChildEntry(new DefaultPreferencesEntry(new CityObjectGroupBalloonPanel(plugin)));

		root.addChildEntry(new DefaultPreferencesEntry(new GeneralPanel(plugin)));
		root.addChildEntry(renderingNode);
		root.addChildEntry(balloonNode);
		root.addChildEntry(new DefaultPreferencesEntry(new AltitudePanel(plugin)));
	}

}
