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
package org.citydb.plugins.CityGMLConverter.content;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.citydb.api.event.EventDispatcher;
import org.citydb.database.adapter.BlobExportAdapter;
import org.citydb.plugins.CityGMLConverter.config.Balloon;
import org.citydb.plugins.CityGMLConverter.config.ColladaOptions;
import org.citydb.plugins.CityGMLConverter.config.ConfigImpl;
import org.citydb.plugins.CityGMLConverter.config.DisplayForm;
import org.citydb.plugins.CityGMLConverter.content.BalloonTemplateHandlerImpl;
import org.citydb.plugins.CityGMLConverter.content.ElevationServiceHandler;
import org.citydb.plugins.CityGMLConverter.content.KmlExporterManager;
import org.citydb.plugins.CityGMLConverter.content.KmlGenericObject;
import org.citydb.plugins.CityGMLConverter.content.KmlSplittingResult;
import org.citydb.util.Util;
import org.citygml4j.factory.GMLGeometryFactory;
import org.citygml4j.model.citygml.building.AbstractBuilding;
import org.citygml4j.model.citygml.core.AbstractCityObject;
import org.citygml4j.model.gml.basicTypes.DoubleOrNull;
import org.citygml4j.model.gml.basicTypes.MeasureOrNullList;


public class CityObject extends KmlGenericObject{

	public static final String STYLE_BASIS_NAME = "Group";

	public CityObject(
			KmlExporterManager kmlExporterManager,
			GMLGeometryFactory cityGMLFactory,
			net.opengis.kml._2.ObjectFactory kmlFactory,
			ElevationServiceHandler elevationServiceHandler,
			BalloonTemplateHandlerImpl balloonTemplateHandler,
			EventDispatcher eventDispatcher,
			ConfigImpl config) {

		super(
			  kmlExporterManager,
			  cityGMLFactory,
			  kmlFactory,
			  elevationServiceHandler,
			  balloonTemplateHandler,
			  eventDispatcher,
			  config);
	}

	protected List<DisplayForm> getDisplayForms() {
		return config.getCityObjectGroupDisplayForms();
	}

	public ColladaOptions getColladaOptions() {
		return null; // no COLLADA display form for CityObjectGroups
	}

	public Balloon getBalloonSettings() {
		return config.getCityObjectGroupBalloon();
	}

	public String getStyleBasisName() {
		return STYLE_BASIS_NAME;
	}

	protected String getHighlightingQuery() {
		return null;  // no COLLADA or Geometry display form for CityObjectGroups
	}

	public void read(KmlSplittingResult work) {

		
	}
	
	public static HashMap<String,Object> getCityObjectProperties(AbstractCityObject cityObject){

		HashMap<String, Object> cityObjectMap = new HashMap<String,Object>();

		//cityObject GmlID
		if (cityObject.isSetId()) {
			cityObjectMap.put("GMLID",cityObject.getId());
		}   	


		// gml:name
		if (cityObject.isSetName()) {
			String[] dbGmlName = Util.codeList2string(cityObject.getName());
			cityObjectMap.put("NAME",dbGmlName[0]);
		}

		// gml:description
		if (cityObject.isSetDescription()) {
			String description = cityObject.getDescription().getValue();
			if (description != null){
				description = description.trim();
			cityObjectMap.put("DESCRIPTION",description);
			}
		}

			   

		return cityObjectMap;
	}
	
	
	
	
}
