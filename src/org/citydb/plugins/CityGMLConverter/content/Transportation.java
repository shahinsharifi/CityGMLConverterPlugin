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
import org.citygml4j.model.citygml.CityGMLClass;
import org.citygml4j.model.citygml.transportation.*;
import org.citygml4j.model.gml.geometry.aggregates.MultiSurfaceProperty;
import org.citygml4j.model.gml.geometry.complexes.GeometricComplex;
import org.citygml4j.model.gml.geometry.complexes.GeometricComplexProperty;
import org.citygml4j.model.gml.geometry.primitives.AbstractGeometricPrimitive;
import org.citygml4j.model.gml.geometry.primitives.GeometricPrimitiveProperty;
import org.opengis.geometry.aggregate.MultiCurve;

import javax.vecmath.Point3d;
import javax.xml.bind.JAXBException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class Transportation extends KmlGenericObject{

    public static final String STYLE_BASIS_NAME = "Transportation";
    private Matrix transformation;
    private SqliteImporterManager sqlliteImporterManager;
    private List<SurfaceObject> _ParentSurfaceList = new ArrayList<SurfaceObject>();
    private final Logger LOG = Logger.getInstance();
    private double refPointX;
    private double refPointY;
    private double refPointZ;


    public Transportation(KmlExporterManager kmlExporterManager,
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

            TransportationComplex transComplex = (TransportationComplex)work.getCityGmlClass();

            SurfaceAppearance _SurfaceAppear = new SurfaceAppearance();

            //this function reads all geometries and returns a list of surfaces.
            List<SurfaceObject> _surfaceList = GetGeometries(transComplex);

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

    
    public List<SurfaceObject> GetGeometries(TransportationComplex transComplex) throws Exception
    {
        List<SurfaceObject> _SurfaceList = new ArrayList<SurfaceObject>();
        SurfaceGeometry surfaceGeom = new SurfaceGeometry(config , sqlliteImporterManager);
        String _SurfaceType = "undefined";
        String transComplexGmlId = transComplex.getId();
        OtherGeometry others = new OtherGeometry(config , sqlliteImporterManager,3068);



        // Geometry
        // lod0Network

        if (transComplex.isSetLod0Network()) {
            GeometricComplex aggregate = new GeometricComplex();

            for (GeometricComplexProperty complexProperty : transComplex.getLod0Network()) {
                // for lod0Network we just consider appropriate curve geometries

                if (complexProperty.isSetCompositeCurve()) {
                    GeometricPrimitiveProperty primitiveProperty = new GeometricPrimitiveProperty(complexProperty.getCompositeCurve());
                    aggregate.addElement(primitiveProperty);
                } else if (complexProperty.getGeometricComplex() != null) {
                    GeometricComplex complex = complexProperty.getGeometricComplex();

                    if (complex.isSetElement()) {
                        for (GeometricPrimitiveProperty primitiveProperty : complex.getElement()) {
                            if (primitiveProperty.isSetGeometricPrimitive()) {
                                AbstractGeometricPrimitive primitive = primitiveProperty.getGeometricPrimitive();

                                switch (primitive.getGMLClass()) {
                                    case LINE_STRING:
                                    case COMPOSITE_CURVE:
                                    case ORIENTABLE_CURVE:
                                    case CURVE:
                                        aggregate.addElement(primitiveProperty);
                                        break;
                                    default:
                                        // geometry type not supported by lod0Network
                                }
                            } else {
                                // xlinks are not supported
                            }
                        }
                    }
                }

                // we do not support XLinks or further geometry types so far
            }

            // free memory of geometry object
            transComplex.unsetLod0Network();

            if (aggregate.isSetElement() && !aggregate.getElement().isEmpty()) {
                List<List<Double>> _pointList = others.getPointList(aggregate);

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


        // lodXMultiSurface
        for (int lod = 1; lod < 5; lod++) {

            //if (lodGeometry[lod - 1])
            //continue;

            MultiSurfaceProperty multiSurfaceProperty = null;


            switch (lod) {
                case 1:
                    multiSurfaceProperty = transComplex.getLod1MultiSurface();
                    break;
                case 2:
                    multiSurfaceProperty = transComplex.getLod2MultiSurface();
                    break;
                case 3:
                    multiSurfaceProperty = transComplex.getLod3MultiSurface();
                    break;
                case 4:
                    multiSurfaceProperty = transComplex.getLod4MultiSurface();
                    break;
            }

            if (multiSurfaceProperty != null) {

                if (multiSurfaceProperty.isSetMultiSurface()) {

                    surfaceGeom.ClearPointList();
                    List<List<Double>> _pointList = surfaceGeom.getSurfaceGeometry(transComplexGmlId, multiSurfaceProperty.getMultiSurface(), false);

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
                                transComplex.getId(),
                                TableEnum.TRANSPORTATION_COMPLEX,
                                href,
                                TableEnum.SURFACE_GEOMETRY
                        );

                        xlink.setAttrName("LOD" + lod + "_GEOMETRY_ID");
                        sqlliteImporterManager.propagateXlink(xlink);
                    }
                }
            }

        }


        // AuxiliaryTrafficArea
        if (transComplex.isSetAuxiliaryTrafficArea()) {
            for (AuxiliaryTrafficAreaProperty auxTrafficAreaProperty : transComplex.getAuxiliaryTrafficArea()) {
                AuxiliaryTrafficArea auxArea = auxTrafficAreaProperty.getAuxiliaryTrafficArea();

                if (auxArea != null) {


                    for (int lod = 0; lod < 3; lod++) {
                        MultiSurfaceProperty multiSurfaceProperty = null;
                        long multiGeometryId = 0;

                        switch (lod) {
                            case 0:
                                multiSurfaceProperty = auxArea.getLod2MultiSurface();
                                break;
                            case 1:
                                multiSurfaceProperty = auxArea.getLod3MultiSurface();
                                break;
                            case 2:
                                multiSurfaceProperty = auxArea.getLod4MultiSurface();
                                break;
                        }

                        if (multiSurfaceProperty != null) {

                            if (multiSurfaceProperty.isSetMultiSurface()) {

                                surfaceGeom.ClearPointList();
                                List<List<Double>> _pointList = surfaceGeom.getSurfaceGeometry(transComplexGmlId, multiSurfaceProperty.getMultiSurface(), false);

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
                                            transComplex.getId(),
                                            TableEnum.TRANSPORTATION_COMPLEX,
                                            href,
                                            TableEnum.SURFACE_GEOMETRY
                                    );

                                    xlink.setAttrName("LOD" + lod + "_GEOMETRY_ID");
                                    sqlliteImporterManager.propagateXlink(xlink);
                                }
                            }
                        }

                    }


                    // free memory of nested feature
                    auxTrafficAreaProperty.unsetAuxiliaryTrafficArea();
                }
            }
        }


        // TrafficArea
        if (transComplex.isSetTrafficArea()) {
            for (TrafficAreaProperty trafficAreaProperty : transComplex.getTrafficArea()) {
                TrafficArea area = trafficAreaProperty.getTrafficArea();

                if (area != null) {
                    for (int lod = 0; lod < 3; lod++) {
                        MultiSurfaceProperty multiSurfaceProperty = null;
                        long multiGeometryId = 0;

                        switch (lod) {
                            case 0:
                                multiSurfaceProperty = area.getLod2MultiSurface();
                                break;
                            case 1:
                                multiSurfaceProperty = area.getLod3MultiSurface();
                                break;
                            case 2:
                                multiSurfaceProperty = area.getLod4MultiSurface();
                                break;
                        }

                        if (multiSurfaceProperty != null) {

                            if (multiSurfaceProperty.isSetMultiSurface()) {

                                surfaceGeom.ClearPointList();
                                List<List<Double>> _pointList = surfaceGeom.getSurfaceGeometry(transComplexGmlId, multiSurfaceProperty.getMultiSurface(), false);

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
                                            transComplex.getId(),
                                            TableEnum.TRANSPORTATION_COMPLEX,
                                            href,
                                            TableEnum.SURFACE_GEOMETRY
                                    );

                                    xlink.setAttrName("LOD" + lod + "_GEOMETRY_ID");
                                    sqlliteImporterManager.propagateXlink(xlink);
                                }
                            }
                        }

                    }

                    // free memory of nested feature
                    trafficAreaProperty.unsetTrafficArea();

                }
            }
        }

        return _SurfaceList;
    }


    public static HashMap<String,Object> getObjectProperties(org.citygml4j.model.citygml.transportation.TransportationComplex transportation){

        HashMap<String, Object> objectgMap = new HashMap<String,Object>();

        //GmlID
        if (transportation.isSetId()) {
            objectgMap.put("GMLID",transportation.getId());
        }

        //name and codespace
        if (transportation.isSetName()) {
            objectgMap.put("NAME",transportation.getName());
            if(transportation.getName().get(0).isSetCodeSpace())
                objectgMap.put("NAME_CODESPACE", transportation.getName().get(0).getCodeSpace());
        }

        // class
        if (transportation.isSetClazz() && transportation.getClazz().isSetValue()) {
            objectgMap.put("CLASS",transportation.getClazz().getValue());
        }

        //Description
        if(transportation.isSetDescription())
        {
            objectgMap.put("DESCRIPTION",transportation.getDescription());
        }

        // function
        if (transportation.isSetFunction()) {
            String[] function = Util.codeList2string(transportation.getFunction());
            objectgMap.put("FUNCTION",function[0]);
        }

        // usage
        if (transportation.isSetUsage()) {
            String[] usage = Util.codeList2string(transportation.getUsage());
            objectgMap.put("USAGE",usage[0]);
        }


        return objectgMap;
    }

}