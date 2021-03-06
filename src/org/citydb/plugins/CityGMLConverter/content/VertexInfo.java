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
 * License aString with this program. If not, see 
 * <http://www.gnu.org/licenses/>.
 * 
 * The development of the 3D City Database Importer/Exporter has 
 * been financially supported by the following cooperation partners:
 * 
 * Business Location Center, Berlin <http://www.businesslocationcenter.de/>
 * virtualcitySYSTEMS GmbH, Berlin <http://www.virtualcitysystems.de/>
 * Berlin Senate of Business, Technology and Women <http://www.berlin.de/sen/wtf/>
 */
package org.citydb.plugins.CityGMLConverter.content;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class VertexInfo {
	private BigInteger vertexId;
	private double x;
	private double y;
	private double z;
	private HashMap<String, TexCoords> texCoords;
	private VertexInfo nextVertexInfo = null;

	protected VertexInfo (BigInteger vertexId, double x, double y, double z) {
		setVertexId(vertexId);
		setX(x);
		setY(y);
		setZ(z);
	}

	protected VertexInfo (BigInteger vertexId) {
		setVertexId(vertexId);
	}

	protected void addTexCoords (String surfaceId, TexCoords texCoordsForThisSurface) {
		if (texCoordsForThisSurface == null) {
			return;
		}
		if (texCoords == null) {
			texCoords = new HashMap<String, TexCoords>();
		}
		texCoords.put(surfaceId, texCoordsForThisSurface);
	}

	protected TexCoords getTexCoords (String surfaceId) {
		TexCoords value = null;
		if (texCoords != null) {
			value = texCoords.get(surfaceId);
		}
		return value;
	}

	protected HashMap<String, TexCoords> getAllTexCoords () {
		return texCoords;
	}

	protected void addTexCoordsFrom (VertexInfo anotherVertexInfo) {
		if (anotherVertexInfo.texCoords == null) {
			return;
		}
		if (texCoords == null) {
			texCoords = new HashMap<String, TexCoords>();
		}
		Set<String> keySet = anotherVertexInfo.texCoords.keySet();
		Iterator<String> iterator = keySet.iterator();
		while (iterator.hasNext()) {
			String surfaceId = iterator.next();
			texCoords.put(surfaceId, anotherVertexInfo.getTexCoords(surfaceId));
		}
	}
	

	protected void setVertexId(BigInteger vertexId) {
		this.vertexId = vertexId;
	}

	protected BigInteger getVertexId() {
		return vertexId;
	}

	protected void setX(double x) {
		this.x = x;
	}

	protected double getX() {
		return x;
	}

	protected void setY(double y) {
		this.y = y;
	}

	protected double getY() {
		return y;
	}

	protected void setZ(double z) {
		this.z = z;
	}

	protected double getZ() {
		return z;
	}

	protected void setNextVertexInfo(VertexInfo nextVertexInfo) {
		this.nextVertexInfo = nextVertexInfo;
	}

	protected VertexInfo getNextVertexInfo() {
		return nextVertexInfo;
	}

}