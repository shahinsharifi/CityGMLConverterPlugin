package org.citydb.plugins.CityGMLConverter.concurrent;

import org.citydb.api.concurrent.Worker;
import org.citydb.api.concurrent.WorkerFactory;
import org.citydb.api.event.EventDispatcher;
import org.citydb.plugins.CityGMLConverter.config.ConfigImpl;
import org.citygml4j.builder.jaxb.JAXBBuilder;
import org.citygml4j.model.citygml.CityGML;

public class AppearanceWorkerFactory  implements WorkerFactory<CityGML>{
	private final JAXBBuilder jaxbBuilder;
	private final ConfigImpl config;
	private final EventDispatcher eventDispatcher;

	public AppearanceWorkerFactory(
			JAXBBuilder jaxbBuilder,
			ConfigImpl config,
			EventDispatcher eventDispatcher) {
		this.jaxbBuilder = jaxbBuilder;
		this.config = config;
		this.eventDispatcher = eventDispatcher;
	}

	@Override
	public Worker<CityGML> createWorker() {
		AppearanceWorker appearanceWorker = null;

		try {
			appearanceWorker = new AppearanceWorker(
					jaxbBuilder,
					config,
					eventDispatcher);
		} catch (Exception Ex) {
			// could not instantiate DBWorker
		}

		return appearanceWorker;
	}
}
