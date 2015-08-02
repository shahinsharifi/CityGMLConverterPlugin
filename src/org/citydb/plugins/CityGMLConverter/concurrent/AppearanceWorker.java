package org.citydb.plugins.CityGMLConverter.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.citydb.api.concurrent.Worker;
import org.citydb.api.concurrent.WorkerPool;
import org.citydb.api.concurrent.WorkerPool.WorkQueue;
import org.citydb.api.event.EventDispatcher;
import org.citydb.log.Logger;
import org.citydb.modules.common.event.CounterEvent;
import org.citydb.modules.common.event.CounterType;
import org.citydb.plugins.CityGMLConverter.config.ConfigImpl;
import org.citydb.plugins.CityGMLConverter.util.BoundingBox;
import org.citydb.plugins.CityGMLConverter.util.ThemeUtil;
import org.citydb.plugins.CityGMLConverter.xlink.content.DBXlink;
import org.citygml4j.builder.jaxb.JAXBBuilder;
import org.citygml4j.model.citygml.CityGML;
import org.citygml4j.model.citygml.CityGMLClass;
import org.citygml4j.model.citygml.appearance.Appearance;
import org.citygml4j.model.citygml.building.AbstractBuilding;
import org.citygml4j.model.citygml.building.BuildingPart;
import org.citygml4j.model.citygml.building.BuildingPartProperty;
import org.citygml4j.model.citygml.core.AbstractCityObject;
import org.citygml4j.model.gml.feature.AbstractFeature;
import org.citygml4j.model.gml.feature.BoundingShape;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;

public class AppearanceWorker  implements Worker<CityGML>{
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
	List<Object> tmpAppearanceList = new ArrayList<Object>();

	

	public AppearanceWorker(JAXBBuilder jaxbBuilder,
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
			CityGML cityGML = work;
			if(cityGML.getCityGMLClass() == CityGMLClass.APPEARANCE)
			{
				Appearance _appreance = (Appearance)cityGML;
				ThemeUtil.getTmpAppearanceList().add(_appreance);

			}
			else{

				AbstractCityObject cityObject = (AbstractCityObject)cityGML;

				if (cityObject!=null) {					
					if(cityObject.isSetAppearance())
						ThemeUtil.getTmpAppearanceList().addAll(cityObject.getAppearance());
					else if(cityGML.getCityGMLClass() == CityGMLClass.BUILDING){

						AbstractBuilding building = (AbstractBuilding)cityObject;
						if(building.isSetConsistsOfBuildingPart())
						{
							for(BuildingPartProperty buidingPart : building.getConsistsOfBuildingPart())
							{
								BuildingPart tmpBuildingPart = buidingPart.getBuildingPart();
								if(tmpBuildingPart.isSetAppearance())
								{	
									ThemeUtil.getTmpAppearanceList().addAll(tmpBuildingPart.getAppearance());
									break;
								}
							}
						}
					}
				}
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
