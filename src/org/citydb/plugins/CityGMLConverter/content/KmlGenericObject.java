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

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
// import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.media.j3d.GeometryArray;
import javax.media.jai.JAI;
import javax.media.jai.NullOpImage;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.vecmath.Point3d;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.opengis.kml._2.AltitudeModeEnumType;
import net.opengis.kml._2.BoundaryType;
import net.opengis.kml._2.LinearRingType;
import net.opengis.kml._2.LinkType;
import net.opengis.kml._2.LocationType;
import net.opengis.kml._2.ModelType;
import net.opengis.kml._2.MultiGeometryType;
import net.opengis.kml._2.OrientationType;
import net.opengis.kml._2.PlacemarkType;
import net.opengis.kml._2.PolygonType;

import org.citydb.api.database.DatabaseSrs;
import org.citydb.api.event.EventDispatcher;
import org.citydb.api.geometry.GeometryObject;
import org.citydb.api.log.LogLevel;
import org.citydb.io.DirectoryScanner;
import org.citydb.log.Logger;
import org.citydb.modules.common.event.CounterEvent;
import org.citydb.modules.common.event.CounterType;
import org.citydb.modules.common.event.GeometryCounterEvent;
import org.citydb.plugins.CityGMLConverter.CityKMLExportPlugin;
import org.citydb.plugins.CityGMLConverter.config.Balloon;
import org.citydb.plugins.CityGMLConverter.config.ColladaOptions;
import org.citydb.plugins.CityGMLConverter.config.ConfigImpl;
import org.citydb.plugins.CityGMLConverter.config.DisplayForm;
import org.citydb.plugins.CityGMLConverter.config.Internal;
import org.citydb.plugins.CityGMLConverter.util.ElevationHelper;
import org.citydb.plugins.CityGMLConverter.util.ProjConvertor;
import org.citydb.util.Util;
import org.citygml.textureAtlasAPI.TextureAtlasGenerator;
import org.citygml.textureAtlasAPI.dataStructure.TexImage;
import org.citygml.textureAtlasAPI.dataStructure.TexImageInfo;
import org.citygml4j.factory.GMLGeometryFactory;
import org.citygml4j.model.citygml.CityGMLClass;
import org.citygml4j.model.citygml.appearance.Color;
import org.citygml4j.model.citygml.appearance.X3DMaterial;
import org.citygml4j.model.citygml.core.AbstractCityObject;
import org.citygml4j.model.citygml.generics.AbstractGenericAttribute;
import org.citygml4j.model.citygml.generics.DateAttribute;
import org.citygml4j.model.citygml.generics.DoubleAttribute;
import org.citygml4j.model.citygml.generics.GenericAttributeSet;
import org.citygml4j.model.citygml.generics.IntAttribute;
import org.citygml4j.model.citygml.generics.MeasureAttribute;
import org.citygml4j.model.citygml.generics.StringAttribute;
import org.citygml4j.model.citygml.generics.UriAttribute;
import org.collada._2005._11.colladaschema.*;
//import org.collada._2005._11.colladaschema.Geometry;					// collides with org.postgis.Geometry
import org.geotools.geometry.jts.GeometryBuilder;
import org.geotools.geometry.jts.JTS;
import org.postgis.Geometry;											// collides with Collada-Geometry
import org.postgis.MultiPolygon;
import org.postgis.PGgeometry;
import org.postgis.Polygon;

// import org.postgresql.largeobject.LargeObject;
// import org.postgresql.largeobject.LargeObjectManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.SeekableStream;
import com.sun.media.jai.codec.TIFFDecodeParam;

public abstract class KmlGenericObject {

    protected final int GEOMETRY_AMOUNT_WARNING = 10000;
    private final double TOLERANCE = Math.pow(10, -7);
    private final double PRECISION = Math.pow(10, 7);
    private final String NO_TEXIMAGE = "default";

    private HashMap<String, SurfaceInfo> surfaceInfos = new HashMap<String, SurfaceInfo>();
    private NodeZ coordinateTree;

    // key is surfaceId, surfaceId is originally a Long, here we use an Object for compatibility with the textureAtlasAPI
    private HashMap<Object, String> texImageUris = new HashMap<Object, String>();
    // key is imageUri
    private HashMap<String, BufferedImage> texImages = new HashMap<String, BufferedImage>();
    // for images in unusual formats or wrapping textures. Most times it will be null.
    // key is imageUri
    private HashMap<String, Long> unsupportedTexImageIds = null;
    // key is surfaceId, surfaceId is originally a Long
    private HashMap<String, X3DMaterial> x3dMaterials = null;

    private long id;
    private String gmlId;
    private BigInteger vertexIdCounter = new BigInteger("-1");
    protected VertexInfo firstVertexInfo = null;
    private VertexInfo lastVertexInfo = null;

    // origin of the relative coordinates for the object
    private List<Point3d> origins = new ArrayList<Point3d>();
    private Point3d origin = new Point3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);

    // placemark location in WGS84
    private Point3d location = new Point3d();
    private double zOffset;
    private boolean ignoreSurfaceOrientation = true;

    protected Connection connection;
    protected GMLGeometryFactory cityGMLFactory;
    protected KmlExporterManager kmlExporterManager;
    protected net.opengis.kml._2.ObjectFactory kmlFactory;
    protected ElevationServiceHandler elevationServiceHandler;
    protected BalloonTemplateHandlerImpl balloonTemplateHandler;
    protected EventDispatcher eventDispatcher;
    protected ConfigImpl config;

    protected int currentLod;
    protected DatabaseSrs dbSrs;
    protected X3DMaterial defaultX3dMaterial;

    private SimpleDateFormat dateFormatter;
    private DirectoryScanner directoryScanner;

    public KmlGenericObject(Connection connection,
                            KmlExporterManager kmlExporterManager,
                            GMLGeometryFactory cityGMLFactory,
                            net.opengis.kml._2.ObjectFactory kmlFactory,
                            ElevationServiceHandler elevationServiceHandler,
                            BalloonTemplateHandlerImpl balloonTemplateHandler,
                            EventDispatcher eventDispatcher,
                            ConfigImpl config) {

        this.connection = connection;
        this.kmlExporterManager = kmlExporterManager;
        this.cityGMLFactory = cityGMLFactory;
        this.kmlFactory = kmlFactory;
        this.elevationServiceHandler = elevationServiceHandler;
        this.balloonTemplateHandler = balloonTemplateHandler;
        this.eventDispatcher = eventDispatcher;
        this.config = config;

		dateFormatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

		defaultX3dMaterial = new X3DMaterial();
		defaultX3dMaterial.setAmbientIntensity(0.2d);
		defaultX3dMaterial.setShininess(0.2d);
		defaultX3dMaterial.setTransparency(0d);
		defaultX3dMaterial.setDiffuseColor(getX3dColorFromString("0.8 0.8 0.8"));
		defaultX3dMaterial.setSpecularColor(getX3dColorFromString("1.0 1.0 1.0"));
		defaultX3dMaterial.setEmissiveColor(getX3dColorFromString("0.0 0.0 0.0"));
    }

    public abstract void read(KmlSplittingResult work);
    public abstract String getStyleBasisName();
    public abstract ColladaOptions getColladaOptions();
    public abstract Balloon getBalloonSettings();
    protected abstract List<DisplayForm> getDisplayForms();
    protected abstract String getHighlightingQuery();

    protected BalloonTemplateHandlerImpl getBalloonTemplateHandler() {
        return balloonTemplateHandler;
    }

    protected void setBalloonTemplateHandler(BalloonTemplateHandlerImpl balloonTemplateHandler) {
        this.balloonTemplateHandler = balloonTemplateHandler;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setGmlId(String gmlId) {
        this.gmlId = gmlId.replace(':', '_');
    }

    public String getGmlId() {
        return gmlId;
    }

    protected void updateOrigins(double x, double y, double z) {
        // update origin and list of lowest points
        if (z < origin.z) {
            origins.clear();
            origin.x = x;
            origin.y = y;
            origin.z = z;
            origins.add(origin);
        } else if (z == origin.z)
            origins.add(new Point3d(x, y, z));
    }

    protected Point3d getOrigin() {
        return origin;
    }

    protected List<Point3d> getOrigins() {
        return origins;
    }

    protected void setZOffset(double zOffset) {
        this.zOffset = zOffset;
    }

    protected double getZOffset() {
        return zOffset;
    }

    protected Point3d getLocation() {
    	
        return location;
    }

    protected void setLocation(double x, double y, double z) {
        location.x = x;
        location.y = y;
        location.z = z;
    }

    protected void setIgnoreSurfaceOrientation(boolean ignoreSurfaceOrientation) {
        this.ignoreSurfaceOrientation = ignoreSurfaceOrientation;
    }

    protected boolean isIgnoreSurfaceOrientation() {
        return ignoreSurfaceOrientation;
    }

    protected void addSurfaceInfo(String surfaceId, SurfaceInfo surfaceInfo) {
        surfaceInfos.put(surfaceId, surfaceInfo);
    }


    public COLLADA generateColladaTree() throws DatatypeConfigurationException{

        ObjectFactory colladaFactory = new ObjectFactory();

        // java and XML...
        DatatypeFactory df = DatatypeFactory.newInstance();
        XMLGregorianCalendar xmlGregorianCalendar = df.newXMLGregorianCalendar(new GregorianCalendar());
        xmlGregorianCalendar.setTimezone(DatatypeConstants.FIELD_UNDEFINED);

        COLLADA	collada = colladaFactory.createCOLLADA();
        collada.setVersion("1.4.1");
        // --------------------------- asset ---------------------------

        Asset asset = colladaFactory.createAsset();
        asset.setCreated(xmlGregorianCalendar);
        asset.setModified(xmlGregorianCalendar);
        Asset.Unit unit = colladaFactory.createAssetUnit();
        unit.setName("meters");
        unit.setMeter(1.0);
        asset.setUnit(unit);
        asset.setUpAxis(UpAxisType.Z_UP);
        Asset.Contributor contributor = colladaFactory.createAssetContributor();
        // System.getProperty("line.separator") produces weird effects here
        contributor.setAuthoringTool(this.getClass().getPackage().getImplementationTitle() + ", version " +
                this.getClass().getPackage().getImplementationVersion() + "; " +
                this.getClass().getPackage().getImplementationVendor());
        asset.getContributor().add(contributor);
        collada.setAsset(asset);

        LibraryImages libraryImages = colladaFactory.createLibraryImages();
        LibraryMaterials libraryMaterials = colladaFactory.createLibraryMaterials();
        LibraryEffects libraryEffects = colladaFactory.createLibraryEffects();
        LibraryGeometries libraryGeometries = colladaFactory.createLibraryGeometries();
        LibraryVisualScenes libraryVisualScenes = colladaFactory.createLibraryVisualScenes();

        // --------------------------- geometry (constant part) ---------------------------
        org.collada._2005._11.colladaschema.Geometry geometry = colladaFactory.createGeometry();
        geometry.setId("geometry0");

        Source positionSource = colladaFactory.createSource();
        positionSource.setId("geometry0-position");

        FloatArray positionArray = colladaFactory.createFloatArray();
        positionArray.setId("geometry0-position-array");
        List<Double> positionValues = positionArray.getValue();
        positionSource.setFloatArray(positionArray);

        Accessor positionAccessor = colladaFactory.createAccessor();
        positionAccessor.setSource("#" + positionArray.getId());
        positionAccessor.setStride(new BigInteger("3"));
        Param paramX = colladaFactory.createParam();
        paramX.setType("float");
        paramX.setName("X");
        Param paramY = colladaFactory.createParam();
        paramY.setType("float");
        paramY.setName("Y");
        Param paramZ = colladaFactory.createParam();
        paramZ.setType("float");
        paramZ.setName("Z");
        positionAccessor.getParam().add(paramX);
        positionAccessor.getParam().add(paramY);
        positionAccessor.getParam().add(paramZ);
        Source.TechniqueCommon positionTechnique = colladaFactory.createSourceTechniqueCommon();
        positionTechnique.setAccessor(positionAccessor);
        positionSource.setTechniqueCommon(positionTechnique);

        Source texCoordsSource = colladaFactory.createSource();
        texCoordsSource.setId("geometry0-texCoords");

        FloatArray texCoordsArray = colladaFactory.createFloatArray();
        texCoordsArray.setId("geometry0-texCoords-array");
        List<Double> texCoordsValues = texCoordsArray.getValue();
        texCoordsSource.setFloatArray(texCoordsArray);

        Accessor texCoordsAccessor = colladaFactory.createAccessor();
        texCoordsAccessor.setSource("#" + texCoordsArray.getId());
        texCoordsAccessor.setStride(new BigInteger("2"));
        Param paramS = colladaFactory.createParam();
        paramS.setType("float");
        paramS.setName("S");
        Param paramT = colladaFactory.createParam();
        paramT.setType("float");
        paramT.setName("T");
        texCoordsAccessor.getParam().add(paramS);
        texCoordsAccessor.getParam().add(paramT);
        Source.TechniqueCommon texCoordsTechnique = colladaFactory.createSourceTechniqueCommon();
        texCoordsTechnique.setAccessor(texCoordsAccessor);
        texCoordsSource.setTechniqueCommon(texCoordsTechnique);

        Vertices vertices = colladaFactory.createVertices();
        vertices.setId("geometry0-vertex");
        InputLocal input = colladaFactory.createInputLocal();
        input.setSemantic("POSITION");
        input.setSource("#" + positionSource.getId());
        vertices.getInput().add(input);

        Mesh mesh = colladaFactory.createMesh();
        mesh.getSource().add(positionSource);
        mesh.getSource().add(texCoordsSource);
        mesh.setVertices(vertices);
        geometry.setMesh(mesh);
        libraryGeometries.getGeometry().add(geometry);
        BigInteger texCoordsCounter = BigInteger.ZERO;

        // --------------------------- visual scenes ---------------------------
        VisualScene visualScene = colladaFactory.createVisualScene();
        visualScene.setId("Building_" + gmlId);
        BindMaterial.TechniqueCommon techniqueCommon = colladaFactory.createBindMaterialTechniqueCommon();
        BindMaterial bindMaterial = colladaFactory.createBindMaterial();
        bindMaterial.setTechniqueCommon(techniqueCommon);
        InstanceGeometry instanceGeometry = colladaFactory.createInstanceGeometry();
        instanceGeometry.setUrl("#" + geometry.getId());
        instanceGeometry.setBindMaterial(bindMaterial);
        org.collada._2005._11.colladaschema.Node node = colladaFactory.createNode();
        node.getInstanceGeometry().add(instanceGeometry);
        visualScene.getNode().add(node);
        libraryVisualScenes.getVisualScene().add(visualScene);

        // --------------------------- now the variable part ---------------------------
        Triangles triangles = null;
        HashMap<String, Triangles> trianglesByTexImageName = new HashMap<String, Triangles>();

        // geometryInfos contains all surfaces, textured or not
        Set<String> keySet = surfaceInfos.keySet();
        Iterator<String> iterator = keySet.iterator();
        while (iterator.hasNext()) {
            String surfaceId = iterator.next();
            String texImageName = texImageUris.get(surfaceId);
            X3DMaterial x3dMaterial = getX3dMaterial(surfaceId);
            boolean surfaceTextured = true;
            if (texImageName == null) {
                surfaceTextured = false;
                texImageName = (x3dMaterial != null) ?
                        buildNameFromX3dMaterial(x3dMaterial):
                        NO_TEXIMAGE; // <- should never happen
            }

            triangles = trianglesByTexImageName.get(texImageName);
            if (triangles == null) { // never worked on this image or material before

                // --------------------------- materials ---------------------------
                Material material = colladaFactory.createMaterial();
                material.setId(replaceExtensionWithSuffix(texImageName, "_mat"));
                InstanceEffect instanceEffect = colladaFactory.createInstanceEffect();
                instanceEffect.setUrl("#" + replaceExtensionWithSuffix(texImageName, "_eff"));
                material.setInstanceEffect(instanceEffect);
                libraryMaterials.getMaterial().add(material);

                // --------------------- effects common part 1 ---------------------
                Effect effect = colladaFactory.createEffect();
                effect.setId(replaceExtensionWithSuffix(texImageName, "_eff"));
                ProfileCOMMON profileCommon = colladaFactory.createProfileCOMMON();

                if (surfaceTextured) {
                    // --------------------------- images ---------------------------
                    Image image = colladaFactory.createImage();
                    image.setId(replaceExtensionWithSuffix(texImageName, "_img"));
                    image.setInitFrom(texImageName);
                    libraryImages.getImage().add(image);

                    // --------------------------- effects ---------------------------
                    FxSurfaceInitFromCommon initFrom = colladaFactory.createFxSurfaceInitFromCommon();
                    initFrom.setValue(image); // evtl. image.getId();
                    FxSurfaceCommon surface = colladaFactory.createFxSurfaceCommon();
                    surface.setType("2D"); // ColladaConstants.SURFACE_TYPE_2D
                    surface.getInitFrom().add(initFrom);

                    CommonNewparamType newParam1 = colladaFactory.createCommonNewparamType();
                    newParam1.setSurface(surface);
                    newParam1.setSid(replaceExtensionWithSuffix(texImageName, "_surface"));
                    profileCommon.getImageOrNewparam().add(newParam1);

                    FxSampler2DCommon sampler2D = colladaFactory.createFxSampler2DCommon();
                    sampler2D.setSource(newParam1.getSid());
                    CommonNewparamType newParam2 = colladaFactory.createCommonNewparamType();
                    newParam2.setSampler2D(sampler2D);
                    newParam2.setSid(replaceExtensionWithSuffix(texImageName, "_sampler"));
                    profileCommon.getImageOrNewparam().add(newParam2);

                    ProfileCOMMON.Technique profileCommonTechnique = colladaFactory.createProfileCOMMONTechnique();
                    profileCommonTechnique.setSid("COMMON");
                    ProfileCOMMON.Technique.Lambert lambert = colladaFactory.createProfileCOMMONTechniqueLambert();
                    CommonColorOrTextureType.Texture texture = colladaFactory.createCommonColorOrTextureTypeTexture();
                    texture.setTexture(newParam2.getSid());
                    texture.setTexcoord("TEXCOORD"); // ColladaConstants.INPUT_SEMANTIC_TEXCOORD
                    CommonColorOrTextureType ccott = colladaFactory.createCommonColorOrTextureType();
                    ccott.setTexture(texture);
                    lambert.setDiffuse(ccott);
                    profileCommonTechnique.setLambert(lambert);
                    profileCommon.setTechnique(profileCommonTechnique);
                }
                else {
                    // --------------------------- effects ---------------------------
                    ProfileCOMMON.Technique profileCommonTechnique = colladaFactory.createProfileCOMMONTechnique();
                    profileCommonTechnique.setSid("COMMON");
                    ProfileCOMMON.Technique.Lambert lambert = colladaFactory.createProfileCOMMONTechniqueLambert();

                    CommonFloatOrParamType cfopt = colladaFactory.createCommonFloatOrParamType();
                    CommonFloatOrParamType.Float cfoptf = colladaFactory.createCommonFloatOrParamTypeFloat();
                    if (x3dMaterial.isSetShininess()) {
                        cfoptf.setValue(x3dMaterial.getShininess());
                        cfopt.setFloat(cfoptf);
                        lambert.setReflectivity(cfopt);
                    }

                    if (x3dMaterial.isSetTransparency()) {
                        cfopt = colladaFactory.createCommonFloatOrParamType();
                        cfoptf = colladaFactory.createCommonFloatOrParamTypeFloat();
                        cfoptf.setValue(1.0 - x3dMaterial.getTransparency());
                        cfopt.setFloat(cfoptf);
                        lambert.setTransparency(cfopt);
                        CommonTransparentType transparent = colladaFactory.createCommonTransparentType();
                        transparent.setOpaque(FxOpaqueEnum.A_ONE);
                        CommonColorOrTextureType.Color color = colladaFactory.createCommonColorOrTextureTypeColor();
                        color.getValue().add(1.0);
                        color.getValue().add(1.0);
                        color.getValue().add(1.0);
                        color.getValue().add(1.0);
                        transparent.setColor(color);
                        lambert.setTransparent(transparent);
                    }

                    if (x3dMaterial.isSetDiffuseColor()) {
                        CommonColorOrTextureType.Color color = colladaFactory.createCommonColorOrTextureTypeColor();
                        color.getValue().add(x3dMaterial.getDiffuseColor().getRed());
                        color.getValue().add(x3dMaterial.getDiffuseColor().getGreen());
                        color.getValue().add(x3dMaterial.getDiffuseColor().getBlue());
                        color.getValue().add(1d); // alpha
                        CommonColorOrTextureType ccott = colladaFactory.createCommonColorOrTextureType();
                        ccott.setColor(color);
                        lambert.setDiffuse(ccott);
                    }

                    if (x3dMaterial.isSetSpecularColor()) {
                        CommonColorOrTextureType.Color color = colladaFactory.createCommonColorOrTextureTypeColor();
                        color.getValue().add(x3dMaterial.getSpecularColor().getRed());
                        color.getValue().add(x3dMaterial.getSpecularColor().getGreen());
                        color.getValue().add(x3dMaterial.getSpecularColor().getBlue());
                        color.getValue().add(1d); // alpha
                        CommonColorOrTextureType ccott = colladaFactory.createCommonColorOrTextureType();
                        ccott.setColor(color);
                        lambert.setReflective(ccott);
                    }

                    if (x3dMaterial.isSetEmissiveColor()) {
                        CommonColorOrTextureType.Color color = colladaFactory.createCommonColorOrTextureTypeColor();
                        color.getValue().add(x3dMaterial.getEmissiveColor().getRed());
                        color.getValue().add(x3dMaterial.getEmissiveColor().getGreen());
                        color.getValue().add(x3dMaterial.getEmissiveColor().getBlue());
                        color.getValue().add(1d); // alpha
                        CommonColorOrTextureType ccott = colladaFactory.createCommonColorOrTextureType();
                        ccott.setColor(color);
                        lambert.setEmission(ccott);
                    }

                    profileCommonTechnique.setLambert(lambert);
                    profileCommon.setTechnique(profileCommonTechnique);
                }

                // --------------------- effects common part 2 ---------------------
                Technique geTechnique = colladaFactory.createTechnique();
                geTechnique.setProfile("GOOGLEEARTH");

                try {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder docBuilder = factory.newDocumentBuilder();
                    Document document = docBuilder.newDocument();
                    factory.setNamespaceAware(true);
                    Element doubleSided = document.createElementNS("http://www.collada.org/2005/11/COLLADASchema", "double_sided");
                    doubleSided.setTextContent(ignoreSurfaceOrientation ? "1": "0");
                    geTechnique.getAny().add(doubleSided);
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                }

                Extra extra = colladaFactory.createExtra();
                extra.getTechnique().add(geTechnique);
                profileCommon.getExtra().add(extra);

                effect.getFxProfileAbstract().add(colladaFactory.createProfileCOMMON(profileCommon));

                libraryEffects.getEffect().add(effect);

                // --------------------------- triangles ---------------------------
                triangles = colladaFactory.createTriangles();
                triangles.setMaterial(replaceExtensionWithSuffix(texImageName, "_tri"));
                InputLocalOffset inputV = colladaFactory.createInputLocalOffset();
                inputV.setSemantic("VERTEX"); // ColladaConstants.INPUT_SEMANTIC_VERTEX
                inputV.setSource("#" + vertices.getId());
                inputV.setOffset(BigInteger.ZERO);
                triangles.getInput().add(inputV);
                if (surfaceTextured) {
                    InputLocalOffset inputT = colladaFactory.createInputLocalOffset();
                    inputT.setSemantic("TEXCOORD"); // ColladaConstants.INPUT_SEMANTIC_TEXCOORD
                    inputT.setSource("#" + texCoordsSource.getId());
                    inputT.setOffset(BigInteger.ONE);
                    triangles.getInput().add(inputT);
                }

                trianglesByTexImageName.put(texImageName, triangles);
            }

            // --------------------------- geometry (variable part) ---------------------------
            SurfaceInfo surfaceInfo = surfaceInfos.get(surfaceId);
            List<VertexInfo> vertexInfos = surfaceInfo.getVertexInfos();
            double[] ordinatesArray = new double[vertexInfos.size() * 3];

            int count = 0;
            for (VertexInfo vertexInfo : vertexInfos) {
                ordinatesArray[count++] = vertexInfo.getX() - origin.x;
                ordinatesArray[count++] = vertexInfo.getY() - origin.y;
                ordinatesArray[count++] = vertexInfo.getZ() - origin.z;
            }

            GeometryInfo ginfo = new GeometryInfo(GeometryInfo.POLYGON_ARRAY);
            ginfo.setCoordinates(ordinatesArray);
            ginfo.setContourCounts(surfaceInfo.getRingCountAsArray());
            ginfo.setStripCounts(surfaceInfo.getVertexCount());
            int outerRingCount = ginfo.getStripCounts()[0];

            // triangulate the surface geometry
            ginfo.convertToIndexedTriangles();

            // fix a reversed orientation of the triangulated surface
            int[] indexes = ginfo.getCoordinateIndices();
            byte[] edges = {0, 1, 1, 2, 2, 0};
            boolean hasFound = false;
            boolean reverseIndexes = false;

            for (int i = 0; !hasFound && i < indexes.length; i += 3) {
                // skip degenerated triangles
                if (indexes[i] == indexes[i + 1] || indexes[i + 1] == indexes[i + 2] || indexes[i] == indexes[i + 2])
                    continue;

                // find the first edge on the exterior ring
                for (int j = 0; j < edges.length; j += 2) {
                    int first = i + edges[j];
                    int second = i + edges[j + 1];

                    if (indexes[first] < outerRingCount && indexes[second] < outerRingCount && Math.abs(indexes[first] - indexes[second]) == 1) {
                        // ok, we found it. now check the order of the vertex indices
                        hasFound = true;
                        if (indexes[first] > indexes[second])
                            reverseIndexes = true;

                        break;
                    }
                }
            }

            if (reverseIndexes) {
                int[] tmp = new int[indexes.length];
                int j = 0;
                for (int i = 0; i < indexes.length; i+=3) {
                    tmp[j++] = indexes[i+2];
                    tmp[j++] = indexes[i+1];
                    tmp[j++] = indexes[i];
                }

                indexes = tmp;
            }

            // use vertex indices of the triangulation to populate
            // the vertex arrays in the collada file
            for(int i = 0; i < indexes.length; i++) {
                VertexInfo vertexInfo = vertexInfos.get(indexes[i]);
                triangles.getP().add(vertexInfo.getVertexId());

                if (surfaceTextured) {
                    TexCoords texCoords = vertexInfo.getTexCoords(surfaceId);
                    if (texCoords != null) {
                        // trying to save some texture points
                        int indexOfT = texCoordsValues.indexOf(texCoords.getT());
                        if (indexOfT > 0 && indexOfT%2 == 1 && // avoid coincidences
                                texCoordsValues.get(indexOfT - 1).equals(texCoords.getS())) {
                            triangles.getP().add(new BigInteger(String.valueOf((indexOfT - 1)/2)));
                        }
                        else {
                            texCoordsValues.add(texCoords.getS());
                            texCoordsValues.add(texCoords.getT());
                            triangles.getP().add(texCoordsCounter);
                            texCoordsCounter = texCoordsCounter.add(BigInteger.ONE);
                        }
                    }
                    else { // should never happen
                        triangles.getP().add(texCoordsCounter); // wrong data is better than triangles out of sync
                        Logger.getInstance().log(LogLevel.DEBUG,
                                "texCoords not found for (" + vertexInfo.getX() + ", " + vertexInfo.getY() + ", "
                                        + vertexInfo.getZ() + "). TOLERANCE = " + TOLERANCE);
                    }
                }
            }
        }

        VertexInfo vertexInfoIterator = firstVertexInfo;
        while (vertexInfoIterator != null) {
            positionValues.add(reducePrecisionForXorY((vertexInfoIterator.getX() - origin.x)));
            positionValues.add(reducePrecisionForXorY((vertexInfoIterator.getY() - origin.y)));
            positionValues.add(reducePrecisionForZ((vertexInfoIterator.getZ() - origin.z)));
            vertexInfoIterator = vertexInfoIterator.getNextVertexInfo();
        }

        positionArray.setCount(new BigInteger(String.valueOf(positionValues.size()))); // gotta love BigInteger!
        texCoordsArray.setCount(new BigInteger(String.valueOf(texCoordsValues.size())));
        positionAccessor.setCount(positionArray.getCount().divide(positionAccessor.getStride()));
        texCoordsAccessor.setCount(texCoordsArray.getCount().divide(texCoordsAccessor.getStride()));

        Set<String> trianglesKeySet = trianglesByTexImageName.keySet();
        Iterator<String> trianglesIterator = trianglesKeySet.iterator();
        while (trianglesIterator.hasNext()) {
            String texImageName = trianglesIterator.next();
            triangles = trianglesByTexImageName.get(texImageName);
            triangles.setCount(new BigInteger(String.valueOf(triangles.getP().size()/(3*triangles.getInput().size()))));
            if (texImageName.startsWith(NO_TEXIMAGE)) { // materials first, textures last
                mesh.getLinesOrLinestripsOrPolygons().add(0, triangles);
            }
            else {
                mesh.getLinesOrLinestripsOrPolygons().add(triangles);
            }
            InstanceMaterial instanceMaterial = colladaFactory.createInstanceMaterial();
            instanceMaterial.setSymbol(triangles.getMaterial());
            instanceMaterial.setTarget("#" + replaceExtensionWithSuffix(texImageName, "_mat"));
            techniqueCommon.getInstanceMaterial().add(instanceMaterial);
        }

        List<Object> libraries = collada.getLibraryAnimationsOrLibraryAnimationClipsOrLibraryCameras();

        if (!libraryImages.getImage().isEmpty()) { // there may be buildings with no textures at all
            libraries.add(libraryImages);
        }
        libraries.add(libraryMaterials);
        libraries.add(libraryEffects);
        libraries.add(libraryGeometries);
        libraries.add(libraryVisualScenes);

        InstanceWithExtra instanceWithExtra = colladaFactory.createInstanceWithExtra();
        instanceWithExtra.setUrl("#" + visualScene.getId());
        COLLADA.Scene scene = colladaFactory.createCOLLADAScene();
        scene.setInstanceVisualScene(instanceWithExtra);
        collada.setScene(scene);

        return collada;
    }

    private String replaceExtensionWithSuffix (String imageName, String suffix) {
        int indexOfExtension = imageName.lastIndexOf('.');
        if (indexOfExtension != -1) {
            imageName = imageName.substring(0, indexOfExtension);
        }
        return imageName + suffix;
    }

    protected HashMap<Object, String> getTexImageUris(){
        return texImageUris;
    }
/*
    public void addGeometryInfo(String surfaceId, GeometryInfo geometryInfo){
        geometryInfos.put(surfaceId, geometryInfo);
    }

    protected int getGeometryAmount(){
        return geometryInfos.size();
    }

    public GeometryInfo getGeometryInfo(String surfaceId){
        return geometryInfos.get(surfaceId);
    }
*/

    protected int getGeometryAmount(){
        return surfaceInfos.size();
    }

    protected void addX3dMaterial(String surfaceId, X3DMaterial x3dMaterial){
        if (x3dMaterial == null) return;
        if (x3dMaterial.isSetAmbientIntensity()
                || x3dMaterial.isSetShininess()
                || x3dMaterial.isSetTransparency()
                || x3dMaterial.isSetDiffuseColor()
                || x3dMaterial.isSetSpecularColor()
                || x3dMaterial.isSetEmissiveColor()) {

            if (x3dMaterials == null) {
                x3dMaterials = new HashMap<String, X3DMaterial>();
            }
            x3dMaterials.put(surfaceId, x3dMaterial);
        }
    }

    protected X3DMaterial getX3dMaterial(String surfaceId) {
        X3DMaterial x3dMaterial = null;
        if (x3dMaterials != null) {
            x3dMaterial = x3dMaterials.get(surfaceId);
        }
        return x3dMaterial;
    }

    protected void addTexImageUri(String surfaceId, String texImageUri){
        if (texImageUri != null) {
            texImageUris.put(surfaceId, texImageUri);
        }
    }

    protected void addTexImage(String texImageUri, BufferedImage texImage){
        if (texImage != null) {
            texImages.put(texImageUri, texImage);

        }
    }

    protected void removeTexImage(String texImageUri){
        texImages.remove(texImageUri);
    }

    public HashMap<String, BufferedImage> getTexImages(){
        return texImages;
    }

    protected BufferedImage getTexImage(String texImageUri){
        BufferedImage texImage = null;
        if (texImages != null) {
            texImage = texImages.get(texImageUri);
        }
        return texImage;
    }

    protected VertexInfo setVertexInfoForXYZ(String surfaceId, double x, double y, double z){
        vertexIdCounter = vertexIdCounter.add(BigInteger.ONE);
        VertexInfo vertexInfo = new VertexInfo(vertexIdCounter, x, y, z);
        NodeZ nodeToInsert = new NodeZ(z, new NodeY(y, new NodeX(x, vertexInfo)));
        if (coordinateTree == null) {
            coordinateTree =  nodeToInsert;
            firstVertexInfo = vertexInfo;
            lastVertexInfo = vertexInfo;
        }
        else {
            Node node = insertNode(coordinateTree, nodeToInsert);
            if (node.value instanceof VertexInfo)
                vertexInfo = (VertexInfo)node.value;
        }

        return vertexInfo;
    }

    private Node insertNode(Node currentBasis, Node nodeToInsert) {
        int compareKeysResult = compareKeys(nodeToInsert.key, currentBasis.key, TOLERANCE);
        if (compareKeysResult > 0) {
            if (currentBasis.rightArc == null){
                currentBasis.setRightArc(nodeToInsert);
                linkCurrentVertexInfoToLastVertexInfo(nodeToInsert);
                return nodeToInsert;
            }
            else {
                return insertNode(currentBasis.rightArc, nodeToInsert);
            }
        }
        else if (compareKeysResult < 0) {
            if (currentBasis.leftArc == null){
                currentBasis.setLeftArc(nodeToInsert);
                linkCurrentVertexInfoToLastVertexInfo(nodeToInsert);
                return nodeToInsert;
            }
            else {
                return insertNode(currentBasis.leftArc, nodeToInsert);
            }
        }
        else {
            return replaceOrAddValue(currentBasis, nodeToInsert);
        }
    }

    private Node replaceOrAddValue(Node currentBasis, Node nodeToInsert) {
        if (nodeToInsert.value instanceof VertexInfo) {
            VertexInfo vertexInfoToInsert = (VertexInfo)nodeToInsert.value;
            if (currentBasis.value == null) { // no vertexInfo yet for this point
                currentBasis.value = nodeToInsert.value;
                linkCurrentVertexInfoToLastVertexInfo(vertexInfoToInsert);
            } else
                vertexIdCounter = vertexIdCounter.subtract(BigInteger.ONE);

            return currentBasis;
        }
        else { // Node
            return insertNode((Node)currentBasis.value, (Node)nodeToInsert.value);
        }
    }

    private void linkCurrentVertexInfoToLastVertexInfo (Node node) {
        while (!(node.value instanceof VertexInfo)) {
            node = (Node)node.value;
        }
        linkCurrentVertexInfoToLastVertexInfo((VertexInfo)node.value);
    }

    private void linkCurrentVertexInfoToLastVertexInfo (VertexInfo currentVertexInfo) {
        lastVertexInfo.setNextVertexInfo(currentVertexInfo);
        lastVertexInfo = currentVertexInfo;
    }

    private int compareKeys (double key1, double key2, double tolerance){
        int result = 0;
        if (Math.abs(key1 - key2) > tolerance) {
            result = key1 > key2 ? 1 : -1;
        }
        return result;
    }

    //Should be considered
    public void appendObject (KmlGenericObject objectToAppend) {

        VertexInfo vertexInfoIterator = objectToAppend.firstVertexInfo;
        while (vertexInfoIterator != null) {
            if (vertexInfoIterator.getAllTexCoords() == null) {
                VertexInfo tmp = this.setVertexInfoForXYZ("-1", // dummy
                        vertexInfoIterator.getX(),
                        vertexInfoIterator.getY(),
                        vertexInfoIterator.getZ());
                vertexInfoIterator.setVertexId(tmp.getVertexId());
            }
            else {
                Set<String> keySet = vertexInfoIterator.getAllTexCoords().keySet();
                Iterator<String> iterator = keySet.iterator();
                while (iterator.hasNext()) {
                    String surfaceId = iterator.next();
                    VertexInfo tmp = this.setVertexInfoForXYZ(surfaceId,
                            vertexInfoIterator.getX(),
                            vertexInfoIterator.getY(),
                            vertexInfoIterator.getZ());
                    vertexInfoIterator.setVertexId(tmp.getVertexId());
                    tmp.addTexCoords(surfaceId, vertexInfoIterator.getTexCoords(surfaceId));
                }
            }
            vertexInfoIterator = vertexInfoIterator.getNextVertexInfo();
        }

        Set<String> keySet = objectToAppend.surfaceInfos.keySet();
        Iterator<String> iterator = keySet.iterator();
        while (iterator.hasNext()) {
            String surfaceId = iterator.next();
            this.addX3dMaterial(surfaceId, objectToAppend.getX3dMaterial(surfaceId));
            String imageUri = objectToAppend.texImageUris.get(surfaceId);
            this.addTexImageUri(surfaceId, imageUri);
            this.addTexImage(imageUri, objectToAppend.getTexImage(imageUri));
            this.surfaceInfos.put(surfaceId, objectToAppend.surfaceInfos.get(surfaceId));
        }

        // adapt id accordingly
        int indexOf_to_ = this.gmlId.indexOf("_to_");
        String ownLowerLimit = "";
        String ownUpperLimit = "";
        if (indexOf_to_ != -1) { // already more than one building in here
            ownLowerLimit = this.gmlId.substring(0, indexOf_to_);
            ownUpperLimit = this.gmlId.substring(indexOf_to_ + 4);
        }
        else {
            ownLowerLimit = this.gmlId;
            ownUpperLimit = ownLowerLimit;
        }

        int btaIndexOf_to_ = objectToAppend.gmlId.indexOf("_to_");
        String btaLowerLimit = "";
        String btaUpperLimit = "";
        if (btaIndexOf_to_ != -1) { // already more than one building in there
            btaLowerLimit = objectToAppend.gmlId.substring(0, btaIndexOf_to_);
            btaUpperLimit = objectToAppend.gmlId.substring(btaIndexOf_to_ + 4);
        }
        else {
            btaLowerLimit = objectToAppend.gmlId;
            btaUpperLimit = btaLowerLimit;
        }

        ownLowerLimit = ownLowerLimit.compareTo(btaLowerLimit)<0 ? ownLowerLimit: btaLowerLimit;
        ownUpperLimit = ownUpperLimit.compareTo(btaUpperLimit)>0 ? ownUpperLimit: btaUpperLimit;

        this.setGmlId(String.valueOf(ownLowerLimit) + "_to_" + ownUpperLimit);
    }


    public void createTextureAtlas(int packingAlgorithm, double imageScaleFactor, boolean pots) throws SQLException, IOException {

        if (texImages.size() < 2) {
            // building has not enough textures or they are in an unknown image format
            return;
        }

        useExternalTAGenerator(packingAlgorithm, imageScaleFactor, pots);
    }

    private void useExternalTAGenerator(int packingAlgorithm, double scaleFactor, boolean pots) throws SQLException, IOException {
        org.citygml.textureAtlasAPI.TextureAtlasGenerator taGenerator = new org.citygml.textureAtlasAPI.TextureAtlasGenerator();
        TexImageInfo tiInfo = new TexImageInfo();
        tiInfo.setTexImageURIs(texImageUris);

        HashMap<String, TexImage> tiInfoImages = new HashMap<String, TexImage>();

        Set<String> texImagesSet = texImages.keySet();
        Iterator<String> texImagesIterator = texImagesSet.iterator();
        while (texImagesIterator.hasNext()) {
            String imageName = texImagesIterator.next();
            TexImage image = new TexImage(texImages.get(imageName));
            tiInfoImages.put(imageName, image);
        }

        //		if (texOrdImages != null) {
        //			texImagesSet = texOrdImages.keySet();
        //			texImagesIterator = texImagesSet.iterator();
        //			while (texImagesIterator.hasNext()) {
        //				String imageName = texImagesIterator.next();
        //				TexImage image = new TexImage(texOrdImages.get(imageName));
        //				tiInfoImages.put(imageName, image);
        //			}
        //		}

        tiInfo.setTexImages(tiInfoImages);

        // texture coordinates
        HashMap<Object, String> tiInfoCoords = new HashMap<Object, String>();

        Set<Object> sgIdSet = texImageUris.keySet();
        Iterator<Object> sgIdIterator = sgIdSet.iterator();
        while (sgIdIterator.hasNext()) {
            String sgId = (String) sgIdIterator.next();
            VertexInfo vertexInfoIterator = firstVertexInfo;
            while (vertexInfoIterator != null) {
                if (vertexInfoIterator.getAllTexCoords() != null &&
                        vertexInfoIterator.getAllTexCoords().containsKey(sgId)) {
                    double s = vertexInfoIterator.getTexCoords(sgId).getS();
                    double t = vertexInfoIterator.getTexCoords(sgId).getT();
                    String tiInfoCoordsForSgId = tiInfoCoords.get(sgId);
                    tiInfoCoordsForSgId = (tiInfoCoordsForSgId == null) ?
                            "" :
                            tiInfoCoordsForSgId + " ";
                    tiInfoCoords.put(sgId, tiInfoCoordsForSgId + String.valueOf(s) + " " + String.valueOf(t));
                }
                vertexInfoIterator = vertexInfoIterator.getNextVertexInfo();
            }
        }

        tiInfo.setTexCoordinates(tiInfoCoords);

        taGenerator.setUsePOTS(pots);
        taGenerator.setScaleFactor(scaleFactor);
        tiInfo = taGenerator.convert(tiInfo, packingAlgorithm);

        texImageUris = tiInfo.getTexImageURIs();
        tiInfoImages = tiInfo.getTexImages();
        tiInfoCoords = tiInfo.getTexCoordinates();

        texImages.clear();
        //		if (texOrdImages != null) {
        //			texOrdImages.clear();
        //		}

        texImagesSet = tiInfoImages.keySet();
        texImagesIterator = texImagesSet.iterator();
        while (texImagesIterator.hasNext()) {
            String texImageName = texImagesIterator.next();
            TexImage texImage = tiInfoImages.get(texImageName);
            if (texImage.getBufferedImage() != null) {
                texImages.put(texImageName, texImage.getBufferedImage());
            }
            //			else if (texImage.getOrdImage() != null) {
            //				if (texOrdImages == null) {
            //					texOrdImages = new HashMap<String, OrdImage>();
            //				}
            //				texOrdImages.put(texImageName, texImage.getOrdImage());
            //			}
        }

        sgIdIterator = sgIdSet.iterator();
        while (sgIdIterator.hasNext()) {
            String sgId = (String) sgIdIterator.next();
            StringTokenizer texCoordsTokenized = new StringTokenizer(tiInfoCoords.get(sgId), " ");
            VertexInfo vertexInfoIterator = firstVertexInfo;
            while (texCoordsTokenized.hasMoreElements() &&
                    vertexInfoIterator != null) {
                if (vertexInfoIterator.getAllTexCoords() != null &&
                        vertexInfoIterator.getAllTexCoords().containsKey(sgId)) {
                    vertexInfoIterator.getTexCoords(sgId).setS(Double.parseDouble(texCoordsTokenized.nextToken()));
                    vertexInfoIterator.getTexCoords(sgId).setT(Double.parseDouble(texCoordsTokenized.nextToken()));
                }
                vertexInfoIterator = vertexInfoIterator.getNextVertexInfo();
            }
        }
    }

    public void resizeAllImagesByFactor (double factor) throws SQLException, IOException {
        if (texImages.size() == 0) { // building has no textures at all
            return;
        }

        Set<String> keySet = texImages.keySet();
        Iterator<String> iterator = keySet.iterator();
        while (iterator.hasNext()) {
            String imageName = iterator.next();
            BufferedImage imageToResize = texImages.get(imageName);
            if (imageToResize.getWidth()*factor < 1 || imageToResize.getHeight()*factor < 1) {
                continue;
            }
            BufferedImage resizedImage = getScaledInstance(imageToResize,
                    (int)(imageToResize.getWidth()*factor),
                    (int)(imageToResize.getHeight()*factor),
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR,
                    true);
            texImages.put(imageName, resizedImage);
        }
    }




    /**
     * Convenience method that returns a scaled instance of the
     * provided {@code BufferedImage}.
     *
     * @param img the original image to be scaled
     * @param targetWidth the desired width of the scaled instance,
     *    in pixels
     * @param targetHeight the desired height of the scaled instance,
     *    in pixels
     * @param hint one of the rendering hints that corresponds to
     *    {@code RenderingHints.KEY_INTERPOLATION} (e.g.
     *    {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
     *    {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
     *    {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
     * @param higherQuality if true, this method will use a multi-step
     *    scaling technique that provides higher quality than the usual
     *    one-step technique (only useful in downscaling cases, where
     *    {@code targetWidth} or {@code targetHeight} is
     *    smaller than the original dimensions, and generally only when
     *    the {@code BILINEAR} hint is specified)
     * @return a scaled version of the original {@code BufferedImage}
     */
    private BufferedImage getScaledInstance(BufferedImage img,
                                            int targetWidth,
                                            int targetHeight,
                                            Object hint,
                                            boolean higherQuality) {

        int type = (img.getTransparency() == Transparency.OPAQUE) ?
                BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = (BufferedImage)img;
        int w, h;
        if (higherQuality) {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = img.getWidth();
            h = img.getHeight();
        }
        else {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = targetWidth;
            h = targetHeight;
        }

        do {
            if (higherQuality && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (higherQuality && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        }
        while (w != targetWidth || h != targetHeight);

        return ret;
    }

    private String buildNameFromX3dMaterial(X3DMaterial x3dMaterial) {
        String name = NO_TEXIMAGE;
        if (x3dMaterial.isSetAmbientIntensity()) {
            name = name + "_ai_" + x3dMaterial.getAmbientIntensity();
        }
        if (x3dMaterial.isSetShininess()) {
            name = name + "_sh_" + x3dMaterial.getShininess();
        }
        if (x3dMaterial.isSetTransparency()) {
            name = name + "_tr_" + x3dMaterial.getTransparency();
        }
        if (x3dMaterial.isSetDiffuseColor()) {
            name = name + "_dc_r_" + x3dMaterial.getDiffuseColor().getRed()
                    + "_g_" + x3dMaterial.getDiffuseColor().getGreen()
                    + "_b_" + x3dMaterial.getDiffuseColor().getBlue();
        }
        if (x3dMaterial.isSetSpecularColor()) {
            name = name + "_sc_r_" + x3dMaterial.getSpecularColor().getRed()
                    + "_g_" + x3dMaterial.getSpecularColor().getGreen()
                    + "_b_" + x3dMaterial.getSpecularColor().getBlue();
        }
        if (x3dMaterial.isSetEmissiveColor()) {
            name = name + "_ec_r_" + x3dMaterial.getEmissiveColor().getRed()
                    + "_g_" + x3dMaterial.getEmissiveColor().getGreen()
                    + "_b_" + x3dMaterial.getEmissiveColor().getBlue();
        }
        return name;
    }

    protected double reducePrecisionForXorY (double originalValue) {
        return Math.rint(originalValue * PRECISION) / PRECISION;
    }

    protected double reducePrecisionForZ (double originalValue) {
        return Math.rint(originalValue * PRECISION) / PRECISION;
    }

    protected List<PlacemarkType> createPlacemarksForFootprint(List<BuildingSurface> result, KmlSplittingResult work) throws Exception {

    	List<PlacemarkType> placemarkList = new ArrayList<PlacemarkType>();
    	PlacemarkType placemark = kmlFactory.createPlacemarkType();
    	placemark.setName(work.getGmlId());
    	placemark.setId(DisplayForm.FOOTPRINT_PLACEMARK_ID + placemark.getName());

    	if (work.getDisplayForm().isHighlightingEnabled()) {
    		placemark.setStyleUrl("#" + getStyleBasisName() + DisplayForm.FOOTPRINT_STR + "Style");
    	}
    	else {
    		placemark.setStyleUrl("#" + getStyleBasisName() + DisplayForm.FOOTPRINT_STR + "Normal");
    	}

    	if (getBalloonSettings().isIncludeDescription()) {
    		addBalloonContents(placemark, work);
    	}
    	MultiGeometryType multiGeometry = kmlFactory.createMultiGeometryType();
    	placemark.setAbstractGeometryGroup(kmlFactory.createMultiGeometry(multiGeometry));

    	PolygonType polygon = null;
    	try {

    		for (BuildingSurface Row: result) {

    			if (Row != null && Row.getType().equals("GroundSurface")) {

    				eventDispatcher.triggerEvent(new GeometryCounterEvent(null, this));

    				@SuppressWarnings("unchecked")
    				List<Double> _Geometry = (List<Double>)Row.getGeometry();
    				org.postgis.Point[] tmpPoint = new org.postgis.Point[_Geometry.size()/3];
    				for (int i = 1,j = 0; i < _Geometry.size(); j++, i = i+3) {
    					List<Double> Target_Coordinates = ProjConvertor.transformPoint(_Geometry.get(i-1),_Geometry.get(i),_Geometry.get(i+1), work.getTargetSrs(), "4326");
    					tmpPoint[j] = new org.postgis.Point(
    							Target_Coordinates.get(1),
    							Target_Coordinates.get(0),
    							Target_Coordinates.get(2));
    				}
    				Polygon surface = new Polygon(new org.postgis.LinearRing[] {new org.postgis.LinearRing(tmpPoint)});

    				int dim = surface.getDimension();

    				for (int i = 0; i < surface.numRings(); i++) {

    					LinearRingType linearRing = kmlFactory.createLinearRingType();
    					BoundaryType boundary = kmlFactory.createBoundaryType();
    					boundary.setLinearRing(linearRing);
    					if (surface.getSubGeometry(i).type == org.postgis.Geometry.LINEARRING) {

    						polygon = kmlFactory.createPolygonType();
    						polygon.setTessellate(true);
    						polygon.setExtrude(false);
    						polygon.setAltitudeModeGroup(kmlFactory.createAltitudeMode(AltitudeModeEnumType.CLAMP_TO_GROUND));
    						polygon.setOuterBoundaryIs(boundary);
    						multiGeometry.getAbstractGeometryGroup().add(kmlFactory.createPolygon(polygon));

    					} else
    						polygon.getInnerBoundaryIs().add(boundary);

    					double[] ordinatesArray = new double[surface.numPoints()*3];
    					for (int temp = 0, j = 0; temp < surface.numPoints(); temp++, j+=3){
    						ordinatesArray[j] = surface.getPoint(temp).x;
    						ordinatesArray[j+1] = surface.getPoint(temp).y;
    						ordinatesArray[j+2] = surface.getPoint(temp).z;
    					}

    					for (int j = 0; j < ordinatesArray.length; j = j+dim)
    						linearRing.getCoordinates().add(String.valueOf(ordinatesArray[j] + "," + ordinatesArray[j+1] + ",0"));
    				}
    			}
    		}

    		if (polygon != null) { // if there is at least some content
    			placemarkList.add(placemark);
    		}

    	} catch (Exception e) {
    		Logger.getInstance().error(e.toString());
    	}
    	return placemarkList;       
    }
    

    protected List<PlacemarkType> createPlacemarksForExtruded(List<BuildingSurface> result,
                                                              KmlSplittingResult work,
                                                              double measuredHeight,
                                                              boolean reversePointOrder) throws Exception {

        List<PlacemarkType> placemarkList = new ArrayList<PlacemarkType>();
        PlacemarkType placemark = kmlFactory.createPlacemarkType();
        placemark.setName(work.getGmlId());
        placemark.setId(DisplayForm.EXTRUDED_PLACEMARK_ID + placemark.getName());
        try {
            if (work.getDisplayForm().isHighlightingEnabled()) {
                placemark.setStyleUrl("#" + getStyleBasisName() + DisplayForm.EXTRUDED_STR + "Style");
            }
            else {
                placemark.setStyleUrl("#" + getStyleBasisName() + DisplayForm.EXTRUDED_STR + "Normal");
            }
            if (getBalloonSettings().isIncludeDescription()) {
                addBalloonContents(placemark, work);
            }
            MultiGeometryType multiGeometry = kmlFactory.createMultiGeometryType();
            placemark.setAbstractGeometryGroup(kmlFactory.createMultiGeometry(multiGeometry));

            PolygonType polygon = null;            
            for (BuildingSurface Row: result) {

            	if (Row != null && Row.getType().equals("GroundSurface")) {
            		
            		eventDispatcher.triggerEvent(new GeometryCounterEvent(null, this));
            		
            		@SuppressWarnings("unchecked")
                    List<Double> _Geometry = (List<Double>)Row.getGeometry();
                    org.postgis.Point[] tmpPoint = new org.postgis.Point[_Geometry.size()/3];
                    for (int i = 1,j = 0; i < _Geometry.size(); j++, i = i+3) {
                        List<Double> Target_Coordinates = ProjConvertor.transformPoint(_Geometry.get(i-1),_Geometry.get(i),_Geometry.get(i+1), work.getTargetSrs(), "4326");
                        tmpPoint[j] = new org.postgis.Point(
                                Target_Coordinates.get(1),
                                Target_Coordinates.get(0),
                                Target_Coordinates.get(2));
                    }
                    Polygon surface = new Polygon(new org.postgis.LinearRing[] {new org.postgis.LinearRing(tmpPoint)});
                    
            		int dim = surface.getDimension();
            		
            		for (int i = 0; i < surface.numRings(); i++) {
            		
            			LinearRingType linearRing = kmlFactory.createLinearRingType();
            			BoundaryType boundary = kmlFactory.createBoundaryType();
            			boundary.setLinearRing(linearRing);
            			if (surface.getSubGeometry(i).type == org.postgis.Geometry.LINEARRING) {
            			
            				polygon = kmlFactory.createPolygonType();
            				polygon.setTessellate(true);
            				polygon.setExtrude(true);
            				polygon.setAltitudeModeGroup(kmlFactory.createAltitudeMode(AltitudeModeEnumType.RELATIVE_TO_GROUND));
            				polygon.setOuterBoundaryIs(boundary);
            				multiGeometry.getAbstractGeometryGroup().add(kmlFactory.createPolygon(polygon));
            			
            			} else
            				polygon.getInnerBoundaryIs().add(boundary);
            			
            			double[] ordinatesArray = new double[surface.numPoints()*3];
                        for (int temp = 0, j = 0; temp < surface.numPoints(); temp++, j+=3){
                            ordinatesArray[j] = surface.getPoint(temp).x;
                            ordinatesArray[j+1] = surface.getPoint(temp).y;
                            ordinatesArray[j+2] = surface.getPoint(temp).z;
                        }
            			
            			if (reversePointOrder) {
            				for (int j = 0; j < ordinatesArray.length; j = j+dim)
            					linearRing.getCoordinates().add(String.valueOf(ordinatesArray[j] + "," + ordinatesArray[j+1] + "," + measuredHeight));
            			} else if (polygon != null)
            				// order points counter-clockwise
            				for (int j = ordinatesArray.length - dim; j >= 0; j = j-dim)
            					linearRing.getCoordinates().add(String.valueOf(ordinatesArray[j] + "," + ordinatesArray[j+1] + "," + measuredHeight));
            		}
            	}
            }

            
            if (polygon != null) { // if there is at least some content
                placemarkList.add(placemark);
            }
        } catch (Exception e) {
            Logger.getInstance().error(e.toString());
        }


        return placemarkList;
    }

    
    protected List<PlacemarkType> createPlacemarksForGeometry(List<BuildingSurface> rs,
                                                              KmlSplittingResult work) throws Exception{
        return createPlacemarksForGeometry(rs, work, false, false);
    }

    
    protected List<PlacemarkType> createPlacemarksForGeometry(List<BuildingSurface> result,
                                                              KmlSplittingResult work,
                                                              boolean includeGroundSurface,
                                                              boolean includeClosureSurface) throws Exception {


        HashMap<String, MultiGeometryType> multiGeometries = new HashMap<String, MultiGeometryType>();
        MultiGeometryType multiGeometry = null;
        PolygonType polygon = null;

        double zOffset = getZOffsetFromDBorConfig(work.getGmlId(),work.GetElevation());
        if (zOffset == Double.MAX_VALUE) {
            List<Point3d> lowestPointCandidates = getLowestPointsCoordinates(result,  work);
            zOffset = getZOffsetFromGEService(work.getGmlId(),lowestPointCandidates,work.getTargetSrs(),work.GetElevation());
        }
        

        for (BuildingSurface Row: result) {

            String surfaceType = (String)Row.getType();
            if (surfaceType != null && !surfaceType.endsWith("Surface")) {
                surfaceType = surfaceType + "Surface";
            }
            if ((!includeGroundSurface && TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.BUILDING_GROUND_SURFACE).toString().equalsIgnoreCase(surfaceType)) ||
                    (!includeClosureSurface && TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.BUILDING_CLOSURE_SURFACE).toString().equalsIgnoreCase(surfaceType)))	{
                continue;
            }


            @SuppressWarnings("unchecked")
            List<Double> _Geometry = (List<Double>)Row.getGeometry();

            org.postgis.Point[] tmpPoint = new org.postgis.Point[_Geometry.size()/3];

            for (int i = 1,j = 0; i < _Geometry.size(); j++, i = i+3) {

                List<Double> Target_Coordinates = ProjConvertor.transformPoint(_Geometry.get(i-1),_Geometry.get(i),_Geometry.get(i+1), work.getTargetSrs(), "4326");
                tmpPoint[j] = new org.postgis.Point(
                        Target_Coordinates.get(1),
                        Target_Coordinates.get(0),
                        Target_Coordinates.get(2));
            }

            Polygon surface = new Polygon(new org.postgis.LinearRing[] {new org.postgis.LinearRing(tmpPoint)});
            double[] ordinatesArray = new double[surface.numPoints()*3];

            for (int i = 0, j = 0; i < surface.numPoints(); i++, j+=3){
                ordinatesArray[j] = surface.getPoint(i).x;
                ordinatesArray[j+1] = surface.getPoint(i).y;
                ordinatesArray[j+2] = surface.getPoint(i).z;
            }

            eventDispatcher.triggerEvent(new GeometryCounterEvent(null, this));
            //		eventDispatcher.triggerEvent(new CounterEvent(CounterType.TOPLEVEL_FEATURE,counter , this));

            polygon = kmlFactory.createPolygonType();
         
            switch (config.getAltitudeMode()) {
                case ABSOLUTE:
                    polygon.setAltitudeModeGroup(kmlFactory.createAltitudeMode(AltitudeModeEnumType.ABSOLUTE));
                    break;
                case RELATIVE:
                    polygon.setAltitudeModeGroup(kmlFactory.createAltitudeMode(AltitudeModeEnumType.RELATIVE_TO_GROUND));
                    break;
            }


            // just in case surfaceType == null
            boolean probablyRoof = true;
            double nx = 0;
            double ny = 0;
            double nz = 0;
            int cellCount = 0;

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
                            + reducePrecisionForZ(ordinatesArray[j+2] + zOffset)));

                    if (currentLod == 1) { // calculate normal
                        int current = j;
                        int next = j+3;
                        if (next >= ordinatesArray.length) next = 0;
                        nx = nx + ((ordinatesArray[current+1] - ordinatesArray[next+1]) * (ordinatesArray[current+2] + ordinatesArray[next+2]));
                        ny = ny + ((ordinatesArray[current+2] - ordinatesArray[next+2]) * (ordinatesArray[current] + ordinatesArray[next]));
                        nz = nz + ((ordinatesArray[current] - ordinatesArray[next]) * (ordinatesArray[current+1] + ordinatesArray[next+1]));
                    }
                }
                cellCount += (surface.getRing(i).numPoints()*3);
            }

            if (currentLod == 1) { // calculate normal
                double value = Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (value == 0) { // not a surface, but a line
                    continue;
                }
                nx = nx / value;
                ny = ny / value;
                nz = nz / value;
            }

            if (surfaceType == null) {
                surfaceType = TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.BUILDING_WALL_SURFACE).toString();
                switch (currentLod) {
                    case 1:
                        if (probablyRoof && (nz > 0.999)) {
                            surfaceType = TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.BUILDING_ROOF_SURFACE).toString();
                        }
                        break;
                    case 2:
                        if (probablyRoof) {
                            surfaceType = TypeAttributeValueEnum.fromCityGMLClass(CityGMLClass.BUILDING_ROOF_SURFACE).toString();
                        }
                        break;
                }
            }

            multiGeometry = multiGeometries.get(surfaceType);
            if (multiGeometry == null) {
                multiGeometry = kmlFactory.createMultiGeometryType();
                multiGeometries.put(surfaceType, multiGeometry);
            }
            multiGeometry.getAbstractGeometryGroup().add(kmlFactory.createPolygon(polygon));
        }

        List<PlacemarkType> placemarkList = new ArrayList<PlacemarkType>();
        Set<String> keySet = multiGeometries.keySet();
        Iterator<String> iterator = keySet.iterator();
        while (iterator.hasNext()) {
            String surfaceType = iterator.next();
            PlacemarkType placemark = kmlFactory.createPlacemarkType();
            placemark.setName(work.getGmlId() + "_" + surfaceType);
            placemark.setId(DisplayForm.GEOMETRY_PLACEMARK_ID + placemark.getName());
            if (work.isBuilding())
                placemark.setStyleUrl("#" + surfaceType + "Normal");
            else
                placemark.setStyleUrl("#" + getStyleBasisName() + DisplayForm.GEOMETRY_STR + "Normal");
            if (getBalloonSettings().isIncludeDescription() &&
                    !work.getDisplayForm().isHighlightingEnabled()) { // avoid double description
                addBalloonContents(placemark, work);
            }
            multiGeometry = multiGeometries.get(surfaceType);
            placemark.setAbstractGeometryGroup(kmlFactory.createMultiGeometry(multiGeometry));
            placemarkList.add(placemark);
        }
        return placemarkList;
    }

    protected void fillGenericObjectForCollada(KmlSplittingResult work ,
    		List<BuildingSurface> _SurfaceList ,
            SurfaceAppearance _SurfaceAppearance,
            List<BuildingSurface> _ParentSurfaceList) throws Exception {

        String selectedTheme = config.getAppearanceTheme();
        String filePath=GetImagePath();
        int texImageCounter = 0;

        try {

            for(BuildingSurface Row:_ParentSurfaceList){
                String parentid= String.valueOf(Row.getPId());
                String id = Row.getId();
                Map<String, Object> tmpResult = _SurfaceAppearance.GetAppearanceBySurfaceID("#" + id , work.getAppearanceList() , selectedTheme);
                String AppreanceType = (String)tmpResult.get("type");

                if(AppreanceType != null){
                    if(AppreanceType.equals("X3D_MATERIAL"))
                    {
                        X3DMaterial x3dMaterial = new X3DMaterial();
                        fillX3dMaterialValues(x3dMaterial, tmpResult);
                        addX3dMaterial(parentid, x3dMaterial);
                    }
                }
            }

            for (BuildingSurface Row: _SurfaceList)  {

                Map<String, Object> _AppResult = _SurfaceAppearance.GetAppearanceBySurfaceID("#" + Row.getId() , work.getAppearanceList() , selectedTheme);
                String surfaceId = Row.getId();
                String parentId = String.valueOf(Row.getPId());

                // from here on it is a surfaceMember
                eventDispatcher.triggerEvent(new GeometryCounterEvent(null, this));
                String texImageUri = null;
                InputStream texImage = null;
                StringTokenizer texCoordsTokenized = null;

                if (_AppResult.get("type") == null) {

                    if(getX3dMaterial(parentId) != null)  {
                        addX3dMaterial(surfaceId, getX3dMaterial(parentId));
                    }
                    else {

                        if (getX3dMaterial(surfaceId) == null) {
                            defaultX3dMaterial = new X3DMaterial();
                            defaultX3dMaterial.setAmbientIntensity(0.2d);
                            defaultX3dMaterial.setShininess(0.2d);
                            defaultX3dMaterial.setTransparency(0d);
                            defaultX3dMaterial.setDiffuseColor(getX3dColorFromString("0.8 0.8 0.8"));
                            defaultX3dMaterial.setSpecularColor(getX3dColorFromString("1.0 1.0 1.0"));
                            defaultX3dMaterial.setEmissiveColor(getX3dColorFromString("0.0 0.0 0.0"));
                            addX3dMaterial(surfaceId, defaultX3dMaterial);
                        }
                    }
                }
                else{

                	texImageUri = (_AppResult.get("imageuri") != null) ? _AppResult.get("imageuri").toString() : null;
                	String texCoords = (_AppResult.get("coord") != null) ? _AppResult.get("coord").toString() : null;

                	if (texImageUri != null && texImageUri.trim().length() != 0 &&  texCoords != null && texCoords.trim().length() != 0) {

                		String finalImagePath = filePath + "\\" + texImageUri;
                		int fileSeparatorIndex = Math.max(texImageUri.lastIndexOf("\\"), texImageUri.lastIndexOf("/"));
                		texImageUri = "_" + texImageUri.substring(fileSeparatorIndex + 1);    
                		addTexImageUri(surfaceId, texImageUri);

                		if (getTexImage(texImageUri) == null) {

                			texImage = new BufferedInputStream(new FileInputStream(finalImagePath));

                			BufferedImage bufferedImage = null;

                			try {

                				String imageFileExtension = finalImagePath.substring(finalImagePath.lastIndexOf(".") + 1).toLowerCase();
                				if(imageFileExtension.equals("tif") || imageFileExtension.equals("tiff"))// this is just for reading tiff images
                					bufferedImage = TiffToJpg(finalImagePath);
                				else 
                					bufferedImage = ImageIO.read(texImage);

                			}                            
                			catch (Exception ioe) {
                				Logger.getInstance().error(ioe.toString());
                			}

                			if (bufferedImage != null) { // image in JPEG, PNG or another usual format
                				addTexImage(texImageUri, bufferedImage);
                			}

                			texImageCounter++;
                			if (texImageCounter > 20) {
                				eventDispatcher.triggerEvent(new CounterEvent(CounterType.TEXTURE_IMAGE, texImageCounter, this));
                				texImageCounter = 0;
                			}
                		}

                		texCoords = texCoords.replaceAll(";", " "); // substitute of ; for internal ring
                		texCoordsTokenized = new StringTokenizer(texCoords.trim(), " ");
                	}
                	else {
                		X3DMaterial x3dMaterial = new X3DMaterial();
                		fillX3dMaterialValues(x3dMaterial, _AppResult);
                		// x3dMaterial will only added if not all x3dMaterial members are null
                		addX3dMaterial(surfaceId, x3dMaterial);
                		if (getX3dMaterial(surfaceId) == null) {
                			// untextured surface and no x3dMaterial -> default x3dMaterial (gray)
                			addX3dMaterial(surfaceId, defaultX3dMaterial);

                		}
                    }
                }

                @SuppressWarnings("unchecked")
                List<Double> _Geometry = (List<Double>)Row.getGeometry();

                org.postgis.Point[] tmpPoint = new org.postgis.Point[_Geometry.size()/3];

                for (int i = 1,j = 0; i < _Geometry.size(); j++, i = i+3) {

                    List<Double> Target_Coordinates = ProjConvertor.transformPoint(_Geometry.get(i-1),_Geometry.get(i),_Geometry.get(i+1), work.getTargetSrs(), work.getTargetSrs());
                    tmpPoint[j] = new org.postgis.Point(
                            Target_Coordinates.get(1),
                            Target_Coordinates.get(0),
                            Target_Coordinates.get(2)
                    );

                }

                Polygon surface = new Polygon(
                        new org.postgis.LinearRing[] {
                                new org.postgis.LinearRing(tmpPoint)
                        });



                List<VertexInfo> vertexInfos = new ArrayList<VertexInfo>();

                int ringCount = surface.numRings();
                int[] vertexCount = new int[ringCount];

                for (int i = 0; i < surface.numRings(); i++) {

                    double[] ordinatesArray = new double[surface.numPoints()*3];
                    for (int temp = 0, j = 0; temp < surface.numPoints(); temp++, j+=3){
                        ordinatesArray[j] = surface.getPoint(temp).x;
                        ordinatesArray[j+1] = surface.getPoint(temp).y;
                        ordinatesArray[j+2] = surface.getPoint(temp).z;
                    }

                    int vertices = 0;

                    for (int j = 0; j < ordinatesArray.length - 3; j = j+3) {

                        // calculate origin and list of lowest points
                        updateOrigins(ordinatesArray[j], ordinatesArray[j + 1], ordinatesArray[j + 2]);

                        // get or create node in vertex info tree
                        VertexInfo vertexInfo = setVertexInfoForXYZ(surfaceId,
                                ordinatesArray[j],
                                ordinatesArray[j+1],
                                ordinatesArray[j+2]);

                        if (texCoordsTokenized != null && texCoordsTokenized.hasMoreTokens()) {
                            double s = Double.parseDouble(texCoordsTokenized.nextToken());
                            double t = Double.parseDouble(texCoordsTokenized.nextToken());
                            vertexInfo.addTexCoords(surfaceId, new TexCoords(s, t));
                        }

                        vertexInfos.add(vertexInfo);
                        vertices++;
                    }

                    vertexCount[i] = vertices;

                    if (texCoordsTokenized != null && texCoordsTokenized.hasMoreTokens()) {
                        texCoordsTokenized.nextToken(); // geometryInfo ignores last point in a polygon
                        texCoordsTokenized.nextToken(); // keep texture coordinates in sync
                    }
                }

                addSurfaceInfo(surfaceId, new SurfaceInfo(ringCount, vertexCount, vertexInfos));
            }
        }
        catch (Exception Ex) {
            Logger.getInstance().error("The error while querying city object: " + Ex.toString());
        }
        finally {
        		
        }
        eventDispatcher.triggerEvent(new CounterEvent(CounterType.TEXTURE_IMAGE, texImageCounter, this));
    }

    public PlacemarkType createPlacemarkForColladaModel(KmlSplittingResult work) throws Exception {

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
                addBalloonContents(placemark, work);
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

        location.setLatitude(this.location.y);
        location.setLongitude(this.location.x);
        location.setAltitude(this.location.z);
        model.setLocation(location);

        // correct heading value
        double lat1 = Math.toRadians(this.location.y);
        // undo trick for very close coordinates
        List<Double> dummy = ProjConvertor.transformPoint(
                origin.getX(),
                origin.getY() - 20,
                origin.getZ(),
                work.getTargetSrs(),
                "4326");
        double lat2 = Math.toRadians(dummy.get(0));
        double dLon = Math.toRadians(dummy.get(1) - this.location.x);
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1)*Math.sin(lat2) - Math.sin(lat1)*Math.cos(lat2)*Math.cos(dLon);
        double bearing = Math.toDegrees(Math.atan2(y, x));
        bearing = (bearing + 180) % 360;



        OrientationType orientation = kmlFactory.createOrientationType();
        orientation.setHeading(reducePrecisionForZ(bearing));
        model.setOrientation(orientation);

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


    protected List<PlacemarkType> createPlacemarksForHighlighting(List<BuildingSurface> result , KmlSplittingResult work) throws SQLException {

    	List<PlacemarkType> placemarkList= new ArrayList<PlacemarkType>();

        PlacemarkType placemark = kmlFactory.createPlacemarkType();
        placemark.setStyleUrl("#" + getStyleBasisName() + work.getDisplayForm().getName() + "Style");
        placemark.setName(work.getGmlId());
        placemark.setId(DisplayForm.GEOMETRY_HIGHLIGHTED_PLACEMARK_ID + placemark.getName());
        placemarkList.add(placemark);

        if (getBalloonSettings().isIncludeDescription()) {
            addBalloonContents(placemark, work);
        }

        MultiGeometryType multiGeometry =  kmlFactory.createMultiGeometryType();
        placemark.setAbstractGeometryGroup(kmlFactory.createMultiGeometry(multiGeometry));

        double hlDistance = work.getDisplayForm().getHighlightingDistance();

        try {
        	double zOffset = getZOffsetFromDBorConfig(work.getGmlId(),work.GetElevation());
            if (zOffset == Double.MAX_VALUE) {
                List<Point3d> lowestPointCandidates = getLowestPointsCoordinates(result,  work);
                zOffset = getZOffsetFromGEService(work.getGmlId(),lowestPointCandidates,work.getTargetSrs(),work.GetElevation());
            }

            for (BuildingSurface Row: result) {
            	
            	List<Double> _Geometry = (List<Double>)Row.getGeometry();
                org.postgis.Point[] tmpPoint = new org.postgis.Point[_Geometry.size()/3];
                for (int i = 1,j = 0; i < _Geometry.size(); j++, i = i+3) {
                   tmpPoint[j] = new org.postgis.Point(_Geometry.get(i-1),_Geometry.get(i),_Geometry.get(i+1));
                }
                Polygon unconvertedSurface = new Polygon(new org.postgis.LinearRing[] {new org.postgis.LinearRing(tmpPoint)});
            	

                double[] ordinatesArray = new double[unconvertedSurface.numPoints()*3];
                for (int i = 0, j = 0; i < unconvertedSurface.numPoints(); i++, j+=3){
                    ordinatesArray[j] = unconvertedSurface.getPoint(i).x;
                    ordinatesArray[j+1] = unconvertedSurface.getPoint(i).y;
                    ordinatesArray[j+2] = unconvertedSurface.getPoint(i).z;
                }

                int contourCount = unconvertedSurface.numRings();
                // remove normal-irrelevant points
                int startContour1 = 0;
                int endContour1 = (contourCount == 1) ?
                        ordinatesArray.length: // last
                        (unconvertedSurface.getRing(startContour1).numPoints()*3); // holes are irrelevant for normal calculation
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

                for (int i = 0, j = 0; i < unconvertedSurface.numPoints(); i++, j+=3){
                    unconvertedSurface.getPoint(i).x = ordinatesArray[j] + hlDistance * nx;
                    unconvertedSurface.getPoint(i).y = ordinatesArray[j+1] + hlDistance * ny;
                    unconvertedSurface.getPoint(i).z = ordinatesArray[j+2] + zOffset + hlDistance * nz;
                }

                // now convert to WGS84
                org.postgis.Point[] tmpPointWGS84 = new org.postgis.Point[_Geometry.size()/3];
                for (int i = 0; i < unconvertedSurface.numPoints(); i++){
                	List<Double> Target_Coordinates = ProjConvertor.transformPoint(
                			unconvertedSurface.getPoint(i).x,
                			unconvertedSurface.getPoint(i).y,
                			unconvertedSurface.getPoint(i).z,
                			work.getTargetSrs(),
                			"4326");
                	tmpPointWGS84[i] = new org.postgis.Point(
                            Target_Coordinates.get(1),
                            Target_Coordinates.get(0),
                            Target_Coordinates.get(2));
                }
                Polygon surface = new Polygon(new org.postgis.LinearRing[] {new org.postgis.LinearRing(tmpPointWGS84)});
                //End of conversion

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

                for (int i = 0; i < surface.numRings(); i++){
                    LinearRingType linearRing = kmlFactory.createLinearRingType();
                    BoundaryType boundary = kmlFactory.createBoundaryType();
                    boundary.setLinearRing(linearRing);
                    if (i == 0) { // EXTERIOR_POLYGON_RING
                        polygon.setOuterBoundaryIs(boundary);
                    }
                    else { // INTERIOR_POLYGON_RING
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
            Logger.getInstance().warn("Exception when generating highlighting geometry of object " + work.getGmlId());
            e.printStackTrace();
        }

        return placemarkList;
    }

    private String getBalloonContentFromGenericAttribute(KmlSplittingResult work) {

        String balloonContent = null;
        String genericAttribName = "Balloon_Content";

        try {
        	
        	AbstractCityObject cityObject = (AbstractCityObject)work.getCityGmlClass();
        	if(cityObject.isSetGenericAttribute())
        	{
        		for(AbstractGenericAttribute genericAttribute :  cityObject.getGenericAttribute()){
        			if (genericAttribute.getCityGMLClass() == CityGMLClass.GENERIC_ATTRIBUTE_SET) {
        				GenericAttributeSet set = (GenericAttributeSet)genericAttribute;
        				for(AbstractGenericAttribute subGenericAttribute :  set.getGenericAttribute()){
        	    			if(subGenericAttribute.getCityGMLClass() == CityGMLClass.STRING_ATTRIBUTE && subGenericAttribute.isSetName()){
        	    				if(subGenericAttribute.getName().equals(genericAttribName)){
        	    					StringAttribute stringAttribute = (StringAttribute)subGenericAttribute;
        	    					if (stringAttribute.isSetValue())
        	    						balloonContent = stringAttribute.getValue();
        	    				}
        	    			}
        	    		}
        					
        			}else{
        				if(genericAttribute.getCityGMLClass() == CityGMLClass.STRING_ATTRIBUTE && genericAttribute.isSetName()){
            				if(genericAttribute.getName().equals(genericAttribName)){
            					StringAttribute stringAttribute = (StringAttribute)genericAttribute;
            					if (stringAttribute.isSetValue())
            						balloonContent = stringAttribute.getValue();
            				}
            			}
        			}
        		}
        	}
        }
        catch (Exception ex) {}
        
        return balloonContent;
    }
   
    
    protected void addBalloonContents(PlacemarkType placemark, KmlSplittingResult work) {
    	

        try {
            switch (getBalloonSettings().getBalloonContentMode()) {
                case GEN_ATTRIB:
                    String balloonTemplate = getBalloonContentFromGenericAttribute(work);
                    if (balloonTemplate != null) {
                        if (balloonTemplateHandler == null) { // just in case
                            balloonTemplateHandler = new BalloonTemplateHandlerImpl((File) null, connection);
                        }
                        placemark.setDescription(balloonTemplateHandler.getBalloonContent(balloonTemplate, id, currentLod));
                    }
                    break;
                case GEN_ATTRIB_AND_FILE:
                    balloonTemplate = getBalloonContentFromGenericAttribute(work);
                    if (balloonTemplate != null) {
                        placemark.setDescription(balloonTemplateHandler.getBalloonContent(balloonTemplate, id, currentLod));
                        break;
                    }
                case FILE :
                    if (balloonTemplateHandler != null) {
                        placemark.setDescription(balloonTemplateHandler.getBalloonContent(id, currentLod));
                    }
                    break;
            }
        }
        catch (Exception e) { } // invalid balloons are silently discarded
    }

    protected void fillX3dMaterialValues (X3DMaterial x3dMaterial, Map<String, Object> rs) throws SQLException {


        Double ambientIntensity = (Double)rs.get("x3d_ambient_intensity");

        if (ambientIntensity!=null) {
            x3dMaterial.setAmbientIntensity(ambientIntensity);
        }


        Double shininess = (Double)rs.get("x3d_shininess");
        if (shininess!=null) {
            x3dMaterial.setShininess(shininess);
        }


        Double transparency = (Double)rs.get("x3d_transparency");
        if (transparency != null) {
            x3dMaterial.setTransparency(transparency);
        }


        Color color = (Color)rs.get("x3d_diffuse_color");
        if (color != null) {
            x3dMaterial.setDiffuseColor(color);
        }

        color = (Color)rs.get("x3d_specular_color");
        if (color != null) {
            x3dMaterial.setSpecularColor(color);

        }


        color = (Color)rs.get("x3d_emissive_color");
        if (color != null) {
            x3dMaterial.setEmissiveColor(color);
        }


        x3dMaterial.setIsSmooth((Boolean) rs.get("x3d_is_smooth") == true);


    }


    private Color getX3dColorFromString(String colorString) {
        Color color = null;
        if (colorString != null) {
            List<Double> colorList = Util.string2double(colorString, "\\s+");

            if (colorList != null && colorList.size() >= 3) {
                color = new Color(colorList.get(0), colorList.get(1), colorList.get(2));
            }
        }
        return color;
    }


    protected double getZOffsetFromDBorConfig (String id , ElevationHelper Elevation) throws SQLException {

        double zOffset = Double.MAX_VALUE;
        
        switch (config.getAltitudeOffsetMode()) {
	        case NO_OFFSET:
				zOffset = 0;
				break;
			case CONSTANT:
				zOffset = config.getAltitudeOffsetValue();
				break;
			case GENERIC_ATTRIBUTE:
		        if(Elevation.IsTableCreated())
		        {
		            ResultSet rs = Elevation.SelectElevationOffSet(id, 0);
		            while ( rs.next() ) {
		                zOffset = rs.getDouble("zoffset");
		            }
		        }
        }
        return zOffset;
    }
    

    protected double getZOffsetFromGEService (String gmlId, List<Point3d> candidates, String _TargetSrs , ElevationHelper Elevation) {

        double zOffset = Double.MAX_VALUE;

        try{
            double[] coords = new double[candidates.size()*3];
            int index = 0;
            for (Point3d point3d: candidates) {
                // undo trick for very close coordinates
                List<Double> tmpPointList = ProjConvertor.transformPoint(point3d.x / 100 , point3d.y / 100 , point3d.z / 100 , _TargetSrs , "4326");
                coords[index++] = tmpPointList.get(1);
                coords[index++] = tmpPointList.get(0);
                coords[index++] = tmpPointList.get(2);
            }

            Logger.getInstance().info("Getting zOffset from Google's elevation API with " + candidates.size() + " points.");

            zOffset = elevationServiceHandler.getZOffset(coords);
            if(!Elevation.IsTableCreated())
                Elevation.CreateElevationTable(0);
            Elevation.InsertElevationOffSet(gmlId , zOffset , 0);
        }

        catch (Exception e) {

            Logger.getInstance().error(e.toString());

        }

        return zOffset;
    }
    
    
    private BufferedImage TiffToJpg(String tiff) throws IOException    	    
    {
      File tiffFile = new File(tiff);
      SeekableStream s = new FileSeekableStream(tiffFile);
      TIFFDecodeParam param = null;
      ImageDecoder dec = ImageCodec.createImageDecoder("tiff", s, param);
      RenderedImage op = dec.decodeAsRenderedImage(0);
      return convertRenderedImage(op);
    }


    private  BufferedImage convertRenderedImage(RenderedImage img) {
    	if (img instanceof BufferedImage) {
    		return (BufferedImage) img;
    	}
    	ColorModel cm = img.getColorModel();
    	int width = img.getWidth();
    	int height = img.getHeight();
    	WritableRaster raster = cm
    			.createCompatibleWritableRaster(width, height);
    	boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
    	Hashtable properties = new Hashtable();
    	String[] keys = img.getPropertyNames();
    	if (keys != null) {
    		for (int i = 0; i < keys.length; i++) {
    			properties.put(keys[i], img.getProperty(keys[i]));
    		}
    	}
    	BufferedImage result = new BufferedImage(cm, raster,
    			isAlphaPremultiplied, properties);
    	img.copyData(raster);
    	return result;
    } 
    

    public String GetImagePath()
    {
        Internal intConfig = config.getInternal();
        directoryScanner = new DirectoryScanner(true);
        List<File> importFiles = directoryScanner.getFiles(intConfig.getImportFiles());
        return importFiles.get(0).getParent();
    }

    @SuppressWarnings("unchecked")
    protected List<Point3d> getLowestPointsCoordinates(List<BuildingSurface> result, KmlSplittingResult work) throws Exception {

        double currentlyLowestZCoordinate = Double.MAX_VALUE;

        List<Point3d> coords = new ArrayList<Point3d>();
        List<Double> ordinates = new ArrayList<Double>();


        for(BuildingSurface _row :result)
        {
            List<Double> PointList = (List<Double>)_row.getGeometry();
            //ordinates.addAll(ProjConvertor.TransformProjection(PointList.get(0), PointList.get(1), PointList.get(2), work.getTargetSrs() , "4326"));
            ordinates.addAll(PointList);
        }

        // we are only interested in the z coordinate
        for (int j = 2; j < ordinates.size(); j+=3) {
            if (ordinates.get(j) < currentlyLowestZCoordinate) {
                coords.clear();
                Point3d point3d = new Point3d(ordinates.get(j-2), ordinates.get(j-1), ordinates.get(j));
                coords.add(point3d);
                currentlyLowestZCoordinate = point3d.z;
            }
            if (ordinates.get(j) == currentlyLowestZCoordinate) {
                Point3d point3d = new Point3d(ordinates.get(j-2), ordinates.get(j-1), ordinates.get(j));
                if (!coords.contains(point3d)) {
                    coords.add(point3d);
                }
            }
        }

        for (Point3d point3d: coords) {
            point3d.x = point3d.x * 100; // trick for very close coordinates
            point3d.y = point3d.y * 100;
            point3d.z = point3d.z * 100;
        }


        return coords;
    }


    protected class Node{
        double key;
        Object value;
        Node rightArc;
        Node leftArc;

        protected Node(double key, Object value){
            this.key = key;
            this.value = value;
        }

        protected void setLeftArc(Node leftArc) {
            this.leftArc = leftArc;
        }

        protected Node getLeftArc() {
            return leftArc;
        }

        protected void setRightArc (Node rightArc) {
            this.rightArc = rightArc;
        }

        protected Node getRightArc() {
            return rightArc;
        }

    }

    protected class NodeX extends Node{
        protected NodeX(double key, Object value){
            super(key, value);
        }
    }
    protected class NodeY extends Node{
        protected NodeY(double key, Object value){
            super(key, value);
        }
    }
    protected class NodeZ extends Node{
        protected NodeZ(double key, Object value){
            super(key, value);
        }
    }


}