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
package org.citydb.plugins.CityGMLConverter.util;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.namespace.QName;

import org.citydb.api.concurrent.PoolSizeAdaptationStrategy;
import org.citydb.api.concurrent.WorkerPool;
import org.citydb.api.event.Event;
import org.citydb.api.event.EventDispatcher;
import org.citydb.api.event.EventHandler;
import org.citydb.log.Logger;
import org.citydb.modules.common.event.CounterEvent;
import org.citydb.modules.common.event.CounterType;
import org.citydb.modules.common.event.EventType;
import org.citydb.plugins.CityGMLConverter.concurrent.AppearanceWorkerFactory;
import org.citydb.plugins.CityGMLConverter.concurrent.BBoxCalculatorWorkerFactory;
import org.citydb.plugins.CityGMLConverter.config.ConfigImpl;
import org.citydb.plugins.CityGMLConverter.content.Building;
import org.citydb.plugins.CityGMLConverter.util.rtree.RTree;
import org.citydb.plugins.CityGMLConverter.util.rtree.memory.MemoryPageStore;
import org.citygml4j.CityGMLContext;
import org.citygml4j.builder.CityGMLBuilder;
import org.citygml4j.builder.jaxb.JAXBBuilder;
import org.citygml4j.model.citygml.CityGML;
import org.citygml4j.model.citygml.CityGMLClass;
import org.citygml4j.model.citygml.appearance.Appearance;
import org.citygml4j.model.citygml.appearance.AppearanceMember;
import org.citygml4j.model.citygml.appearance.AppearanceProperty;
import org.citygml4j.model.citygml.building.AbstractBuilding;
import org.citygml4j.model.citygml.building.BuildingPart;
import org.citygml4j.model.citygml.building.BuildingPartProperty;
import org.citygml4j.model.citygml.core.AbstractCityObject;
import org.citygml4j.model.citygml.core.CityModel;
import org.citygml4j.model.citygml.core.CityObjectMember;
import org.citygml4j.xml.io.CityGMLInputFactory;
import org.citygml4j.xml.io.reader.CityGMLInputFilter;
import org.citygml4j.xml.io.reader.CityGMLReadException;
import org.citygml4j.xml.io.reader.CityGMLReader;
import org.citygml4j.xml.io.reader.FeatureReadMode;
import org.citygml4j.xml.io.reader.XMLChunk;
import org.geotools.index.DataDefinition;


public class ThemeUtil implements EventHandler{

	private static List<Object> tmpAppearanceList = new ArrayList<Object>();
	private WorkerPool<CityGML> appearanceWorkerPool;
	private EnumMap<CityGMLClass, Long>featureCounterMap = new EnumMap<CityGMLClass, Long>(CityGMLClass.class);
	private volatile boolean shouldRun = true;
	private AtomicBoolean isInterrupted = new AtomicBoolean(false);

	public  List<String> getAppearanceThemeList(JAXBBuilder jaxbBuilder, File file) throws Exception {

		ArrayList<String> appearanceThemes = new ArrayList<String>();
		final String THEME_UNKNOWN = "<unknown>";
		try {

			if(ThemeUtil.tmpAppearanceList.size() > 0){
				for(Object app : ThemeUtil.tmpAppearanceList){
					
					String THEME_VALID = "";
					if(app.getClass() == AppearanceProperty.class)
						THEME_VALID = ((AppearanceProperty)app).getAppearance().getTheme();		
					else{
						THEME_VALID = ((Appearance)app).getTheme();
					}
					if(THEME_VALID != null){
						if(!appearanceThemes.contains(THEME_VALID))
							appearanceThemes.add(THEME_VALID);
					}else {
						if(!appearanceThemes.contains(THEME_UNKNOWN))
							appearanceThemes.add(THEME_UNKNOWN);
					}
				}
			}


		} catch (Exception Ex) {
			Logger.getInstance().error(Ex.toString());
		} finally {
			
		}

		return appearanceThemes;
	}

	

	public static List<Object> getTmpAppearanceList() {
		return tmpAppearanceList;
	}


	@Override
	public void handleEvent(Event e) throws Exception {

		if (e.getEventType() == EventType.COUNTER && ((CounterEvent)e).getType() == CounterType.TOPLEVEL_FEATURE) {

			CityGMLClass type = null;
			Object cityObject = e.getSource();

			if (cityObject instanceof Building) {
				type = CityGMLClass.BUILDING;
			}
			/*else if (kmlExportObject instanceof WaterBody) {
				type = CityGMLClass.WATER_BODY;
			}
			else if (kmlExportObject instanceof LandUse) {
				type = CityGMLClass.LAND_USE;
			}
			else if (kmlExportObject instanceof CityObjectGroup) {
				type = CityGMLClass.CITY_OBJECT_GROUP;
			}
			else if (kmlExportObject instanceof Transportation) {
				type = CityGMLClass.TRANSPORTATION_COMPLEX;
			}
			else if (kmlExportObject instanceof Relief) {
				type = CityGMLClass.RELIEF_FEATURE;
			}
			else if (kmlExportObject instanceof Vegetation) {
				type = CityGMLClass.SOLITARY_VEGETATION_OBJECT;
			}
			else if (kmlExportObject instanceof PlantCover) {
				type = CityGMLClass.PLANT_COVER;
			}
			else if (kmlExportObject instanceof GenericCityObject) {
				type = CityGMLClass.GENERIC_CITY_OBJECT;
			}
			else if (kmlExportObject instanceof CityFurniture) {
				type = CityGMLClass.CITY_FURNITURE;
			}*/
			else
				return;

			Long counter = featureCounterMap.get(type);
			Long update = ((CounterEvent)e).getCounter();

			if (counter == null)
				featureCounterMap.put(type, update);
			else
				featureCounterMap.put(type, counter + update);
		}
		else if (e.getEventType() == EventType.INTERRUPT) {
			if (isInterrupted.compareAndSet(false, true)) {
				shouldRun = false;
			}
			
			if (appearanceWorkerPool != null) {
				appearanceWorkerPool.shutdownNow();
			}
		}
	}
	
	
}
