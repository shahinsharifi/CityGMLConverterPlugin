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
import java.util.List;

import javax.xml.namespace.QName;

import org.citydb.api.geometry.BoundingBoxCorner;
import org.citydb.log.Logger;
import org.citygml4j.CityGMLContext;
import org.citygml4j.builder.CityGMLBuilder;
import org.citygml4j.builder.jaxb.JAXBBuilder;
import org.citygml4j.builder.jaxb.xml.io.reader.JAXBChunkReader;
import org.citygml4j.model.citygml.CityGML;
import org.citygml4j.model.citygml.CityGMLClass;
import org.citygml4j.model.citygml.core.AbstractCityObject;
import org.citygml4j.model.citygml.core.CityModel;
import org.citygml4j.model.citygml.core.CityObjectMember;
import org.citygml4j.xml.io.CityGMLInputFactory;
import org.citygml4j.xml.io.reader.CityGMLInputFilter;
import org.citygml4j.xml.io.reader.CityGMLReadException;
import org.citygml4j.xml.io.reader.CityGMLReader;
import org.citygml4j.xml.io.reader.FeatureReadMode;
import org.citygml4j.xml.io.reader.XMLChunk;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;





public class BoundingBox {

	private org.opengis.geometry.BoundingBox nativeBounds;
	private String SourceSRS;


	public BoundingBox(double MinX, double MinY, double MaxX, double MaxY, String EPSG) throws Exception {

		SourceSRS = EPSG;
		CoordinateReferenceSystem nativeCrs = CRS.decode("EPSG:" + EPSG, true);
		nativeBounds = new ReferencedEnvelope(MinX, MaxX, MinY, MaxY, nativeCrs);

	}



	public boolean Contains(Envelope bounds)
	{

		if(this.nativeBounds.intersects((org.opengis.geometry.BoundingBox)bounds))
		{
			return true;
		}
		else {
			return false;
		}

	}




	public double OverlapArea(Envelope bounds)
	{	
		Geometry _buildingPolygon = JTS.toGeometry((org.opengis.geometry.BoundingBox)bounds);

		Geometry _nativePolygon = JTS.toGeometry(this.nativeBounds);		
		Polygon _overlapArea = (Polygon)_nativePolygon.intersection(_buildingPolygon);
		double TargetArea = _overlapArea.getArea();
		double BuildingArea = _buildingPolygon.getArea();

		return (TargetArea/BuildingArea)*100;		
	}




	public boolean ContainCentroid(Envelope bounds, String targetSRS) throws Exception
	{					
		Geometry _buildingPolygon = JTS.toGeometry((org.opengis.geometry.BoundingBox)bounds);		
		return ContainPoint(_buildingPolygon.getCentroid(),targetSRS);		
	}




	private boolean ContainPoint(com.vividsolutions.jts.geom.Point _point, String targetSRS) throws Exception
	{	
		double pointX = 0.0, pointY = 0.0;

		if(SourceSRS.equals("4326"))
		{
			List<Double> tmpPoint = ProjConvertor.transformPoint(_point.getX(), _point.getY(), 0, targetSRS, SourceSRS);//We should convert the CRS of the building's center point to the CRS of BoundingBox. 
			pointX = tmpPoint.get(1);
			pointY = tmpPoint.get(0);					
		}
		else {			
			pointX = _point.getX();
			pointY = _point.getY();				
		}

		if(Double.compare(pointX,this.nativeBounds.getMaxX()) < 0 && Double.compare(pointX,this.nativeBounds.getMinX()) > 0 && 			
				Double.compare(pointY,this.nativeBounds.getMaxY()) < 0 && Double.compare(pointY,this.nativeBounds.getMinY()) > 0)
			return true;		
		else 
			return false;						
	}


	public static org.citydb.api.geometry.BoundingBox BboxCalculator(JAXBBuilder jaxbBuilder, File sourceFile,String sourceSrs,String targetSrs) throws Exception
	{	

		double Xmin = 0,
				Xmax = 0,
				Ymin = 0,
				Ymax = 0;

		org.citydb.api.geometry.BoundingBox bounds = new org.citydb.api.geometry.BoundingBox();

		try {



			CityGMLContext ctx = new CityGMLContext();
			CityGMLBuilder builder = ctx.createCityGMLBuilder();
			CityGMLInputFactory in = builder.createCityGMLInputFactory();
			CityGMLReader reader = in.createCityGMLReader(sourceFile);
			CityModel cityModel = (CityModel)reader.nextFeature();


			if(cityModel.getBoundedBy() != null)
			{
				org.citygml4j.model.gml.geometry.primitives.Envelope envelope = cityModel.getBoundedBy().getEnvelope().convert3d();
				Xmin =  envelope.getLowerCorner().toList3d().get(0);
				Xmax =  envelope.getUpperCorner().toList3d().get(0);
				Ymin =  envelope.getLowerCorner().toList3d().get(1);
				Ymax =  envelope.getUpperCorner().toList3d().get(1);									
			}				
			else
			{
				boolean IsFirstTime = true;

				CityGMLReader _ChunkReader = in.createCityGMLReader(sourceFile);
				CityModel _cityModel = (CityModel)_ChunkReader.nextFeature();

				if(_cityModel.isSetCityObjectMember()){

					for (CityObjectMember member : _cityModel.getCityObjectMember()) {

						if (member.isSetCityObject()) {

							AbstractCityObject cityObject = member.getCityObject();

							org.citygml4j.model.gml.geometry.primitives.Envelope envelope = cityObject.getBoundedBy().getEnvelope().convert3d();
							if(IsFirstTime)
							{
								Xmin =  envelope.getLowerCorner().toList3d().get(0);
								Xmax =  envelope.getUpperCorner().toList3d().get(0);
								Ymin =  envelope.getLowerCorner().toList3d().get(1);
								Ymax =  envelope.getUpperCorner().toList3d().get(1);
								IsFirstTime = false;					
							}
							else {

								Xmin = (Xmin < envelope.getLowerCorner().toList3d().get(0)) ? Xmin : envelope.getLowerCorner().toList3d().get(0);
								Xmax = (Xmax > envelope.getUpperCorner().toList3d().get(0)) ? Xmax : envelope.getUpperCorner().toList3d().get(0);
								Ymin = (Ymin < envelope.getLowerCorner().toList3d().get(1)) ? Ymin : envelope.getLowerCorner().toList3d().get(1);
								Ymax = (Ymax > envelope.getUpperCorner().toList3d().get(1)) ? Ymax : envelope.getUpperCorner().toList3d().get(1);					
							}
						}
					}
				}


			}


			List<Double> LowerCorner =  ProjConvertor.transformPoint(Xmin, Ymin, 0, sourceSrs, targetSrs);
			List<Double> UpperCorner =  ProjConvertor.transformPoint(Xmax, Ymax, 0, sourceSrs, targetSrs);

			bounds.setLowerLeftCorner(new BoundingBoxCorner(LowerCorner.get(1), LowerCorner.get(0)));
			bounds.setUpperRightCorner(new BoundingBoxCorner(UpperCorner.get(1),UpperCorner.get(0)));


		} catch (CityGMLReadException e) {
			Logger.getInstance().error("Failed to calculate CityGML parser. Aborting.");

		}		
		return bounds;

	}

}
