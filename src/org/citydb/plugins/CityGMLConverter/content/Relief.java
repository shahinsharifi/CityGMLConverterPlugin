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
import org.citygml4j.model.citygml.CityGMLClass;
import org.citygml4j.model.citygml.core.ImplicitGeometry;
import org.citygml4j.model.citygml.core.ImplicitRepresentationProperty;
import org.citygml4j.model.citygml.relief.*;
import org.citygml4j.model.gml.GMLClass;
import org.citygml4j.model.gml.geometry.AbstractGeometry;
import org.citygml4j.model.gml.geometry.GeometryProperty;
import org.citygml4j.model.gml.geometry.aggregates.MultiCurveProperty;
import org.citygml4j.model.gml.geometry.primitives.Tin;
import org.citygml4j.model.gml.geometry.primitives.TriangulatedSurface;

import javax.vecmath.Point3d;
import javax.xml.bind.JAXBException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class Relief extends KmlGenericObject{

    public static final String STYLE_BASIS_NAME = "Relief";
    private Matrix transformation;
    private SqliteImporterManager sqlliteImporterManager;
    private List<SurfaceObject> _ParentSurfaceList = new ArrayList<SurfaceObject>();
    private final Logger LOG = Logger.getInstance();
    private double refPointX;
    private double refPointY;
    private double refPointZ;


    public Relief(KmlExporterManager kmlExporterManager,
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
        return config.getReliefDisplayForms();
    }


    public ColladaOptions getColladaOptions() {
        return config.getReliefColladaOptions();
    }


    public Balloon getBalloonSettings() {
        return config.getReliefBalloon();
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
            Logger.getInstance().error("Error while getting relief parts for object " + work.getGmlId() + ": " + Ex.getMessage());
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

            ReliefFeature _relief = (ReliefFeature)work.getCityGmlClass();
            SurfaceAppearance _SurfaceAppear = new SurfaceAppearance();

            //this function reads all geometries and returns a list of surfaces.
            List<SurfaceObject> _surfaceList = GetGeometries(_relief);

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


    public List<SurfaceObject> GetGeometries(ReliefFeature _relief) throws Exception
    {
        List<SurfaceObject> _SurfaceList = new ArrayList<SurfaceObject>();
        SurfaceGeometry surfaceGeom = new SurfaceGeometry(config , sqlliteImporterManager);
        String _SurfaceType = "undefined";
        String RootGmlId = _relief.getId();
        OtherGeometry otherGeom = new OtherGeometry(config , sqlliteImporterManager , 3068);

        /// Geometry
        // relief component
        if (_relief.isSetReliefComponent()) {
            for (ReliefComponentProperty property : _relief.getReliefComponent()) {
                AbstractReliefComponent reliefComponent = property.getReliefComponent();
                List<List<Double>> _pointList = null;
                if (reliefComponent != null) {

                    if (reliefComponent.getCityGMLClass() != CityGMLClass.RASTER_RELIEF) {

                        if (reliefComponent.getCityGMLClass() == CityGMLClass.TIN_RELIEF) {

                            TINRelief tinRelief = (TINRelief)reliefComponent;


                            GeometryObject stopLines, breakLines, controlPoints;
                            stopLines = breakLines = controlPoints = null;

                            // gml:TriangulatedSurface
                            if (tinRelief.isSetTin()) {
                                TinProperty tinProperty = tinRelief.getTin();
                                TriangulatedSurface triangulatedSurface = tinProperty.getObject();

                                if (triangulatedSurface != null) {

                                    surfaceGeom.ClearPointList();
                                    _pointList = surfaceGeom.getSurfaceGeometry(RootGmlId, triangulatedSurface, false);


                                    // gml:Tin
                                    if (triangulatedSurface.getGMLClass() == GMLClass.TIN) {
                                        Tin tin = (Tin)triangulatedSurface;

                                        // stopLines
                                        if (tin.isSetStopLines()) {
                                            stopLines = otherGeom.getMultiCurve(tin.getStopLines());
                                            _pointList.addAll(otherGeom.ConvertGeomObjectToPointList(stopLines));
                                        }
                                        // breakLines
                                        if (tin.isSetBreakLines()) {
                                            breakLines = otherGeom.getMultiCurve(tin.getBreakLines());
                                            _pointList.addAll(otherGeom.ConvertGeomObjectToPointList(breakLines));
                                        }
                                        // controlPoints
                                        if (tin.isSetControlPoint()) {
                                            controlPoints = otherGeom.getMultiPoint(tin.getControlPoint());
                                            _pointList.addAll(otherGeom.ConvertGeomObjectToPointList(controlPoints));
                                        }
                                    }

                                    tinProperty.unsetTriangulatedSurface();

                                } else {
                                    // xlink
                                    String href = tinProperty.getHref();

                                    if (href != null && href.length() != 0)
                                        LOG.error("XLink reference '" + href + "' to " + GMLClass.TRIANGULATED_SURFACE + " element is not supported");
                                }
                            }


                        }else if (reliefComponent.getCityGMLClass() == CityGMLClass.MASSPOINT_RELIEF) {

                            MassPointRelief massPointRelief = (MassPointRelief)reliefComponent;

                            // reliefPoints
                            GeometryObject reliefPoints = null;
                            if (massPointRelief.isSetReliefPoints()) {
                                reliefPoints = otherGeom.getMultiPoint(massPointRelief.getReliefPoints());
                                _pointList.addAll(otherGeom.ConvertGeomObjectToPointList(reliefPoints));

                                massPointRelief.unsetReliefPoints();
                            }

                        }else if (reliefComponent.getCityGMLClass() == CityGMLClass.BREAKLINE_RELIEF) {
                            BreaklineRelief breakLineRelief = (BreaklineRelief)reliefComponent;


                            GeometryObject ridgeOrValleyLines, breakLines;
                            ridgeOrValleyLines = breakLines = null;

                            if (breakLineRelief.isSetRidgeOrValleyLines()) {
                                ridgeOrValleyLines = otherGeom.getMultiCurve(breakLineRelief.getRidgeOrValleyLines());
                                _pointList.addAll(otherGeom.ConvertGeomObjectToPointList(ridgeOrValleyLines));

                                breakLineRelief.unsetRidgeOrValleyLines();
                            }

                            if (breakLineRelief.isSetBreaklines()) {
                                breakLines = otherGeom.getMultiCurve(breakLineRelief.getBreaklines());
                                _pointList.addAll(otherGeom.ConvertGeomObjectToPointList(ridgeOrValleyLines));

                                breakLineRelief.unsetBreaklines();
                            }




                        }

                        //iterating the geometry lists
                        int counter = 0;
                        for(List<Double> _geometry : _pointList){

                            SurfaceObject BSurface = new SurfaceObject();
                            _SurfaceType = surfaceGeom.DetectSurfaceType(_geometry);
                            BSurface.setId(surfaceGeom.GetSurfaceID().get(counter));
                            BSurface.setType(_SurfaceType);
                            BSurface.setGeometry(_geometry);
                            _SurfaceList.add(BSurface);
                            counter++;
                        }

                    }else {

                        LOG.error("Raster relief components are not supported.");
                    }
                    // free memory of nested feature
                    property.unsetReliefComponent();

                } else{
                    // xlink
                    String href = property.getHref();

                    if (href != null && href.length() != 0) {
                        DBXlinkBasic xlink = new DBXlinkBasic(
                                reliefComponent.getId(),
                                TableEnum.RELIEF_FEATURE,
                                href,
                                TableEnum.RELIEF_COMPONENT
                        );

                   //     xlink.setAttrName("LOD" + lod + "_GEOMETRY_ID");
                        sqlliteImporterManager.propagateXlink(xlink);
                    }
                }
            }
        }

        return _SurfaceList;
    }

    public static HashMap<String,Object> getObjectProperties(ReliefFeature reliefFeature){

        HashMap<String, Object> objectgMap = new HashMap<String , Object>();


        return objectgMap;
    }

}