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
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.namespace.QName;

import org.citydb.api.concurrent.PoolSizeAdaptationStrategy;
import org.citydb.api.concurrent.WorkerPool;
import org.citydb.api.event.Event;
import org.citydb.api.event.EventDispatcher;
import org.citydb.api.event.EventHandler;
import org.citydb.api.geometry.BoundingBoxCorner;
import org.citydb.api.registry.ObjectRegistry;
import org.citydb.config.Config;
import org.citydb.config.language.Language;
import org.citydb.io.DirectoryScanner;
import org.citydb.log.Logger;
import org.citydb.modules.common.event.*;
import org.citydb.modules.common.filter.ImportFilter;
import org.citydb.plugins.CityGMLConverter.concurrent.AppearanceWorkerFactory;
import org.citydb.plugins.CityGMLConverter.concurrent.BBoxCalculatorWorkerFactory;
import org.citydb.plugins.CityGMLConverter.concurrent.CityKmlExportWorkerFactory;
import org.citydb.plugins.CityGMLConverter.config.ConfigImpl;
import org.citydb.plugins.CityGMLConverter.config.Internal;
import org.citydb.plugins.CityGMLConverter.content.*;
import org.citydb.plugins.CityGMLConverter.util.rtree.CityObjectData;
import org.citydb.plugins.CityGMLConverter.util.rtree.RTree;
import org.citydb.plugins.CityGMLConverter.util.rtree.memory.MemoryPageStore;
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
import org.geotools.referencing.CRS;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;





public class BoundingBox implements EventHandler{

	private org.opengis.geometry.BoundingBox nativeBounds;
	private String SourceSRS;
	private WorkerPool<CityGML> bboxWorkerPool;
	private WorkerPool<CityGML> appearanceWorkerPool;
	private static DataDefinition definition = null;
	private static RTree tree = null;
	private EnumMap<CityGMLClass, Long>featureCounterMap = new EnumMap<CityGMLClass, Long>(CityGMLClass.class);
	private volatile boolean shouldRun = true;
	private AtomicBoolean isInterrupted = new AtomicBoolean(false);
	


	public BoundingBox() throws Exception{
				
	}
	
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


	
	private static com.vividsolutions.jts.geom.Point getCentroid(Envelope bounds) throws Exception
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
			String targetSrs) throws Exception {


        Logger.getInstance().info("Indexing the dataset ...");

        eventDispatcher.addEventHandler(EventType.COUNTER, this);
        eventDispatcher.addEventHandler(EventType.INTERRUPT, this);
        shouldRun = true;

        int availableCores = Runtime.getRuntime().availableProcessors();
        int minThreads = availableCores;//resources.getThreadPool().getDefaultPool().getMinThreads();
        int maxThreads = availableCores;//resources.getThreadPool().getDefaultPool().getMaxThreads();
        int queueSize = maxThreads * 2;


        definition = new DataDefinition("UTF-8");
        definition.addField(1);
        tree = new RTree(new MemoryPageStore(definition));


        org.citydb.api.geometry.BoundingBox bounds = new org.citydb.api.geometry.BoundingBox();

        bboxWorkerPool = new WorkerPool<CityGML>(
                "bboxWorkerPool",
                minThreads,
                maxThreads,
                PoolSizeAdaptationStrategy.AGGRESSIVE,
                new BBoxCalculatorWorkerFactory(
                        jaxbBuilder,
                        config,
                        targetSrs,
                        eventDispatcher),
                queueSize,
                false);


        appearanceWorkerPool = new WorkerPool<CityGML>(
                "appearanceWorkerPool",
                minThreads,
                maxThreads,
                PoolSizeAdaptationStrategy.AGGRESSIVE,
                new AppearanceWorkerFactory(
                        jaxbBuilder,
                        config,
                        eventDispatcher),
                queueSize,
                false);

        appearanceWorkerPool.prestartCoreWorkers();
        bboxWorkerPool.prestartCoreWorkers();

        // build list of files to be imported
        Internal intConfig = config.getInternal();
        DirectoryScanner directoryScanner = new DirectoryScanner(true);
        directoryScanner.addFilenameFilter(new DirectoryScanner.CityGMLFilenameFilter());
        List<File> importFiles = directoryScanner.getFiles(intConfig.getImportFiles());
        int fileCounter = 0;
        int remainingFiles = importFiles.size();
        Logger.getInstance().info("List of import files successfully created.");
        Logger.getInstance().info(remainingFiles + " file(s) will be indexed.");

        try {

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

                while (shouldRun && fileCounter < importFiles.size()) {

                    File file = importFiles.get(fileCounter++);
                    intConfig.setImportPath(file.getParent());
                    reader = in.createFilteredCityGMLReader(in.createCityGMLReader(file), inputFilter);
                    while (reader.hasNext() && shouldRun) {
                        XMLChunk chunk = reader.nextChunk();
                        CityGML cityGML = chunk.unmarshal();
                        eventDispatcher.triggerEvent(new CounterEvent(CounterType.TOPLEVEL_FEATURE, 1, this));

                        if (cityGML.getCityGMLClass() != CityGMLClass.APPEARANCE)
                            bboxWorkerPool.addWork(cityGML);
                        appearanceWorkerPool.addWork(cityGML);
                    }
                    if(remainingFiles > 1)
                        Logger.getInstance().info(--remainingFiles + " file(s) remained.");
                }

            } catch (CityGMLReadException e) {
                Logger.getInstance().error("Failed to parse CityGML file. Aborting.");
            } finally {
                bboxWorkerPool.join();
                appearanceWorkerPool.join();
                reader.close();
                eventDispatcher.flushEvents();
                bboxWorkerPool.shutdownAndWait();
                appearanceWorkerPool.shutdownAndWait();
            }

            //Calculating BoundingBox
            try {

                com.vividsolutions.jts.geom.Envelope envelope = null;
                if (isTreeAvailable())
                    envelope = tree.getBounds();

                if (envelope != null) {
                    List<Double> LowerCorner = ProjConvertor.transformPoint(envelope.getMinX(), envelope.getMinY(), 8.19, sourceSrs, targetSrs);
                    List<Double> UpperCorner = ProjConvertor.transformPoint(envelope.getMaxX(), envelope.getMaxY(), 8.19, sourceSrs, targetSrs);

                    bounds.setLowerLeftCorner(new BoundingBoxCorner(LowerCorner.get(1), LowerCorner.get(0)));
                    bounds.setUpperRightCorner(new BoundingBoxCorner(UpperCorner.get(1), UpperCorner.get(0)));
                }

            } catch (Exception ex) {
                Logger.getInstance().error(ex.toString());
            }


        } catch (CityGMLReadException e) {
            Logger.getInstance().error("Failed to calculate CityGML parser. Aborting.");

        }

        return bounds;

    }
	
	
	public static void addNodeToRtree(CityGML cityobject , Envelope bbox , String targetSrs)throws Exception{
		
		CityObjectData data = new CityObjectData(definition);
		data.addValue(cityobject);
		com.vividsolutions.jts.geom.Point centerPoint = getCentroid(bbox);
		ReferencedEnvelope tmpEnvelope=new ReferencedEnvelope(
				centerPoint.getX(),
				centerPoint.getX(),	
				centerPoint.getY(),							
				centerPoint.getY(),
				CRS.decode("EPSG:" + targetSrs, true));
				
		tree.insert(tmpEnvelope , data);		
	}
	
	
	
	public List<CityObjectData> SelectCityObject(org.citydb.api.geometry.BoundingBox bbox, String sourceSrs) throws Exception{
				
		org.citydb.api.geometry.BoundingBox tmpEnvelope = null;
		if(SourceSRS.equals("4326"))
		{
			tmpEnvelope = ProjConvertor.transformBBox(new org.citydb.api.geometry.BoundingBox(
					new BoundingBoxCorner(bbox.getLowerLeftCorner().getX(),bbox.getLowerLeftCorner().getY()),
					new BoundingBoxCorner(bbox.getUpperRightCorner().getX(),bbox.getUpperRightCorner().getY())),
					"4326",
					sourceSrs);
		}else{
			tmpEnvelope = bbox;
		}
		
		ReferencedEnvelope envelope = new ReferencedEnvelope(
				tmpEnvelope.getLowerLeftCorner().getX(),
				tmpEnvelope.getUpperRightCorner().getX(),	
				tmpEnvelope.getLowerLeftCorner().getY(),							
				tmpEnvelope.getUpperRightCorner().getY(),
				CRS.decode("EPSG:" + sourceSrs, true));

		List<CityObjectData> result = tree.search(envelope);
		return result;
	}
	
	
	
	public List<CityObjectData> SelectCityObject(String sourceSrs) throws Exception{

		org.citydb.api.geometry.BoundingBox tmpEnvelope  = new org.citydb.api.geometry.BoundingBox(
				new BoundingBoxCorner(this.nativeBounds.getMinX(),this.nativeBounds.getMinY()),
				new BoundingBoxCorner(this.nativeBounds.getMaxX(),this.nativeBounds.getMaxY()));
		
		return SelectCityObject(tmpEnvelope,sourceSrs);
	}
	
	
	public static boolean isTreeAvailable(){
		if(tree != null)
			return true;
		else {
			return false;
		}
	}
	
	
	@Override
	public void handleEvent(Event e) throws Exception {

		if (e.getEventType() == EventType.COUNTER && ((CounterEvent)e).getType() == CounterType.TOPLEVEL_FEATURE) {

			CityGMLClass type = null;
			Object cityObject = e.getSource();

			if (cityObject instanceof Building) {
				type = CityGMLClass.BUILDING;
			}
			else if (cityObject instanceof WaterBody) {
				type = CityGMLClass.WATER_BODY;
			}
			else if (cityObject instanceof LandUse) {
				type = CityGMLClass.LAND_USE;
			}
			else if (cityObject instanceof CityObjectGroup) {
				type = CityGMLClass.CITY_OBJECT_GROUP;
			}
			else if (cityObject instanceof Transportation) {
				type = CityGMLClass.TRANSPORTATION_COMPLEX;
			}
			else if (cityObject instanceof Relief) {
				type = CityGMLClass.RELIEF_FEATURE;
			}
			else if (cityObject instanceof Vegetation) {
				type = CityGMLClass.SOLITARY_VEGETATION_OBJECT;
			}
			else if (cityObject instanceof PlantCover) {
				type = CityGMLClass.PLANT_COVER;
			}
			else if (cityObject instanceof GenericCityObject) {
				type = CityGMLClass.GENERIC_CITY_OBJECT;
			}
			else if (cityObject instanceof CityFurniture) {
				type = CityGMLClass.CITY_FURNITURE;
			}
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
			
			
			if (bboxWorkerPool != null) {
				bboxWorkerPool.shutdownNow();
			}
		}
	}

}
