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


import java.util.concurrent.ConcurrentLinkedQueue;

import javax.xml.bind.JAXBContext;

import net.opengis.kml._2.ObjectFactory;

import org.citydb.api.concurrent.Worker;
import org.citydb.api.concurrent.WorkerFactory;
import org.citydb.api.concurrent.WorkerPool;
import org.citydb.api.event.EventDispatcher;
import org.citydb.plugins.CityGMLConverter.config.ConfigImpl;
import org.citydb.plugins.CityGMLConverter.content.KmlSplittingResult;
import org.citydb.plugins.CityGMLConverter.xlink.content.DBXlink;
import org.citygml4j.builder.jaxb.JAXBBuilder;
import org.citygml4j.factory.GMLGeometryFactory;
import org.citygml4j.model.citygml.CityGML;
import org.citygml4j.util.xml.SAXEventBuffer;



public class BBoxCalculatorWorkerFactory implements WorkerFactory<CityGML> {
	private final JAXBBuilder jaxbBuilder;
	private final ConfigImpl config;
	private final String SourceSRS;
	private final EventDispatcher eventDispatcher;

	public BBoxCalculatorWorkerFactory(
			JAXBBuilder jaxbBuilder,
			ConfigImpl config,
			String SourceSRS,
			EventDispatcher eventDispatcher) {
		this.jaxbBuilder = jaxbBuilder;
		this.config = config;
		this.SourceSRS = SourceSRS;
		this.eventDispatcher = eventDispatcher;
	}

	@Override
	public Worker<CityGML> createWorker() {
		BBoxCalculatorWorker bboxWorker = null;

		try {
			bboxWorker = new BBoxCalculatorWorker(
					jaxbBuilder,
					config,
					SourceSRS,
					eventDispatcher);
		} catch (Exception Ex) {
			// could not instantiate DBWorker
		}

		return bboxWorker;
	}

}
