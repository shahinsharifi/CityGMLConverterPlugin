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
package org.citydb.plugins.CityGMLConverter.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamResult;

import net.opengis.kml._2.BalloonStyleType;
import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.LatLonAltBoxType;
import net.opengis.kml._2.LineStringType;
import net.opengis.kml._2.LineStyleType;
import net.opengis.kml._2.LinkType;
import net.opengis.kml._2.LodType;
import net.opengis.kml._2.LookAtType;
import net.opengis.kml._2.NetworkLinkType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.PairType;
import net.opengis.kml._2.PlacemarkType;
import net.opengis.kml._2.PolyStyleType;
import net.opengis.kml._2.RegionType;
import net.opengis.kml._2.StyleMapType;
import net.opengis.kml._2.StyleStateEnumType;
import net.opengis.kml._2.StyleType;
import net.opengis.kml._2.ViewRefreshModeEnumType;

import org.citydb.api.concurrent.PoolSizeAdaptationStrategy;
import org.citydb.api.concurrent.SingleWorkerPool;
import org.citydb.api.concurrent.WorkerPool;
import org.citydb.api.database.DatabaseSrs;
import org.citydb.api.event.Event;
import org.citydb.api.event.EventDispatcher;
import org.citydb.api.event.EventHandler;
import org.citydb.api.geometry.BoundingBox;
import org.citydb.api.registry.ObjectRegistry;
import org.citydb.config.project.filter.FeatureClass;
import org.citydb.config.project.filter.TiledBoundingBox;
import org.citydb.config.project.filter.Tiling;
import org.citydb.config.project.filter.TilingMode;
import org.citydb.log.Logger;
import org.citydb.modules.common.concurrent.IOWriterWorkerFactory;
import org.citydb.modules.common.event.*;
import org.citydb.plugins.CityGMLConverter.concurrent.CityKmlExportWorkerFactory;
import org.citydb.plugins.CityGMLConverter.concurrent.DBImportXlinkWorkerFactory;
import org.citydb.plugins.CityGMLConverter.config.ConfigImpl;
import org.citydb.plugins.CityGMLConverter.content.*;
import org.citydb.plugins.CityGMLConverter.content.Bridge;
import org.citydb.plugins.CityGMLConverter.content.Building;
import org.citydb.plugins.CityGMLConverter.content.KmlSplitter;
import org.citydb.plugins.CityGMLConverter.content.KmlSplittingResult;
import org.citydb.plugins.CityGMLConverter.content.Vegetation;
import org.citydb.plugins.CityGMLConverter.content.Tunnel;
import org.citygml4j.builder.jaxb.JAXBBuilder;
import org.citygml4j.factory.GMLGeometryFactory;
import org.citygml4j.model.citygml.CityGMLClass;
import org.citygml4j.util.xml.SAXEventBuffer;
import org.citygml4j.util.xml.SAXFragmentWriter;
import org.citygml4j.util.xml.SAXFragmentWriter.WriteMode;
import org.citygml4j.util.xml.SAXWriter;
import org.xml.sax.SAXException;
import org.citydb.plugins.CityGMLConverter.config.Balloon;
import org.citydb.plugins.CityGMLConverter.config.BalloonContentMode;
import org.citydb.plugins.CityGMLConverter.config.DisplayForm;
import org.citydb.plugins.CityGMLConverter.util.ProjConvertor;
import org.citydb.plugins.CityGMLConverter.util.CityObject4JSON;
import org.citydb.plugins.CityGMLConverter.util.Util;
import org.citydb.plugins.CityGMLConverter.util.Sqlite.SQLiteFactory;
import org.citydb.plugins.CityGMLConverter.util.Sqlite.cache.CacheManager;
import org.citydb.plugins.CityGMLConverter.util.filter.ExportFilter;
import org.citydb.plugins.CityGMLConverter.util.filter.FilterMode;
import org.citydb.plugins.CityGMLConverter.xlink.content.DBXlink;
import org.citydb.plugins.CityGMLConverter.xlink.resolver.DBXlinkSplitter;


public class CityKmlExporter implements EventHandler {
	
	private final JAXBContext jaxbKmlContext;
	private final JAXBContext jaxbColladaContext;
	private final ConfigImpl config;
	private final EventDispatcher eventDispatcher;
	private WorkerPool<DBXlink> tmpXlinkPool;
	private WorkerPool<DBXlink> xlinkResolverPool;
	private CacheManager cacheManager;
	private GMLGeometryFactory cityGMLFactory; 
	private ObjectFactory kmlFactory; 
	private WorkerPool<KmlSplittingResult> kmlWorkerPool;
	private SingleWorkerPool<SAXEventBuffer> ioWriterPool;
	private KmlSplitter kmlSplitter;
	private DBXlinkSplitter tmpSplitter;
	private volatile boolean shouldRun = true;
	private AtomicBoolean isInterrupted = new AtomicBoolean(false);
	private static final double BORDER_GAP = 0.000001;
	private static final String ENCODING = "UTF-8";
	private static final Charset CHARSET = Charset.forName(ENCODING);
	private static final String TEMP_FOLDER = "__temp";
	private final Logger LOG = Logger.getInstance();
	
	private String path;
	private String filename;
	private EnumMap<CityGMLClass, Long>featureCounterMap = new EnumMap<CityGMLClass, Long>(CityGMLClass.class);
	private long geometryCounter;
	private String TargetSrs = "";
	private File lastTempFolder;
	private static HashMap<String, CityObject4JSON> alreadyExported;
	
	private BoundingBox extent;
	private BoundingBox wgs84TileMatrix;
	
	private double wgs84DeltaLongitude;
	private double wgs84DeltaLatitude;
	
	private int rows = 1;
	private int columns = 1;

	public CityKmlExporter (JAXBContext jaxbKmlContext,
						JAXBContext jaxbColladaContext,
						ConfigImpl config,
						String _TargetSrs,
						EventDispatcher eventDispatcher) throws SQLException {
		
		this.jaxbKmlContext = jaxbKmlContext;
		this.jaxbColladaContext = jaxbColladaContext;
		this.config = config;
		this.eventDispatcher = ObjectRegistry.getInstance().getEventDispatcher();
		this.TargetSrs = _TargetSrs;
		kmlFactory = new ObjectFactory();		
		cityGMLFactory = new GMLGeometryFactory();		
	}
	
	public void cleanup() {
		eventDispatcher.removeEventHandler(this);
	}
	
	
	public void SetTargetSrs(String _TargetSrs)
	{
		this.TargetSrs = _TargetSrs;		
	}
		

	public boolean doProcess(JAXBBuilder jaxbBuilder , File reader) throws Exception{
		
		geometryCounter = 0;
		
		// worker pool settings		
		//  Resources resources = new Resources();	
		int availableCores = Runtime.getRuntime().availableProcessors();
		int minThreads = availableCores;//resources.getThreadPool().getDefaultPool().getMinThreads();
		int maxThreads = availableCores;//resources.getThreadPool().getDefaultPool().getMaxThreads();
		int queueSize = maxThreads * 2;

		// adding listener
		eventDispatcher.addEventHandler(EventType.COUNTER, this);
		eventDispatcher.addEventHandler(EventType.GEOMETRY_COUNTER, this);
		eventDispatcher.addEventHandler(EventType.INTERRUPT, this);

		boolean balloonCheck = checkBalloonSettings(CityGMLClass.BUILDING);
		balloonCheck = checkBalloonSettings(CityGMLClass.WATER_BODY) && balloonCheck;
		balloonCheck = checkBalloonSettings(CityGMLClass.LAND_USE) && balloonCheck;
		balloonCheck = checkBalloonSettings(CityGMLClass.SOLITARY_VEGETATION_OBJECT) && balloonCheck;
		balloonCheck = checkBalloonSettings(CityGMLClass.TRANSPORTATION_COMPLEX) && balloonCheck;
		balloonCheck = checkBalloonSettings(CityGMLClass.RELIEF_FEATURE) && balloonCheck;
		balloonCheck = checkBalloonSettings(CityGMLClass.CITY_FURNITURE) && balloonCheck;
		balloonCheck = checkBalloonSettings(CityGMLClass.GENERIC_CITY_OBJECT) && balloonCheck;
		balloonCheck = checkBalloonSettings(CityGMLClass.CITY_OBJECT_GROUP) && balloonCheck;
		if (!balloonCheck) 
			return false;

		// getting export filter
		ExportFilter exportFilter = new ExportFilter(config, FilterMode.CityKML);
		boolean isBBoxActive = config.getFilter().getComplexFilter().getTiledBoundingBox().getActive().booleanValue();
		// bounding box config
		Tiling tiling = config.getFilter().getComplexFilter().getTiledBoundingBox().getTiling();

		// create a saxWriter instance 
		// define indent for xml output and namespace mappings
		SAXWriter saxWriter = new SAXWriter();
		saxWriter.setIndentString("  ");
		saxWriter.setHeaderComment("Written by " + this.getClass().getPackage().getImplementationTitle() + ", version \"" +
				this.getClass().getPackage().getImplementationVersion() + '"', 
				this.getClass().getPackage().getImplementationVendor());
		saxWriter.setDefaultNamespace("http://www.opengis.net/kml/2.2"); // default namespace
		saxWriter.setPrefix("gx", "http://www.google.com/kml/ext/2.2");
		saxWriter.setPrefix("atom", "http://www.w3.org/2005/Atom");
		saxWriter.setPrefix("xal", "urn:oasis:names:tc:ciq:xsdschema:xAL:2.0");
		
		Properties props = new Properties();
		props.put(Marshaller.JAXB_FRAGMENT, new Boolean(true));

		path = config.getInternal().getExportFileName().trim();
		if (path.lastIndexOf(File.separator) == -1) {
			if (path.lastIndexOf(".") == -1) {
				filename = path;
			}
			else {
				filename = path.substring(0, path.lastIndexOf("."));
			}
			path = ".";
		}
		else {
			if (path.lastIndexOf(".") == -1) {
				filename = path.substring(path.lastIndexOf(File.separator) + 1);
			}
			else {
				filename = path.substring(path.lastIndexOf(File.separator) + 1, path.lastIndexOf("."));
			}
			path = path.substring(0, path.lastIndexOf(File.separator));
		}

		if (isBBoxActive && tiling.getMode() != TilingMode.NO_TILING) {
			try {
				
				int activeDisplayFormsAmount = 
					config.getActiveDisplayFormsAmount(config.getBuildingDisplayForms());
				
				Logger.getInstance().info(String.valueOf(rows * columns * activeDisplayFormsAmount) +
					 	" (" + rows + "x" + columns + "x" + activeDisplayFormsAmount +
					 	") tiles will be generated."); 
			}
			catch (Exception ex) {
				ex.printStackTrace();
				return false;
			}
		}
		else {
			rows = 1;
			columns = 1;
		}

		for (DisplayForm displayForm : config.getBuildingDisplayForms()) {

			if (!displayForm.isActive()) 
				continue;

			alreadyExported = new HashMap<String, CityObject4JSON>();
			for (int i = 0; shouldRun && i < rows; i++) {
				for (int j = 0; shouldRun && j < columns; j++) {
					
					if (lastTempFolder != null && lastTempFolder.exists()) 
						deleteFolder(lastTempFolder); // just in case
					
					File file = null;
					OutputStreamWriter fileWriter = null;
					ZipOutputStream zipOut = null;

					try {
						String fileExtension = config.isExportAsKmz() ? ".kmz" : ".kml";
						if (isBBoxActive && tiling.getMode() != TilingMode.NO_TILING) {
							
							exportFilter.getBoundingBoxFilter().setActiveTile(i, j);							
							file = new File(path + File.separator + filename + "_Tile_"
									 	 	+ i + "_" + j + "_" + displayForm.getName() + fileExtension);
						}
						else {
							file = new File(path + File.separator + filename + "_" + displayForm.getName() + fileExtension);
						}

						eventDispatcher.triggerEvent(new StatusDialogTitle(file.getName(), this));

						// open file for writing
						try {
							if (config.isExportAsKmz()) { 
								zipOut = new ZipOutputStream(new FileOutputStream(file));
								ZipEntry zipEntry = new ZipEntry("doc.kml");
								zipOut.putNextEntry(zipEntry);
								fileWriter = new OutputStreamWriter(zipOut, CHARSET);
							}
							else {
								fileWriter = new OutputStreamWriter(new FileOutputStream(file), CHARSET);
							}
								
							// set output for SAXWriter
							saxWriter.setOutput(new StreamResult(fileWriter), ENCODING);	
						} catch (IOException ioE) {
							Logger.getInstance().error("Failed to open file '" + file.getName() + "' for writing: " + ioE.getMessage());
							return false;
						}

						SQLiteFactory sqliteFactory = new SQLiteFactory("tmp_" + filename + ".db",  file.getParent() , "org.sqlite.JDBC");
						cacheManager = new CacheManager(sqliteFactory.getConnection(), maxThreads);
						tmpSplitter = new DBXlinkSplitter(cacheManager, 
								xlinkResolverPool, 
								tmpXlinkPool,
								eventDispatcher);
						config.setXlinkSplitter(tmpSplitter);
						
						// this pool is for registering xlinks
						tmpXlinkPool = new WorkerPool<DBXlink>(
								"xlinkworkerPool",
								minThreads,
								maxThreads,
								PoolSizeAdaptationStrategy.AGGRESSIVE,
								new DBImportXlinkWorkerFactory(cacheManager, config, eventDispatcher),
								queueSize,
								false);
						
						// create worker pools
						// here we have an open issue: queue sizes are fix...
						ioWriterPool = new SingleWorkerPool<SAXEventBuffer>(
								"ioWriterPool",
								new IOWriterWorkerFactory(saxWriter),
								100,
								true);
						

						kmlWorkerPool = new WorkerPool<KmlSplittingResult>(
								"kmlWorkerPool",
								minThreads,
								maxThreads,
								PoolSizeAdaptationStrategy.AGGRESSIVE,
								new CityKmlExportWorkerFactory(
										jaxbKmlContext,
										jaxbColladaContext,
										ioWriterPool,
										tmpXlinkPool,
										kmlFactory,
										cityGMLFactory,
										config,
										eventDispatcher),
										300,
										false);
						
						
						

						// prestart pool workers
						tmpXlinkPool.prestartCoreWorkers();
						ioWriterPool.prestartCoreWorkers();
						kmlWorkerPool.prestartCoreWorkers();

					
						// create file header writer
						SAXFragmentWriter fragmentWriter = new SAXFragmentWriter(kmlFactory.createDocument(null).getName(), saxWriter);
						

						// ok, preparations done. inform user...
						Logger.getInstance().info("Exporting to file: " + file.getAbsolutePath());

						// create kml root element
						KmlType kmlType = kmlFactory.createKmlType();
						JAXBElement<KmlType> kml = kmlFactory.createKml(kmlType);

						DocumentType document = kmlFactory.createDocumentType();
						if (isBBoxActive &&	tiling.getMode() != TilingMode.NO_TILING) {
							document.setName(filename + "_Tile_" + i + "_" + j + "_" + displayForm.getName());
						}
						else {
							document.setName(filename + "_" + displayForm.getName());
						}
						document.setOpen(false);
						kmlType.setAbstractFeatureGroup(kmlFactory.createDocument(document));

						
						Marshaller marshaller = null;						
						try {
							marshaller = jaxbKmlContext.createMarshaller();
							marshaller.setProperty(Marshaller.JAXB_FRAGMENT, new Boolean(true));
						} catch (JAXBException e) {
							Logger.getInstance().error("Failed to create JAXB marshaller object.");
							return false;
						}
						
						try {
							
							fragmentWriter.setWriteMode(WriteMode.HEAD);
							marshaller.marshal(kml, fragmentWriter);
							saxWriter.flush();

							if (isBBoxActive &&	tiling.getMode() != TilingMode.NO_TILING) {
								addBorder(i, j);
							}
							
						} catch (JAXBException jaxBE) {
							Logger.getInstance().error("I/O error: " + jaxBE.getMessage());
							return false;
						} catch (SAXException saxE) {
							Logger.getInstance().error("I/O error: " + saxE.getMessage());
							return false;
						}

						// get database splitter and start query
						kmlSplitter = null;
						try {
							
							kmlSplitter = new KmlSplitter(
									kmlWorkerPool,
									exportFilter,
									TargetSrs,
									jaxbBuilder,
									displayForm,
									config);

							if (shouldRun)
								kmlSplitter.startQuery(reader);
							
							
						} catch (SQLException sqlE) {
							Logger.getInstance().error("SQL error: " + sqlE.getMessage());
							return false;
						}

						try {
							
							if (shouldRun)
							{
								tmpXlinkPool.shutdown();
								tmpXlinkPool.join();
							}
							if (!featureCounterMap.isEmpty() &&
									(!config.isOneFilePerObject() ||
									  config.getFilter().isSetSimpleFilter())) {
								for (CityGMLClass type : featureCounterMap.keySet()) {
									if (featureCounterMap.get(type) > 0)
										addStyle(displayForm, type);
								}
							}
							ioWriterPool.shutdownAndWait();
						
						} catch (InterruptedException e) {
							Logger.getInstance().error(e.getMessage());
							return false;
						} catch (Exception jaxBE) {
							Logger.getInstance().error("I/O error: " + jaxBE.getMessage());
							return false;
						}

						// write footer element
						try {
							fragmentWriter.setWriteMode(WriteMode.TAIL);
							marshaller.marshal(kml, fragmentWriter);
						} catch (JAXBException  saxE) {
							Logger.getInstance().error("XML error: " + saxE.getMessage());
							return false;
						}

						eventDispatcher.triggerEvent(new StatusDialogMessage(Util.I18N.getString("kmlExport.dialog.writingToFile"), this));

						// flush sax writer and close file
						try {
							saxWriter.flush();
							if (config.isExportAsKmz()) { 
								zipOut.closeEntry();

								List<File> filesToZip = new ArrayList<File>();
								File tempFolder = new File(path, TEMP_FOLDER);
								lastTempFolder = tempFolder;
								int indexOfZipFilePath = tempFolder.getCanonicalPath().length() + 1;

								if (tempFolder.exists()) { // !config.isOneFilePerObject()
									Logger.getInstance().info("Zipping to kmz archive from temporary folder...");
									getAllFiles(tempFolder, filesToZip);
									for (File fileToZip : filesToZip) {
										if (!fileToZip.isDirectory()) {
											FileInputStream inputStream = new FileInputStream(fileToZip);
											String zipEntryName = fileToZip.getCanonicalPath().substring(indexOfZipFilePath);
											zipEntryName = zipEntryName.replace(File.separator, "/"); // MUST
											ZipEntry zipEntry = new ZipEntry(zipEntryName);
											zipOut.putNextEntry(zipEntry);
	
											byte[] bytes = new byte[64*1024]; // 64K should be enough for most
											int length;
											while ((length = inputStream.read(bytes)) >= 0) {
												zipOut.write(bytes, 0, length);
											}
											inputStream.close();
											zipOut.closeEntry();
										}
									}
									Logger.getInstance().info("Removing temporary folder...");
									deleteFolder(tempFolder);
								}
								zipOut.close();
							}
							fileWriter.close();
						}
						catch (Exception ioe) {
							Logger.getInstance().error("I/O error: " + ioe.getMessage());
							try {
								fileWriter.close();
							}
							catch (Exception e) {}
							return false;
						}

						eventDispatcher.triggerEvent(new StatusDialogMessage(" ", this));

						// finally join eventDispatcher
						try {
							eventDispatcher.flushEvents();
						} catch (InterruptedException iE) {
							Logger.getInstance().error("Internal error: " + iE.getMessage());
							return false;
						}
						
						

						// set null
						ioWriterPool = null;
						kmlWorkerPool = null;
						kmlSplitter = null;					
						boolean IsConnectionKilled = sqliteFactory.KillConnection();
						if(IsConnectionKilled)
						{
							boolean IsDbDropped = sqliteFactory.dropDatabase();
							if(IsDbDropped)
								LOG.info("Temporary database has been dropped");
							else
								LOG.error("Temporary database can not be dropped");
						}

					}
					finally {}
				}
			}
		}

		if (isBBoxActive) {
			try {
				eventDispatcher.triggerEvent(new StatusDialogTitle(filename + ".kml", this));
				eventDispatcher.triggerEvent(new StatusDialogMessage(Util.I18N.getString("kmlExport.dialog.writingMainFile"), this));
				generateMasterFile();
			}
			catch (Exception ex) {
				ex.printStackTrace();
				return false;
			}
		}

		if (config.isWriteJSONFile()) {
			try {
				Logger.getInstance().info("Writing file: " + filename + ".json");
				File jsonFile = new File(path + File.separator + filename + ".json");
				FileOutputStream outputStream = new FileOutputStream(jsonFile);
				if (config.isWriteJSONPFile())
					outputStream.write((config.getCallbackNameJSONP() + "({\n").getBytes(CHARSET));
				else
					outputStream.write("{\n".getBytes(CHARSET));

				Iterator<String> iterator = alreadyExported.keySet().iterator();
				while (iterator.hasNext()) {
					String id = iterator.next();
//					outputStream.write(("\t\"" + id + "\": {").toString().getBytes(CHARSET));
					outputStream.write(alreadyExported.get(id).toString().getBytes(CHARSET));
					if (iterator.hasNext()) {
						outputStream.write(",\n".getBytes(CHARSET));
					}
				}

				if (config.isWriteJSONPFile())
					outputStream.write("\n});\n".getBytes(CHARSET));
				else
					outputStream.write("\n}\n".getBytes(CHARSET));
				outputStream.close();
			}
			catch (IOException ioe) {
				Logger.getInstance().error("I/O error: " + ioe.getMessage());
//				ioe.printStackTrace();
			}
		}
		
		eventDispatcher.triggerEvent(new StatusDialogMessage(Util.I18N.getString("export.dialog.finish.msg"), this));

		// show exported features
		if (!featureCounterMap.isEmpty()) {
			Logger.getInstance().info("Exported CityGML features:");
			for (CityGMLClass type : featureCounterMap.keySet())
				Logger.getInstance().info(type + ": " + featureCounterMap.get(type));
		}
		Logger.getInstance().info("Processed geometry objects: " + geometryCounter);

		if (lastTempFolder != null && lastTempFolder.exists()) deleteFolder(lastTempFolder); // just in case

		return shouldRun;
	}


	private void generateMasterFile() throws FileNotFoundException,
											 SQLException,
											 JAXBException,
											 DatatypeConfigurationException { 

		// create a saxWriter instance 
		// define indent for xml output and namespace mappings
		SAXWriter saxWriter = new SAXWriter();
		saxWriter.setIndentString("  ");
		saxWriter.setHeaderComment("Written by " + this.getClass().getPackage().getImplementationTitle() + ", version \"" +
				this.getClass().getPackage().getImplementationVersion() + '"', 
				this.getClass().getPackage().getImplementationVendor());
		saxWriter.setDefaultNamespace("http://www.opengis.net/kml/2.2"); // default namespace
		saxWriter.setPrefix("gx", "http://www.google.com/kml/ext/2.2");
		saxWriter.setPrefix("atom", "http://www.w3.org/2005/Atom");
		saxWriter.setPrefix("xal", "urn:oasis:names:tc:ciq:xsdschema:xAL:2.0");

		Marshaller marshaller = jaxbKmlContext.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);

		Properties props = new Properties();
		props.put(Marshaller.JAXB_FRAGMENT, new Boolean(true));
//		props.put(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
//		props.put(Marshaller.JAXB_ENCODING, ENCODING);
		TilingMode tilingMode = config.getFilter().getComplexFilter().getTiledBoundingBox().getTiling().getMode();

		try {
			File mainFile = new File(path + File.separator + filename + ".kml");
			FileOutputStream outputStream = new FileOutputStream(mainFile);
			saxWriter.setOutput(new StreamResult(outputStream), ENCODING);	

		
			ioWriterPool = new SingleWorkerPool<SAXEventBuffer>(
					"ioWriterPool",
					new IOWriterWorkerFactory(saxWriter),
					100,
					true);
			ioWriterPool.prestartCoreWorkers();

			// create file header
			SAXFragmentWriter fragmentWriter = new SAXFragmentWriter(
								new QName("http://www.opengis.net/kml/2.2", "Document"), saxWriter);						

			// create kml root element
			KmlType kmlType = kmlFactory.createKmlType();
			JAXBElement<KmlType> kml = kmlFactory.createKml(kmlType);
			DocumentType document = kmlFactory.createDocumentType();
			document.setOpen(true);
			document.setName(filename);
			LookAtType lookAtType = kmlFactory.createLookAtType();
			lookAtType.setLongitude(wgs84TileMatrix.getLowerLeftCorner().getX() + Math.abs((wgs84TileMatrix.getUpperRightCorner().getX() - wgs84TileMatrix.getLowerLeftCorner().getX())/2));
			lookAtType.setLatitude(wgs84TileMatrix.getLowerLeftCorner().getY() + Math.abs((wgs84TileMatrix.getUpperRightCorner().getY() - wgs84TileMatrix.getLowerLeftCorner().getY())/2));
			lookAtType.setAltitude(0.0);
			lookAtType.setHeading(0.0);
			lookAtType.setTilt(60.0);
			lookAtType.setRange(970.0);
			document.setAbstractViewGroup(kmlFactory.createLookAt(lookAtType));
			kmlType.setAbstractFeatureGroup(kmlFactory.createDocument(document));


			try {
				
				fragmentWriter.setWriteMode(WriteMode.HEAD);
				marshaller.marshal(kml, fragmentWriter);
				
				if (config.isOneFilePerObject()) {
					FeatureClass featureFilter = config.getFilter().getComplexFilter().getFeatureClass();

                    if (featureFilter.isSetBuilding()) {
						for (DisplayForm displayForm : config.getBuildingDisplayForms()) {
							addStyle(displayForm, CityGMLClass.BUILDING);
						}
					}

					if (featureFilter.isSetCityFurniture()) {
						for (DisplayForm displayForm : config.getCityFurnitureDisplayForms()) {
							addStyle(displayForm, CityGMLClass.CITY_FURNITURE);
						}
					}

                    if (featureFilter.isSetBridge()) {
                        for (DisplayForm displayForm : config.getTransportationDisplayForms()) {
                            addStyle(displayForm, CityGMLClass.BRIDGE);
                        }
                    }

                    if (featureFilter.isSetTunnel()) {
                        for (DisplayForm displayForm : config.getTransportationDisplayForms()) {
                            addStyle(displayForm, CityGMLClass.TUNNEL);
                        }
                    }


					if (featureFilter.isSetCityObjectGroup()) {
						for (DisplayForm displayForm : config.getCityObjectGroupDisplayForms()) {
							addStyle(displayForm, CityGMLClass.CITY_OBJECT_GROUP);
						}
					}
					if (featureFilter.isSetGenericCityObject()) {
						for (DisplayForm displayForm : config.getGenericCityObjectDisplayForms()) {
							addStyle(displayForm, CityGMLClass.GENERIC_CITY_OBJECT);
						}
					}
					if (featureFilter.isSetLandUse()) {
						for (DisplayForm displayForm : config.getLandUseDisplayForms()) {
							addStyle(displayForm, CityGMLClass.LAND_USE);
						}
					}
					if (featureFilter.isSetReliefFeature()) {
						for (DisplayForm displayForm : config.getReliefDisplayForms()) {
							addStyle(displayForm, CityGMLClass.RELIEF_FEATURE);
						}
					}
					if (featureFilter.isSetTransportation()) {
						for (DisplayForm displayForm : config.getTransportationDisplayForms()) {
							addStyle(displayForm, CityGMLClass.TRANSPORTATION_COMPLEX);
						}
					}
					if (featureFilter.isSetVegetation()) {
						for (DisplayForm displayForm : config.getVegetationDisplayForms()) {
							addStyle(displayForm, CityGMLClass.SOLITARY_VEGETATION_OBJECT);
						}
					}

					if (featureFilter.isSetWaterBody()) {
						for (DisplayForm displayForm : config.getWaterBodyDisplayForms()) {
							addStyle(displayForm, CityGMLClass.WATER_BODY);
						}
					}
				}
				// make sure header has been written
				saxWriter.flush();
			}
			catch (SAXException saxE) {
				Logger.getInstance().error("I/O error: " + saxE.getMessage());
			}

			if (config.isShowBoundingBox()) {
				SAXEventBuffer tmp = new SAXEventBuffer();
				
				StyleType frameStyleType = kmlFactory.createStyleType();
				frameStyleType.setId("frameStyle");
				LineStyleType frameLineStyleType = kmlFactory.createLineStyleType();
				frameLineStyleType.setWidth(4.0);
				frameStyleType.setLineStyle(frameLineStyleType);
				marshaller.marshal(kmlFactory.createStyle(frameStyleType), tmp);

				PlacemarkType placemarkType = kmlFactory.createPlacemarkType();
				placemarkType.setName("Bounding box border");
				placemarkType.setStyleUrl("#" + frameStyleType.getId());
				LineStringType lineStringType = kmlFactory.createLineStringType();
				lineStringType.setTessellate(true);
				lineStringType.getCoordinates().add("" + (wgs84TileMatrix.getLowerLeftCorner().getX() - BORDER_GAP) + "," + (wgs84TileMatrix.getLowerLeftCorner().getY() - BORDER_GAP * .5));
				lineStringType.getCoordinates().add(" " + (wgs84TileMatrix.getLowerLeftCorner().getX() - BORDER_GAP) + "," + (wgs84TileMatrix.getUpperRightCorner().getY() + BORDER_GAP * .5));
				lineStringType.getCoordinates().add(" " + (wgs84TileMatrix.getUpperRightCorner().getX() + BORDER_GAP) + "," + (wgs84TileMatrix.getUpperRightCorner().getY() + BORDER_GAP * .5));
				lineStringType.getCoordinates().add(" " + (wgs84TileMatrix.getUpperRightCorner().getX() + BORDER_GAP) + "," + (wgs84TileMatrix.getLowerLeftCorner().getY() - BORDER_GAP * .5));
				lineStringType.getCoordinates().add(" " + (wgs84TileMatrix.getLowerLeftCorner().getX() - BORDER_GAP) + "," + (wgs84TileMatrix.getLowerLeftCorner().getY() - BORDER_GAP * .5));
				placemarkType.setAbstractGeometryGroup(kmlFactory.createLineString(lineStringType));
				
				marshaller.marshal(kmlFactory.createPlacemark(placemarkType), tmp);
				ioWriterPool.addWork(tmp);
			}

			for (int i = 0; i < rows; i++) {
				for (int j = 0; j < columns; j++) {

					// must be done like this to avoid non-matching tile limits
					double wgs84TileSouthLimit = wgs84TileMatrix.getLowerLeftCorner().getY() + (i * wgs84DeltaLatitude);
					double wgs84TileNorthLimit = wgs84TileMatrix.getLowerLeftCorner().getY() + ((i+1) * wgs84DeltaLatitude);
					double wgs84TileWestLimit = wgs84TileMatrix.getLowerLeftCorner().getX() + (j * wgs84DeltaLongitude);
					double wgs84TileEastLimit = wgs84TileMatrix.getLowerLeftCorner().getX() + ((j+1) * wgs84DeltaLongitude);

					// tileName should not contain special characters,
					// since it will be used as filename for all displayForm files
					String tileName = filename;

					if (tilingMode != TilingMode.NO_TILING) {		
						tileName = tileName + "_Tile_" + i + "_" + j;
					}
					FolderType folderType = kmlFactory.createFolderType();
					folderType.setName(tileName);

					for (DisplayForm displayForm : config.getBuildingDisplayForms()) {

						if (!displayForm.isActive()) continue;

						String fileExtension = config.isExportAsKmz() ? ".kmz" : ".kml";
						String tilenameForDisplayForm = tileName + "_" + displayForm.getName() + fileExtension; 

						NetworkLinkType networkLinkType = kmlFactory.createNetworkLinkType();
						networkLinkType.setName("Display as " + displayForm.getName());

						RegionType regionType = kmlFactory.createRegionType();

						LatLonAltBoxType latLonAltBoxType = kmlFactory.createLatLonAltBoxType();
						latLonAltBoxType.setNorth(wgs84TileNorthLimit);
						latLonAltBoxType.setSouth(wgs84TileSouthLimit);
						latLonAltBoxType.setEast(wgs84TileEastLimit);
						latLonAltBoxType.setWest(wgs84TileWestLimit);

						LodType lodType = kmlFactory.createLodType();
						lodType.setMinLodPixels((double)displayForm.getVisibleFrom());
						lodType.setMaxLodPixels((double)displayForm.getVisibleUpTo());

						regionType.setLatLonAltBox(latLonAltBoxType);
						regionType.setLod(lodType);

						LinkType linkType = kmlFactory.createLinkType();
						linkType.setHref(tilenameForDisplayForm);
						linkType.setViewRefreshMode(ViewRefreshModeEnumType.fromValue(config.getViewRefreshMode()));
						linkType.setViewFormat("");
						if (linkType.getViewRefreshMode() == ViewRefreshModeEnumType.ON_STOP) {
							linkType.setViewRefreshTime(config.getViewRefreshTime());
						}

						// confusion between atom:link and kml:Link in ogckml22.xsd
						networkLinkType.getRest().add(kmlFactory.createLink(linkType));
						networkLinkType.setRegion(regionType);
						folderType.getAbstractFeatureGroup().add(kmlFactory.createNetworkLink(networkLinkType));
					}
					SAXEventBuffer tmp = new SAXEventBuffer();
					marshaller.marshal(kmlFactory.createFolder(folderType), tmp);
					ioWriterPool.addWork(tmp);
				}
			}

			try {
				
				ioWriterPool.shutdownAndWait();
				fragmentWriter.setWriteMode(WriteMode.TAIL);
				marshaller.marshal(kml, fragmentWriter);
				saxWriter.flush();
			
			} catch (SAXException saxE) {
				Logger.getInstance().error("I/O error: " + saxE.getMessage());
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
			}
			outputStream.close();
		}
		catch (IOException ioe) {
			LOG.error(ioe.toString());
		}
		
	}

	private void addStyle(DisplayForm currentDisplayForm, CityGMLClass featureClass) throws JAXBException {

		
	//	if (!currentDisplayForm.isActive()) return;
		switch (featureClass) {
			case SOLITARY_VEGETATION_OBJECT:
			case PLANT_COVER:
				addStyle(currentDisplayForm,
						 config.getVegetationDisplayForms(),
						 Vegetation.STYLE_BASIS_NAME);
				break;

            case TUNNEL:
                addStyle(currentDisplayForm,
                        config.getTunnelDisplayForms(),
                        Tunnel.STYLE_BASIS_NAME);
                break;

            case BRIDGE:
                addStyle(currentDisplayForm,
                        config.getBridgeDisplayForms(),
                        Bridge.STYLE_BASIS_NAME);
                break;


			case TRAFFIC_AREA:
			case AUXILIARY_TRAFFIC_AREA:
			case TRANSPORTATION_COMPLEX:
			case TRACK:
			case RAILWAY:
			case ROAD:
			case SQUARE:
				addStyle(currentDisplayForm,
						 config.getTransportationDisplayForms(),
						 Transportation.STYLE_BASIS_NAME);
				break;


			case RASTER_RELIEF:
			case MASSPOINT_RELIEF:
			case BREAKLINE_RELIEF:
			case TIN_RELIEF:
			case RELIEF_FEATURE:
				addStyle(currentDisplayForm,
						 config.getReliefDisplayForms(),
						 Relief.STYLE_BASIS_NAME);
				break;

			case CITY_OBJECT_GROUP:
				addStyle(new DisplayForm(DisplayForm.FOOTPRINT, -1, -1), // hard-coded for groups
						 config.getCityObjectGroupDisplayForms(),
						 CityObjectGroup.STYLE_BASIS_NAME);
				break;

			case CITY_FURNITURE:
                addStyle(currentDisplayForm,
                        config.getCityFurnitureDisplayForms(),
                        CityFurniture.STYLE_BASIS_NAME);
				break;

			case GENERIC_CITY_OBJECT:
				addStyle(currentDisplayForm,
						 config.getGenericCityObjectDisplayForms(),
						 GenericCityObject.STYLE_BASIS_NAME);
				break;

			case LAND_USE:
				addStyle(currentDisplayForm,
						 config.getLandUseDisplayForms(),
						 LandUse.STYLE_BASIS_NAME);
				break;

			case WATER_BODY:
			case WATER_CLOSURE_SURFACE:
			case WATER_GROUND_SURFACE:
			case WATER_SURFACE:
				addStyle(currentDisplayForm,
						 config.getWaterBodyDisplayForms(),
						 WaterBody.STYLE_BASIS_NAME);
				break;

			case BUILDING: // must be last
			default:
				addStyle(currentDisplayForm,
						 config.getBuildingDisplayForms(),
						 Building.STYLE_BASIS_NAME);
		}
	}

	
	private void addStyle(DisplayForm currentDisplayForm,
						  List<DisplayForm> displayFormsForObjectType,
						  String styleBasisName) throws JAXBException {

		
		
		SAXEventBuffer saxBuffer = new SAXEventBuffer();
		Marshaller marshaller = jaxbKmlContext.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);

		BalloonStyleType balloonStyle = new BalloonStyleType();
		balloonStyle.setText("$[description]");

		switch (currentDisplayForm.getForm()) {
		case DisplayForm.FOOTPRINT:
		case DisplayForm.EXTRUDED:
			int indexOfDf = displayFormsForObjectType.indexOf(currentDisplayForm);
			String fillColor = Integer.toHexString(DisplayForm.DEFAULT_FILL_COLOR);
			String lineColor = Integer.toHexString(DisplayForm.DEFAULT_LINE_COLOR);
			String hlFillColor = Integer.toHexString(DisplayForm.DEFAULT_FILL_HIGHLIGHTED_COLOR);
			String hlLineColor = Integer.toHexString(DisplayForm.DEFAULT_LINE_HIGHLIGHTED_COLOR);
			
			if (indexOfDf != -1) {
				currentDisplayForm = displayFormsForObjectType.get(indexOfDf);
				if (currentDisplayForm.isSetRgba0()) {
					fillColor = DisplayForm.formatColorStringForKML(Integer.toHexString(currentDisplayForm.getRgba0()));
				}
				if (currentDisplayForm.isSetRgba1()) {
					lineColor = DisplayForm.formatColorStringForKML(Integer.toHexString(currentDisplayForm.getRgba1()));
				}
				if (currentDisplayForm.isSetRgba4()) {
					hlFillColor = DisplayForm.formatColorStringForKML(Integer.toHexString(currentDisplayForm.getRgba4()));
				}
				if (currentDisplayForm.isSetRgba5()) {
					hlLineColor = DisplayForm.formatColorStringForKML(Integer.toHexString(currentDisplayForm.getRgba5()));
				}
			}

			LineStyleType lineStyleFootprintNormal = kmlFactory.createLineStyleType();
			lineStyleFootprintNormal.setColor(hexStringToByteArray(lineColor));
			lineStyleFootprintNormal.setWidth(1.5);
			PolyStyleType polyStyleFootprintNormal = kmlFactory.createPolyStyleType();
			polyStyleFootprintNormal.setColor(hexStringToByteArray(fillColor));
			StyleType styleFootprintNormal = kmlFactory.createStyleType();
			styleFootprintNormal.setId(styleBasisName + currentDisplayForm.getName() + "Normal");
			styleFootprintNormal.setLineStyle(lineStyleFootprintNormal);
			styleFootprintNormal.setPolyStyle(polyStyleFootprintNormal);
			styleFootprintNormal.setBalloonStyle(balloonStyle);
			
			marshaller.marshal(kmlFactory.createStyle(styleFootprintNormal), saxBuffer);

			if (currentDisplayForm.isHighlightingEnabled()) {
				LineStyleType lineStyleFootprintHighlight = kmlFactory.createLineStyleType();
				lineStyleFootprintHighlight.setColor(hexStringToByteArray(hlLineColor));
				lineStyleFootprintHighlight.setWidth(1.5);
				PolyStyleType polyStyleFootprintHighlight = kmlFactory.createPolyStyleType();
				polyStyleFootprintHighlight.setColor(hexStringToByteArray(hlFillColor));
				StyleType styleFootprintHighlight = kmlFactory.createStyleType();
				styleFootprintHighlight.setId(styleBasisName + currentDisplayForm.getName() + "Highlight");
				styleFootprintHighlight.setLineStyle(lineStyleFootprintHighlight);
				styleFootprintHighlight.setPolyStyle(polyStyleFootprintHighlight);
				styleFootprintHighlight.setBalloonStyle(balloonStyle);

				PairType pairFootprintNormal = kmlFactory.createPairType();
				pairFootprintNormal.setKey(StyleStateEnumType.NORMAL);
				pairFootprintNormal.setStyleUrl("#" + styleFootprintNormal.getId());
				PairType pairFootprintHighlight = kmlFactory.createPairType();
				pairFootprintHighlight.setKey(StyleStateEnumType.HIGHLIGHT);
				pairFootprintHighlight.setStyleUrl("#" + styleFootprintHighlight.getId());
				StyleMapType styleMapFootprint = kmlFactory.createStyleMapType();
				styleMapFootprint.setId(styleBasisName + currentDisplayForm.getName() + "Style");
				styleMapFootprint.getPair().add(pairFootprintNormal);
				styleMapFootprint.getPair().add(pairFootprintHighlight);

				marshaller.marshal(kmlFactory.createStyle(styleFootprintHighlight), saxBuffer);
				marshaller.marshal(kmlFactory.createStyleMap(styleMapFootprint), saxBuffer);
			}

			ioWriterPool.addWork(saxBuffer);
			break;


		case DisplayForm.GEOMETRY:

            boolean isBuilding = Building.STYLE_BASIS_NAME.equals(styleBasisName);
            boolean isBridge = Bridge.STYLE_BASIS_NAME.equals(styleBasisName);
            boolean isTunnel = Tunnel.STYLE_BASIS_NAME.equals(styleBasisName);

            indexOfDf = displayFormsForObjectType.indexOf(currentDisplayForm);
            String wallFillColor = Integer.toHexString(org.citydb.config.project.kmlExporter.DisplayForm.DEFAULT_WALL_FILL_COLOR);
            String wallLineColor = Integer.toHexString(org.citydb.config.project.kmlExporter.DisplayForm.DEFAULT_WALL_LINE_COLOR);
            String roofFillColor = Integer.toHexString(org.citydb.config.project.kmlExporter.DisplayForm.DEFAULT_ROOF_FILL_COLOR);
            String roofLineColor = Integer.toHexString(org.citydb.config.project.kmlExporter.DisplayForm.DEFAULT_ROOF_LINE_COLOR);

			if (indexOfDf != -1) {
                currentDisplayForm = displayFormsForObjectType.get(indexOfDf);
                if (currentDisplayForm.isSetRgba0()) {
                    wallFillColor = org.citydb.config.project.kmlExporter.DisplayForm.formatColorStringForKML(Integer.toHexString(currentDisplayForm.getRgba0()));
                }
                if (currentDisplayForm.isSetRgba1()) {
                    wallLineColor = org.citydb.config.project.kmlExporter.DisplayForm.formatColorStringForKML(Integer.toHexString(currentDisplayForm.getRgba1()));
                }
                if (currentDisplayForm.isSetRgba2()) {
                    roofFillColor = org.citydb.config.project.kmlExporter.DisplayForm.formatColorStringForKML(Integer.toHexString(currentDisplayForm.getRgba2()));
                }
                if (currentDisplayForm.isSetRgba3()) {
                    roofLineColor = org.citydb.config.project.kmlExporter.DisplayForm.formatColorStringForKML(Integer.toHexString(currentDisplayForm.getRgba3()));
                }
            }

            LineStyleType lineStyleWallNormal = kmlFactory.createLineStyleType();
            lineStyleWallNormal.setColor(hexStringToByteArray(wallLineColor));
            PolyStyleType polyStyleWallNormal = kmlFactory.createPolyStyleType();
            polyStyleWallNormal.setColor(hexStringToByteArray(wallFillColor));
            StyleType styleWallNormal = kmlFactory.createStyleType();

            styleWallNormal.setLineStyle(lineStyleWallNormal);
            styleWallNormal.setPolyStyle(polyStyleWallNormal);
            styleWallNormal.setBalloonStyle(balloonStyle);

            if (isBuilding)
                styleWallNormal.setId(TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.BUILDING_WALL_SURFACE).toString() + "Normal");
            else if (isBridge)
                styleWallNormal.setId(TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.BRIDGE_WALL_SURFACE).toString() + "Normal");
            else if (isTunnel)
                styleWallNormal.setId(TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.TUNNEL_WALL_SURFACE).toString() + "Normal");
            else
                styleWallNormal.setId(styleBasisName + currentDisplayForm.getName() + "Normal");

            marshaller.marshal(kmlFactory.createStyle(styleWallNormal), saxBuffer);

            if (isBuilding)
                styleWallNormal.setId(TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.BUILDING_GROUND_SURFACE).toString() + "Normal");
            else if (isBridge)
                styleWallNormal.setId(TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.BRIDGE_GROUND_SURFACE).toString() + "Normal");
            else if (isTunnel)
                styleWallNormal.setId(TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.TUNNEL_GROUND_SURFACE).toString() + "Normal");

            marshaller.marshal(kmlFactory.createStyle(styleWallNormal), saxBuffer);

            LineStyleType lineStyleRoofNormal = kmlFactory.createLineStyleType();
            lineStyleRoofNormal.setColor(hexStringToByteArray(roofLineColor));
            PolyStyleType polyStyleRoofNormal = kmlFactory.createPolyStyleType();
            polyStyleRoofNormal.setColor(hexStringToByteArray(roofFillColor));
            StyleType styleRoofNormal = kmlFactory.createStyleType();

            styleRoofNormal.setLineStyle(lineStyleRoofNormal);
            styleRoofNormal.setPolyStyle(polyStyleRoofNormal);
            styleRoofNormal.setBalloonStyle(balloonStyle);

            if (isBuilding)
                styleRoofNormal.setId(TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.BUILDING_ROOF_SURFACE).toString() + "Normal");
            else if (isBridge)
                styleRoofNormal.setId(TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.BRIDGE_ROOF_SURFACE).toString() + "Normal");
            else if (isTunnel)
                styleRoofNormal.setId(TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.TUNNEL_ROOF_SURFACE).toString() + "Normal");

            marshaller.marshal(kmlFactory.createStyle(styleRoofNormal), saxBuffer);

            if (currentDisplayForm.isHighlightingEnabled()) {
                String highlightFillColor = Integer.toHexString(org.citydb.config.project.kmlExporter.DisplayForm.DEFAULT_FILL_HIGHLIGHTED_COLOR);
                String highlightLineColor = Integer.toHexString(org.citydb.config.project.kmlExporter.DisplayForm.DEFAULT_LINE_HIGHLIGHTED_COLOR);

                if (currentDisplayForm.isSetRgba4()) {
                    highlightFillColor = org.citydb.config.project.kmlExporter.DisplayForm.formatColorStringForKML(Integer.toHexString(currentDisplayForm.getRgba4()));
                }
                if (currentDisplayForm.isSetRgba5()) {
                    highlightLineColor = org.citydb.config.project.kmlExporter.DisplayForm.formatColorStringForKML(Integer.toHexString(currentDisplayForm.getRgba5()));
                }

                LineStyleType lineStyleGeometryInvisible = kmlFactory.createLineStyleType();
                lineStyleGeometryInvisible.setColor(hexStringToByteArray("01" + highlightLineColor.substring(2)));
                PolyStyleType polyStyleGeometryInvisible = kmlFactory.createPolyStyleType();
                polyStyleGeometryInvisible.setColor(hexStringToByteArray("00" + highlightFillColor.substring(2)));
                StyleType styleGeometryInvisible = kmlFactory.createStyleType();
                styleGeometryInvisible.setId(styleBasisName + currentDisplayForm.getName() + "StyleInvisible");
                styleGeometryInvisible.setLineStyle(lineStyleGeometryInvisible);
                styleGeometryInvisible.setPolyStyle(polyStyleGeometryInvisible);
                styleGeometryInvisible.setBalloonStyle(balloonStyle);

                LineStyleType lineStyleGeometryHighlight = kmlFactory.createLineStyleType();
                lineStyleGeometryHighlight.setColor(hexStringToByteArray(highlightLineColor));
                PolyStyleType polyStyleGeometryHighlight = kmlFactory.createPolyStyleType();
                polyStyleGeometryHighlight.setColor(hexStringToByteArray(highlightFillColor));
                StyleType styleGeometryHighlight = kmlFactory.createStyleType();
                styleGeometryHighlight.setId(styleBasisName + currentDisplayForm.getName() + "StyleHighlight");
                styleGeometryHighlight.setLineStyle(lineStyleGeometryHighlight);
                styleGeometryHighlight.setPolyStyle(polyStyleGeometryHighlight);
                styleGeometryHighlight.setBalloonStyle(balloonStyle);

                PairType pairGeometryNormal = kmlFactory.createPairType();
                pairGeometryNormal.setKey(StyleStateEnumType.NORMAL);
                pairGeometryNormal.setStyleUrl("#" + styleGeometryInvisible.getId());
                PairType pairGeometryHighlight = kmlFactory.createPairType();
                pairGeometryHighlight.setKey(StyleStateEnumType.HIGHLIGHT);
                pairGeometryHighlight.setStyleUrl("#" + styleGeometryHighlight.getId());
                StyleMapType styleMapGeometry = kmlFactory.createStyleMapType();
                styleMapGeometry.setId(styleBasisName + currentDisplayForm.getName() + "Style");
                styleMapGeometry.getPair().add(pairGeometryNormal);
                styleMapGeometry.getPair().add(pairGeometryHighlight);

                marshaller.marshal(kmlFactory.createStyle(styleGeometryInvisible), saxBuffer);
                marshaller.marshal(kmlFactory.createStyle(styleGeometryHighlight), saxBuffer);
                marshaller.marshal(kmlFactory.createStyleMap(styleMapGeometry), saxBuffer);
            }

			ioWriterPool.addWork(saxBuffer);
			break;

		case DisplayForm.COLLADA:
			
			indexOfDf = displayFormsForObjectType.indexOf(currentDisplayForm);
			if (indexOfDf != -1) {
				currentDisplayForm = displayFormsForObjectType.get(indexOfDf);
				if (currentDisplayForm.isHighlightingEnabled()) {
					String highlightFillColor = Integer.toHexString(DisplayForm.DEFAULT_FILL_HIGHLIGHTED_COLOR);
					String highlightLineColor = Integer.toHexString(DisplayForm.DEFAULT_LINE_HIGHLIGHTED_COLOR);
					if (currentDisplayForm.isSetRgba4()) {
						highlightFillColor = DisplayForm.formatColorStringForKML(Integer.toHexString(currentDisplayForm.getRgba4()));
					}
					if (currentDisplayForm.isSetRgba5()) {
						highlightLineColor = DisplayForm.formatColorStringForKML(Integer.toHexString(currentDisplayForm.getRgba5()));
					}

					LineStyleType lineStyleColladaInvisible = kmlFactory.createLineStyleType();
					lineStyleColladaInvisible.setColor(hexStringToByteArray("01" + highlightLineColor.substring(2)));
					PolyStyleType polyStyleColladaInvisible = kmlFactory.createPolyStyleType();
					polyStyleColladaInvisible.setColor(hexStringToByteArray("00" + highlightFillColor.substring(2)));
					StyleType styleColladaInvisible = kmlFactory.createStyleType();
					styleColladaInvisible.setId(styleBasisName + currentDisplayForm.getName() + "StyleInvisible");
					styleColladaInvisible.setLineStyle(lineStyleColladaInvisible);
					styleColladaInvisible.setPolyStyle(polyStyleColladaInvisible);
					styleColladaInvisible.setBalloonStyle(balloonStyle);
					
					LineStyleType lineStyleColladaHighlight = kmlFactory.createLineStyleType();
					lineStyleColladaHighlight.setColor(hexStringToByteArray(highlightLineColor));
					PolyStyleType polyStyleColladaHighlight = kmlFactory.createPolyStyleType();
					polyStyleColladaHighlight.setColor(hexStringToByteArray(highlightFillColor));
					StyleType styleColladaHighlight = kmlFactory.createStyleType();
					styleColladaHighlight.setId(styleBasisName + currentDisplayForm.getName() + "StyleHighlight");
					styleColladaHighlight.setLineStyle(lineStyleColladaHighlight);
					styleColladaHighlight.setPolyStyle(polyStyleColladaHighlight);
					styleColladaHighlight.setBalloonStyle(balloonStyle);
		
					PairType pairColladaNormal = kmlFactory.createPairType();
					pairColladaNormal.setKey(StyleStateEnumType.NORMAL);
					pairColladaNormal.setStyleUrl("#" + styleColladaInvisible.getId());
					PairType pairColladaHighlight = kmlFactory.createPairType();
					pairColladaHighlight.setKey(StyleStateEnumType.HIGHLIGHT);
					pairColladaHighlight.setStyleUrl("#" + styleColladaHighlight.getId());
					StyleMapType styleMapCollada = kmlFactory.createStyleMapType();
					styleMapCollada.setId(styleBasisName + currentDisplayForm.getName() +"Style");
					styleMapCollada.getPair().add(pairColladaNormal);
					styleMapCollada.getPair().add(pairColladaHighlight);
		
					marshaller.marshal(kmlFactory.createStyle(styleColladaInvisible), saxBuffer);
					marshaller.marshal(kmlFactory.createStyle(styleColladaHighlight), saxBuffer);
					marshaller.marshal(kmlFactory.createStyleMap(styleMapCollada), saxBuffer);
					ioWriterPool.addWork(saxBuffer);
				}
			}
			break;

			default:
			// no style
			break;
		}
	}

	private void addBorder(int i, int j) throws JAXBException {
		
		SAXEventBuffer saxBuffer = new SAXEventBuffer();
		Marshaller marshaller = jaxbKmlContext.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);

		
		if (config.getFilter().isSetComplexFilter() &&
				config.isShowTileBorders()) {
			saxBuffer = new SAXEventBuffer();

			// must be done like this to avoid non-matching tile limits
			double wgs84TileSouthLimit = wgs84TileMatrix.getLowerLeftCorner().getY() + (i * wgs84DeltaLatitude); 
			double wgs84TileNorthLimit = wgs84TileMatrix.getLowerLeftCorner().getY() + ((i+1) * wgs84DeltaLatitude); 
			double wgs84TileWestLimit = wgs84TileMatrix.getLowerLeftCorner().getX() + (j * wgs84DeltaLongitude); 
			double wgs84TileEastLimit = wgs84TileMatrix.getLowerLeftCorner().getX() + ((j+1) * wgs84DeltaLongitude); 

			PlacemarkType placemark = kmlFactory.createPlacemarkType();
			placemark.setName("Tile border");
			LineStringType lineString = kmlFactory.createLineStringType();
			lineString.setTessellate(true);
			lineString.getCoordinates().add(String.valueOf(wgs84TileWestLimit) + "," + wgs84TileSouthLimit);
			lineString.getCoordinates().add(String.valueOf(wgs84TileWestLimit) + "," + wgs84TileNorthLimit);
			lineString.getCoordinates().add(String.valueOf(wgs84TileEastLimit) + "," + wgs84TileNorthLimit);
			lineString.getCoordinates().add(String.valueOf(wgs84TileEastLimit) + "," + wgs84TileSouthLimit);
			lineString.getCoordinates().add(String.valueOf(wgs84TileWestLimit) + "," + wgs84TileSouthLimit);
			placemark.setAbstractGeometryGroup(kmlFactory.createLineString(lineString));
			
			
			marshaller.marshal(kmlFactory.createPlacemark(placemark), saxBuffer);

			ioWriterPool.addWork(saxBuffer);

			
		}
	}


	private byte[] hexStringToByteArray(String hex) {
		// padding if needed
		if (hex.length()/2 != (hex.length()+1)/2) {
			hex = "0" + hex;
		}
			
		byte[] bytes = new byte[hex.length()/2];
		try {
			for (int i = 0; i < bytes.length; i++) {
				bytes[i] = (byte) Integer.parseInt(hex.substring(2*i, 2*i+2), 16);
			}
		} catch ( Exception e ) {
			e.printStackTrace();
			return null;
		}
		return bytes;
	}


	private boolean checkBalloonSettings (CityGMLClass cityObjectType) {
		Balloon balloonSettings = null;
		boolean settingsMustBeChecked = false;
		switch (cityObjectType) {
			case BUILDING:
				balloonSettings = config.getBuildingBalloon();
				settingsMustBeChecked = config.getFilter().getComplexFilter().getFeatureClass().isSetBuilding();
				break;
			case WATER_BODY:
				balloonSettings = config.getWaterBodyBalloon();
				settingsMustBeChecked = config.getFilter().getComplexFilter().getFeatureClass().isSetWaterBody();
				break;
			case LAND_USE:
				balloonSettings = config.getLandUseBalloon();
				settingsMustBeChecked = config.getFilter().getComplexFilter().getFeatureClass().isSetLandUse();
				break;
			case SOLITARY_VEGETATION_OBJECT:
				balloonSettings = config.getVegetationBalloon();
				settingsMustBeChecked = config.getFilter().getComplexFilter().getFeatureClass().isSetVegetation();
				break;
			case TRANSPORTATION_COMPLEX:
				balloonSettings = config.getTransportationBalloon();
				settingsMustBeChecked = config.getFilter().getComplexFilter().getFeatureClass().isSetTransportation();
				break;
			case RELIEF_FEATURE:
				balloonSettings = config.getReliefBalloon();
				settingsMustBeChecked = config.getFilter().getComplexFilter().getFeatureClass().isSetReliefFeature();
				break;
			case CITY_FURNITURE:
				balloonSettings = config.getCityFurnitureBalloon();
				settingsMustBeChecked = config.getFilter().getComplexFilter().getFeatureClass().isSetCityFurniture();
				break;
			case GENERIC_CITY_OBJECT:
				balloonSettings = config.getGenericCityObjectBalloon();
				settingsMustBeChecked = config.getFilter().getComplexFilter().getFeatureClass().isSetGenericCityObject();
				break;
			case CITY_OBJECT_GROUP:
				balloonSettings = config.getCityObjectGroupBalloon();
				settingsMustBeChecked = config.getFilter().getComplexFilter().getFeatureClass().isSetCityObjectGroup();
				break;
			default:
				return false;
		}
		
		if (settingsMustBeChecked &&
			balloonSettings.isIncludeDescription() &&
			balloonSettings.getBalloonContentMode() != BalloonContentMode.GEN_ATTRIB) {
			String balloonTemplateFilename = balloonSettings.getBalloonContentTemplateFile();
			if (balloonTemplateFilename != null && balloonTemplateFilename.length() > 0) {
				File ballonTemplateFile = new File(balloonTemplateFilename);
				if (!ballonTemplateFile.exists()) {
					Logger.getInstance().error("Balloon template file \"" + balloonTemplateFilename + "\" not found.");
					return false;
				}
			}
		}
		return true;
	}	
	
	
	public int calculateRowsColumns() throws SQLException {
		
		TiledBoundingBox bbox = config.getFilter().getComplexFilter().getTiledBoundingBox();
		double autoTileSideLength = config.getAutoTileSideLength();
		extent = bbox;

		DatabaseSrs bboxSrs = bbox.getSrs();
		if (bboxSrs.getSrid() != 0 && bboxSrs.getSrid() != Integer.parseInt(TargetSrs)) {
			wgs84TileMatrix = ProjConvertor.transformBBox(extent, String.valueOf(bbox.getSrs().getSrid()), "4326");
			extent = ProjConvertor.transformBBox(extent, String.valueOf(bbox.getSrs().getSrid()), TargetSrs);
		}
		else {
			wgs84TileMatrix = ProjConvertor.transformBBox(extent, TargetSrs, "4326");
		}

		// retrieve WGS84 extent of bbox
//		wgs84Extent = convertTileToWGS84(extent);

		// determine tile sizes and derive number of rows and columns
		switch (bbox.getTiling().getMode()) {
		case AUTOMATIC:
			// approximate
			rows = (int)((extent.getUpperRightCorner().getY() - extent.getLowerLeftCorner().getY()) / autoTileSideLength) + 1;
			columns = (int)((extent.getUpperRightCorner().getX() - extent.getLowerLeftCorner().getX()) / autoTileSideLength) + 1;
			bbox.getTiling().setRows(rows);
			bbox.getTiling().setColumns(columns);
			break;
		case NO_TILING:
			// no_tiling is internally mapped to manual tiling with one tile
			bbox.getTiling().setMode(TilingMode.MANUAL);
			bbox.getTiling().setRows(1);
			bbox.getTiling().setColumns(1);
		default:
			rows = bbox.getTiling().getRows();
			columns = bbox.getTiling().getColumns();
		}
		
		wgs84DeltaLatitude = (wgs84TileMatrix.getUpperRightCorner().getY() - wgs84TileMatrix.getLowerLeftCorner().getY()) / rows;
		wgs84DeltaLongitude = (wgs84TileMatrix.getUpperRightCorner().getX() - wgs84TileMatrix.getLowerLeftCorner().getX()) / columns;
	

		return rows * columns;
	}
	

	private static void getAllFiles(File startFolder, List<File> fileList) {
		File[] files = startFolder.listFiles();
		for (File file : files) {
			fileList.add(file);
			if (file.isDirectory())
				getAllFiles(file, fileList);
		}
	}


	private static void deleteFolder(File folder) {
	    if (folder == null) return;
	    File[] files = folder.listFiles();
	    if (files != null) {
	        for (File f: files) {
	            if (f.isDirectory())
	                deleteFolder(f);
	            else
	                f.delete();
	        }
	    }
	    folder.delete();
	}

	@Override
	public void handleEvent(Event e) throws Exception {

		if (e.getEventType() == EventType.COUNTER &&
				((CounterEvent)e).getType() == CounterType.TOPLEVEL_FEATURE) {

			CityGMLClass type = null;
			Object kmlExportObject = e.getSource();

			if (kmlExportObject instanceof Building) {
				type = CityGMLClass.BUILDING;
			}
			else if (kmlExportObject instanceof WaterBody) {
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
            else if (kmlExportObject instanceof Bridge) {
                type = CityGMLClass.BRIDGE;
            }
            else if (kmlExportObject instanceof Tunnel) {
                type = CityGMLClass.TUNNEL;
            }
			else if (kmlExportObject instanceof CityFurniture) {
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
		else if (e.getEventType() == EventType.GEOMETRY_COUNTER) {
			geometryCounter++;
		}
		else if (e.getEventType() == EventType.INTERRUPT) {
			if (isInterrupted.compareAndSet(false, true)) {
				shouldRun = false;

				String log = ((InterruptEvent)e).getLogMessage();
				if (log != null)
					Logger.getInstance().log(((InterruptEvent)e).getLogLevelType(), log);

				Logger.getInstance().info("Waiting for objects being currently processed to end...");

				if (kmlSplitter != null)
					kmlSplitter.shutdown();

				if (kmlWorkerPool != null) {
					kmlWorkerPool.shutdownNow();
				}

				if (lastTempFolder != null && lastTempFolder.exists()) deleteFolder(lastTempFolder); // just in case
			}
		}
	}


	public static HashMap<String, CityObject4JSON> getAlreadyExported() {
		return alreadyExported;
	}
	

}
