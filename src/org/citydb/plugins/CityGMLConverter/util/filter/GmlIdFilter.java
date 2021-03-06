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
package org.citydb.plugins.CityGMLConverter.util.filter;

import java.util.List;

import org.citydb.config.Config;
import org.citydb.config.project.filter.AbstractFilterConfig;
import org.citydb.config.project.filter.GmlId;
import org.citydb.plugins.CityGMLConverter.config.ConfigImpl;

public class GmlIdFilter implements Filter<String> {
	private final AbstractFilterConfig filterConfig;
	private boolean isActive;
	private GmlId gmlIdFilter;

	public GmlIdFilter(ConfigImpl config, FilterMode mode) {
	
		filterConfig = config.getFilter();	
		init();
	}

	private void init() {
		isActive = filterConfig.isSetSimpleFilter();
		if (isActive)
			gmlIdFilter = filterConfig.getSimpleFilter().getGmlIdFilter();			
	}

	@Override
	public boolean isActive() {
		return isActive;
	}

	public void reset() {
		init();
	}

	public boolean filter(String gmlId) {
		if (isActive) {
			List<String> gmlIdList = gmlIdFilter.getGmlIds();
			if (gmlIdList != null) {
				for (String item : gmlIdList)
					if (gmlId.equals(item))
						return false;
			}

			return true;
		}

		return false;
	}

	public List<String> getFilterState() {
		return getInternalState(false);
	}

	public List<String> getNotFilterState() {
		return getInternalState(true);
	}

	private List<String> getInternalState(boolean inverse) {
		if (isActive) {
			if (!inverse)
				return gmlIdFilter.getGmlIds();
			else
				return null;			
		} 

		return null;
	}
}
