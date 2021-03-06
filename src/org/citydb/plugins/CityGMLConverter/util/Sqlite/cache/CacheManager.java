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
package org.citydb.plugins.CityGMLConverter.util.Sqlite.cache;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

import org.citydb.plugins.CityGMLConverter.util.Sqlite.cache.model.CacheTableModelEnum;


public class CacheManager {
	private Connection conn;	
	private ConcurrentHashMap<CacheTableModelEnum, CacheTable> cacheTableMap;
	
	public CacheManager() {
		
	}
	
	public CacheManager(Connection conn, int concurrencyLevel) {
		this.conn = conn;
		
		cacheTableMap = new ConcurrentHashMap<CacheTableModelEnum, CacheTable>(
				CacheTableModelEnum.values().length,
				0.75f,
				concurrencyLevel
		);
	}

	public TemporaryCacheTable createTemporaryCacheTable(CacheTableModelEnum model) throws SQLException {
		TemporaryCacheTable temporaryTable = (TemporaryCacheTable)getOrCreateCacheTable(model, CacheTableEnum.TEMPORARY);
		if (!temporaryTable.isCreated())
			temporaryTable.create();
		
		return temporaryTable;
	}
	
	public TemporaryCacheTable createTemporaryCacheTableWithIndexes(CacheTableModelEnum model) throws SQLException {
		TemporaryCacheTable temporaryTable = (TemporaryCacheTable)getOrCreateCacheTable(model, CacheTableEnum.TEMPORARY);
		if (!temporaryTable.isCreated())
			temporaryTable.createWithIndexes();
		
		return temporaryTable;
	}
	
	public BranchTemporaryCacheTable createBranchTemporaryCacheTable(CacheTableModelEnum model) throws SQLException {
		BranchTemporaryCacheTable branchTable = (BranchTemporaryCacheTable)getOrCreateCacheTable(model, CacheTableEnum.BRANCH_TEMPORARY);
		if (!branchTable.isCreated())
			branchTable.create();
		
		return branchTable;
	}
	
	public BranchTemporaryCacheTable createBranchTemporaryCacheTableWithIndexes(CacheTableModelEnum model) throws SQLException {
		BranchTemporaryCacheTable branchTable = (BranchTemporaryCacheTable)getOrCreateCacheTable(model, CacheTableEnum.BRANCH_TEMPORARY);
		if (!branchTable.isCreated())
			branchTable.createWithIndexes();
		
		return branchTable;
	}
	
	public CacheTable getCacheTable(CacheTableModelEnum type) {
		return cacheTableMap.get(type);
	}
	
	public HeapCacheTable getDerivedHeapCacheTable(CacheTableModelEnum type) {
		CacheTable cacheTable = cacheTableMap.get(type);
		if (cacheTable instanceof TemporaryCacheTable)
			return ((TemporaryCacheTable)cacheTable).getHeapCacheTable();
		else
			return null;
	}
	
	public boolean existsTemporaryCacheTable(CacheTableModelEnum type) {
		CacheTable cacheTable = cacheTableMap.get(type);
		return (cacheTable instanceof TemporaryCacheTable 
				&& ((TemporaryCacheTable)cacheTable).isCreated());
	}
	
	private CacheTable getOrCreateCacheTable(CacheTableModelEnum model, CacheTableEnum type) {
		CacheTable cacheTable = cacheTableMap.get(model);
		
		if (cacheTable == null) {
			CacheTable newCacheTable = null;
			
			switch (type) {
			case TEMPORARY:
				newCacheTable = new TemporaryCacheTable(model, conn);
				break;
			case HEAP:
				newCacheTable = new HeapCacheTable(model, conn);
				break;
			case BRANCH_TEMPORARY:
				newCacheTable = new BranchTemporaryCacheTable(model, conn);
				break;
			default:
				throw new IllegalArgumentException("Unsupported cache table type " + type);
			}
			
			cacheTable = cacheTableMap.putIfAbsent(model, newCacheTable);
			if (cacheTable == null)
				cacheTable = newCacheTable;
		}
			
		return cacheTable;
	}
		
	public void drop(CacheTable cacheTable) throws SQLException {
		switch (cacheTable.getType()) {
		case TEMPORARY:
			((TemporaryCacheTable)cacheTable).drop();
			break;
		case HEAP:
			((HeapCacheTable)cacheTable).drop();
			break;
		case BRANCH_TEMPORARY:
			((BranchTemporaryCacheTable)cacheTable).drop();
			break;
		default:
			throw new IllegalArgumentException("Unsupported cache table type " + cacheTable.getType());
		}
		
		cacheTableMap.remove(cacheTable.getType());
	}
	
	public void dropAll() throws SQLException {
		for (CacheTable cacheTable : cacheTableMap.values())
			switch (cacheTable.getType()) {
			case TEMPORARY:
				((TemporaryCacheTable)cacheTable).drop();
				break;
			case HEAP:
				((HeapCacheTable)cacheTable).drop();
				break;
			case BRANCH_TEMPORARY:
				((BranchTemporaryCacheTable)cacheTable).drop();
				break;
			default:
				throw new IllegalArgumentException("Unsupported cache table type " + cacheTable.getType());
			}
		
		cacheTableMap.clear();
	}
}
