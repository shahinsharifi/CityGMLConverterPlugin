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

import org.citygml4j.builder.jaxb.JAXBBuilder;
import org.citygml4j.factory.GMLGeometryFactory;
import org.citygml4j.model.citygml.CityGML;
import org.citygml4j.model.citygml.CityGMLClass;
import org.citygml4j.util.xml.SAXEventBuffer;
import org.citydb.api.concurrent.Worker;
import org.citydb.api.concurrent.WorkerPool;
import org.citydb.api.concurrent.WorkerPool.WorkQueue;
import org.citydb.api.database.DatabaseAdapter;
import org.citydb.api.database.DatabaseType;
import org.citydb.api.event.EventDispatcher;
import org.citydb.api.event.EventHandler;
import org.citydb.config.project.exporter.ExportFilterConfig;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
import org.citydb.database.adapter.BlobExportAdapter;
import org.citydb.database.adapter.BlobType;
import org.citydb.database.adapter.DatabaseAdapterFactory;
import org.citydb.log.Logger;
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
import org.citydb.plugins.CityGMLConverter.util.BoundingBox;
import org.citydb.plugins.CityGMLConverter.util.Sqlite.SqliteImporterManager;
import org.citydb.plugins.CityGMLConverter.xlink.content.DBXlink;


public class BBoxCalculatorWorker implements Worker<CityGML>{

	// instance members needed for WorkPool
	private volatile boolean shouldRun = true;
	private ReentrantLock runLock = new ReentrantLock();
	private WorkQueue<CityGML> workQueue = null;
	private WorkerPool<DBXlink> tmpXlinkPool;
	private CityGML firstWork;
	private Thread workerThread = null;

	// instance members needed to do work
	private final JAXBBuilder jaxbBuilder;
	private final ConfigImpl config;
	private final EventDispatcher eventDispatcher;

	

	public BBoxCalculatorWorker(JAXBBuilder jaxbBuilder,
			ConfigImpl config,
			EventDispatcher eventDispatcher) throws Exception {
		
		this.jaxbBuilder = jaxbBuilder;
		this.config = config;
		this.eventDispatcher = eventDispatcher;

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
	public void setFirstWork(CityGML firstWork) {
		this.firstWork = firstWork;
	}

	@Override
	public void setThread(Thread workerThread) {
		this.workerThread = workerThread;
	}

	@Override
	public void setWorkQueue(WorkQueue<CityGML> workQueue) {
		this.workQueue = workQueue;
	}

	@Override
	public void run() {
		try {
			if (firstWork != null && shouldRun) {
				doWork(firstWork);
				firstWork = null;
			}

			CityGML work = null; 
			while (shouldRun) {
				try {
					work = workQueue.take();
					doWork(work);
				}
				catch (InterruptedException ie) {
					// re-check state
				}
			}

		}
		finally {
			
		}
	}

	private void doWork(CityGML work) {
		final ReentrantLock runLock = this.runLock;
		runLock.lock();
		
		try {

			Logger.getInstance().error("BoundingBox for thread: ");
		}
		finally {
			runLock.unlock();
		}
	}


}