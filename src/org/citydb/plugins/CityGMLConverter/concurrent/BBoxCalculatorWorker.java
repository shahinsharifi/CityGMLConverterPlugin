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

import java.util.concurrent.locks.ReentrantLock;

import org.citygml4j.builder.jaxb.JAXBBuilder;
import org.citygml4j.model.citygml.CityGML;
import org.citygml4j.model.citygml.core.AbstractCityObject;
import org.citygml4j.model.gml.feature.AbstractFeature;
import org.citygml4j.model.gml.feature.BoundingShape;
import org.citydb.api.concurrent.Worker;
import org.citydb.api.concurrent.WorkerPool;
import org.citydb.api.concurrent.WorkerPool.WorkQueue;
import org.citydb.api.event.EventDispatcher;
import org.citydb.log.Logger;
import org.citydb.plugins.CityGMLConverter.config.ConfigImpl;
import org.citydb.plugins.CityGMLConverter.util.BoundingBox;
import org.citydb.plugins.CityGMLConverter.xlink.content.DBXlink;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;


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
	private final String SourceSRS;
	private final EventDispatcher eventDispatcher;

	

	public BBoxCalculatorWorker(JAXBBuilder jaxbBuilder,
			ConfigImpl config,
			String SourceSRS,
			EventDispatcher eventDispatcher) throws Exception {
		
		this.jaxbBuilder = jaxbBuilder;
		this.config = config;
		this.SourceSRS = SourceSRS;
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
			AbstractCityObject cityObject = (AbstractCityObject)work;
			org.citygml4j.model.gml.geometry.primitives.Envelope envelope = null;
			
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
						CRS.decode("EPSG:" + SourceSRS, true));
				
				
				BoundingBox.addNodeToRtree(work, _refEnvelope, SourceSRS);
			}

		}
		catch(Exception ex){
			Logger.getInstance().error(ex.toString());
		}
		finally {
			runLock.unlock();
		}
	}

}
