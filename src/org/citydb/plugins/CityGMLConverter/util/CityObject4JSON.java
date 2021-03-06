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
package org.citydb.plugins.CityGMLConverter.util;

public class CityObject4JSON {

	private String gmlId;

	private double envelopeXmin;
	private double envelopeXmax;
	private double envelopeYmin;
	private double envelopeYmax;

	private int tileRow = 0;
	private int tileColumn = 0;
	
	public CityObject4JSON (String gmlId) {
		this.gmlId = gmlId;
	}

	@Override
	public String toString() {

		return "\t\"" + gmlId + "\": {" +
			   "\n\t\"envelope\": [" + envelopeXmin + ", " + envelopeYmin + ", " + envelopeXmax + ", " + envelopeYmax +
			   "],\n\t\"tile\": [" + tileRow + ", " + tileColumn + "]}";
/*
		return "\n\t\"envelope\": [" + envelopeXmin + ", " + envelopeYmin + ", " + envelopeXmax + ", " + envelopeYmax +
		   	   "],\n\t\"tile\": [" + tileRow + ", " + tileColumn + "]}";
*/
	}

/*
	@Override
	public boolean equals(Object obj) {
		
		try {
			CityObject4JSON cityObject4Json = (CityObject4JSON) obj;
			return this.gmlId.equals(cityObject4Json.getGmlId());
		}
		catch (Exception e) {}
		return false;
	}
	
	@Override
	public int hashCode(){
		return this.gmlId.hashCode();
	}
*/

	public void setEnvelope (double[] ordinatesArray) {
		if (ordinatesArray == null) return;
		// different from Oracle version
		envelopeXmin = Math.min(ordinatesArray[0], ordinatesArray[3]); 
		envelopeYmin = Math.min(ordinatesArray[1], ordinatesArray[4]);
		envelopeXmax = Math.max(ordinatesArray[0], ordinatesArray[3]);
		envelopeYmax = Math.max(ordinatesArray[1], ordinatesArray[4]);
	}
	
	public void setEnvelopeXmin(double envelopeXmin) {
		this.envelopeXmin = envelopeXmin;
	}

	public double getEnvelopeXmin() {
		return envelopeXmin;
	}

	public void setEnvelopeXmax(double envelopeXmax) {
		this.envelopeXmax = envelopeXmax;
	}

	public double getEnvelopeXmax() {
		return envelopeXmax;
	}

	public void setEnvelopeYmin(double envelopeYmin) {
		this.envelopeYmin = envelopeYmin;
	}

	public double getEnvelopeYmin() {
		return envelopeYmin;
	}

	public void setEnvelopeYmax(double envelopeYmax) {
		this.envelopeYmax = envelopeYmax;
	}

	public double getEnvelopeYmax() {
		return envelopeYmax;
	}

	public void setTileRow(int tileRow) {
		this.tileRow = tileRow;
	}

	public int getTileRow() {
		return tileRow;
	}

	public void setTileColumn(int tileColumn) {
		this.tileColumn = tileColumn;
	}

	public int getTileColumn() {
		return tileColumn;
	}
/*
	public String getGmlId() {
		return gmlId;
	}
*/
}
