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
package org.citydb.plugins.CityGMLConverter.xlink.importer;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.citydb.plugins.CityGMLConverter.config.Internal;
import org.citydb.plugins.CityGMLConverter.util.Sqlite.cache.TemporaryCacheTable;
import org.citydb.plugins.CityGMLConverter.xlink.content.DBXlinkTextureAssociation;


public class DBXlinkImporterTextureAssociation implements DBXlinkImporter {
	private final TemporaryCacheTable tempTable;
	private PreparedStatement psXlink;
	private int batchCounter;

	public DBXlinkImporterTextureAssociation(TemporaryCacheTable tempTable) throws SQLException {
		this.tempTable = tempTable;

		init();
	}

	private void init() throws SQLException {
		psXlink = tempTable.getConnection().prepareStatement("insert into " + tempTable.getTableName() + 
			" (SURFACE_DATA_ID, SURFACE_GEOMETRY_ID, GMLID) values " +
			"(?, ?, ?)");
	}

	public boolean insert(DBXlinkTextureAssociation xlinkEntry) throws SQLException {
		psXlink.setString(1, xlinkEntry.getSurfaceDataId());
		psXlink.setString(2, xlinkEntry.getSurfaceGeometryId());
		psXlink.setString(3, xlinkEntry.getGmlId());

		psXlink.addBatch();
		if (++batchCounter == Internal.Sqlite_MAX_BATCH_SIZE)
			executeBatch();

		return true;
	}

	@Override
	public void executeBatch() throws SQLException {
		psXlink.executeBatch();
		batchCounter = 0;
	}

	@Override
	public void close() throws SQLException {
		psXlink.close();
	}

	@Override
	public DBXlinkImporterEnum getDBXlinkImporterType() {
		return DBXlinkImporterEnum.XLINK_TEXTUREASSOCIATION;
	}

}
