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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.vecmath.Point3d;
import javax.xml.bind.JAXBException;

import net.opengis.kml._2.MultiGeometryType;
import net.opengis.kml._2.PlacemarkType;

import org.citydb.api.event.EventDispatcher;
import org.citydb.api.geometry.GeometryObject;
import org.citydb.log.Logger;
import org.citydb.modules.common.event.CounterEvent;
import org.citydb.modules.common.event.CounterType;
import org.citygml4j.factory.GMLGeometryFactory;
import org.citygml4j.model.citygml.CityGMLClass;
import org.citygml4j.model.citygml.building.AbstractBoundarySurface;
import org.citygml4j.model.citygml.building.AbstractBuilding;
import org.citygml4j.model.citygml.building.AbstractOpening;
import org.citygml4j.model.citygml.building.BoundarySurfaceProperty;
import org.citygml4j.model.citygml.building.BuildingFurniture;
import org.citygml4j.model.citygml.building.BuildingInstallation;
import org.citygml4j.model.citygml.building.BuildingInstallationProperty;
import org.citygml4j.model.citygml.building.BuildingPart;
import org.citygml4j.model.citygml.building.BuildingPartProperty;
import org.citygml4j.model.citygml.building.IntBuildingInstallation;
import org.citygml4j.model.citygml.building.IntBuildingInstallationProperty;
import org.citygml4j.model.citygml.building.InteriorFurnitureProperty;
import org.citygml4j.model.citygml.building.InteriorRoomProperty;
import org.citygml4j.model.citygml.building.OpeningProperty;
import org.citygml4j.model.citygml.building.Room;
import org.citygml4j.model.citygml.core.Address;
import org.citygml4j.model.citygml.core.XalAddressProperty;
import org.citygml4j.model.gml.basicTypes.DoubleOrNull;
import org.citygml4j.model.gml.basicTypes.MeasureOrNullList;
import org.citygml4j.model.gml.geometry.AbstractGeometry;
import org.citygml4j.model.gml.geometry.GeometryProperty;
import org.citygml4j.model.gml.geometry.aggregates.MultiCurveProperty;
import org.citygml4j.model.gml.geometry.aggregates.MultiSurfaceProperty;
import org.citygml4j.model.gml.geometry.primitives.SolidProperty;
import org.citygml4j.model.xal.AddressDetails;
import org.citygml4j.model.xal.Country;
import org.citygml4j.model.xal.CountryName;
import org.citygml4j.model.xal.DependentLocality;
import org.citygml4j.model.xal.Locality;
import org.citygml4j.model.xal.LocalityName;
import org.citygml4j.model.xal.PostBox;
import org.citygml4j.model.xal.PostalCode;
import org.citygml4j.model.xal.PostalCodeNumber;
import org.citygml4j.model.xal.Thoroughfare;
import org.citygml4j.model.xal.ThoroughfareName;
import org.citygml4j.model.xal.ThoroughfareNumberOrRange;
import org.citydb.plugins.CityGMLConverter.config.Balloon;
import org.citydb.plugins.CityGMLConverter.config.ColladaOptions;
import org.citydb.plugins.CityGMLConverter.config.ConfigImpl;
import org.citydb.plugins.CityGMLConverter.config.DisplayForm;
import org.citydb.plugins.CityGMLConverter.util.ProjConvertor;
import org.citydb.plugins.CityGMLConverter.util.Sqlite.SqliteImporterManager;
import org.citydb.plugins.CityGMLConverter.xlink.content.DBXlinkBasic;
import org.citydb.util.Util;


public class Building extends KmlGenericObject{

    public static final String STYLE_BASIS_NAME = "";//"Building";
    private SqliteImporterManager sqlliteImporterManager;
    private List<SurfaceObject> _ParentSurfaceList = new ArrayList<SurfaceObject>();
    private final Logger LOG = Logger.getInstance();


    public Building(KmlExporterManager kmlExporterManager,
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
        return config.getBuildingDisplayForms();
    }

    
    public ColladaOptions getColladaOptions() {
        return config.getBuildingColladaOptions();
    }

    
    public Balloon getBalloonSettings() {
        return config.getBuildingBalloon();
    }

    
    public String getStyleBasisName() {
        return STYLE_BASIS_NAME;
    }

    
    protected String getHighlightingQuery() {
        return null;//Queries.getBuildingPartHighlightingQuery(currentLod);
    }

    
    public void read(KmlSplittingResult work) {

        List<PlacemarkType> placemarks = new ArrayList<PlacemarkType>();

        try {

            List<PlacemarkType> placemarkBPart = readBuildingPart(work);
            if (placemarkBPart != null)
                placemarks.addAll(placemarkBPart);
        }
        catch (Exception Ex) {
            Logger.getInstance().error("Error getting building parts for building " + work.getGmlId() + ": " + Ex.getMessage());
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

            AbstractBuilding _building = (AbstractBuilding)work.getCityGmlClass();
            SurfaceAppearance _SurfaceAppear = new SurfaceAppearance();

            //this function reads all geometries and returns a list of surfaces.
            List<SurfaceObject> _surfaceList = GetBuildingGeometries(_building);

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

                        double measuredHeight = (_building.getMeasuredHeight() != null) ? _building.getMeasuredHeight().getValue(): 10;
                        return createPlacemarksForExtruded(_surfaceList, work, measuredHeight, reversePointOrder);

                    case DisplayForm.GEOMETRY:

                        if (work.getDisplayForm().isHighlightingEnabled()) {
                            if (config.getFilter().isSetComplexFilter()) { // region

                                List<PlacemarkType> hlPlacemarks = createPlacemarksForHighlighting(_surfaceList, work);
                                hlPlacemarks.addAll(createPlacemarksForGeometry(_surfaceList, work));
                                return hlPlacemarks;
                            }
                            else { // reverse order for single buildings

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
            Logger.getInstance().error("Error querying city object " + work.getGmlId() + ": " + sqlEx.getMessage());
            return null;
        }
        finally {

        }

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

    
    public List<SurfaceObject> GetBuildingGeometries(AbstractBuilding _building) throws Exception
    {
        List<SurfaceObject> _SurfaceList = new ArrayList<SurfaceObject>();
        SurfaceGeometry surfaceGeom = new SurfaceGeometry(config , sqlliteImporterManager);
        String _SurfaceType = "undefined";
        String buildingGmlId = _building.getId();
        
        
        // lodXSolid
        for (int lod = 1; lod < 5; lod++) {

            SolidProperty solidProperty = null;

            switch (lod) {
                case 1:
                    solidProperty = _building.getLod1Solid();
                    break;
                case 2:
                    solidProperty = _building.getLod2Solid();
                    break;
                case 3:
                    solidProperty = _building.getLod3Solid();
                    break;
                case 4:
                    solidProperty = _building.getLod4Solid();
                    break;
            }

            if (solidProperty != null) {


                if (solidProperty.isSetSolid()) {

                    surfaceGeom.ClearPointList();
                    List<List<Double>> _pointList = surfaceGeom.GetSurfaceGeometry(buildingGmlId , solidProperty.getSolid() , false);

                    int counter = 0;
                    for(List<Double> _Geometry : _pointList){

                        SurfaceObject BSurface = new SurfaceObject();
                        _SurfaceType = surfaceGeom.DetectSurfaceType(_Geometry);
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
                                _building.getId(),
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
                    multiSurfaceProperty = _building.getLod1MultiSurface();
                    break;
                case 2:
                    multiSurfaceProperty = _building.getLod2MultiSurface();
                    break;
                case 3:
                    multiSurfaceProperty = _building.getLod3MultiSurface();
                    break;
                case 4:
                    multiSurfaceProperty = _building.getLod4MultiSurface();
                    break;
            }

            if (multiSurfaceProperty != null) {

                if (multiSurfaceProperty.isSetMultiSurface()) {

                    surfaceGeom.ClearPointList();
                    List<List<Double>> _pointList = surfaceGeom.GetSurfaceGeometry(buildingGmlId,multiSurfaceProperty.getMultiSurface(), false);

                    int counter = 0;
                    for(List<Double> _Geometry : _pointList){


                        SurfaceObject BSurface = new SurfaceObject();
                        _SurfaceType = surfaceGeom.DetectSurfaceType(_Geometry);
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
                                _building.getId(),
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
                    multiCurveProperty = _building.getLod1TerrainIntersection();
                    break;
                case 2:
                    multiCurveProperty = _building.getLod2TerrainIntersection();
                    break;
                case 3:
                    multiCurveProperty = _building.getLod3TerrainIntersection();
                    break;
                case 4:
                    multiCurveProperty = _building.getLod4TerrainIntersection();
                    break;
            }

            if (multiCurveProperty != null)
            {

                surfaceGeom.ClearPointList();
                List<List<Double>> _pointList  = surfaceGeom.getMultiCurve(multiCurveProperty);

                int counter = 0;
                for(List<Double> _Geometry : _pointList){

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



        // lodXMultiCurve
        for (int lod = 2; lod < 5; lod++) {

            MultiCurveProperty multiCurveProperty = null;


            switch (lod) {
                case 2:
                    multiCurveProperty = _building.getLod2MultiCurve();
                    break;
                case 3:
                    multiCurveProperty = _building.getLod3MultiCurve();
                    break;
                case 4:
                    multiCurveProperty = _building.getLod4MultiCurve();
                    break;
            }

            if (multiCurveProperty != null)
            {
                surfaceGeom.ClearPointList();

                List<List<Double>> _pointList  = surfaceGeom.getMultiCurve(multiCurveProperty);

                int counter = 0;
                for(List<Double> _Geometry : _pointList){

                    SurfaceObject BSurface = new SurfaceObject();
                    _SurfaceType = surfaceGeom.DetectSurfaceType(_Geometry);
                    BSurface.setId(surfaceGeom.GetSurfaceID().get(counter));
                    BSurface.setType(_SurfaceType);
                    BSurface.setGeometry(_Geometry);
                    _SurfaceList.add(BSurface);
                    counter ++;
                }

            }

        }

        
        // BoundarySurfaces
        if (_building.isSetBoundedBySurface()) {

            long ParentCounter = 1;
            for (BoundarySurfaceProperty boundarySurfaceProperty : _building.getBoundedBySurface()) {
            	                
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
                                _SurfaceType = TypeAttributeValueEnum.fromCityGMLClass(boundarySurface.getCityGMLClass()).toString();
                                List<List<Double>> _pointList  = surfaceGeom.GetSurfaceGeometry(buildingGmlId,multiSurfaceProperty.getMultiSurface(), false);

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
                                            List<List<Double>> _pointList = surfaceGeom.GetSurfaceGeometry(buildingGmlId,multiSurfaceProperty.getMultiSurface(), false);
                                            _SurfaceType = TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.BUILDING_WALL_SURFACE).toString();

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


        // BuildingInstallation
        if (_building.isSetOuterBuildingInstallation()) {
            for (BuildingInstallationProperty buildingInstProperty : _building.getOuterBuildingInstallation()) {
                BuildingInstallation buildingInst = buildingInstProperty.getBuildingInstallation();

                if (buildingInst != null) {


                    // Geometry
                    for (int lod = 2; lod < 5; lod++) {
                        GeometryProperty<? extends AbstractGeometry> geometryProperty = null;


                        switch (lod) {
                            case 2:
                                geometryProperty = buildingInst.getLod2Geometry();
                                break;
                            case 3:
                                geometryProperty = buildingInst.getLod3Geometry();
                                break;
                            case 4:
                                geometryProperty = buildingInst.getLod4Geometry();
                                break;
                        }

                        if (geometryProperty != null) {
                            if (geometryProperty.isSetGeometry()) {

                                surfaceGeom.ClearPointList();
                                surfaceGeom.ClearIdList();
                                List<List<Double>> _pointList = surfaceGeom.GetSurfaceGeometry(buildingGmlId,geometryProperty.getGeometry(), false);
                                _SurfaceType = TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.BUILDING_WALL_SURFACE).toString();

                                int counter = 0;
                                for(List<Double> _Geometry : _pointList){

                                    SurfaceObject BSurface = new SurfaceObject();
                                    _SurfaceType = surfaceGeom.DetectSurfaceType(_Geometry);
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
                    buildingInstProperty.unsetBuildingInstallation();
                } else {
                    // xlink
                    String href = buildingInstProperty.getHref();

                    if (href != null && href.length() != 0) {
                        LOG.error("XLink reference '" + href + "' to BuildingInstallation feature is not supported.");
                    }
                }

            }
        }


        // IntBuildingInstallation
        if (_building.isSetInteriorBuildingInstallation()) {
            for (IntBuildingInstallationProperty intBuildingInstProperty : _building.getInteriorBuildingInstallation()) {
                IntBuildingInstallation intBuildingInst = intBuildingInstProperty.getIntBuildingInstallation();

                if (intBuildingInst != null) {


                    if (intBuildingInst.isSetLod4Geometry()) {
                        GeometryProperty<? extends AbstractGeometry> geometryProperty = intBuildingInst.getLod4Geometry();

                        if (geometryProperty.isSetGeometry()) {

                            surfaceGeom.ClearPointList();
                            surfaceGeom.ClearIdList();
                            List<List<Double>> _pointList = surfaceGeom.GetSurfaceGeometry(buildingGmlId,geometryProperty.getGeometry(), false);
                            _SurfaceType = TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.BUILDING_WALL_SURFACE).toString();

                            int counter = 0;
                            for(List<Double> _Geometry : _pointList){

                                SurfaceObject BSurface = new SurfaceObject();
                                _SurfaceType = surfaceGeom.DetectSurfaceType(_Geometry);
                                BSurface.setId(surfaceGeom.GetSurfaceID().get(counter));
                                BSurface.setType(_SurfaceType);
                                BSurface.setGeometry(_Geometry);
                                _SurfaceList.add(BSurface);
                                counter ++;
                            }
                        }
                    }


                    // free memory of nested feature
                    intBuildingInstProperty.unsetIntBuildingInstallation();
                }
                else {
                    // xlink
                    String href = intBuildingInstProperty.getHref();

                    if (href != null && href.length() != 0) {
                        LOG.error("XLink reference '" + href + "' to IntBuildingInstallation feature is not supported.");
                    }
                }
            }
        }



        // Room
        if (_building.isSetInteriorRoom()) {
            for (InteriorRoomProperty roomProperty : _building.getInteriorRoom()) {
                Room room = roomProperty.getRoom();

                if (room != null) {

                    if (room.isSetLod4MultiSurface() && room.isSetLod4Solid()) {

                        StringBuilder msg = new StringBuilder();
                        msg.append("Found both elements lod4Solid and lod4MultiSurface. Only lod4Solid will be imported.");
                        Logger.getInstance().warn(msg.toString());

                        room.unsetLod4MultiSurface();
                    }



                    if (room.isSetLod4Solid()) {

                        SolidProperty solidProperty = room.getLod4Solid();

                        if (solidProperty.isSetSolid()) {

                            surfaceGeom.ClearPointList();
                            surfaceGeom.ClearIdList();
                            List<List<Double>> _pointList = surfaceGeom.GetSurfaceGeometry(buildingGmlId,solidProperty.getSolid(), false);
                            _SurfaceType = TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.BUILDING_WALL_SURFACE).toString();

                            int counter = 0;
                            for(List<Double> _Geometry : _pointList){

                                SurfaceObject BSurface = new SurfaceObject();
                                _SurfaceType = surfaceGeom.DetectSurfaceType(_Geometry);
                                BSurface.setId(surfaceGeom.GetSurfaceID().get(counter));
                                BSurface.setType(_SurfaceType);
                                BSurface.setGeometry(_Geometry);
                                _SurfaceList.add(BSurface);
                                counter ++;
                            }

                        }

                    } else if (room.isSetLod4MultiSurface()) {

                        MultiSurfaceProperty multiSurfacePropery = room.getLod4MultiSurface();

                        if (multiSurfacePropery.isSetMultiSurface()) {

                            surfaceGeom.ClearPointList();
                            surfaceGeom.ClearIdList();
                            List<List<Double>> _pointList = surfaceGeom.GetSurfaceGeometry(buildingGmlId,multiSurfacePropery.getMultiSurface(), false);
                            _SurfaceType = TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.BUILDING_WALL_SURFACE).toString();

                            int counter = 0;
                            for(List<Double> _Geometry : _pointList){

                                SurfaceObject BSurface = new SurfaceObject();
                                _SurfaceType = surfaceGeom.DetectSurfaceType(_Geometry);
                                BSurface.setId(surfaceGeom.GetSurfaceID().get(counter));
                                BSurface.setType(_SurfaceType);
                                BSurface.setGeometry(_Geometry);
                                _SurfaceList.add(BSurface);
                                counter ++;
                            }
                        }
                    }


                    // Room - BoundarySurfaces
                    if (room.isSetBoundedBySurface()) {

                        long ParentCounter = 1;
                        for (BoundarySurfaceProperty boundarySurfaceProperty : room.getBoundedBySurface()) {
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
                                            _SurfaceType = TypeAttributeValueEnum.fromCityGMLClass(boundarySurface.getCityGMLClass()).toString();
                                            List<List<Double>> _pointList  = surfaceGeom.GetSurfaceGeometry(buildingGmlId,multiSurfaceProperty.getMultiSurface(), false);

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

                                }

                                // Room - BoundrySurface - Openings
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
                                                        List<List<Double>> _pointList = surfaceGeom.GetSurfaceGeometry(buildingGmlId,multiSurfaceProperty.getMultiSurface(), false);
                                                        _SurfaceType = TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.BUILDING_WALL_SURFACE).toString();

                                                        int counter = 0;
                                                        for(List<Double> _Geometry : _pointList){



                                                            SurfaceObject BSurface = new SurfaceObject();
                                                            BSurface.setPId(null);
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


                        // Room - IntBuildingInstallation
                        if (room.isSetRoomInstallation()) {
                            for (IntBuildingInstallationProperty intBuildingInstProperty : room.getRoomInstallation()) {
                                IntBuildingInstallation intBuildingInst = intBuildingInstProperty.getObject();

                                if (intBuildingInst != null) {

                                    if (intBuildingInst.isSetLod4Geometry()) {
                                        GeometryProperty<? extends AbstractGeometry> geometryProperty = intBuildingInst.getLod4Geometry();

                                        if (geometryProperty.isSetGeometry()) {

                                            surfaceGeom.ClearPointList();
                                            surfaceGeom.ClearIdList();
                                            List<List<Double>> _pointList = surfaceGeom.GetSurfaceGeometry(buildingGmlId,geometryProperty.getGeometry(), false);
                                            _SurfaceType = TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.BUILDING_WALL_SURFACE).toString();

                                            int counter = 0;
                                            for(List<Double> _Geometry : _pointList){

                                                SurfaceObject BSurface = new SurfaceObject();
                                                _SurfaceType = surfaceGeom.DetectSurfaceType(_Geometry);
                                                BSurface.setId(surfaceGeom.GetSurfaceID().get(counter));
                                                BSurface.setType(_SurfaceType);
                                                BSurface.setGeometry(_Geometry);
                                                _SurfaceList.add(BSurface);
                                                counter ++;
                                            }
                                        }
                                    }


                                    // free memory of nested feature
                                    intBuildingInstProperty.unsetIntBuildingInstallation();
                                }
                            }
                        }



                        // Room - BuildingFurniture
                        if (room.isSetInteriorFurniture()) {
                            for (InteriorFurnitureProperty intFurnitureProperty : room.getInteriorFurniture()) {
                                BuildingFurniture furniture = intFurnitureProperty.getObject();

                                if (furniture != null) {


                                    if (furniture.isSetLod4Geometry()) {
                                        GeometryProperty<? extends AbstractGeometry> geometryProperty = furniture.getLod4Geometry();

                                        if (geometryProperty.isSetGeometry()) {

                                            surfaceGeom.ClearPointList();
                                            surfaceGeom.ClearIdList();
                                            List<List<Double>> _pointList = surfaceGeom.GetSurfaceGeometry(buildingGmlId,geometryProperty.getGeometry(), false);
                                            _SurfaceType = TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.BUILDING_WALL_SURFACE).toString();

                                            int counter = 0;
                                            for(List<Double> _Geometry : _pointList){

                                                SurfaceObject BSurface = new SurfaceObject();
                                                _SurfaceType = surfaceGeom.DetectSurfaceType(_Geometry);
                                                BSurface.setId(surfaceGeom.GetSurfaceID().get(counter));
                                                BSurface.setType(_SurfaceType);
                                                BSurface.setGeometry(_Geometry);
                                                _SurfaceList.add(BSurface);
                                                counter ++;
                                            }
                                        }
                                    }


                                    // free memory of nested feature
                                    intFurnitureProperty.unsetBuildingFurniture();
                                }
                            }
                        }


                        // free memory of nested feature
                        roomProperty.unsetRoom();
                    }
                }
                else {
                    // xlink
                    String href = roomProperty.getHref();

                    if (href != null && href.length() != 0) {
                        LOG.error("XLink reference '" + href + "' to Room feature is not supported.");
                    }
                }
            }

        }



        // BuildingPart
        if (_building.isSetConsistsOfBuildingPart()) {
            for (BuildingPartProperty buildingPartProperty : _building.getConsistsOfBuildingPart()) {
                BuildingPart buildingPart = buildingPartProperty.getBuildingPart();

                if (buildingPart != null) {

                		_SurfaceList.addAll(GetBuildingGeometries(buildingPart));


                    // free memory of nested feature
                    buildingPartProperty.unsetBuildingPart();
                }
                else {
                    // xlink
                    String href = buildingPartProperty.getHref();

                    if (href != null && href.length() != 0) {
                        LOG.error("XLink reference '" + href + "' to BuildingPart feature is not supported.");
                    }
                }
            }
            
        }


        return _SurfaceList;

    }
    
    
    public static HashMap<String,Object> getBuildingAddressProperties(Address address){
		
		HashMap<String, Object> addressMap = new HashMap<String,Object>();
		if (!address.isSetXalAddress() || !address.getXalAddress().isSetAddressDetails())
			return addressMap;

		XalAddressProperty xalAddressProperty = address.getXalAddress();
		AddressDetails addressDetails = xalAddressProperty.getAddressDetails();
		
		String streetAttr, houseNoAttr, poBoxAttr, zipCodeAttr, cityAttr, countryAttr, xalSource;
		streetAttr = houseNoAttr = poBoxAttr = zipCodeAttr = cityAttr = countryAttr = xalSource = null;
		GeometryObject multiPoint = null;		

		// try and interpret <country> child element
		if (addressDetails.isSetCountry()) {
			Country country = addressDetails.getCountry();

			// country name
			if (country.isSetCountryName()) {
				List<String> countryName = new ArrayList<String>();				
				for (CountryName name : country.getCountryName())
					countryName.add(name.getContent());

				countryAttr = Util.collection2string(countryName, ",");
				addressMap.put("COUNTRY", countryAttr);
			} 

			// locality
			if (country.isSetLocality()) {
				Locality locality = country.getLocality();

				// check whether we deal with a city or a town
				if (locality.isSetType() && 
						(locality.getType().toUpperCase().equals("CITY") ||
								locality.getType().toUpperCase().equals("TOWN"))) {

					// city name
					if (locality.isSetLocalityName()) {
						List<String> localityName = new ArrayList<String>();						
						for (LocalityName name : locality.getLocalityName())
							localityName.add(name.getContent());

						cityAttr = Util.collection2string(localityName, ",");
						addressMap.put("CITY", cityAttr);
					} 

					// thoroughfare - just streets are supported
					if (locality.isSetThoroughfare()) {
						Thoroughfare thoroughfare = locality.getThoroughfare();

						// check whether we deal with a street
						if (thoroughfare.isSetType() && 
								(thoroughfare.getType().toUpperCase().equals("STREET") ||
										thoroughfare.getType().toUpperCase().equals("ROAD"))) {

							// street name
							if (thoroughfare.isSetThoroughfareName()) {
								List<String> fareName = new ArrayList<String>();								
								for (ThoroughfareName name : thoroughfare.getThoroughfareName())
									fareName.add(name.getContent());

								streetAttr = Util.collection2string(fareName, ",");
								addressMap.put("STREET", streetAttr);
							}

							// house number - we do not support number ranges so far...						
							if (thoroughfare.isSetThoroughfareNumberOrThoroughfareNumberRange()) {
								List<String> houseNumber = new ArrayList<String>();								
								for (ThoroughfareNumberOrRange number : thoroughfare.getThoroughfareNumberOrThoroughfareNumberRange()) {
									if (number.isSetThoroughfareNumber())
										houseNumber.add(number.getThoroughfareNumber().getContent());
								}

								houseNoAttr = Util.collection2string(houseNumber, ",");
								addressMap.put("HOUSE_NUMBER", streetAttr);
							}
						}
					}

					// dependent locality
					if (streetAttr == null && houseNoAttr == null && locality.isSetDependentLocality()) {
						DependentLocality dependentLocality = locality.getDependentLocality();

						if (dependentLocality.isSetType() && 
								dependentLocality.getType().toUpperCase().equals("DISTRICT")) {

							if (dependentLocality.isSetThoroughfare()) {
								Thoroughfare thoroughfare = dependentLocality.getThoroughfare();

								// street name
								if (streetAttr == null && thoroughfare.isSetThoroughfareName()) {
									List<String> fareName = new ArrayList<String>();								
									for (ThoroughfareName name : thoroughfare.getThoroughfareName())
										fareName.add(name.getContent());

									streetAttr = Util.collection2string(fareName, ",");
									addressMap.put("STREET", streetAttr);
								}

								// house number - we do not support number ranges so far...						
								if (houseNoAttr == null && thoroughfare.isSetThoroughfareNumberOrThoroughfareNumberRange()) {
									List<String> houseNumber = new ArrayList<String>();								
									for (ThoroughfareNumberOrRange number : thoroughfare.getThoroughfareNumberOrThoroughfareNumberRange()) {
										if (number.isSetThoroughfareNumber())
											houseNumber.add(number.getThoroughfareNumber().getContent());
									}

									houseNoAttr = Util.collection2string(houseNumber, ",");
									addressMap.put("HOUSE_NUMBER", houseNoAttr);
								}
							}
						}
					}

					// postal code
					if (locality.isSetPostalCode()) {
						PostalCode postalCode = locality.getPostalCode();

						// get postal code number
						if (postalCode.isSetPostalCodeNumber()) {
							List<String> zipCode = new ArrayList<String>();							
							for (PostalCodeNumber number : postalCode.getPostalCodeNumber())
								zipCode.add(number.getContent());

							zipCodeAttr = Util.collection2string(zipCode, ",");
							addressMap.put("ZIP_CODE", zipCodeAttr);
						}
					}

					// post box
					if (locality.isSetPostBox()) {
						PostBox postBox = locality.getPostBox();

						// get post box nummber
						if (postBox.isSetPostBoxNumber()){
							poBoxAttr = postBox.getPostBoxNumber().getContent();
							addressMap.put("PO_BOX", poBoxAttr);
						}
							
					}
				}
			}
						
		}
		return addressMap;
	}		


    
    public static HashMap<String,Object> getBuildingProperties(AbstractBuilding building){
    	
    	 HashMap<String, Object> buildingMap = new HashMap<String,Object>();

		 //Building GmlID
		 if (building.isSetId()) {
			 buildingMap.put("GMLID",building.getId());
		 }   	
		 
		 //Building name and codespace
		 if (building.isSetName()) {
			 buildingMap.put("NAME",building.getName());
			 if(building.getName().get(0).isSetCodeSpace())
				 buildingMap.put("NAME_CODESPACE", building.getName().get(0).getCodeSpace());
		 }    	
		 
		 // bldg:class
    	if (building.isSetClazz() && building.getClazz().isSetValue()) {
    		buildingMap.put("CLASS",building.getClazz().getValue());
    	}
    	
    	//Building Description
    	if(building.isSetDescription())
    	{
    		buildingMap.put("DESCRIPTION",building.getDescription());
    	}
    	
    	// bldg:function
    	if (building.isSetFunction()) {
    		String[] function = Util.codeList2string(building.getFunction());
    		 buildingMap.put("FUNCTION",function[0]);    		 
    	} 
    	
    	// bldg:usage
    	if (building.isSetUsage()) {
    		String[] usage = Util.codeList2string(building.getUsage());
   		 	buildingMap.put("USAGE",usage[0]);
    	}
        // bldg:yearOfConstruction
        if (building.isSetYearOfConstruction()) {
            buildingMap.put("YEAR_OF_CONSTRUCTION",new Date(building.getYearOfConstruction().getTime().getTime()).toString());
        }

        // bldg:yearOfDemolition
        if (building.isSetYearOfDemolition()) {
            buildingMap.put("YEAR_OF_DEMOLITION",new Date(building.getYearOfDemolition().getTime().getTime()).toString());
        }

    	
    	// bldg:roofType
    	if (building.isSetRoofType() && building.getRoofType().isSetValue()) {
    		buildingMap.put("ROOF_TYPE",building.getRoofType().getValue());
    	}
    	
    	// bldg:measuredHeight
    	if (building.isSetMeasuredHeight() && building.getMeasuredHeight().isSetValue()) {
    		 buildingMap.put("MEASURED_HEIGHT",building.getMeasuredHeight().getValue());
    	}

    	
    	// bldg:storeysAboveGround
    	if (building.isSetStoreysAboveGround()) {
    		buildingMap.put("STOREYS_ABOVE_GROUND", building.getStoreysAboveGround());
    	}
    	
    	// bldg:storeysBelowGround
    	if (building.isSetStoreysBelowGround()) {
    		buildingMap.put("STOREYS_BELOW_GROUND", building.getStoreysBelowGround());
    	}
    	
    	
    	// bldg:storeyHeightsAboveGround
    	String heights = null;
    	if (building.isSetStoreyHeightsAboveGround()) {
    		MeasureOrNullList measureOrNullList = building.getStoreyHeightsAboveGround();
    		if (measureOrNullList.isSetDoubleOrNull()) {
    			List<String> values = new ArrayList<String>();
    			for (DoubleOrNull doubleOrNull : measureOrNullList.getDoubleOrNull()) {
    				if (doubleOrNull.isSetDouble())
    					values.add(String.valueOf(doubleOrNull.getDouble()));
    				else
    					doubleOrNull.getNull().getValue();
    			}
    			heights = Util.collection2string(values, " ");
    		}
    	}    	
    	if (heights != null) {
    		buildingMap.put("STOREY_HEIGHTS_ABOVE_GROUND", heights);
    	}
    	
    	
    	// bldg:storeyHeightsBelowGround
    	heights = null;
    	if (building.isSetStoreyHeightsBelowGround()) {
    		MeasureOrNullList measureOrNullList = building.getStoreyHeightsBelowGround();
    		if (measureOrNullList.isSetDoubleOrNull()) {
    			List<String> values = new ArrayList<String>();
    			for (DoubleOrNull doubleOrNull : measureOrNullList.getDoubleOrNull()) {
    				if (doubleOrNull.isSetDouble())
    					values.add(String.valueOf(doubleOrNull.getDouble()));
    				else
    					doubleOrNull.getNull().getValue();
    			}
    			heights = Util.collection2string(values, " ");
    		}
    	}    	
    	if (heights != null) {
    		buildingMap.put("STOREY_HEIGHTS_BELOW_GROUND", heights);
    	}   

    	return buildingMap;
    }

}