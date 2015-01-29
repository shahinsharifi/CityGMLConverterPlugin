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
import java.util.List;

import javax.xml.namespace.QName;

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


public class DSUtil {


	public static List<String> getAppearanceThemeList(JAXBBuilder jaxbBuilder , File file) throws Exception {


		CityGMLReader reader = null;
		CityGMLInputFactory in = null;		

		ArrayList<String> appearanceThemes = new ArrayList<String>();
		List<Object> tmpAppearanceList = new ArrayList<Object>();
		final String THEME_UNKNOWN = "<unknown>";


		try {

			CityGMLInputFilter inputFilter = new CityGMLInputFilter() {
				public boolean accept(CityGMLClass type) {
					return true;
				}
			};

			in = jaxbBuilder.createCityGMLInputFactory();
			in.setProperty(CityGMLInputFactory.FEATURE_READ_MODE, FeatureReadMode.SPLIT_PER_COLLECTION_MEMBER);
			in.setProperty(CityGMLInputFactory.FAIL_ON_MISSING_ADE_SCHEMA, false);
			in.setProperty(CityGMLInputFactory.PARSE_SCHEMA, false);
			in.setProperty(CityGMLInputFactory.SPLIT_AT_FEATURE_PROPERTY, new QName("generalizesTo"));
			in.setProperty(CityGMLInputFactory.EXCLUDE_FROM_SPLITTING, CityModel.class);
			reader = in.createCityGMLReader(file);

			while (reader.hasNext()) {
				XMLChunk chunk = reader.nextChunk();
				CityGML cityGML = chunk.unmarshal();

				if(cityGML.getCityGMLClass() == CityGMLClass.APPEARANCE)
				{
					Appearance _appreance = (Appearance)cityGML;
					tmpAppearanceList.add(_appreance);

				}
				else if(cityGML.getCityGMLClass() == CityGMLClass.BUILDING){

					AbstractCityObject cityObject = (AbstractCityObject)cityGML;

					if (cityObject!=null) {					
						if(cityObject.isSetAppearance())
							tmpAppearanceList.addAll(cityObject.getAppearance());
						else {

							AbstractBuilding building = (AbstractBuilding)cityObject;
							if(building.isSetConsistsOfBuildingPart())
							{
								for(BuildingPartProperty buidingPart : building.getConsistsOfBuildingPart())
								{
									BuildingPart tmpBuildingPart = buidingPart.getBuildingPart();
									if(tmpBuildingPart.isSetAppearance())
									{	
										tmpAppearanceList.addAll(tmpBuildingPart.getAppearance());
										break;
									}
								}
								break;
							}
						}
					}
				}
				else
				{
					//this part will be added in the future.
				}
			}

			if(tmpAppearanceList.size() == 0){
				CityModel cityModel = (CityModel)reader.nextFeature();			

				if(cityModel.isSetCityObjectMember()){				
					for (CityObjectMember member : cityModel.getCityObjectMember()) {
						if (member.isSetCityObject()) 
							tmpAppearanceList.addAll(cityModel.getAppearanceMember());
					}
				}
			}

			if(tmpAppearanceList.size() > 0){
				for(Object app : tmpAppearanceList){
					
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
			throw Ex;
		} finally {
			reader.close();
		}

		return appearanceThemes;
	}


}
