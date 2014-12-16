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
package org.citydb.plugins.CityGMLConverter.content;

import java.awt.image.BufferedImage;
// import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;
import javax.vecmath.Point3d;
import javax.xml.bind.JAXBException;

import net.opengis.kml._2.AltitudeModeEnumType;
import net.opengis.kml._2.BoundaryType;
import net.opengis.kml._2.LinearRingType;
import net.opengis.kml._2.LinkType;
import net.opengis.kml._2.LocationType;
import net.opengis.kml._2.ModelType;
import net.opengis.kml._2.MultiGeometryType;
import net.opengis.kml._2.PlacemarkType;
import net.opengis.kml._2.PolygonType;

import org.citydb.api.event.EventDispatcher;
import org.citydb.database.adapter.BlobExportAdapter;
import org.citydb.log.Logger;
import org.citydb.plugins.CityGMLConverter.config.ConfigImpl;
import org.citydb.plugins.CityGMLConverter.content.BalloonTemplateHandlerImpl;
import org.citydb.plugins.CityGMLConverter.content.ElevationServiceHandler;
import org.citydb.plugins.CityGMLConverter.content.KmlExporterManager;
import org.citydb.plugins.CityGMLConverter.content.KmlGenericObject;
import org.citydb.plugins.CityGMLConverter.content.KmlSplittingResult;
import org.citydb.plugins.CityGMLConverter.content.Queries;
import org.citygml4j.factory.GMLGeometryFactory;
import org.citygml4j.geometry.Matrix;
import org.citygml4j.model.citygml.appearance.X3DMaterial;
import org.postgis.Geometry;
import org.postgis.PGgeometry;
import org.postgis.Polygon;

import com.sun.j3d.utils.geometry.GeometryInfo;

import org.citydb.plugins.CityGMLConverter.config.Balloon;
import org.citydb.plugins.CityGMLConverter.config.ColladaOptions;
import org.citydb.plugins.CityGMLConverter.config.DisplayForm;



public class CityFurniture extends KmlGenericObject{

	public static final String STYLE_BASIS_NAME = "Furniture";

	private long sgRootId;
	private Matrix transformation;

	private double refPointX;
	private double refPointY;
	private double refPointZ;

	public CityFurniture(Connection connection,
			KmlExporterManager kmlExporterManager,
			GMLGeometryFactory cityGMLFactory,
			net.opengis.kml._2.ObjectFactory kmlFactory,
			ElevationServiceHandler elevationServiceHandler,
			BalloonTemplateHandlerImpl balloonTemplateHandler,
			EventDispatcher eventDispatcher,
			ConfigImpl config) {

		super(connection,
			  kmlExporterManager,
			  cityGMLFactory,
			  kmlFactory,
			  elevationServiceHandler,
			  balloonTemplateHandler,
			  eventDispatcher,
			  config);
	}

	protected List<DisplayForm> getDisplayForms() {
		return config.getCityFurnitureDisplayForms();
	}

	public ColladaOptions getColladaOptions() {
		return config.getCityFurnitureColladaOptions();
	}

	public Balloon getBalloonSettings() {
		return config.getCityFurnitureBalloon();
	}

	public String getStyleBasisName() {
		return STYLE_BASIS_NAME;
	}

	protected String getHighlightingQuery() {
		return Queries.getCityFurnitureHighlightingQuery(currentLod);
	}

	public void read(KmlSplittingResult work) {
		
	}
	
	protected Geometry applyTransformationMatrix(Geometry geometry) throws SQLException {
		if (transformation != null) {
			for (int i = 0; i < geometry.numPoints(); i++) {
				double[] vals = new double[]{geometry.getPoint(i).getX(), geometry.getPoint(i).getY(), geometry.getPoint(i).getZ(), 1};
				Matrix v = new Matrix(vals, 4);
		
				v = transformation.times(v);
				geometry.getPoint(i).setX(v.get(0, 0) + refPointX);
				geometry.getPoint(i).setY(v.get(1, 0) + refPointY);
				geometry.getPoint(i).setZ(v.get(2, 0) + refPointZ);
			}
		}
		return geometry;
	}
	
	protected Geometry convertToWGS84(Geometry geom) throws SQLException {
		return super.convertToWGS84(applyTransformationMatrix(geom));
	}
	
	public PlacemarkType createPlacemarkForColladaModel() throws SQLException {

		if (transformation == null) { // no implicit geometry
			// undo trick for very close coordinates
		/*	double[] originInWGS84 = convertPointCoordinatesToWGS84(new double[] {getOriginX()/100, getOriginY()/100, getOriginZ()/100});
			setLocationX(reducePrecisionForXorY(originInWGS84[0]));
			setLocationY(reducePrecisionForXorY(originInWGS84[1]));
			setLocationZ(reducePrecisionForZ(originInWGS84[2]));
			return super.createPlacemarkForColladaModel();*/
		}

		double[] originInWGS84 = convertPointCoordinatesToWGS84(new double[] {0, 0, 0}); // will be turned into refPointX,Y,Z by convertToWGS84
		setLocationX(reducePrecisionForXorY(originInWGS84[0]));
		setLocationY(reducePrecisionForXorY(originInWGS84[1]));
		setLocationZ(reducePrecisionForZ(originInWGS84[2]));

		PlacemarkType placemark = kmlFactory.createPlacemarkType();
		placemark.setName(getGmlId());
		placemark.setId(DisplayForm.COLLADA_PLACEMARK_ID + placemark.getName());

		DisplayForm colladaDisplayForm = null;
		for (DisplayForm displayForm: getDisplayForms()) {
			if (displayForm.getForm() == DisplayForm.COLLADA) {
				colladaDisplayForm = displayForm;
				break;
			}
		}

		if (getBalloonSettings().isIncludeDescription() 
				&& !colladaDisplayForm.isHighlightingEnabled()) { // avoid double description

			ColladaOptions colladaOptions = getColladaOptions();
			if (!colladaOptions.isGroupObjects() || colladaOptions.getGroupSize() == 1) {
				addBalloonContents(placemark, getId());
			}
		}

		ModelType model = kmlFactory.createModelType();
		LocationType location = kmlFactory.createLocationType();

		switch (config.getAltitudeMode()) {
		case ABSOLUTE:
			model.setAltitudeModeGroup(kmlFactory.createAltitudeMode(AltitudeModeEnumType.ABSOLUTE));
			break;
		case RELATIVE:
			model.setAltitudeModeGroup(kmlFactory.createAltitudeMode(AltitudeModeEnumType.RELATIVE_TO_GROUND));
			break;
		}

		location.setLatitude(getLocationY());
		location.setLongitude(getLocationX());
		location.setAltitude(getLocationZ() + reducePrecisionForZ(getZOffset()));
		model.setLocation(location);

		// no heading value to correct

		LinkType link = kmlFactory.createLinkType();

		if (config.isOneFilePerObject() &&
			!config.isExportAsKmz() &&
			config.getFilter().getComplexFilter().getTiledBoundingBox().getActive().booleanValue())
		{
			link.setHref(getGmlId() + ".dae");
		}
		else {
			// File.separator would be wrong here, it MUST be "/"
			link.setHref(getGmlId() + "/" + getGmlId() + ".dae");
		}
		model.setLink(link);

		placemark.setAbstractGeometryGroup(kmlFactory.createModel(model));
		return placemark;
	}

    protected List<Point3d> setOrigins() {
		List<Point3d> coords = new ArrayList<Point3d>();
		
		if (transformation != null) { // for implicit geometries
			setOriginX(refPointX * 100); // trick for very close coordinates
			setOriginY(refPointY * 100);
			setOriginZ(refPointZ * 100);
			// dummy
			Point3d point3d = new Point3d(getOriginX(), getOriginY(), getOriginZ());
			coords.add(point3d);
		}
		else {
			coords = super.setOrigins();
		}
		
		return coords;
    }
    
    protected void fillGenericObjectForCollada(ResultSet rs) throws SQLException {
    /*
		if (transformation == null) { // no implicit geometry
		//	super.fillGenericObjectForCollada(rs);
			return;
		}

		String selectedTheme = config.getAppearanceTheme();

		int texImageCounter = 0;
		PGgeometry pgBuildingGeometry = null;

		while (rs.next()) {
			long surfaceRootId = rs.getLong(1);
			for (String colladaQuery: Queries.COLLADA_GEOMETRY_AND_APPEARANCE_FROM_ROOT_ID) { // parent surfaces come first
				PreparedStatement psQuery = null;
				ResultSet rs2 = null;
				
				try {
					psQuery = connection.prepareStatement(colladaQuery);
					psQuery.setLong(1, surfaceRootId);
//					psQuery.setString(2, selectedTheme);
					rs2 = psQuery.executeQuery();
					
	
					while (rs2.next()) {
						String theme = rs2.getString("theme");
	
						pgBuildingGeometry = (PGgeometry)rs2.getObject(1);
						// surfaceId is the key to all Hashmaps in building
						// for implicit geometries it must be randomized with
						// gmlId.hashCode() in order to properly group objects
						// otherwise surfaces with the same id would be overwritten
						long surfaceId = rs2.getLong("id") + getGmlId().hashCode();
						long parentId = rs2.getLong("parent_id");
	
//						if (buildingGeometryObj == null) { // root or parent
						if (pgBuildingGeometry == null) { // root or parent	
							if (selectedTheme.equalsIgnoreCase(theme)) {
								X3DMaterial x3dMaterial = cityGMLFactory.createX3DMaterial();
							//	fillX3dMaterialValues(x3dMaterial, rs2);
								// x3dMaterial will only added if not all x3dMaterial members are null
								addX3dMaterial(surfaceId, x3dMaterial);
							}
							else if (theme == null) { // no theme for this parent surface
								if (getX3dMaterial(parentId) != null) { // material for parent's parent known
									addX3dMaterial(surfaceId, getX3dMaterial(parentId));
								}
							}
							continue;
						}
	
						// from hier on it is a surfaceMember
						eventDispatcher.triggerEvent(new GeometryCounterEvent(null, this));
	
						String texImageUri = null;
//						OrdImage texImage = null;
						InputStream texImage = null;
//						byte buf[] = null;
						StringTokenizer texCoordsTokenized = null;
	
						if (selectedTheme.equals(CityKmlExporter.THEME_NONE)) {
							addX3dMaterial(surfaceId, defaultX3dMaterial);
						}
						else if	(!selectedTheme.equalsIgnoreCase(theme) && // no surface data for this surface and theme
								getX3dMaterial(parentId) != null) { // material for parent surface known
							addX3dMaterial(surfaceId, getX3dMaterial(parentId));
						}
						else {
							texImageUri = rs2.getString("tex_image_uri");
//							texImage = (OrdImage)rs2.getORAData("tex_image", OrdImage.getORADataFactory());
							String texCoords = rs2.getString("texture_coordinates");
	
							if (texImageUri != null && texImageUri.trim().length() != 0
									&&  texCoords != null && texCoords.trim().length() != 0
									 && texImage != null ) {
	
								int fileSeparatorIndex = Math.max(texImageUri.lastIndexOf("\\"), texImageUri.lastIndexOf("/")); 
								texImageUri = ".." + File.separator + "_" + texImageUri.substring(fileSeparatorIndex + 1);
	
								addTexImageUri(surfaceId, texImageUri);
//								if (getTexOrdImage(texImageUri) == null) { // not already marked as wrapping texture
								if (getTexImage(texImageUri) == null) { // not already read in
									PreparedStatement psQuery3 = null;
									ResultSet rs3 = null;
									try {
										psQuery3 = connection.prepareStatement(Queries.GET_TEXIMAGE_FROM_SURFACE_DATA_ID);
										psQuery3.setLong(1, rs2.getLong("surface_data_id"));
										rs3 = psQuery3.executeQuery();
										while (rs3.next()) {

											// read large object (OID) data type from database
											// Get the Large Object Manager to perform operations with
											LargeObjectManager lobj = ((org.postgresql.PGConnection)connection).getLargeObjectAPI();

											// Open the large object for reading
											long oid = rs3.getLong("tex_image");
											if (oid == 0) {
												Logger.getInstance().error("Database error while reading library object: " + texImageUri);
											}
											LargeObject obj = lobj.open(oid, LargeObjectManager.READ);

											// Read the data
											buf = new byte[obj.size()];
											obj.read(buf, 0, obj.size());

											// read bytea data type from database
											texImage = rs3.getBinaryStream("tex_image");
										}
									}
									finally {
										if (rs3 != null)
											try { rs3.close(); } catch (SQLException e) {}
										if (psQuery3 != null)
											try { psQuery3.close(); } catch (SQLException e) {}
									}

									BufferedImage bufferedImage = null;
									try {
//										texImage = new ByteArrayInputStream(buf); // for Large Objects
										
//										bufferedImage = ImageIO.read(texImage.getDataInStream());
										bufferedImage = ImageIO.read(texImage);
									}
									catch (IOException ioe) {}

									if (bufferedImage != null) { // image in JPEG, PNG or another usual format
										addTexImage(texImageUri, bufferedImage);
									}
//									else {
//										addTexOrdImage(texImageUri, texImage);
//									}

									texImageCounter++;
									if (texImageCounter > 20) {
										eventDispatcher.triggerEvent(new CounterEvent(CounterType.TEXTURE_IMAGE, texImageCounter, this));
										texImageCounter = 0;
									}
								}
	
								texCoords = texCoords.replaceAll(";", " "); // substitute of ; for internal ring
								texCoordsTokenized = new StringTokenizer(texCoords, " ");
							}
							else {
								X3DMaterial x3dMaterial = cityGMLFactory.createX3DMaterial();
							//	fillX3dMaterialValues(x3dMaterial, rs2);
								// x3dMaterial will only added if not all x3dMaterial members are null
								addX3dMaterial(surfaceId, x3dMaterial);
								if (getX3dMaterial(surfaceId) == null) {
									// untextured surface and no x3dMaterial -> default x3dMaterial (gray)
									addX3dMaterial(surfaceId, defaultX3dMaterial);
								}
							}
						}
	
//						JGeometry surface = JGeometry.load(buildingGeometryObj);
//						surface = applyTransformationMatrix(surface);
//						double[] ordinatesArray = surface.getOrdinatesArray();
						Geometry surface = pgBuildingGeometry.getGeometry();
						surface = applyTransformationMatrix(surface);
						double[] ordinatesArray = new double[surface.numPoints()*3];
						for (int i = 0, j = 0; i < surface.numPoints(); i++, j+=3){
							ordinatesArray[j] = surface.getPoint(i).x;
							ordinatesArray[j+1] = surface.getPoint(i).y;
							ordinatesArray[j+2] = surface.getPoint(i).z;
						}
						
						Polygon surfacePolygon = (Polygon) surface;
						GeometryInfo gi = new GeometryInfo(GeometryInfo.POLYGON_ARRAY);
//						int contourCount = surface.getElemInfo().length/3;
						int contourCount = surfacePolygon.numRings();
						int cellCount = 0;
						// last point of polygons in gml is identical to first and useless for GeometryInfo
						double[] giOrdinatesArray = new double[ordinatesArray.length - (contourCount*3)];
	
						int[] stripCountArray = new int[contourCount];
						int[] countourCountArray = {contourCount};
	
//						for (int currentContour = 1; currentContour <= contourCount; currentContour++) {
//							int startOfCurrentRing = surface.getElemInfo()[(currentContour-1)*3] - 1;
//							int startOfNextRing = (currentContour == contourCount) ? 
//									ordinatesArray.length: // last
//									surface.getElemInfo()[currentContour*3] - 1; // still holes to come
						for (int currentContour = 1; currentContour <= contourCount; currentContour++) {
							int startOfCurrentRing = cellCount;
							cellCount += (surfacePolygon.getRing(currentContour-1).numPoints()*3);
							int startOfNextRing = (currentContour == contourCount) ? 
									ordinatesArray.length: // last
										cellCount; // still holes to come
	
							for (int j = startOfCurrentRing; j < startOfNextRing - 3; j = j+3) {

								giOrdinatesArray[(j-(currentContour-1)*3)] = ordinatesArray[j] * 100; // trick for very close coordinates
								giOrdinatesArray[(j-(currentContour-1)*3)+1] = ordinatesArray[j+1] * 100;
								giOrdinatesArray[(j-(currentContour-1)*3)+2] = ordinatesArray[j+2] * 100;
	
								TexCoords texCoordsForThisSurface = null;
								if (texCoordsTokenized != null) {
									double s = Double.parseDouble(texCoordsTokenized.nextToken());
									double t = Double.parseDouble(texCoordsTokenized.nextToken());
									if (s > 1.1 || s < -0.1 || t < -0.1 || t > 1.1) { // texture wrapping -- it conflicts with texture atlas
										removeTexImage(texImageUri);
										BufferedImage bufferedImage = null;
										try {
											bufferedImage = ImageIO.read(texImage);
										} catch (IOException e) {}
										addTexImage(texImageUri, bufferedImage);
//										addTexOrdImage(texImageUri, texImage);
									}

									texCoordsForThisSurface = new TexCoords(s, t);
								}
								setVertexInfoForXYZ(surfaceId,
										giOrdinatesArray[(j-(currentContour-1)*3)],
										giOrdinatesArray[(j-(currentContour-1)*3)+1],
										giOrdinatesArray[(j-(currentContour-1)*3)+2],
										texCoordsForThisSurface);
							}
							stripCountArray[currentContour-1] = (startOfNextRing -3 - startOfCurrentRing)/3;
							if (texCoordsTokenized != null) {
								texCoordsTokenized.nextToken(); // geometryInfo ignores last point in a polygon
								texCoordsTokenized.nextToken(); // keep texture coordinates in sync
							}
						}
						gi.setCoordinates(giOrdinatesArray);
						gi.setContourCounts(countourCountArray);
						gi.setStripCounts(stripCountArray);
						addGeometryInfo(surfaceId, gi);
					}
				}
				catch (SQLException sqlEx) {
					Logger.getInstance().error("SQL error while querying city object: " + sqlEx.getMessage());
				}
				finally {
					if (rs2 != null)
						try { rs2.close(); } catch (SQLException e) {}
					if (psQuery != null)
						try { psQuery.close(); } catch (SQLException e) {}
				}
			}
		}

		// count rest images
		eventDispatcher.triggerEvent(new CounterEvent(CounterType.TEXTURE_IMAGE, texImageCounter, this));*/
	}

	protected List<PlacemarkType> createPlacemarksForHighlighting(KmlSplittingResult work) throws SQLException {
		if (transformation == null) { // no implicit geometry
	//		return super.createPlacemarksForHighlighting(work);
		}

		List<PlacemarkType> placemarkList= new ArrayList<PlacemarkType>();

		PlacemarkType placemark = kmlFactory.createPlacemarkType();
		placemark.setStyleUrl("#" + getStyleBasisName() + work.getDisplayForm().getName() + "Style");
		placemark.setName(work.getGmlId());
		placemark.setId(DisplayForm.GEOMETRY_HIGHLIGHTED_PLACEMARK_ID + placemark.getName());
		placemarkList.add(placemark);

		if (getBalloonSettings().isIncludeDescription()) {
			addBalloonContents(placemark, work.getId());
		}

		MultiGeometryType multiGeometry =  kmlFactory.createMultiGeometryType();
		placemark.setAbstractGeometryGroup(kmlFactory.createMultiGeometry(multiGeometry));

		PreparedStatement getGeometriesStmt = null;
//		OracleResultSet rs = null;
		ResultSet rs = null;

		double hlDistance = work.getDisplayForm().getHighlightingDistance();

		try {
			getGeometriesStmt = connection.prepareStatement(getHighlightingQuery(),
															ResultSet.TYPE_SCROLL_INSENSITIVE,
															ResultSet.CONCUR_READ_ONLY);

			for (int i = 1; i <= getGeometriesStmt.getParameterMetaData().getParameterCount(); i++) {
				getGeometriesStmt.setLong(i, work.getId());
			}
//			rs = (OracleResultSet)getGeometriesStmt.executeQuery();
			rs = getGeometriesStmt.executeQuery();

			double zOffset = getZOffsetFromDB(work.getGmlId(),work.GetElevation());
			

			while (rs.next()) {
//				STRUCT unconverted = (STRUCT)rs.getObject(1);
//				JGeometry unconvertedSurface = JGeometry.load(unconverted);
//				unconvertedSurface = applyTransformationMatrix(unconvertedSurface);
//				double[] ordinatesArray = unconvertedSurface.getOrdinatesArray();
//				if (ordinatesArray == null) {
//					continue;
//				}
				PGgeometry unconverted = (PGgeometry)rs.getObject(1);
//				Polygon unconvertedSurface = (Polygon)unconverted.getGeometry();
				Geometry unconvertedSurface = unconverted.getGeometry();
				unconvertedSurface = applyTransformationMatrix(unconvertedSurface);
				double[] ordinatesArray = new double[unconvertedSurface.numPoints()*3];
		
				for (int i = 0, j = 0; i < unconvertedSurface.numPoints(); i++, j+=3){
					ordinatesArray[j] = unconvertedSurface.getPoint(i).x;
					ordinatesArray[j+1] = unconvertedSurface.getPoint(i).y;
					ordinatesArray[j+2] = unconvertedSurface.getPoint(i).z;
				}		

//				int contourCount = unconvertedSurface.getElemInfo().length/3;
//				// remove normal-irrelevant points
//				int startContour1 = unconvertedSurface.getElemInfo()[0] - 1;
//				int endContour1 = (contourCount == 1) ? 
//						ordinatesArray.length: // last
//							unconvertedSurface.getElemInfo()[3] - 1; // holes are irrelevant for normal calculation
//				// last point of polygons in gml is identical to first and useless for GeometryInfo
//				endContour1 = endContour1 - 3;
				
				int contourCount = ((Polygon)unconvertedSurface).numRings();
				// remove normal-irrelevant points
				int startContour1 = 0;
				int endContour1 = (contourCount == 1) ? 
								  ordinatesArray.length: // last
								  ((Polygon)unconvertedSurface).getRing(startContour1).numPoints()*3; // holes are irrelevant for normal calculation
				// last point of polygons in gml is identical to first and useless for GeometryInfo
				endContour1 = endContour1 - 3;

				double nx = 0;
				double ny = 0;
				double nz = 0;
				int cellCount = 0;

				for (int current = startContour1; current < endContour1; current = current+3) {
					int next = current+3;
					if (next >= endContour1) next = 0;
					nx = nx + ((ordinatesArray[current+1] - ordinatesArray[next+1]) * (ordinatesArray[current+2] + ordinatesArray[next+2])); 
					ny = ny + ((ordinatesArray[current+2] - ordinatesArray[next+2]) * (ordinatesArray[current] + ordinatesArray[next])); 
					nz = nz + ((ordinatesArray[current] - ordinatesArray[next]) * (ordinatesArray[current+1] + ordinatesArray[next+1])); 
				}

				double value = Math.sqrt(nx * nx + ny * ny + nz * nz);
				if (value == 0) { // not a surface, but a line
					continue;
				}
				nx = nx / value;
				ny = ny / value;
				nz = nz / value;

				for (int i = 0, j = 0; i < unconvertedSurface.numPoints(); i++, j+=3) {
					unconvertedSurface.getPoint(i).setX(ordinatesArray[j] + hlDistance * nx);
					unconvertedSurface.getPoint(i).setY(ordinatesArray[j+1] + hlDistance * ny);
					unconvertedSurface.getPoint(i).setZ(ordinatesArray[j+2] + + zOffset + hlDistance * nz);
				}

				// now convert to WGS84 without applying transformation matrix (already done)
//				JGeometry surface = super.convertToWGS84(unconvertedSurface);
//				ordinatesArray = surface.getOrdinatesArray();
				Polygon surface = (Polygon)super.convertToWGS84(unconvertedSurface);
				
				for (int i = 0, j = 0; i < surface.numPoints(); i++, j+=3){
					ordinatesArray[j] = surface.getPoint(i).x;
					ordinatesArray[j+1] = surface.getPoint(i).y;
					ordinatesArray[j+2] = surface.getPoint(i).z;
				}

				PolygonType polygon = kmlFactory.createPolygonType();
				switch (config.getAltitudeMode()) {
				case ABSOLUTE:
					polygon.setAltitudeModeGroup(kmlFactory.createAltitudeMode(AltitudeModeEnumType.ABSOLUTE));
					break;
				case RELATIVE:
					polygon.setAltitudeModeGroup(kmlFactory.createAltitudeMode(AltitudeModeEnumType.RELATIVE_TO_GROUND));
					break;
				}
				multiGeometry.getAbstractGeometryGroup().add(kmlFactory.createPolygon(polygon));

//				for (int i = 0; i < surface.getElemInfo().length; i = i+3) {
//					LinearRingType linearRing = kmlFactory.createLinearRingType();
//					BoundaryType boundary = kmlFactory.createBoundaryType();
//					boundary.setLinearRing(linearRing);
//					if (surface.getElemInfo()[i+1] == EXTERIOR_POLYGON_RING) {
//						polygon.setOuterBoundaryIs(boundary);
//					}
//					else { // INTERIOR_POLYGON_RING
//						polygon.getInnerBoundaryIs().add(boundary);
//					}
//
//					int startNextRing = ((i+3) < surface.getElemInfo().length) ? 
//							surface.getElemInfo()[i+3] - 1: // still holes to come
//								ordinatesArray.length; // default
//
//							// order points clockwise
//							for (int j = surface.getElemInfo()[i] - 1; j < startNextRing; j = j+3) {
//								linearRing.getCoordinates().add(String.valueOf(reducePrecisionForXorY(ordinatesArray[j]) + "," 
//										+ reducePrecisionForXorY(ordinatesArray[j+1]) + ","
//										+ reducePrecisionForZ(ordinatesArray[j+2])));
//							}
//				}
				for (int i = 0; i < surface.numRings(); i++){
					LinearRingType linearRing = kmlFactory.createLinearRingType();
					BoundaryType boundary = kmlFactory.createBoundaryType();
					boundary.setLinearRing(linearRing);
					if (i == 0) {
						polygon.setOuterBoundaryIs(boundary);
					}
					else {
						polygon.getInnerBoundaryIs().add(boundary);
					}
					
					int startNextRing = ((i+1) < surface.numRings()) ? 
						(surface.getRing(i).numPoints()*3): // still holes to come
							ordinatesArray.length; // default
							
							// order points clockwise
							for (int j = cellCount; j < startNextRing; j+=3) {
								linearRing.getCoordinates().add(String.valueOf(reducePrecisionForXorY(ordinatesArray[j]) + "," 
										+ reducePrecisionForXorY(ordinatesArray[j+1]) + ","
										+ reducePrecisionForZ(ordinatesArray[j+2])));
							}
							cellCount += (surface.getRing(i).numPoints()*3);
				}
			}
		}
		catch (Exception e) {
			Logger.getInstance().warn("Exception when generating highlighting geometry of cityFurniture object " + work.getGmlId());
			e.printStackTrace();
		}
		finally {
			if (rs != null) rs.close();
			if (getGeometriesStmt != null) getGeometriesStmt.close();
		}

		return placemarkList;
	}

}
