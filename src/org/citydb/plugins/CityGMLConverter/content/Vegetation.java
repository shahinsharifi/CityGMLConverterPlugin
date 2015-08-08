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

import net.opengis.kml._2.MultiGeometryType;
import net.opengis.kml._2.PlacemarkType;
import org.citydb.api.event.EventDispatcher;
import org.citydb.api.geometry.GeometryObject;
import org.citydb.config.internal.Internal;
import org.citydb.log.Logger;
import org.citydb.modules.common.event.CounterEvent;
import org.citydb.modules.common.event.CounterType;
import org.citydb.plugins.CityGMLConverter.config.Balloon;
import org.citydb.plugins.CityGMLConverter.config.ColladaOptions;
import org.citydb.plugins.CityGMLConverter.config.ConfigImpl;
import org.citydb.plugins.CityGMLConverter.config.DisplayForm;
import org.citydb.plugins.CityGMLConverter.util.ProjConvertor;
import org.citydb.plugins.CityGMLConverter.util.Sqlite.SqliteImporterManager;
import org.citydb.plugins.CityGMLConverter.xlink.content.DBXlinkBasic;
import org.citydb.util.Util;
import org.citygml4j.factory.GMLGeometryFactory;
import org.citygml4j.geometry.Matrix;
import org.citygml4j.model.citygml.core.ImplicitGeometry;
import org.citygml4j.model.citygml.core.ImplicitRepresentationProperty;
import org.citygml4j.model.citygml.vegetation.SolitaryVegetationObject;
import org.citygml4j.model.gml.geometry.AbstractGeometry;
import org.citygml4j.model.gml.geometry.GeometryProperty;

import javax.vecmath.Point3d;
import javax.xml.bind.JAXBException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class Vegetation extends KmlGenericObject{

    public static final String STYLE_BASIS_NAME = "Vegetation";
    private Matrix transformation;
    private SqliteImporterManager sqlliteImporterManager;
    private List<SurfaceObject> _ParentSurfaceList = new ArrayList<SurfaceObject>();
    private final Logger LOG = Logger.getInstance();
    private double refPointX;
    private double refPointY;
    private double refPointZ;


    public Vegetation(KmlExporterManager kmlExporterManager,
                      SqliteImporterManager sqlliteImporterManager,
                      GMLGeometryFactory cityGMLFactory,
                      net.opengis.kml._2.ObjectFactory kmlFactory,
                      ElevationServiceHandler elevationServiceHandler,
                      BalloonTemplateHandlerImpl balloonTemplateHandler,
                      EventDispatcher eventDispatcher,
                      ConfigImpl config) {

        super(kmlExporterManager,
            cityGMLFactory,
            kmlFactory,
            elevationServiceHandler,
            balloonTemplateHandler,
            eventDispatcher,
            config);

        this.sqlliteImporterManager = sqlliteImporterManager;
    }

    protected List<DisplayForm> getDisplayForms() {
        return config.getVegetationDisplayForms();
    }

    
    public ColladaOptions getColladaOptions() {
        return config.getVegetationColladaOptions();
    }

    
    public Balloon getBalloonSettings() {
        return config.getVegetationBalloon();
    }

    
    public String getStyleBasisName() {
        return STYLE_BASIS_NAME;
    }

    
    protected String getHighlightingQuery() {
        return null;
    }

    
    public void read(KmlSplittingResult work) {

        List<PlacemarkType> placemarks = new ArrayList<PlacemarkType>();

        try {

            List<PlacemarkType> placemarkBPart = readObject(work);
            if (placemarkBPart != null)
                placemarks.addAll(placemarkBPart);
        }
        catch (Exception Ex) {
            Logger.getInstance().error("SQL error while getting vegetation parts for object " + work.getGmlId() + ": " + Ex.getMessage());
        }
        finally {

            if (placemarks.size() == 0) {
                int lodToExportFrom = config.getLodToExportFrom();
                String fromMessage = " from LoD" + lodToExportFrom;
                if (lodToExportFrom == 5) {
                    if (work.getDisplayForm().getForm() == DisplayForm.COLLADA)
                        fromMessage = ". LoD1 or higher required";
                    else
                        fromMessage = " from any LoD";
                }
                Logger.getInstance().info("Could not display object " + work.getGmlId()
                        + " as " + work.getDisplayForm().getName() + fromMessage + ".");
            }
            else {
                try {
                    // compact list before exporting
                    for (int i = 0; i < placemarks.size(); i++) {
                        PlacemarkType placemark1 = placemarks.get(i);
                        if (placemark1 == null) continue;
                        MultiGeometryType multiGeometry1 = (MultiGeometryType) placemark1.getAbstractGeometryGroup().getValue();
                        for (int j = i+1; j < placemarks.size(); j++) {
                            PlacemarkType placemark2 = placemarks.get(j);
                            if (placemark2 == null || !placemark1.getId().equals(placemark2.getId())) continue;
                            // compact since ids are identical
                            MultiGeometryType multiGeometry2 = (MultiGeometryType) placemark2.getAbstractGeometryGroup().getValue();
                            multiGeometry1.getAbstractGeometryGroup().addAll(multiGeometry2.getAbstractGeometryGroup());
                            placemarks.set(j, null); // polygons transfered, placemark exhausted
                        }
                    }

                    kmlExporterManager.print(placemarks,
                            work,
                            getBalloonSettings().isBalloonContentInSeparateFile());

                    eventDispatcher.triggerEvent(new CounterEvent(CounterType.TOPLEVEL_FEATURE, 1, this));
                }
                catch (JAXBException jaxbEx) {

                    LOG.error(jaxbEx.toString());
                }
            }
        }

    }

    
    @SuppressWarnings("unchecked")
    private List<PlacemarkType> readObject(KmlSplittingResult work) throws Exception {

        boolean reversePointOrder = true;

        try {

            SolitaryVegetationObject _vegetation = (SolitaryVegetationObject)work.getCityGmlClass();
            SurfaceAppearance _SurfaceAppear = new SurfaceAppearance();

            //this function reads all geometries and returns a list of surfaces.
            List<SurfaceObject> _surfaceList = GetGeometries(_vegetation);

            //Restarting Xlink worker.
         //   sqlliteImporterManager.getTmpXlinkPool().join();
          //  DBXlinkSplitter xlinkSplitter = config.getXlinkSplitter();
         //   List<SurfaceObject> tmpList = xlinkSplitter.startQuery(_surfaceList);
         //   if(tmpList != null && tmpList.size() > 0) //We should join xlinks with Main geometries
          //      _surfaceList.addAll(tmpList);

            if (_surfaceList.size()!=0) { // result not empty

                switch (work.getDisplayForm().getForm()) {

                    case DisplayForm.FOOTPRINT:
                        return createPlacemarksForFootprint(_surfaceList, work);

                    case DisplayForm.EXTRUDED:

                        double measuredHeight = 0;//(_landuse.getMeasuredHeight() != null) ? _landuse.getMeasuredHeight().getValue(): 10;
                        return createPlacemarksForExtruded(_surfaceList, work, measuredHeight, reversePointOrder);

                    case DisplayForm.GEOMETRY:

                        if (work.getDisplayForm().isHighlightingEnabled()) {
                            if (config.getFilter().isSetComplexFilter()) { // region

                                List<PlacemarkType> hlPlacemarks = createPlacemarksForHighlighting(_surfaceList, work);
                                hlPlacemarks.addAll(createPlacemarksForGeometry(_surfaceList, work));
                                return hlPlacemarks;
                            }
                            else {// reverse order for single objects

                                List<PlacemarkType> placemarks = createPlacemarksForGeometry(_surfaceList, work);
                                placemarks.addAll(createPlacemarksForHighlighting(_surfaceList, work));
                                return placemarks;
                            }
                        }

                        return createPlacemarksForGeometry(_surfaceList, work);

                    case DisplayForm.COLLADA:

                        fillGenericObjectForCollada(work,_surfaceList,_SurfaceAppear,_ParentSurfaceList);
                        setGmlId(work.getGmlId());
                    //    setId(work.getId());

                        if (getGeometryAmount() > GEOMETRY_AMOUNT_WARNING) {
                            Logger.getInstance().info("Object " + work.getGmlId() + " has more than " + GEOMETRY_AMOUNT_WARNING + " geometries. This may take a while to process...");
                        }

                        List<Point3d> anchorCandidates = getOrigins(); // setOrigins() called mainly for the side-effect
                        double zOffset = getZOffsetFromDBorConfig(work.getGmlId(),work.GetElevation());
                        if (zOffset == Double.MAX_VALUE) {
                            zOffset = getZOffsetFromGEService(work.getGmlId(),anchorCandidates,work.getTargetSrs(),work.GetElevation());
                        }

                        setZOffset(zOffset);

                        ColladaOptions colladaOptions = getColladaOptions();
                        setIgnoreSurfaceOrientation(colladaOptions.isIgnoreSurfaceOrientation());
                        try {

                        	if (work.getDisplayForm().isHighlightingEnabled()) {
                        		return createPlacemarksForHighlighting(_surfaceList, work);
                        	}
                            // just COLLADA, no KML
                            List<PlacemarkType> dummy = new ArrayList<PlacemarkType>();
                            dummy.add(null);
                            return dummy;
                        }
                        catch (Exception ioe) {
                            ioe.printStackTrace();
                        }
                }
            }
        }
        catch (Exception Ex) {
            Logger.getInstance().error("Error while querying city object " + work.getGmlId() + ": " + Ex.getMessage());
            return null;
        }
        finally {}

        return null;
        // nothing found
    }

    
    public PlacemarkType createPlacemarkForColladaModel(KmlSplittingResult work) throws Exception {

        List<Double> originInWGS84 = ProjConvertor.transformPoint(
                getOrigin().x,
                getOrigin().y,
                getOrigin().z,
                work.getTargetSrs(),
                "4326");
        setLocation(reducePrecisionForXorY(originInWGS84.get(1)),
                reducePrecisionForXorY(originInWGS84.get(0)),
                reducePrecisionForZ(originInWGS84.get(2)));

        return super.createPlacemarkForColladaModel(work);
    }

    
    public List<SurfaceObject> GetGeometries(org.citygml4j.model.citygml.vegetation.SolitaryVegetationObject _vegetation) throws Exception
    {
        List<SurfaceObject> _SurfaceList = new ArrayList<SurfaceObject>();
        SurfaceGeometry surfaceGeom = new SurfaceGeometry(config , sqlliteImporterManager);
        String _SurfaceType = "undefined";
        String RootGmlId = _vegetation.getId();
        OtherGeometry others = new OtherGeometry(config , sqlliteImporterManager,3068);

        // //Geometry
        for (int lod = 0; lod < 4; lod++) {

            GeometryProperty<? extends AbstractGeometry> geometryProperty = null;
            long geometryId = 0;
            GeometryObject geometryObject = null;

            switch (lod) {
                case 0:
                    geometryProperty = _vegetation.getLod1Geometry();
                    break;
                case 1:
                    geometryProperty = _vegetation.getLod2Geometry();
                    break;
                case 2:
                    geometryProperty = _vegetation.getLod3Geometry();
                    break;
                case 3:
                    geometryProperty = _vegetation.getLod4Geometry();
                    break;
            }

            List<List<Double>> _pointList = new ArrayList<List<Double>>();
            if (geometryProperty != null) {

                if (geometryProperty.isSetGeometry()) {
                    AbstractGeometry abstractGeometry = geometryProperty.getGeometry();

                    if (surfaceGeom.isSurfaceGeometry(abstractGeometry))
                        _pointList.addAll(surfaceGeom.getSurfaceGeometry(RootGmlId, abstractGeometry, false));
                    else if (others.isPointOrLineGeometry(abstractGeometry)) {
                        geometryObject = others.getPointOrCurveGeometry(abstractGeometry);
                        _pointList.addAll(others.ConvertGeomObjectToPointList(geometryObject));
                    }else {
                        StringBuilder msg = new StringBuilder(Util.getFeatureSignature(
                                _vegetation.getCityGMLClass(),
                                _vegetation.getId()));
                        msg.append(": Unsupported geometry type ");
                        msg.append(abstractGeometry.getGMLClass()).append('.');

                        LOG.error(msg.toString());
                    }

                    int counter = 0;
                    for (List<Double> _Geometry : _pointList) {

                        SurfaceObject BSurface = new SurfaceObject();
                        _SurfaceType = surfaceGeom.DetectSurfaceType(_Geometry);
                        BSurface.setId(surfaceGeom.GetSurfaceID().get(counter));
                        BSurface.setType(_SurfaceType);
                        BSurface.setGeometry(_Geometry);
                        _SurfaceList.add(BSurface);
                        counter++;
                    }

                } else {
                    // xlink
                    String href = geometryProperty.getHref();

                    if (href != null && href.length() != 0) {
                        DBXlinkBasic xlink = new DBXlinkBasic(
                                _vegetation.getId(),
                                TableEnum.SOLITARY_VEGETAT_OBJECT,
                                href,
                                TableEnum.SURFACE_GEOMETRY
                        );

                        xlink.setAttrName("LOD" + lod + "_GEOMETRY_ID");
                        sqlliteImporterManager.propagateXlink(xlink);
                    }
                }
            }
        }

        // implicit geometry
        for (int lod = 0; lod < 4; lod++) {
            ImplicitRepresentationProperty implicit = null;
            GeometryObject pointGeom = null;

            switch (lod) {
                case 0:
                    implicit = _vegetation.getLod1ImplicitRepresentation();
                    break;
                case 1:
                    implicit = _vegetation.getLod2ImplicitRepresentation();
                    break;
                case 2:
                    implicit = _vegetation.getLod3ImplicitRepresentation();
                    break;
                case 3:
                    implicit = _vegetation.getLod4ImplicitRepresentation();
                    break;
            }

            if (implicit != null) {
                if (implicit.isSetObject()) {

                    ImplicitGeometry geometry = implicit.getObject();

                    // reference Point
                    if (geometry.isSetReferencePoint()) {
                        pointGeom = others.getPoint(geometry.getReferencePoint());
                        double[] ordinatesArray = pointGeom.getCoordinates(0);
                        refPointX = ordinatesArray[0];
                        refPointY = ordinatesArray[1];
                        refPointZ = ordinatesArray[2];
                    }

                    // transformation matrix
                    if (geometry.isSetTransformationMatrix())
                        transformation = geometry.getTransformationMatrix().getMatrix();

                    long implicitGeometryId = 0;
                    boolean updateTable = false;
                    boolean isXLink = false;

                    String libraryURI = geometry.getLibraryObject();
                    if (libraryURI != null)
                        libraryURI = libraryURI.trim();

                    AbstractGeometry relativeGeometry = null;
                    String gmlId = null;

                    if (geometry.isSetRelativeGMLGeometry()) {
                        GeometryProperty<? extends AbstractGeometry> property = geometry.getRelativeGMLGeometry();

                        if (property.isSetHref()) {
                            gmlId = property.getHref();
                            if (Util.isRemoteXlink(gmlId)) {
                                LOG.error("XLink reference '" + gmlId + "' to remote relative GML geometry is not supported.");
                            }

                            gmlId = gmlId.replaceAll("^#", "");
                            isXLink = true;

                        } else if (property.isSetGeometry()) {
                            relativeGeometry = property.getGeometry();
                            gmlId = relativeGeometry.getId();
                            updateTable = !relativeGeometry.hasLocalProperty(Internal.GEOMETRY_ORIGINAL);
                        }
                    }

                    GeometryObject geometryObject = null;
                    surfaceGeom.ClearPointList();
                    List<List<Double>> _pointList = new ArrayList<List<Double>>();

                    if (relativeGeometry != null) {
                        if (surfaceGeom.isSurfaceGeometry(relativeGeometry))
                            _pointList.addAll(surfaceGeom.getSurfaceGeometry(RootGmlId, relativeGeometry, false));
                        else if (others.isPointOrLineGeometry(relativeGeometry)) {
                            geometryObject = others.getPointOrCurveGeometry(relativeGeometry);
                            _pointList.addAll(others.ConvertGeomObjectToPointList(geometryObject));
                        }else {
                            StringBuilder msg = new StringBuilder(Util.getFeatureSignature(
                                    _vegetation.getCityGMLClass(),
                                    _vegetation.getId()));
                            msg.append(": Unsupported geometry type ");
                            msg.append(relativeGeometry.getGMLClass()).append('.');

                            LOG.error(msg.toString());
                        }

                        int counter = 0;
                        for (List<Double> _Geometry : _pointList) {

                            SurfaceObject BSurface = new SurfaceObject();
                            _SurfaceType = surfaceGeom.DetectSurfaceType(_Geometry);
                            BSurface.setId(surfaceGeom.GetSurfaceID().get(counter));
                            BSurface.setType(_SurfaceType);
                            BSurface.setGeometry(_Geometry);
                            _SurfaceList.add(BSurface);
                            counter++;
                        }
                    }

                }
                else{
                    // xlink
                    String href = implicit.getHref();

                    if (href != null && href.length() != 0) {
                        DBXlinkBasic xlink = new DBXlinkBasic(
                                _vegetation.getId(),
                                TableEnum.SOLITARY_VEGETAT_OBJECT,
                                href,
                                TableEnum.SURFACE_GEOMETRY
                        );

                        xlink.setAttrName("LOD" + lod + "_GEOMETRY_ID");
                        sqlliteImporterManager.propagateXlink(xlink);
                    }
                }
            }

        }

        return _SurfaceList;
    }



    public static HashMap<String,Object> getObjectProperties(org.citygml4j.model.citygml.vegetation.SolitaryVegetationObject vegetation){

        HashMap<String, Object> objectgMap = new HashMap<String,Object>();

        // GmlID
        if (vegetation.isSetId()) {
            objectgMap.put("GMLID",vegetation.getId());
        }

        // name and codespace
        if (vegetation.isSetName()) {
            objectgMap.put("NAME",vegetation.getName());
            if(vegetation.getName().get(0).isSetCodeSpace())
                objectgMap.put("NAME_CODESPACE", vegetation.getName().get(0).getCodeSpace());
        }

        // class
        if (vegetation.isSetClazz() && vegetation.getClazz().isSetValue()) {
            objectgMap.put("CLASS",vegetation.getClazz().getValue());
        }

        //Description
        if(vegetation.isSetDescription())
        {
            objectgMap.put("DESCRIPTION",vegetation.getDescription());
        }

        // function
        if (vegetation.isSetFunction()) {
            String[] function = Util.codeList2string(vegetation.getFunction());
            objectgMap.put("FUNCTION",function[0]);
        }

        // usage
        if (vegetation.isSetUsage()) {
            String[] usage = Util.codeList2string(vegetation.getUsage());
            objectgMap.put("USAGE",usage[0]);
        }


        // veg:species
        if (vegetation.isSetSpecies() && vegetation.getSpecies().isSetValue()) {
            objectgMap.put("SPECIES_VALUE",vegetation.getSpecies().getValue());
            objectgMap.put("SPECIES_CODESPACE",vegetation.getSpecies().getCodeSpace());
        }

        // veg:height
        if (vegetation.isSetHeight() && vegetation.getHeight().isSetValue()) {
            objectgMap.put("HEIGHT_VALUE",vegetation.getHeight().getValue());
            objectgMap.put("HEIGHT_UOM",vegetation.getSpecies().getCodeSpace());
        }

        // veg:trunkDiameter
        if (vegetation.isSetTrunkDiameter() && vegetation.getTrunkDiameter().isSetValue()) {
            objectgMap.put("TRUNK_VALUE",vegetation.getTrunkDiameter().getValue());
            objectgMap.put("TRUNK_UOM",vegetation.getTrunkDiameter().getUom());
        }

        // veg:crownDiameter
        if (vegetation.isSetCrownDiameter() && vegetation.getCrownDiameter().isSetValue()) {
            objectgMap.put("CROWN_VALUE",vegetation.getCrownDiameter().getValue());
            objectgMap.put("CROWN_UOM",vegetation.getCrownDiameter().getUom());
        }

        return objectgMap;
    }

}