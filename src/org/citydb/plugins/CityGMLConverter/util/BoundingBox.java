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
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;

import org.citydb.api.concurrent.PoolSizeAdaptationStrategy;
import org.citydb.api.concurrent.WorkerPool;
import org.citydb.api.event.EventDispatcher;
import org.citydb.api.geometry.BoundingBoxCorner;
import org.citydb.config.Config;
import org.citydb.log.Logger;
import org.citydb.modules.common.filter.ImportFilter;
import org.citydb.plugins.CityGMLConverter.concurrent.BBoxCalculatorWorkerFactory;
import org.citydb.plugins.CityGMLConverter.concurrent.CityKmlExportWorkerFactory;
import org.citydb.plugins.CityGMLConverter.config.ConfigImpl;
import org.citydb.plugins.CityGMLConverter.content.KmlSplittingResult;
import org.citygml4j.CityGMLContext;
import org.citygml4j.builder.CityGMLBuilder;
import org.citygml4j.builder.jaxb.JAXBBuilder;
import org.citygml4j.builder.jaxb.xml.io.reader.JAXBChunkReader;
import org.citygml4j.model.citygml.CityGML;
import org.citygml4j.model.citygml.CityGMLClass;
import org.citygml4j.model.citygml.core.AbstractCityObject;
import org.citygml4j.model.citygml.core.CityModel;
import org.citygml4j.model.citygml.core.CityObjectMember;
import org.citygml4j.model.gml.feature.AbstractFeature;
import org.citygml4j.model.gml.feature.BoundingShape;
import org.citygml4j.xml.io.CityGMLInputFactory;
import org.citygml4j.xml.io.reader.CityGMLInputFilter;
import org.citygml4j.xml.io.reader.CityGMLReadException;
import org.citygml4j.xml.io.reader.CityGMLReader;
import org.citygml4j.xml.io.reader.FeatureReadMode;
import org.citygml4j.xml.io.reader.XMLChunk;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.index.Data;
import org.geotools.index.DataDefinition;
import org.geotools.index.TreeException;
import org.geotools.index.rtree.Entry;
import org.geotools.index.rtree.Node;
import org.geotools.index.rtree.PageStore;
import org.geotools.index.rtree.RTree;
import org.geotools.index.rtree.memory.MemoryPageStore;
import org.geotools.referencing.CRS;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;





public class BoundingBox {

	private org.opengis.geometry.BoundingBox nativeBounds;
	private String SourceSRS;
	private WorkerPool<CityGML> bboxWorkerPool;
	private  DataDefinition definition = null;
	private static RTree tree = null;
	private static ConcurrentHashMap<String, CityGML> cityMap= null;
	


	public BoundingBox() throws Exception{
		definition = new DataDefinition("UTF-8");
		definition.addField(1);
		tree = new RTree(new MemoryPageStore(definition));		
		
	}
	
	public BoundingBox(double MinX, double MinY, double MaxX, double MaxY, String EPSG) throws Exception {

		SourceSRS = EPSG;
		CoordinateReferenceSystem nativeCrs = CRS.decode("EPSG:" + EPSG, true);
		nativeBounds = new ReferencedEnvelope(MinX, MaxX, MinY, MaxY, nativeCrs);
		
		definition = new DataDefinition("UTF-8");
		definition.addField(CityGML.class);
		tree = new RTree(new MemoryPageStore(definition));		
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


	
	private com.vividsolutions.jts.geom.Point getCentroid(Envelope bounds) throws Exception
	{
		Geometry _buildingPolygon = JTS.toGeometry((org.opengis.geometry.BoundingBox)bounds);		
		return _buildingPolygon.getCentroid();
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


	public  org.citydb.api.geometry.BoundingBox BboxCalculator(
			JAXBBuilder jaxbBuilder,
			ConfigImpl config,
			EventDispatcher eventDispatcher,
			File sourceFile,
			String sourceSrs,
			String targetSrs) throws Exception
	{	
		
		double Xmin = 0,
				Xmax = 0,
				Ymin = 0,
				Ymax = 0;
		
		cityMap = new  ConcurrentHashMap<String, CityGML>();

		org.citydb.api.geometry.BoundingBox bounds = new org.citydb.api.geometry.BoundingBox();
		bboxWorkerPool = new WorkerPool<CityGML>(
				"bboxWorkerPool",
				2,
				8,
				PoolSizeAdaptationStrategy.AGGRESSIVE,
				new BBoxCalculatorWorkerFactory(
						jaxbBuilder,
						config,
						eventDispatcher),
						300,
						false);
		

		try {

			boolean IsFirstTime = true;

			CityGMLInputFactory in = null;
			try {
				in = jaxbBuilder.createCityGMLInputFactory();
				in.setProperty(CityGMLInputFactory.FEATURE_READ_MODE, FeatureReadMode.SPLIT_PER_COLLECTION_MEMBER);
				in.setProperty(CityGMLInputFactory.FAIL_ON_MISSING_ADE_SCHEMA, false);
				in.setProperty(CityGMLInputFactory.PARSE_SCHEMA, false);
				in.setProperty(CityGMLInputFactory.SPLIT_AT_FEATURE_PROPERTY, new QName("generalizesTo"));
				in.setProperty(CityGMLInputFactory.EXCLUDE_FROM_SPLITTING, CityModel.class);
			} catch (CityGMLReadException e) {
				//throw new CityGMLImportException("Failed to initialize CityGML parser. Aborting.", e);
			}



			CityGMLInputFilter inputFilter = new CityGMLInputFilter() {
				public boolean accept(CityGMLClass type) {
					return true;
				}
			};

			CityGMLReader reader = null;
			try {
				ReferencedEnvelope _tmpEnvelope = null;
				reader = in.createFilteredCityGMLReader(in.createCityGMLReader(sourceFile), inputFilter);
				while (reader.hasNext()) {
					XMLChunk chunk = reader.nextChunk();
					CityGML cityGML = chunk.unmarshal();
					if(cityGML.getCityGMLClass() == CityGMLClass.APPEARANCE)
						break;
					AbstractCityObject cityObject = (AbstractCityObject)cityGML;
					org.citygml4j.model.gml.geometry.primitives.Envelope envelope = null;
			//		bboxWorkerPool.addWork(cityObject);		
					if(cityObject.isSetBoundedBy())
					{
						AbstractFeature feature = (AbstractFeature)cityObject;
						try{
							BoundingShape bShape = feature.calcBoundedBy(false);
							if(bShape.isSetEnvelope())
								envelope = bShape.getEnvelope();
						}catch(Exception ex){
						
						}
					}else 
					{
						
						AbstractFeature feature = (AbstractFeature)cityObject;
						try{
							BoundingShape bShape = feature.calcBoundedBy(false);
							if(bShape.isSetEnvelope())
								envelope = bShape.getEnvelope();
						}catch(Exception ex){
						
						}
					}
					
					if(envelope!=null){
						

												
						ReferencedEnvelope _refEnvelope = new ReferencedEnvelope(
								envelope.getLowerCorner().toList3d().get(0),
								envelope.getUpperCorner().toList3d().get(0),	
								envelope.getLowerCorner().toList3d().get(1),							
								envelope.getUpperCorner().toList3d().get(1),
								CRS.decode("EPSG:" + targetSrs, true));
						
						
						setRtree(cityGML, _refEnvelope, targetSrs);
						
					//	cityMap.put(cityObject.getId(), cityObject);
						
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

				String inputs = "13.3910251,52.5409649,13.3995438,52.5453692";
				String[] coor = inputs.split(",");
				org.citydb.api.geometry.BoundingBox tmpEnvelope=ProjConvertor.transformBBox(new org.citydb.api.geometry.BoundingBox(
						new BoundingBoxCorner(Double.parseDouble(coor[0]),Double.parseDouble(coor[1])),
						new BoundingBoxCorner(Double.parseDouble(coor[2]),Double.parseDouble(coor[3]))
						),
						"4326",
						"25833");
				
				
				_tmpEnvelope = new ReferencedEnvelope(
						tmpEnvelope.getLowerLeftCorner().getX(),
						tmpEnvelope.getUpperRightCorner().getX(),	
						tmpEnvelope.getLowerLeftCorner().getY(),							
						tmpEnvelope.getUpperRightCorner().getY(),
						CRS.decode("EPSG:" + targetSrs, true));

				double st = System.currentTimeMillis();				
				List<Data> result = tree.search(_tmpEnvelope);
				double en=System.currentTimeMillis();
				System.out.println(en-st);				
				System.out.println(result.size());
				for(Data dt:result)
					System.out.println(((CityGML)dt.getValue(0)).getCityGMLClass().name());
			//	bboxWorkerPool.join();
			} catch (CityGMLReadException e) {
				//throw new CityGMLImportException("Failed to parse CityGML file. Aborting.", e);
			}
			finally{
				reader.close();
			}
			
		
			List<Double> LowerCorner =  ProjConvertor.transformPoint(Xmin, Ymin, 8.19, sourceSrs, targetSrs);
			List<Double> UpperCorner =  ProjConvertor.transformPoint(Xmax, Ymax, 8.19, sourceSrs, targetSrs);

			bounds.setLowerLeftCorner(new BoundingBoxCorner(LowerCorner.get(1), LowerCorner.get(0)));
			bounds.setUpperRightCorner(new BoundingBoxCorner(UpperCorner.get(1),UpperCorner.get(0)));


		} catch (CityGMLReadException e) {
			Logger.getInstance().error("Failed to calculate CityGML parser. Aborting.");

		}		
		return bounds;

	}
	
	
	public void setRtree(CityGML cityobject , Envelope bbox ,String targetSrs)throws Exception{
		
		Data _data = new Data(definition);
		_data.addValue(String.valueOf(cityobject));
		
		com.vividsolutions.jts.geom.Point centerPoint = getCentroid(bbox);
		
		ReferencedEnvelope tmpEnvelope=new ReferencedEnvelope(
				centerPoint.getX(),
				centerPoint.getX(),	
				centerPoint.getY(),							
				centerPoint.getY(),
				CRS.decode("EPSG:" + targetSrs, true));

		
		tree.insert(tmpEnvelope , _data);		
	}
	

}
