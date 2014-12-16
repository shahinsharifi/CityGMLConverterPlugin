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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;




import org.citydb.api.geometry.BoundingBoxCorner;
import org.citydb.log.Logger;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.geometry.coordinate.GeometryFactory;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.postgis.Geometry;
import org.postgis.PGgeometry;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;




public class ProjConvertor {


	public ProjConvertor() throws SQLException {
		
	}
	
	
	public static List<Double> transformPoint(double x,double y,double z,String _SourceSrs,String _TargetSrs) throws Exception{
		
		List<Double> points = new ArrayList<Double>();
		
		try {
			
		    CRSAuthorityFactory   factory = CRS.getAuthorityFactory(true);
	        CoordinateReferenceSystem srcCRS = factory.createCoordinateReferenceSystem("EPSG:" + _SourceSrs);
	        CoordinateReferenceSystem dstCRS = factory.createCoordinateReferenceSystem("EPSG:" + _TargetSrs);
	        
	        MathTransform transform = CRS.findMathTransform(srcCRS, dstCRS,false);

			com.vividsolutions.jts.geom.GeometryFactory geometryFactory = new com.vividsolutions.jts.geom.GeometryFactory();
			Coordinate coord = new Coordinate(x, y);
			Point sourcePoint = geometryFactory.createPoint(coord);
			Point targetGeometry = (Point)JTS.transform( sourcePoint, transform);
			
			
			points.add(targetGeometry.getY());
			points.add(targetGeometry.getX());
			points.add(z);
			
			
		} catch (Exception e) {
			points.add(0,0.0);
			points.add(1,0.0);
			points.add(2,0.0);
		}
		
	    return points;
	}
	
	
	public static  org.citydb.api.geometry.BoundingBox transformBBox( org.citydb.api.geometry.BoundingBox bbox, String sourceSrs, String targetSrs) throws SQLException {
		
		 org.citydb.api.geometry.BoundingBox result = new  org.citydb.api.geometry.BoundingBox();		

		try {
			
			
			
			Double xMin = bbox.getLowerLeftCorner().getX();
			Double yMin = bbox.getLowerLeftCorner().getY();
			Double xMax = bbox.getUpperRightCorner().getX();
			Double yMax = bbox.getUpperRightCorner().getY();
			
			
			List<Double> LowerCorner =  ProjConvertor.transformPoint(xMin, yMin, 0, sourceSrs, targetSrs);
			List<Double> UpperCorner =  ProjConvertor.transformPoint(xMax, yMax, 0, sourceSrs, targetSrs);
			
			result.setLowerLeftCorner(new BoundingBoxCorner(LowerCorner.get(1), LowerCorner.get(0)));
			result.setUpperRightCorner(new BoundingBoxCorner(UpperCorner.get(1),UpperCorner.get(0)));
			
		} catch (Exception Ex) {
			Logger.getInstance().error(Ex.toString());
		}
		
		return result;
	}


	
}
