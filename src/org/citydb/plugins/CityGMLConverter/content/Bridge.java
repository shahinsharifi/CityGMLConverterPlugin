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
import org.citygml4j.model.citygml.bridge.AbstractBridge;
import org.citygml4j.model.citygml.bridge.*;
import org.citygml4j.model.gml.geometry.AbstractGeometry;
import org.citygml4j.model.gml.geometry.GeometryProperty;
import org.citygml4j.model.gml.geometry.aggregates.MultiCurveProperty;
import org.citygml4j.model.gml.geometry.aggregates.MultiSurfaceProperty;
import org.citygml4j.model.gml.geometry.primitives.SolidProperty;

import javax.vecmath.Point3d;
import javax.xml.bind.JAXBException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class Bridge extends KmlGenericObject{

    public static final String STYLE_BASIS_NAME = "Bridge";
    private Matrix transformation;
    private SqliteImporterManager sqlliteImporterManager;
    private List<SurfaceObject> _ParentSurfaceList = new ArrayList<SurfaceObject>();
    private final Logger LOG = Logger.getInstance();
    private double refPointX;
    private double refPointY;
    private double refPointZ;


    public Bridge(KmlExporterManager kmlExporterManager,
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
        return config.getBridgeDisplayForms();
    }

    
    public ColladaOptions getColladaOptions() {
        return config.getBridgeColladaOptions();
    }

    
    public Balloon getBalloonSettings() {
        return config.getBridgeBalloon();
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

            List<PlacemarkType> placemarkBPart = readBuildingPart(work);
            if (placemarkBPart != null)
                placemarks.addAll(placemarkBPart);
        }
        catch (Exception Ex) {
            Logger.getInstance().error("SQL error while getting building parts for building " + work.getGmlId() + ": " + Ex.getMessage());
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
    private List<PlacemarkType> readBuildingPart(KmlSplittingResult work) throws Exception {

        boolean reversePointOrder = true;

        try {

            org.citygml4j.model.citygml.bridge.AbstractBridge _bridge
                    = (org.citygml4j.model.citygml.bridge.AbstractBridge)work.getCityGmlClass();

            SurfaceAppearance _SurfaceAppear = new SurfaceAppearance();

            //this function reads all geometries and returns a list of surfaces.
            List<SurfaceObject> _surfaceList = GetGeometries(_bridge);

            //Restarting Xlink worker.
         //   sqlliteImporterManager.getTmpXlinkPool().join();
          //  DBXlinkSplitter xlinkSplitter = config.getXlinkSplitter();
         //   List<BuildingSurface> tmpList = xlinkSplitter.startQuery(_surfaceList);
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
        catch (SQLException sqlEx) {
            Logger.getInstance().error("SQL error while querying city object " + work.getGmlId() + ": " + sqlEx.getMessage());
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

    
    public List<SurfaceObject> GetGeometries(AbstractBridge _bridge) throws Exception
    {
        List<SurfaceObject> _SurfaceList = new ArrayList<SurfaceObject>();
        SurfaceGeometry surfaceGeom = new SurfaceGeometry(config , sqlliteImporterManager);
        String _SurfaceType = "undefined";
        String bridgeGmlId = _bridge.getId();
        OtherGeometry others = new OtherGeometry(config , sqlliteImporterManager,3068);

        // //Geometry
        // lodXSolid
        for (int lod = 1; lod < 5; lod++) {

            SolidProperty solidProperty = null;

            switch (lod) {
                case 1:
                    solidProperty = _bridge.getLod1Solid();
                    break;
                case 2:
                    solidProperty = _bridge.getLod2Solid();
                    break;
                case 3:
                    solidProperty = _bridge.getLod3Solid();
                    break;
                case 4:
                    solidProperty = _bridge.getLod4Solid();
                    break;
            }

            if (solidProperty != null) {


                if (solidProperty.isSetSolid()) {

                    surfaceGeom.ClearPointList();
                    List<List<Double>> _pointList = surfaceGeom.getSurfaceGeometry(bridgeGmlId, solidProperty.getSolid(), false);

                    int counter = 0;
                    for(List<Double> _Geometry : _pointList){

                        SurfaceObject BSurface = new SurfaceObject();
                        _SurfaceType = surfaceGeom.DetectSurfaceType(_Geometry , "Bridge");
                        BSurface.setId(surfaceGeom.GetSurfaceID().get(counter));
                        BSurface.setType(_SurfaceType);
                        BSurface.setGeometry(_Geometry);
                        _SurfaceList.add(BSurface);
                        counter++;
                    }

                }else{

                    // xlink
                    String href = solidProperty.getHref();

                    if (href != null && href.length() != 0) {
                        DBXlinkBasic xlink = new DBXlinkBasic(
                                _bridge.getId(),
                                TableEnum.BUILDING,
                                href,
                                TableEnum.SURFACE_GEOMETRY
                        );

                        xlink.setAttrName("LOD" + lod + "_GEOMETRY_ID");
                        sqlliteImporterManager.propagateXlink(xlink);
                    }

                }


            }

        }



        // lodXMultiSurface
        for (int lod = 1; lod < 5; lod++) {

            //if (lodGeometry[lod - 1])
            //continue;

            MultiSurfaceProperty multiSurfaceProperty = null;


            switch (lod) {
                case 1:
                    multiSurfaceProperty = _bridge.getLod1MultiSurface();
                    break;
                case 2:
                    multiSurfaceProperty = _bridge.getLod2MultiSurface();
                    break;
                case 3:
                    multiSurfaceProperty = _bridge.getLod3MultiSurface();
                    break;
                case 4:
                    multiSurfaceProperty = _bridge.getLod4MultiSurface();
                    break;
            }

            if (multiSurfaceProperty != null) {

                if (multiSurfaceProperty.isSetMultiSurface()) {

                    surfaceGeom.ClearPointList();
                    List<List<Double>> _pointList = surfaceGeom.getSurfaceGeometry(bridgeGmlId, multiSurfaceProperty.getMultiSurface(), false);

                    int counter = 0;
                    for(List<Double> _Geometry : _pointList){


                        SurfaceObject BSurface = new SurfaceObject();
                        _SurfaceType = surfaceGeom.DetectSurfaceType(_Geometry , "Bridge");
                        BSurface.setId(surfaceGeom.GetSurfaceID().get(counter));
                        BSurface.setType(_SurfaceType);
                        BSurface.setGeometry(_Geometry);
                        _SurfaceList.add(BSurface);
                        counter++;
                    }

                }
                else{
                    // xlink
                    String href = multiSurfaceProperty.getHref();

                    if (href != null && href.length() != 0) {
                        DBXlinkBasic xlink = new DBXlinkBasic(
                                _bridge.getId(),
                                TableEnum.BUILDING,
                                href,
                                TableEnum.SURFACE_GEOMETRY
                        );

                        xlink.setAttrName("LOD" + lod + "_GEOMETRY_ID");
                        sqlliteImporterManager.propagateXlink(xlink);
                    }
                }
            }

        }



        // lodXTerrainIntersectionCurve
        for (int lod = 1; lod < 5; lod++) {

            MultiCurveProperty multiCurveProperty = null;

            switch (lod) {
                case 1:
                    multiCurveProperty = _bridge.getLod1TerrainIntersection();
                    break;
                case 2:
                    multiCurveProperty = _bridge.getLod2TerrainIntersection();
                    break;
                case 3:
                    multiCurveProperty = _bridge.getLod3TerrainIntersection();
                    break;
                case 4:
                    multiCurveProperty = _bridge.getLod4TerrainIntersection();
                    break;
            }

            if (multiCurveProperty != null)
            {

                surfaceGeom.ClearPointList();
                List<List<Double>> _pointList  = surfaceGeom.getMultiCurve(multiCurveProperty);

                int counter = 0;
                for(List<Double> _Geometry : _pointList){

                    SurfaceObject BSurface = new SurfaceObject();
                    _SurfaceType = surfaceGeom.DetectSurfaceType(_Geometry , "Bridge");
                    BSurface.setId(surfaceGeom.GetSurfaceID().get(counter));
                    BSurface.setType(_SurfaceType);
                    BSurface.setGeometry(_Geometry);
                    _SurfaceList.add(BSurface);
                    counter++;
                }

            }

        }



        // lodXMultiCurve
        for (int lod = 2; lod < 5; lod++) {

            MultiCurveProperty multiCurveProperty = null;


            switch (lod) {
                case 2:
                    multiCurveProperty = _bridge.getLod2MultiCurve();
                    break;
                case 3:
                    multiCurveProperty = _bridge.getLod3MultiCurve();
                    break;
                case 4:
                    multiCurveProperty = _bridge.getLod4MultiCurve();
                    break;
            }

            if (multiCurveProperty != null)
            {
                surfaceGeom.ClearPointList();

                List<List<Double>> _pointList  = surfaceGeom.getMultiCurve(multiCurveProperty);

                int counter = 0;
                for(List<Double> _Geometry : _pointList){

                    SurfaceObject BSurface = new SurfaceObject();
                    _SurfaceType = surfaceGeom.DetectSurfaceType(_Geometry , "Bridge");
                    BSurface.setId(surfaceGeom.GetSurfaceID().get(counter));
                    BSurface.setType(_SurfaceType);
                    BSurface.setGeometry(_Geometry);
                    _SurfaceList.add(BSurface);
                    counter ++;
                }

            }

        }

        // BoundarySurfaces
        if (_bridge.isSetBoundedBySurface()) {

            long ParentCounter = 1;
            for (BoundarySurfaceProperty boundarySurfaceProperty : _bridge.getBoundedBySurface()) {

                AbstractBoundarySurface boundarySurface = boundarySurfaceProperty.getBoundarySurface();

                if (boundarySurface != null) {

                    for (int lod = 2; lod < 5; lod++) {

                        MultiSurfaceProperty multiSurfaceProperty = null;

                        switch (lod) {
                            case 2:
                                multiSurfaceProperty = boundarySurface.getLod2MultiSurface();
                                break;
                            case 3:
                                multiSurfaceProperty = boundarySurface.getLod3MultiSurface();
                                break;
                            case 4:
                                multiSurfaceProperty = boundarySurface.getLod4MultiSurface();
                                break;
                        }

                        if (multiSurfaceProperty != null) {

                            if (multiSurfaceProperty.isSetMultiSurface()) {

                                //We should take care about the parent surfaces, because we need them for exporting collada.
                                if(multiSurfaceProperty.getMultiSurface().isSetId()){

                                    SurfaceObject BPSurface = new SurfaceObject();
                                    BPSurface.setPId(ParentCounter);
                                    BPSurface.setId(multiSurfaceProperty.getMultiSurface().getId());
                                    BPSurface.setType(null);
                                    BPSurface.setGeometry(null);
                                    _ParentSurfaceList.add(BPSurface);
                                }

                                surfaceGeom.ClearPointList();
                                surfaceGeom.ClearIdList();

                                _SurfaceType = "";//TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.TUNNEL_BOUNDARY_SURFACE_PROPERTY).toString();
                                List<List<Double>> _pointList  = surfaceGeom.getSurfaceGeometry(bridgeGmlId, multiSurfaceProperty.getMultiSurface(), false);

                                int counter = 0;
                                for(List<Double> _Geometry : _pointList){


                                    SurfaceObject BSurface = new SurfaceObject();
                                    BSurface.setPId(ParentCounter);
                                    BSurface.setId(surfaceGeom.GetSurfaceID().get(counter));
                                    BSurface.setType(_SurfaceType);
                                    BSurface.setGeometry(_Geometry);
                                    _SurfaceList.add(BSurface);

                                    counter++;

                                }
                            }
                            else {

                                // xlink
                                String href = boundarySurfaceProperty.getHref();

                                if (href != null && href.length() != 0) {
                                    LOG.error("XLink reference '" + href + "' to BoundarySurface feature is not supported.");
                                }
                            }
                        }
                    }


                    // BoundrySurface - Openings
                    if (boundarySurface.isSetOpening()) {

                        for (OpeningProperty openingProperty : boundarySurface.getOpening()) {
                            if (openingProperty.isSetOpening()) {
                                AbstractOpening opening = openingProperty.getOpening();

                                // Opening - Geometry
                                for (int lod = 3; lod < 5; lod++) {

                                    MultiSurfaceProperty multiSurfaceProperty = null;


                                    switch (lod) {
                                        case 3:
                                            multiSurfaceProperty = opening.getLod3MultiSurface();
                                            break;
                                        case 4:
                                            multiSurfaceProperty = opening.getLod4MultiSurface();
                                            break;
                                    }

                                    if (multiSurfaceProperty != null) {
                                        if (multiSurfaceProperty.isSetMultiSurface()) {

                                            surfaceGeom.ClearPointList();
                                            surfaceGeom.ClearIdList();
                                            List<List<Double>> _pointList = surfaceGeom.getSurfaceGeometry(bridgeGmlId, multiSurfaceProperty.getMultiSurface(), false);
                                            _SurfaceType = "";//TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.BUILDING_WALL_SURFACE).toString();

                                            int counter = 0;
                                            for(List<Double> _Geometry : _pointList){

                                                SurfaceObject BSurface = new SurfaceObject();
                                                BSurface.setPId(ParentCounter);
                                                BSurface.setId(surfaceGeom.GetSurfaceID().get(counter));
                                                BSurface.setType(_SurfaceType);
                                                BSurface.setGeometry(_Geometry);
                                                _SurfaceList.add(BSurface);
                                                counter++;
                                            }


                                        }
                                    }

                                    // free memory of nested feature
                                    openingProperty.unsetOpening();
                                }
                            }
                        }

                        // free memory of nested feature
                        boundarySurfaceProperty.unsetBoundarySurface();
                    }
                }
                ParentCounter++;
            }
        }


        // BridgeInstallation
        if (_bridge.isSetOuterBridgeInstallation()) {
            for (BridgeInstallationProperty bridgeInstProperty : _bridge.getOuterBridgeInstallation()) {
                BridgeInstallation bridgeInst = bridgeInstProperty.getBridgeInstallation();

                if (bridgeInst != null) {

                    // Geometry
                    for (int lod = 2; lod < 5; lod++) {
                        GeometryProperty<? extends AbstractGeometry> geometryProperty = null;


                        switch (lod) {
                            case 2:
                                geometryProperty = bridgeInst.getLod2Geometry();
                                break;
                            case 3:
                                geometryProperty = bridgeInst.getLod3Geometry();
                                break;
                            case 4:
                                geometryProperty = bridgeInst.getLod4Geometry();
                                break;
                        }

                        if (geometryProperty != null) {
                            if (geometryProperty.isSetGeometry()) {

                                surfaceGeom.ClearPointList();
                                surfaceGeom.ClearIdList();
                                List<List<Double>> _pointList = surfaceGeom.getSurfaceGeometry(bridgeGmlId, geometryProperty.getGeometry(), false);
                                _SurfaceType = "";//TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.TUNNEL_WALL_SURFACE).toString();

                                int counter = 0;
                                for(List<Double> _Geometry : _pointList){

                                    SurfaceObject BSurface = new SurfaceObject();
                                    _SurfaceType = surfaceGeom.DetectSurfaceType(_Geometry , "Bridge");
                                    BSurface.setId(surfaceGeom.GetSurfaceID().get(counter));
                                    BSurface.setType(_SurfaceType);
                                    BSurface.setGeometry(_Geometry);
                                    _SurfaceList.add(BSurface);
                                    counter ++;
                                }
                            }
                        }

                    }

                    // free memory of nested feature
                    bridgeInstProperty.unsetBridgeInstallation();
                } else {
                    // xlink
                    String href = bridgeInstProperty.getHref();

                    if (href != null && href.length() != 0) {
                        LOG.error("XLink reference '" + href + "' to BuildingInstallation feature is not supported.");
                    }
                }

            }
        }


        // IntBuildingInstallation
        if (_bridge.isSetInteriorBridgeInstallation()) {
            for (IntBridgeInstallationProperty intBridgeInstProperty : _bridge.getInteriorBridgeInstallation()) {
                IntBridgeInstallation intBridgeInst = intBridgeInstProperty.getIntBridgeInstallation();

                if (intBridgeInst != null) {


                    if (intBridgeInst.isSetLod4Geometry()) {
                        GeometryProperty<? extends AbstractGeometry> geometryProperty = intBridgeInst.getLod4Geometry();

                        if (geometryProperty.isSetGeometry()) {

                            surfaceGeom.ClearPointList();
                            surfaceGeom.ClearIdList();
                            List<List<Double>> _pointList = surfaceGeom.getSurfaceGeometry(bridgeGmlId, geometryProperty.getGeometry(), false);
                            _SurfaceType = "";//TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.BUILDING_WALL_SURFACE).toString();

                            int counter = 0;
                            for(List<Double> _Geometry : _pointList){

                                SurfaceObject BSurface = new SurfaceObject();
                                _SurfaceType = surfaceGeom.DetectSurfaceType(_Geometry , "Bridge");
                                BSurface.setId(surfaceGeom.GetSurfaceID().get(counter));
                                BSurface.setType(_SurfaceType);
                                BSurface.setGeometry(_Geometry);
                                _SurfaceList.add(BSurface);
                                counter ++;
                            }
                        }
                    }


                    // free memory of nested feature
                    intBridgeInstProperty.unsetIntBridgeInstallation();
                }
                else {
                    // xlink
                    String href = intBridgeInstProperty.getHref();

                    if (href != null && href.length() != 0) {
                        LOG.error("XLink reference '" + href + "' to IntBuildingInstallation feature is not supported.");
                    }
                }
            }
        }


        // BridgePart
        if (_bridge.isSetConsistsOfBridgePart()) {
            for (BridgePartProperty bridgePartProperty : _bridge.getConsistsOfBridgePart()) {
                BridgePart bridgePart = bridgePartProperty.getBridgePart();

                if (bridgePart != null) {

                    _SurfaceList.addAll(GetGeometries(bridgePart));


                    // free memory of nested feature
                    bridgePartProperty.unsetBridgePart();
                }
                else {
                    // xlink
                    String href = bridgePartProperty.getHref();

                    if (href != null && href.length() != 0) {
                        LOG.error("XLink reference '" + href + "' to BuildingPart feature is not supported.");
                    }
                }
            }

        }
        
        return _SurfaceList;
    }

    public static HashMap<String,Object> getObjectProperties(org.citygml4j.model.citygml.vegetation.SolitaryVegetationObject vegetation){

        HashMap<String, Object> objectgMap = new HashMap<String,Object>();

        //Building GmlID
        if (vegetation.isSetId()) {
            objectgMap.put("GMLID",vegetation.getId());
        }

        //Building name and codespace
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