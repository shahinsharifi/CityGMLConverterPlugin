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
package org.citydb.plugins.CityGMLConverter.concurrent;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.bind.JAXBContext;

import net.opengis.kml._2.ObjectFactory;

import org.citygml4j.factory.GMLGeometryFactory;
import org.citygml4j.model.citygml.CityGMLClass;
import org.citygml4j.util.xml.SAXEventBuffer;
import org.citydb.api.concurrent.Worker;
import org.citydb.api.concurrent.WorkerPool;
import org.citydb.api.concurrent.WorkerPool.WorkQueue;
import org.citydb.api.database.DatabaseAdapter;
import org.citydb.api.database.DatabaseType;
import org.citydb.api.event.EventDispatcher;
import org.citydb.config.project.exporter.ExportFilterConfig;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
import org.citydb.database.adapter.BlobExportAdapter;
import org.citydb.database.adapter.BlobType;
import org.citydb.database.adapter.DatabaseAdapterFactory;
import org.citydb.plugins.CityGMLConverter.config.Balloon;
import org.citydb.plugins.CityGMLConverter.config.BalloonContentMode;
import org.citydb.plugins.CityGMLConverter.config.ColladaOptions;
import org.citydb.plugins.CityGMLConverter.config.ConfigImpl;
import org.citydb.plugins.CityGMLConverter.config.DisplayForm;
import org.citydb.plugins.CityGMLConverter.content.BalloonTemplateHandlerImpl;
import org.citydb.plugins.CityGMLConverter.content.Building;
import org.citydb.plugins.CityGMLConverter.content.CityObjectGroup;
import org.citydb.plugins.CityGMLConverter.content.ColladaBundle;
import org.citydb.plugins.CityGMLConverter.content.ElevationServiceHandler;
import org.citydb.plugins.CityGMLConverter.content.KmlExporterManager;
import org.citydb.plugins.CityGMLConverter.content.KmlGenericObject;
import org.citydb.plugins.CityGMLConverter.content.KmlSplittingResult;
import org.citydb.plugins.CityGMLConverter.util.Sqlite.SqliteImporterManager;
import org.citydb.plugins.CityGMLConverter.xlink.content.DBXlink;
import org.citydb.plugins.CityGMLConverter.xlink.importer.DBXlinkImporterManager;

public class CityKmlExportWorker implements Worker<KmlSplittingResult> {

	// instance members needed for WorkPool
	private volatile boolean shouldRun = true;
	private ReentrantLock runLock = new ReentrantLock();
	private WorkQueue<KmlSplittingResult> workQueue = null;
	private WorkerPool<DBXlink> tmpXlinkPool;
	private KmlSplittingResult firstWork;
	private Thread workerThread = null;

	// instance members needed to do work
	private final ObjectFactory kmlFactory; 
	private final GMLGeometryFactory cityGMLFactory; 
	private BlobExportAdapter textureExportAdapter;
	private final ConfigImpl config;
	private final EventDispatcher eventDispatcher;

	private Connection connection;
	private ExportFilterConfig filterConfig;
	private KmlExporterManager kmlExporterManager;
	private SqliteImporterManager sqliteImporterManager;

	private KmlGenericObject singleObject = null;

	private EnumMap<CityGMLClass, Integer>objectGroupCounter = new EnumMap<CityGMLClass, Integer>(CityGMLClass.class);
	private EnumMap<CityGMLClass, Integer>objectGroupSize = new EnumMap<CityGMLClass, Integer>(CityGMLClass.class);
	private EnumMap<CityGMLClass, KmlGenericObject>objectGroup = new EnumMap<CityGMLClass, KmlGenericObject>(CityGMLClass.class);
	private EnumMap<CityGMLClass, BalloonTemplateHandlerImpl>balloonTemplateHandler = new EnumMap<CityGMLClass, BalloonTemplateHandlerImpl>(CityGMLClass.class);

	private ElevationServiceHandler elevationServiceHandler;

	public CityKmlExportWorker(JAXBContext jaxbKmlContext,
			JAXBContext jaxbColladaContext,
			WorkerPool<SAXEventBuffer> ioWriterPool,
			WorkerPool<DBXlink> tmpXlinkPool,
			ObjectFactory kmlFactory,
			GMLGeometryFactory cityGMLFactory,
			ConfigImpl config,
			EventDispatcher eventDispatcher) throws SQLException {
		this.kmlFactory = kmlFactory;
		this.cityGMLFactory = cityGMLFactory;
		this.config = config;
		this.eventDispatcher = eventDispatcher;



		kmlExporterManager = new KmlExporterManager(jaxbKmlContext,
													jaxbColladaContext,
													ioWriterPool,
													kmlFactory,
													config);
		
		sqliteImporterManager = new SqliteImporterManager(tmpXlinkPool);

		elevationServiceHandler = new ElevationServiceHandler();

		filterConfig = config.getFilter();
		ColladaOptions colladaOptions = null; 

		objectGroupCounter.put(CityGMLClass.BUILDING, 0);
		objectGroupSize.put(CityGMLClass.BUILDING, 1);
		objectGroup.put(CityGMLClass.BUILDING, null);
		if (filterConfig.getComplexFilter().getFeatureClass().isSetBuilding()) {
			colladaOptions = config.getBuildingColladaOptions();
			if (colladaOptions.isGroupObjects()) {
				objectGroupSize.put(CityGMLClass.BUILDING, colladaOptions.getGroupSize());
			}
		}

		objectGroupCounter.put(CityGMLClass.WATER_BODY, 0);
		objectGroupSize.put(CityGMLClass.WATER_BODY, 1);
		objectGroup.put(CityGMLClass.WATER_BODY, null);
		if (filterConfig.getComplexFilter().getFeatureClass().isSetWaterBody()) {
			colladaOptions = config.getWaterBodyColladaOptions();
			if (colladaOptions.isGroupObjects()) {
				objectGroupSize.put(CityGMLClass.WATER_BODY, colladaOptions.getGroupSize());
			}
		}

		objectGroupCounter.put(CityGMLClass.LAND_USE, 0);
		objectGroupSize.put(CityGMLClass.LAND_USE, 1);
		objectGroup.put(CityGMLClass.LAND_USE, null);
		if (filterConfig.getComplexFilter().getFeatureClass().isSetLandUse()) {
			colladaOptions = config.getLandUseColladaOptions();
			if (colladaOptions.isGroupObjects()) {
				objectGroupSize.put(CityGMLClass.LAND_USE, colladaOptions.getGroupSize());
			}
		}

		objectGroupCounter.put(CityGMLClass.SOLITARY_VEGETATION_OBJECT, 0);
		objectGroupSize.put(CityGMLClass.SOLITARY_VEGETATION_OBJECT, 1);
		objectGroup.put(CityGMLClass.SOLITARY_VEGETATION_OBJECT, null);
		if (filterConfig.getComplexFilter().getFeatureClass().isSetVegetation()) {
			colladaOptions = config.getVegetationColladaOptions();
			if (colladaOptions.isGroupObjects()) {
				objectGroupSize.put(CityGMLClass.SOLITARY_VEGETATION_OBJECT, colladaOptions.getGroupSize());
			}
		}

		objectGroupCounter.put(CityGMLClass.TRANSPORTATION_COMPLEX, 0);
		objectGroupSize.put(CityGMLClass.TRANSPORTATION_COMPLEX, 1);
		objectGroup.put(CityGMLClass.TRANSPORTATION_COMPLEX, null);
		if (filterConfig.getComplexFilter().getFeatureClass().isSetTransportation()) {
			colladaOptions = config.getTransportationColladaOptions();
			if (colladaOptions.isGroupObjects()) {
				objectGroupSize.put(CityGMLClass.TRANSPORTATION_COMPLEX, colladaOptions.getGroupSize());
			}
		}

		objectGroupCounter.put(CityGMLClass.RELIEF_FEATURE, 0);
		objectGroupSize.put(CityGMLClass.RELIEF_FEATURE, 1);
		objectGroup.put(CityGMLClass.RELIEF_FEATURE, null);
		if (filterConfig.getComplexFilter().getFeatureClass().isSetReliefFeature()) {
			colladaOptions = config.getReliefColladaOptions();
			if (colladaOptions.isGroupObjects()) {
				objectGroupSize.put(CityGMLClass.RELIEF_FEATURE, colladaOptions.getGroupSize());
			}
		}

		objectGroupCounter.put(CityGMLClass.GENERIC_CITY_OBJECT, 0);
		objectGroupSize.put(CityGMLClass.GENERIC_CITY_OBJECT, 1);
		objectGroup.put(CityGMLClass.GENERIC_CITY_OBJECT, null);
		if (filterConfig.getComplexFilter().getFeatureClass().isSetGenericCityObject()) {
			colladaOptions = config.getGenericCityObjectColladaOptions();
			if (colladaOptions.isGroupObjects()) {
				objectGroupSize.put(CityGMLClass.GENERIC_CITY_OBJECT, colladaOptions.getGroupSize());
			}
		}

		objectGroupCounter.put(CityGMLClass.CITY_FURNITURE, 0);
		objectGroupSize.put(CityGMLClass.CITY_FURNITURE, 1);
		objectGroup.put(CityGMLClass.CITY_FURNITURE, null);
		if (filterConfig.getComplexFilter().getFeatureClass().isSetCityFurniture()) {
			colladaOptions = config.getCityFurnitureColladaOptions();
			if (colladaOptions.isGroupObjects()) {
				objectGroupSize.put(CityGMLClass.CITY_FURNITURE, colladaOptions.getGroupSize());
			}
		}
		// CityGMLClass.CITY_OBJECT_GROUP is left out, it does not make sense to group it without COLLADA DisplayForm 
	}


	@Override
	public Thread getThread() {
		return workerThread;
	}

	@Override
	public void interrupt() {
		shouldRun = false;
		workerThread.interrupt();
	}

	@Override
	public void interruptIfIdle() {
		final ReentrantLock runLock = this.runLock;
		shouldRun = false;

		if (runLock.tryLock()) {
			try {
				workerThread.interrupt();
			} finally {
				runLock.unlock();
			}
		}
	}

	@Override
	public void setFirstWork(KmlSplittingResult firstWork) {
		this.firstWork = firstWork;
	}

	@Override
	public void setThread(Thread workerThread) {
		this.workerThread = workerThread;
	}

	@Override
	public void setWorkQueue(WorkQueue<KmlSplittingResult> workQueue) {
		this.workQueue = workQueue;
	}

	@Override
	public void run() {
		try {
			if (firstWork != null && shouldRun) {
				doWork(firstWork);
				firstWork = null;
			}

			KmlSplittingResult work = null; 
			while (shouldRun) {
				try {
					work = workQueue.take();
					doWork(work);
				}
				catch (InterruptedException ie) {
					// re-check state
				}
			}

			// last objectGroups may be not empty but not big enough
			for (CityGMLClass cityObjectType: objectGroup.keySet()) {
				if (objectGroupCounter.get(cityObjectType) != 0) {  // group is not empty
					KmlGenericObject currentObjectGroup = objectGroup.get(cityObjectType);
					if (currentObjectGroup == null || currentObjectGroup.getGmlId() == null) continue;
					sendGroupToFile(currentObjectGroup,work);
					currentObjectGroup = null;
					objectGroup.put(cityObjectType, currentObjectGroup);
					objectGroupCounter.put(cityObjectType, 0);
				}
			}
		}
		finally {
			
		}
	}

	private void doWork(KmlSplittingResult work) {
		final ReentrantLock runLock = this.runLock;
		runLock.lock();
		
		singleObject = null;
		CityGMLClass featureClass = work.getCityObjectType();
		
		try {
			switch (featureClass) {
				case BUILDING:					
					singleObject = new Building(connection,
												kmlExporterManager,
												sqliteImporterManager,
												cityGMLFactory,
												kmlFactory,
												elevationServiceHandler,
												getBalloonTemplateHandler(featureClass),
												eventDispatcher,
												config);
					break;
					
			/*	case WATER_BODY:
				case WATER_CLOSURE_SURFACE:
				case WATER_GROUND_SURFACE:
				case WATER_SURFACE:
					singleObject = new WaterBody(connection,
												 kmlExporterManager,
												 cityGMLFactory,
												 kmlFactory,
												 elevationServiceHandler,
												 getBalloonTemplateHandler(featureClass),
												 eventDispatcher,
												 config);
					break;

				case LAND_USE:
					singleObject = new LandUse(connection,
											   kmlExporterManager,
											   cityGMLFactory,
											   kmlFactory,
											   elevationServiceHandler,
											   getBalloonTemplateHandler(featureClass),
											   eventDispatcher,
											   config);
					break;

				case SOLITARY_VEGETATION_OBJECT:
					singleObject = new SolitaryVegetationObject(connection,
												   				kmlExporterManager,
												   				cityGMLFactory,
												   				kmlFactory,
												   				elevationServiceHandler,
																getBalloonTemplateHandler(featureClass),
												   				eventDispatcher,
												   				config);
					break;

				case PLANT_COVER:
					singleObject = new PlantCover(connection,
												  kmlExporterManager,
												  cityGMLFactory,
												  kmlFactory,
												  elevationServiceHandler,
												  getBalloonTemplateHandler(featureClass),
												  eventDispatcher,
												  config);
					break;

				case GENERIC_CITY_OBJECT:
					singleObject = new GenericCityObject(connection,
												   	   	 kmlExporterManager,
												   	   	 cityGMLFactory,
												   	   	 kmlFactory,
												   	   	 elevationServiceHandler,
												   	   	 getBalloonTemplateHandler(featureClass),
												   	   	 eventDispatcher,
												   	   	 config);
					break;

				case TRAFFIC_AREA:
				case AUXILIARY_TRAFFIC_AREA:
				case TRANSPORTATION_COMPLEX:
				case TRACK:
				case RAILWAY:
				case ROAD:
				case SQUARE:
					singleObject = new Transportation(connection,
												   	  kmlExporterManager,
												   	  cityGMLFactory,
												   	  kmlFactory,
												   	  elevationServiceHandler,
												   	  getBalloonTemplateHandler(featureClass),
												   	  eventDispatcher,
												   	  config);
					break;

				case RELIEF_FEATURE:
					singleObject = new Relief(connection,
											  kmlExporterManager,
											  cityGMLFactory,
											  kmlFactory,
											  elevationServiceHandler,
											  getBalloonTemplateHandler(featureClass),
											  eventDispatcher,
											  config);
					break;

				case CITY_FURNITURE:
					singleObject = new CityFurniture(connection,
												   	 kmlExporterManager,
												   	 cityGMLFactory,
											   	   	 kmlFactory,
											   	   	 elevationServiceHandler,
											   	   	 getBalloonTemplateHandler(featureClass),
											   	   	 eventDispatcher,
											   	   	 config);
					break;

				case CITY_OBJECT_GROUP:
					singleObject = new CityObjectGroup(connection,
												   	   kmlExporterManager,
												   	   cityGMLFactory,
												   	   kmlFactory,
												   	   elevationServiceHandler,
												   	   getBalloonTemplateHandler(featureClass),
												   	   eventDispatcher,
												   	   config);
					break;*/
					
				default:
					break;
			}

			if(singleObject == null)
				return;
			
			singleObject.read(work);
			
			
			if (!work.isCityObjectGroup() && 
					work.getDisplayForm().getForm() == DisplayForm.COLLADA &&
					singleObject.getGmlId() != null) { // object is filled

					// correction for some CityGML Types exported together
					if (featureClass == CityGMLClass.PLANT_COVER) featureClass = CityGMLClass.SOLITARY_VEGETATION_OBJECT;
					
					if (featureClass == CityGMLClass.WATER_CLOSURE_SURFACE ||
							featureClass == CityGMLClass.WATER_GROUND_SURFACE ||
							featureClass == CityGMLClass.WATER_SURFACE) featureClass = CityGMLClass.WATER_BODY;
					
						if (featureClass == CityGMLClass.TRAFFIC_AREA ||
							featureClass == CityGMLClass.AUXILIARY_TRAFFIC_AREA ||
							featureClass == CityGMLClass.TRACK ||
							featureClass == CityGMLClass.RAILWAY ||
							featureClass == CityGMLClass.ROAD ||
							featureClass == CityGMLClass.SQUARE) featureClass = CityGMLClass.TRANSPORTATION_COMPLEX;
						
					KmlGenericObject currentObjectGroup = objectGroup.get(featureClass);
					if (currentObjectGroup == null) {
						currentObjectGroup = singleObject;
						objectGroup.put(featureClass, currentObjectGroup);
					}
					else {
						currentObjectGroup.appendObject(singleObject);
					}

					objectGroupCounter.put(featureClass, objectGroupCounter.get(featureClass).intValue() + 1);
					if (objectGroupCounter.get(featureClass).intValue() == objectGroupSize.get(featureClass).intValue()) {
						sendGroupToFile(currentObjectGroup,work);
						currentObjectGroup = null;
						objectGroup.put(featureClass, currentObjectGroup);
						objectGroupCounter.put(featureClass, 0);
					}
				}
		}
		finally {
			runLock.unlock();
		}
	}

	
	
	private void sendGroupToFile(KmlGenericObject objectGroup,KmlSplittingResult work) {
		try {
			double imageScaleFactor = 1;
			ColladaOptions colladaOptions = objectGroup.getColladaOptions();
			if (colladaOptions.isGenerateTextureAtlases()) {
//				eventDispatcher.triggerEvent(new StatusDialogMessage(Internal.I18N.getString("kmlExport.dialog.creatingAtlases")));
				if (colladaOptions.isScaleImages()) {
					imageScaleFactor = colladaOptions.getImageScaleFactor();
				}
				objectGroup.createTextureAtlas(colladaOptions.getPackingAlgorithm(),
											   imageScaleFactor,
											   colladaOptions.isTextureAtlasPots());
			}
			else if (colladaOptions.isScaleImages()) {
				imageScaleFactor = colladaOptions.getImageScaleFactor();
				if (imageScaleFactor < 1) {
					objectGroup.resizeAllImagesByFactor(imageScaleFactor);
				}
			}

			ColladaBundle colladaBundle = new ColladaBundle();
			colladaBundle.setCollada(objectGroup.generateColladaTree());
			colladaBundle.setTexImages(objectGroup.getTexImages());
//			colladaBundle.setTexOrdImages(objectGroup.getTexOrdImages());
			colladaBundle.setPlacemark(objectGroup.createPlacemarkForColladaModel(work));
			colladaBundle.setGmlId(objectGroup.getGmlId());
			kmlExporterManager.print(colladaBundle,
					 				 objectGroup.getId(),					
					 				 objectGroup.getBalloonSettings().isBalloonContentInSeparateFile());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private BalloonTemplateHandlerImpl getBalloonTemplateHandler(CityGMLClass cityObjectType) {
		BalloonTemplateHandlerImpl currentBalloonTemplateHandler = balloonTemplateHandler.get(cityObjectType);

		if (currentBalloonTemplateHandler == null) {
			Balloon balloonSettings = getBalloonSettings(cityObjectType);
			if (balloonSettings != null &&	balloonSettings.isIncludeDescription() &&
					balloonSettings.getBalloonContentMode() != BalloonContentMode.GEN_ATTRIB) {
				String balloonTemplateFilename = balloonSettings.getBalloonContentTemplateFile();
				if (balloonTemplateFilename != null && balloonTemplateFilename.length() > 0) {
					currentBalloonTemplateHandler = new BalloonTemplateHandlerImpl(new File(balloonTemplateFilename), connection);
					balloonTemplateHandler.put(cityObjectType, currentBalloonTemplateHandler);
				}
			}
		}

		return currentBalloonTemplateHandler;
	}

	private Balloon getBalloonSettings(CityGMLClass cityObjectType) {
		Balloon balloonSettings = null;
		switch (cityObjectType) {
			case BUILDING:
				balloonSettings = config.getBuildingBalloon();
				break;
			case LAND_USE:
				balloonSettings = config.getLandUseBalloon();
				break;
			case WATER_BODY:
			case WATER_CLOSURE_SURFACE:
			case WATER_GROUND_SURFACE:
			case WATER_SURFACE:
				balloonSettings = config.getWaterBodyBalloon();
				break;
			case SOLITARY_VEGETATION_OBJECT:
			case PLANT_COVER:
				balloonSettings = config.getVegetationBalloon();
				break;
			case TRAFFIC_AREA:
			case AUXILIARY_TRAFFIC_AREA:
			case TRANSPORTATION_COMPLEX:
			case TRACK:
			case RAILWAY:
			case ROAD:
			case SQUARE:
				balloonSettings = config.getTransportationBalloon();
				break;
/*
			case RASTER_RELIEF:
			case MASSPOINT_RELIEF:
			case BREAKLINE_RELIEF:
			case TIN_RELIEF:
*/
			case RELIEF_FEATURE:
				balloonSettings = config.getReliefBalloon();
				break;
			case GENERIC_CITY_OBJECT:
				balloonSettings = config.getGenericCityObjectBalloon();
				break;
			case CITY_FURNITURE:
				balloonSettings = config.getCityFurnitureBalloon();
				break;
			case CITY_OBJECT_GROUP:
				balloonSettings = config.getCityObjectGroupBalloon();
				break;
			default:
				return null;
		}
		return balloonSettings;
	}

}
