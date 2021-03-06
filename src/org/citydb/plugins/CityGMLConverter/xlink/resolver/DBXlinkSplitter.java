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
package org.citydb.plugins.CityGMLConverter.xlink.resolver;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.citydb.api.concurrent.WorkerPool;
import org.citydb.api.event.EventDispatcher;
import org.citydb.log.Logger;
import org.citydb.plugins.CityGMLConverter.content.SurfaceObject;
import org.citydb.plugins.CityGMLConverter.content.TableEnum;
import org.citydb.plugins.CityGMLConverter.events.StatusDialogMessage;
import org.citydb.plugins.CityGMLConverter.events.StatusDialogProgressBar;


import org.citydb.plugins.CityGMLConverter.util.Util;
import org.citydb.plugins.CityGMLConverter.util.Sqlite.cache.CacheManager;
import org.citydb.plugins.CityGMLConverter.util.Sqlite.cache.CacheTable;
import org.citydb.plugins.CityGMLConverter.util.Sqlite.cache.HeapCacheTable;
import org.citydb.plugins.CityGMLConverter.util.Sqlite.cache.TemporaryCacheTable;
import org.citydb.plugins.CityGMLConverter.util.Sqlite.cache.model.CacheTableModelEnum;
import org.citydb.plugins.CityGMLConverter.xlink.content.DBXlink;
import org.citydb.plugins.CityGMLConverter.xlink.content.DBXlinkBasic;
import org.citydb.plugins.CityGMLConverter.xlink.content.DBXlinkDeprecatedMaterial;
import org.citydb.plugins.CityGMLConverter.xlink.content.DBXlinkGroupToCityObject;
import org.citydb.plugins.CityGMLConverter.xlink.content.DBXlinkLibraryObject;
import org.citydb.plugins.CityGMLConverter.xlink.content.DBXlinkTextureFile;
import org.citydb.plugins.CityGMLConverter.xlink.content.DBXlinkTextureFileEnum;
import org.citydb.plugins.CityGMLConverter.xlink.content.DBXlinkTextureParam;
import org.citydb.plugins.CityGMLConverter.xlink.content.DBXlinkTextureParamEnum;




public class DBXlinkSplitter {
	private final Logger LOG = Logger.getInstance();

	private  CacheManager cacheManager;
	private  WorkerPool<DBXlink> xlinkResolverPool;
	private  WorkerPool<DBXlink> tmpXlinkPool;
	private  EventDispatcher eventDispatcher;
	private volatile boolean shouldRun = true;
	private static java.util.List<SurfaceObject> list = new ArrayList<SurfaceObject>();

	
	public DBXlinkSplitter()
	{
		
	}
	
	public DBXlinkSplitter(CacheManager cacheManager, 
			WorkerPool<DBXlink> xlinkResolverPool, 
			WorkerPool<DBXlink> tmpXlinkPool, 
			EventDispatcher eventDispatcher) {
		this.cacheManager = cacheManager;
		this.xlinkResolverPool = xlinkResolverPool;
		this.tmpXlinkPool = tmpXlinkPool;
		this.eventDispatcher = eventDispatcher;
	}

	public void shutdown() {
		shouldRun = false;
	}

	public java.util.List<SurfaceObject> startQuery(java.util.List<SurfaceObject> _list) throws SQLException {
		/*basicXlinks();
		groupMemberXLinks(true);
		appearanceXlinks();
		libraryObjectXLinks();

		if (!shouldRun)
			return;
		
		// restart xlink worker pools
		// just to make sure all appearance xlinks have been handled
		// before starting to work on geometry xlinks
		try {
			xlinkResolverPool.join();
			tmpXlinkPool.join();
		} catch (InterruptedException e) {
			//
		}

		// xlinks to deprecated appearances can only be handled if
		// appearances have been fully written - otherwise information is
		// missing in tables SURFACE_DATA and TEXTURPARAM
		deprecatedMaterialXlinks();

		// handling geometry xlinks is more tricky...
		// the reason is that we really hard copy the entries within the database.
		// now imagine the following situation: a geometry referenced by an xlink
		// itself points to another geometry. in order to really copy any information
		// we have to resolve the inner xlink firstly. afterwards we can deal with the
		// outer xlink. thus, we need a recursive handling here...*/
		list.addAll(_list);

		return surfaceGeometryXlinks(true);
		
		 
	}

	private void basicXlinks() throws SQLException {
		if (!shouldRun)
			return;

		Statement stmt = null;
		ResultSet rs = null;

		try {
			
			CacheTable cacheTable = cacheManager.getCacheTable(CacheTableModelEnum.BASIC);	
			if (!(cacheTable instanceof TemporaryCacheTable))
				return;

			LOG.info("Resolving feature XLinks...");
			eventDispatcher.triggerEvent(new StatusDialogProgressBar(0, 0, this));
			eventDispatcher.triggerEvent(new StatusDialogMessage(Util.I18N.getString("import.dialog.basicXLink.msg"), this));

			TemporaryCacheTable temporaryTable = (TemporaryCacheTable)cacheTable;
			
			int max = (int)temporaryTable.size();
			int current = 0;
			
			stmt = temporaryTable.getConnection().createStatement();
			rs = stmt.executeQuery("select * from " + temporaryTable.getTableName());

			while (rs.next() && shouldRun) {
				eventDispatcher.triggerEvent(new StatusDialogProgressBar(++current, max, this));

				String id = rs.getString("ID");
				int fromTable = rs.getInt("FROM_TABLE");
				String gmlId = rs.getString("GMLID");
				int toTable = rs.getInt("TO_TABLE");
				String attrName = rs.getString("ATTRNAME");

				// set initial context...
				DBXlinkBasic xlink = new DBXlinkBasic(
						id,
						TableEnum.fromInt(fromTable),
						gmlId,
						TableEnum.fromInt(toTable));

				if (attrName != null && attrName.length() != 0)
					xlink.setAttrName(attrName);

				xlinkResolverPool.addWork(xlink);
			}
		} finally {
			if (rs != null) {
				rs.close();
				rs = null;
			}

			if (stmt != null) {
				stmt.close();
				stmt = null;
			}
		}
	}
	
	private void groupMemberXLinks(boolean checkRecursive) throws SQLException {
		if (!shouldRun)
			return;

		CacheTable cacheTable = cacheManager.getCacheTable(CacheTableModelEnum.GROUP_TO_CITYOBJECT);		
		if (!(cacheTable instanceof TemporaryCacheTable))
			return;

		LOG.info("Resolving CityObjectGroup XLinks...");

		queryGroupMemberXLinks((TemporaryCacheTable)cacheTable, checkRecursive, -1, 1);
	}

	private void queryGroupMemberXLinks(TemporaryCacheTable cacheTable, 
			boolean checkRecursive, 
			long remaining, 
			int pass) throws SQLException {
		
		Statement stmt = null;
		ResultSet rs = null;

		try {					
			eventDispatcher.triggerEvent(new StatusDialogProgressBar(0, 0, this));
			String text = Util.I18N.getString("import.dialog.groupXLink.msg");
			Object[] args = new Object[]{ pass };
			eventDispatcher.triggerEvent(new StatusDialogMessage(MessageFormat.format(text, args), this));

			int max = (remaining == -1) ? (int)cacheTable.size() : (int)remaining;
			int current = 0;

			HeapCacheTable heapTable = cacheTable.deriveHeapCacheTableWithIndexes();
			cacheTable.truncate();
			
			stmt = heapTable.getConnection().createStatement();
			rs = stmt.executeQuery("select * from " + heapTable.getTableName());

			while (rs.next() && shouldRun) {
				eventDispatcher.triggerEvent(new StatusDialogProgressBar(++current, max, this));

				String groupId = rs.getString("GROUP_ID");
				String gmlId = rs.getString("GMLID");
				int isParent = rs.getInt("IS_PARENT");
				String role = rs.getString("ROLE");

				// set initial context...
				DBXlinkGroupToCityObject xlink = new DBXlinkGroupToCityObject(
						groupId,
						gmlId,
						isParent == 1);

				xlink.setRole(role);
				xlinkResolverPool.addWork(xlink);
			}

			if (checkRecursive && shouldRun) {
				rs.close();
				stmt.close();

				try {
					xlinkResolverPool.join();
					tmpXlinkPool.join();
				} catch (InterruptedException e) {
					//
				}

				long unresolved = cacheTable.size();
				if (unresolved > 0) {
					if (unresolved != remaining) {
						// we still have unresolved xlinks... so do another recursion
						cacheTable.dropHeapCacheTable();
						queryGroupMemberXLinks(cacheTable, checkRecursive, unresolved, ++pass);
					} else {
						// we detected a cycle and cannot resolve the remaining xlinks
						LOG.error("Illegal graph cycle in grouping detected. XLink references cannot be resolved.");
					}
				}
			}
		} finally {
			if (rs != null) {
				rs.close();
				rs = null;
			}

			if (stmt != null) {
				stmt.close();
				stmt = null;
			}
		}
	}

	private void appearanceXlinks() throws SQLException {
		
		if (!shouldRun)
			return;

		Statement stmt = null;
		ResultSet rs = null;

		try {
			if (!cacheManager.existsTemporaryCacheTable(CacheTableModelEnum.TEXTUREPARAM) && 
					!cacheManager.existsTemporaryCacheTable(CacheTableModelEnum.TEXTURE_FILE))
				return;			

			LOG.info("Resolving appearance XLinks...");
			eventDispatcher.triggerEvent(new StatusDialogProgressBar(0, 0, this));
			eventDispatcher.triggerEvent(new StatusDialogMessage(Util.I18N.getString("import.dialog.appXlink.msg"), this));

			// first run: resolve texture param
			if (cacheManager.existsTemporaryCacheTable(CacheTableModelEnum.TEXTUREPARAM)) {			
				TemporaryCacheTable temporaryTable = (TemporaryCacheTable)cacheManager.getCacheTable(CacheTableModelEnum.TEXTUREPARAM);
				
				temporaryTable.deriveHeapCacheTableWithIndexes();
				if (cacheManager.existsTemporaryCacheTable(CacheTableModelEnum.LINEAR_RING))
					((TemporaryCacheTable)cacheManager.getCacheTable(CacheTableModelEnum.LINEAR_RING)).deriveHeapCacheTableWithIndexes();

				int max = (int)temporaryTable.size();
				int current = 0;

				stmt = temporaryTable.getConnection().createStatement();
				rs = stmt.executeQuery("select * from " + temporaryTable.getTableName() + 
						" where not TYPE=" + DBXlinkTextureParamEnum.XLINK_TEXTUREASSOCIATION.ordinal());

				while (rs.next() && shouldRun) {
					eventDispatcher.triggerEvent(new StatusDialogProgressBar(++current, max, this));

					String id = rs.getString("ID");
					String gmlId = rs.getString("GMLID");
					int appType = rs.getInt("TYPE");

					// set initial context...
					DBXlinkTextureParam xlink = new DBXlinkTextureParam(
							id,
							gmlId,
							DBXlinkTextureParamEnum.fromInt(appType));

					int isTexPara = rs.getInt("IS_TEXTURE_PARAMETERIZATION");
					if (!rs.wasNull())
						xlink.setTextureParameterization(isTexPara != 0);

					String texParamGmlId = rs.getString("TEXPARAM_GMLID");
					if (!rs.wasNull())
						xlink.setTexParamGmlId(texParamGmlId);

					String worldToTexture = rs.getString("WORLD_TO_TEXTURE");
					if (!rs.wasNull())
						xlink.setWorldToTexture(worldToTexture);

					String textureCoord = rs.getString("TEXTURE_COORDINATES");
					if (!rs.wasNull())
						xlink.setTextureCoord(textureCoord);

					String targetURI = rs.getString("TARGET_URI");
					if (!rs.wasNull())
						xlink.setTargetURI(targetURI);

					String texCoordListId = rs.getString("TEXCOORDLIST_ID");
					if (!rs.wasNull())
						xlink.setTexCoordListId(texCoordListId);

					xlinkResolverPool.addWork(xlink);
				}

				rs.close();
				stmt.close();
			}
			
			// joining the threads is required to avoid
			// deadlocks in PostgreSQL/PostGIS
			try {
				xlinkResolverPool.join();
				tmpXlinkPool.join();
			} catch (InterruptedException e) {
				//
			}

			// second run: import texture images and world files
			if (cacheManager.existsTemporaryCacheTable(CacheTableModelEnum.TEXTURE_FILE)) {		
				LOG.info("Importing texture images...");
				eventDispatcher.triggerEvent(new StatusDialogProgressBar(0, 0, this));
				eventDispatcher.triggerEvent(new StatusDialogMessage(Util.I18N.getString("import.dialog.texImg.msg"), this));
				
				TemporaryCacheTable temporaryTable = (TemporaryCacheTable)cacheManager.getCacheTable(CacheTableModelEnum.TEXTURE_FILE);

				int max = (int)temporaryTable.size();
				int current = 0;
				
				stmt = temporaryTable.getConnection().createStatement();
				rs = stmt.executeQuery("select * from " + temporaryTable.getTableName());

				while (rs.next() && shouldRun) {
					eventDispatcher.triggerEvent(new StatusDialogProgressBar(++current, max, this));
					
					String id = rs.getString("ID");
					String imageURI = rs.getString("FILE_URI");
					int dataType = rs.getInt("TYPE");

					// set initial context
					DBXlinkTextureFile xlink = new DBXlinkTextureFile(
							id,
							imageURI,
							DBXlinkTextureFileEnum.fromInt(dataType));
					
					xlinkResolverPool.addWork(xlink);
				}

				rs.close();
				stmt.close();
			}

			// restart xlink worker pools
			try {
				xlinkResolverPool.join();
				tmpXlinkPool.join();
			} catch (InterruptedException e) {
				//
			}

			if (!shouldRun)
				return;

			// third run: identifying xlinks to texture association elements...
			if (cacheManager.existsTemporaryCacheTable(CacheTableModelEnum.TEXTUREPARAM) && 
					cacheManager.existsTemporaryCacheTable(CacheTableModelEnum.TEXTUREASSOCIATION)) {
				eventDispatcher.triggerEvent(new StatusDialogProgressBar(0, 0, this));
				eventDispatcher.triggerEvent(new StatusDialogMessage(Util.I18N.getString("import.dialog.appXlink.msg"), this));
				
				TemporaryCacheTable cacheTable = (TemporaryCacheTable)cacheManager.getCacheTable(CacheTableModelEnum.TEXTUREPARAM);
				((TemporaryCacheTable)cacheManager.getCacheTable(CacheTableModelEnum.TEXTUREASSOCIATION)).deriveHeapCacheTableWithIndexes();

				int max = (int)cacheTable.size();
				int current = 0;
				
				stmt = cacheTable.getConnection().createStatement();
				rs = stmt.executeQuery("select * from " + cacheTable.getTableName() + " where TYPE=" + DBXlinkTextureParamEnum.XLINK_TEXTUREASSOCIATION.ordinal());

				while (rs.next() && shouldRun) {
					eventDispatcher.triggerEvent(new StatusDialogProgressBar(++current, max, this));

					String id = rs.getString("ID");
					String gmlId = rs.getString("GMLID");
					String targetURI = rs.getString("TARGET_URI");

					DBXlinkTextureParam xlink = new DBXlinkTextureParam(
							id,
							gmlId,
							DBXlinkTextureParamEnum.XLINK_TEXTUREASSOCIATION);

					xlink.setTargetURI(targetURI);
					xlinkResolverPool.addWork(xlink);
				}
			}

		} finally {
			if (rs != null) {
				rs.close();
				rs = null;
			}

			if (stmt != null) {
				stmt.close();
				stmt = null;
			}
		}
	}

	private void libraryObjectXLinks() throws SQLException {
		
		if (!shouldRun)
			return;
		
		Statement stmt = null;
		ResultSet rs = null;

		try {
			CacheTable cacheTable = cacheManager.getCacheTable(CacheTableModelEnum.LIBRARY_OBJECT);	
			if (!(cacheTable instanceof TemporaryCacheTable))
				return;
			
			LOG.info("Importing library objects...");
			eventDispatcher.triggerEvent(new StatusDialogProgressBar(0, 0, this));
			eventDispatcher.triggerEvent(new StatusDialogMessage(Util.I18N.getString("import.dialog.libObj.msg"), this));
			
			TemporaryCacheTable temporaryTable = (TemporaryCacheTable)cacheTable;
		
			int max = (int)temporaryTable.size();
			int current = 0;
			
			stmt = temporaryTable.getConnection().createStatement();
			rs = stmt.executeQuery("select * from " + temporaryTable.getTableName());

			while (rs.next() && shouldRun) {
				eventDispatcher.triggerEvent(new StatusDialogProgressBar(++current, max, this));

				String id = rs.getString("ID");
				String imageURI = rs.getString("FILE_URI");

				// set initial context
				DBXlinkLibraryObject xlink = new DBXlinkLibraryObject(
						id,
						imageURI);
				
				xlinkResolverPool.addWork(xlink);
			}
		} finally {
			if (rs != null) {
				rs.close();
				rs = null;
			}

			if (stmt != null) {
				stmt.close();
				stmt = null;
			}
		}
	}
	
	private void deprecatedMaterialXlinks() throws SQLException {
		
		if (!shouldRun)
			return;

		Statement stmt = null;
		ResultSet rs = null;

		try {
			CacheTable cacheTable = cacheManager.getCacheTable(CacheTableModelEnum.DEPRECATED_MATERIAL);
			if (!(cacheTable instanceof TemporaryCacheTable))
				return;

			LOG.info("Resolving TexturedSurface XLinks...");
			eventDispatcher.triggerEvent(new StatusDialogProgressBar(0, 0, this));
			eventDispatcher.triggerEvent(new StatusDialogMessage(Util.I18N.getString("import.dialog.depMat.msg"), this));

			TemporaryCacheTable temporaryTable = (TemporaryCacheTable)cacheTable;
			
			int max = (int)temporaryTable.size();
			int current = 0;
			
			stmt = temporaryTable.getConnection().createStatement();
			rs = stmt.executeQuery("select * from " + temporaryTable.getTableName());

			while (rs.next() && shouldRun) {
				eventDispatcher.triggerEvent(new StatusDialogProgressBar(++current, max, this));

				String appearanceId = rs.getString("ID");
				String gmlId = rs.getString("GMLID");
				String surfaceGeometryId = rs.getString("SURFACE_GEOMETRY_ID");

				// set initial context
				DBXlinkDeprecatedMaterial xlink = new DBXlinkDeprecatedMaterial(
						appearanceId,
						gmlId,
						surfaceGeometryId);

				xlinkResolverPool.addWork(xlink);
			}
		} finally {
			if (rs != null) {
				rs.close();
				rs = null;
			}

			if (stmt != null) {
				stmt.close();
				stmt = null;
			}
		}
	}

	public java.util.List<SurfaceObject> surfaceGeometryXlinks(boolean checkRecursive) throws SQLException {
		if (!shouldRun)
			return null;

		CacheTable cacheTable = cacheManager.getCacheTable(CacheTableModelEnum.SURFACE_GEOMETRY);
		if (cacheTable == null)
			return null;

		LOG.info("Resolving geometry XLinks...");

		return querySurfaceGeometryXlinks((TemporaryCacheTable)cacheTable, checkRecursive, -1, 1);
	}

	private java.util.List<SurfaceObject> querySurfaceGeometryXlinks(TemporaryCacheTable cacheTable,
			boolean checkRecursive, 
			long remaining, 
			int pass) throws SQLException {
		
		Statement stmt = null;
		ResultSet rs = null;
		java.util.List<SurfaceObject> tmpList = new ArrayList<SurfaceObject>();

		try {
			eventDispatcher.triggerEvent(new StatusDialogProgressBar(0, 0, this));
			String text = Util.I18N.getString("import.dialog.geomXLink.msg");
			Object[] args = new Object[]{ pass };
			eventDispatcher.triggerEvent(new StatusDialogMessage(MessageFormat.format(text, args), this));

			int max = (remaining == -1) ? (int)cacheTable.size() : (int)remaining;
			int current = 0;
			
			HeapCacheTable heapTable = cacheTable.deriveHeapCacheTableWithIndexes();
			cacheTable.truncate();
			stmt = heapTable.getConnection().createStatement();
			rs = stmt.executeQuery("select * from " + heapTable.getTableName());
			while (rs.next()) {
				eventDispatcher.triggerEvent(new StatusDialogProgressBar(++current, max, this));
				
				String id = rs.getString("ID");
				String parentId = rs.getString("PARENT_ID");
				String rootId = rs.getString("ROOT_ID");
				boolean reverse = rs.getInt("REVERSE") == 1;
				final String gmlId = rs.getString("GMLID");
		
				SurfaceObject bSurface = null;
				if((bSurface = CompareGeom(gmlId.replace("#",""))) != null)
						tmpList.add(bSurface);
				
				// set initial context...
			/*	DBXlinkSurfaceGeometry xlink = new DBXlinkSurfaceGeometry(
						id,
						parentId,
						rootId,
						reverse,
						gmlId);

				xlinkResolverPool.addWork(xlink);*/
			}


			/*if (checkRecursive && shouldRun) {
				rs.close();
				stmt.close();

				try {
					xlinkResolverPool.join();
					tmpXlinkPool.join();
				} catch (InterruptedException e) {
					//
				}

				long unresolved = cacheTable.size();
				if (unresolved > 0) {
					if (unresolved != remaining) {
						// we still have unresolved xlinks... so do another recursion
						cacheTable.dropHeapCacheTable();
						querySurfaceGeometryXlinks(cacheTable, checkRecursive, unresolved, ++pass);
					} else {
						// we detected a cycle and cannot resolve the remaining xlinks
						LOG.error("Illegal graph cycle in geometry detected. XLink references cannot be resolved.");
					}
				}
			}*/
		} finally {
			if (rs != null) {
				rs.close();
				rs = null;
			}

			if (stmt != null) {
				stmt.close();
				stmt = null;
			}
		}
		return tmpList;
	}

	
	private SurfaceObject CompareGeom(String GmlID)
	{
		final String id = GmlID;
		

        Predicate predicate = new Predicate() {

            public boolean evaluate(Object object) {
            	
            	if(((SurfaceObject) object).getPId() != null  && ((SurfaceObject) object).getPId().equals(id))
            		return true;
            	else
            		return ((SurfaceObject) object).getId().equals(id);
            }
        };
        
        Collection<SurfaceObject> filtered = CollectionUtils.select(list, predicate);
        
		return (filtered.size()>0) ? new ArrayList<SurfaceObject>(filtered).get(0) : null;

	}
}
