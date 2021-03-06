package org.citydb.plugins.CityGMLConverter.config;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import org.citydb.api.plugin.extension.config.PluginConfig;
import org.citydb.config.project.exporter.ExportFilterConfig;
import org.citydb.config.project.general.Path;
import org.citydb.plugins.CityGMLConverter.util.Sqlite.cache.CacheManager;
import org.citydb.plugins.CityGMLConverter.xlink.resolver.DBXlinkSplitter;
import org.citygml4j.util.internal.xml.SystemIDResolver;




@XmlType(name="CityKmlExportType", propOrder={
		"sourcePath",
        "resultPath",
        "filter",
		"lodToExportFrom",
		"internal",
		"tempCacheManager",
		"xlinkSplitter",
		"buildingDisplayForms",
		"buildingColladaOptions",
		"buildingBalloon",
		"waterBodyDisplayForms",
		"waterBodyColladaOptions",
		"waterBodyBalloon",
		"landUseDisplayForms",
		"landUseColladaOptions",
		"landUseBalloon",
		"vegetationDisplayForms",
		"vegetationColladaOptions",
		"vegetationBalloon",
		"transportationDisplayForms",
		"transportationColladaOptions",
		"transportationBalloon",
		"reliefDisplayForms",
		"reliefColladaOptions",
		"reliefBalloon",
		"cityFurnitureDisplayForms",
		"cityFurnitureColladaOptions",
		"cityFurnitureBalloon",
		"genericCityObjectDisplayForms",
		"genericCityObjectColladaOptions",
		"genericCityObjectBalloon",
		"cityObjectGroupDisplayForms",
		"cityObjectGroupBalloon",
		"bridgeDisplayForms",
		"bridgeColladaOptions",
		"bridgeBalloon",
		"tunnelDisplayForms",
		"tunnelColladaOptions",
		"tunnelBalloon",

		"exportAsKmz",
		"showBoundingBox",
		"showTileBorders",
		"autoTileSideLength",
		"oneFilePerObject",
		"singleObjectRegionSize",
		"viewRefreshMode",
		"viewRefreshTime",
		"writeJSONFile",
		"writeJSONPFile",
		"callbackNameJSONP",
		"appearanceTheme",
		"altitudeMode",
		"altitudeOffsetMode",
		"altitudeOffsetValue",
		"callGElevationService",
		"useOriginalZCoords",
		"system"
})
public class ConfigImpl extends PluginConfig{
	private Path sourcePath;
    private Path resultPath;
    private ExportFilterConfig filter;
	private int lodToExportFrom;
	private Internal internal;
	private CacheManager tempCacheManager;
	private DBXlinkSplitter xlinkSplitter;

	@XmlElement(name="displayForm", required=true)
	@XmlElementWrapper(name="buildingDisplayForms")	
	private List<DisplayForm> buildingDisplayForms;
	private ColladaOptions buildingColladaOptions;
	private Balloon buildingBalloon;
	@XmlElement(name="displayForm", required=true)
	@XmlElementWrapper(name="waterBodyDisplayForms")	
	private List<DisplayForm> waterBodyDisplayForms;
	private ColladaOptions waterBodyColladaOptions;
	private Balloon waterBodyBalloon;
	@XmlElement(name="displayForm", required=true)
	@XmlElementWrapper(name="landUseDisplayForms")	
	private List<DisplayForm> landUseDisplayForms;
	private ColladaOptions landUseColladaOptions;
	private Balloon landUseBalloon;
	@XmlElement(name="displayForm", required=true)
	@XmlElementWrapper(name="vegetationDisplayForms")	
	private List<DisplayForm> vegetationDisplayForms;
	private ColladaOptions vegetationColladaOptions;
	private Balloon vegetationBalloon;
	@XmlElement(name="displayForm", required=true)
	@XmlElementWrapper(name="transportationDisplayForms")	
	private List<DisplayForm> transportationDisplayForms;
	private ColladaOptions transportationColladaOptions;
	private Balloon transportationBalloon;
	@XmlElement(name="displayForm", required=true)
	@XmlElementWrapper(name="reliefDisplayForms")	
	private List<DisplayForm> reliefDisplayForms;
	private ColladaOptions reliefColladaOptions;
	private Balloon reliefBalloon;
	@XmlElement(name="displayForm", required=true)
	@XmlElementWrapper(name="cityFurnitureDisplayForms")	
	private List<DisplayForm> cityFurnitureDisplayForms;
	private ColladaOptions cityFurnitureColladaOptions;
	private Balloon cityFurnitureBalloon;
	@XmlElement(name="displayForm", required=true)
	@XmlElementWrapper(name="genericCityObjectDisplayForms")	
	private List<DisplayForm> genericCityObjectDisplayForms;
	private ColladaOptions genericCityObjectColladaOptions;
	private Balloon genericCityObjectBalloon;
	@XmlElement(name="displayForm", required=true)
	@XmlElementWrapper(name="cityObjectGroupDisplayForms")	
	private List<DisplayForm> cityObjectGroupDisplayForms;
	private Balloon cityObjectGroupBalloon;
	@XmlElement(name="displayForm", required=true)
	@XmlElementWrapper(name="bridgeDisplayForms")
	private List<DisplayForm> bridgeDisplayForms;
	private ColladaOptions bridgeColladaOptions;
	private Balloon bridgeBalloon;
	@XmlElement(name="displayForm", required=true)
	@XmlElementWrapper(name="tunnelDisplayForms")
	private List<DisplayForm> tunnelDisplayForms;
	private ColladaOptions tunnelColladaOptions;
	private Balloon tunnelBalloon;

	private boolean exportAsKmz;
	private boolean showBoundingBox;
	private boolean showTileBorders;
	private double autoTileSideLength;
	private boolean oneFilePerObject;
	private double singleObjectRegionSize;
	private String viewRefreshMode;
	private double viewRefreshTime;
	private boolean writeJSONFile;
	private boolean writeJSONPFile;
	private String callbackNameJSONP;
	private String appearanceTheme;
	private AltitudeMode altitudeMode;
	private AltitudeOffsetMode altitudeOffsetMode;
	private double altitudeOffsetValue;
	private boolean callGElevationService;
	private boolean useOriginalZCoords;
	private System system;

	

	public static final String THEME_NONE = "none";

	public ConfigImpl() {
		sourcePath = new Path();
        resultPath = new Path();
        filter = new ExportFilterConfig();
		lodToExportFrom = 2;

		setBuildingDisplayForms(new ArrayList<DisplayForm>());
		setBuildingColladaOptions(new ColladaOptions());
		setBuildingBalloon(new Balloon());
		setWaterBodyDisplayForms(new ArrayList<DisplayForm>());
		setWaterBodyColladaOptions(new ColladaOptions());
		setWaterBodyBalloon(new Balloon());
		setLandUseDisplayForms(new ArrayList<DisplayForm>());
		setLandUseColladaOptions(new ColladaOptions());
		setLandUseBalloon(new Balloon());
		setVegetationDisplayForms(new ArrayList<DisplayForm>());
		setVegetationColladaOptions(new ColladaOptions());
		setVegetationBalloon(new Balloon());
		setTransportationDisplayForms(new ArrayList<DisplayForm>());
		setTransportationColladaOptions(new ColladaOptions());
		setTransportationBalloon(new Balloon());
		setReliefDisplayForms(new ArrayList<DisplayForm>());
		setReliefColladaOptions(new ColladaOptions());
		setReliefBalloon(new Balloon());
		setCityFurnitureDisplayForms(new ArrayList<DisplayForm>());
		setCityFurnitureColladaOptions(new ColladaOptions());
		setCityFurnitureBalloon(new Balloon());
		setGenericCityObjectDisplayForms(new ArrayList<DisplayForm>());
		setGenericCityObjectColladaOptions(new ColladaOptions());
		setGenericCityObjectBalloon(new Balloon());
		setCityObjectGroupDisplayForms(new ArrayList<DisplayForm>());
		setCityObjectGroupBalloon(new Balloon());
		setBridgeDisplayForms(new ArrayList<DisplayForm>());
		setBridgeColladaOptions(new ColladaOptions());
		setBridgeBalloon(new Balloon());
		setTunnelDisplayForms(new ArrayList<DisplayForm>());
		setTunnelColladaOptions(new ColladaOptions());
		setTunnelBalloon(new Balloon());

		exportAsKmz = true;
		showBoundingBox = true;
		showTileBorders = true;
		autoTileSideLength = 125.0;
		oneFilePerObject = false;
		singleObjectRegionSize = 50.0;
		viewRefreshMode = "onRegion";
		viewRefreshTime = 1;
		writeJSONFile = false;
		writeJSONPFile = false;
		callbackNameJSONP = "";
		setAppearanceTheme(THEME_NONE);
		setAltitudeMode(AltitudeMode.ABSOLUTE);
		setAltitudeOffsetMode(AltitudeOffsetMode.GENERIC_ATTRIBUTE);
		altitudeOffsetValue = 0;
		callGElevationService = true;
		setUseOriginalZCoords(false);
		//system = new System();
		internal = new Internal();
	}

    public Path getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(Path sourcePath) {
        sourcePath = sourcePath;
    }

    public Path getResultPath() {
        return resultPath;
    }

    public void setResultPath(Path resultPath) {
        resultPath = resultPath;
    }

    public System getSystem() {
		return system;
	}

	public void setSystem(System system) {
		if (system != null)
			this.system = system;
	}

	public void setFilter(ExportFilterConfig filter) {
		if (filter != null)
			this.filter = filter;
	}

	public ExportFilterConfig getFilter() {
		return filter;
	}

	public void setLodToExportFrom(int lodToExportFrom) {
		this.lodToExportFrom = lodToExportFrom;
	}

	public int getLodToExportFrom() {
		return lodToExportFrom;
	}
	
	public DBXlinkSplitter getXlinkSplitter() {
		return xlinkSplitter;
	}

	public void setXlinkSplitter(DBXlinkSplitter xlinkSplitter) {
		this.xlinkSplitter = xlinkSplitter;
	}

	public CacheManager getTempCacheManager() {
		return tempCacheManager;
	}

	public void setTempCacheManager(CacheManager tempCacheManager) {
		this.tempCacheManager = tempCacheManager;
	}

	public void setBuildingDisplayForms(List<DisplayForm> buildingDisplayForms) {
		this.buildingDisplayForms = buildingDisplayForms;
	}

	public List<DisplayForm> getBuildingDisplayForms() {
		return buildingDisplayForms;
	}

	public void setBuildingColladaOptions(ColladaOptions buildingColladaOptions) {
		this.buildingColladaOptions = buildingColladaOptions;
	}

	public ColladaOptions getBuildingColladaOptions() {
		return buildingColladaOptions;
	}

	public void setWaterBodyDisplayForms(List<DisplayForm> waterBodyDisplayForms) {
		this.waterBodyDisplayForms = waterBodyDisplayForms;
	}

	public List<DisplayForm> getWaterBodyDisplayForms() {
		return waterBodyDisplayForms;
	}

	public void setWaterBodyColladaOptions(ColladaOptions waterBodyColladaOptions) {
		this.waterBodyColladaOptions = waterBodyColladaOptions;
	}

	public ColladaOptions getWaterBodyColladaOptions() {
		return waterBodyColladaOptions;
	}

	public void setLandUseDisplayForms(List<DisplayForm> landUseDisplayForms) {
		this.landUseDisplayForms = landUseDisplayForms;
	}

	public List<DisplayForm> getLandUseDisplayForms() {
		return landUseDisplayForms;
	}

	public void setLandUseColladaOptions(ColladaOptions landUseColladaOptions) {
		this.landUseColladaOptions = landUseColladaOptions;
	}

	public ColladaOptions getLandUseColladaOptions() {
		return landUseColladaOptions;
	}

	public void setCityObjectGroupDisplayForms(List<DisplayForm> cityObjectGroupDisplayForms) {
		this.cityObjectGroupDisplayForms = cityObjectGroupDisplayForms;
	}

	public List<DisplayForm> getCityObjectGroupDisplayForms() {
		return cityObjectGroupDisplayForms;
	}

	public void setVegetationDisplayForms(List<DisplayForm> vegetationDisplayForms) {
		this.vegetationDisplayForms = vegetationDisplayForms;
	}

	public List<DisplayForm> getVegetationDisplayForms() {
		return vegetationDisplayForms;
	}

	public void setVegetationColladaOptions(ColladaOptions vegetationColladaOptions) {
		this.vegetationColladaOptions = vegetationColladaOptions;
	}

	public ColladaOptions getVegetationColladaOptions() {
		return vegetationColladaOptions;
	}

	public void setBridgeDisplayForms(List<DisplayForm> bridgeDisplayForms) {
		this.bridgeDisplayForms = bridgeDisplayForms;
	}

	public List<DisplayForm> getBridgeDisplayForms() {
		return bridgeDisplayForms;
	}

	public void setBridgeColladaOptions(ColladaOptions bridgeColladaOptions) {
		this.bridgeColladaOptions = bridgeColladaOptions;
	}

	public ColladaOptions getBridgeColladaOptions() {
		return bridgeColladaOptions;
	}

	public void setBridgeBalloon(Balloon bridgeBalloon) {
		this.bridgeBalloon = bridgeBalloon;
	}

	public Balloon getBridgeBalloon() {
		return bridgeBalloon;
	}

	public void setTunnelDisplayForms(List<DisplayForm> tunnelDisplayForms) {
		this.tunnelDisplayForms = tunnelDisplayForms;
	}

	public List<DisplayForm> getTunnelDisplayForms() {
		return tunnelDisplayForms;
	}

	public void setTunnelColladaOptions(ColladaOptions tunnelColladaOptions) {
		this.tunnelColladaOptions = tunnelColladaOptions;
	}

	public ColladaOptions getTunnelColladaOptions() {
		return tunnelColladaOptions;
	}

	public void setTunnelBalloon(Balloon tunnelBalloon) {
		this.tunnelBalloon = tunnelBalloon;
	}

	public Balloon getTunnelBalloon() {
		return tunnelBalloon;
	}

	public static int getActiveDisplayFormsAmount(List<DisplayForm> displayForms) {
		int activeAmount = 0; 
		for (DisplayForm displayForm : displayForms) {
			if (displayForm.isActive()) activeAmount++;
		}
		return activeAmount;
	}

	public void setExportAsKmz(boolean exportAsKmz) {
		this.exportAsKmz = exportAsKmz;
	}

	public boolean isExportAsKmz() {
		return exportAsKmz;
	}

	public void setShowBoundingBox(boolean showBoundingBox) {
		this.showBoundingBox = showBoundingBox;
	}

	public boolean isShowBoundingBox() {
		return showBoundingBox;
	}

	public void setShowTileBorders(boolean showTileBorders) {
		this.showTileBorders = showTileBorders;
	}

	public boolean isShowTileBorders() {
		return showTileBorders;
	}

	public void setAppearanceTheme(String appearanceTheme) {
		this.appearanceTheme = appearanceTheme;
	}

	public String getAppearanceTheme() {
		return appearanceTheme;
	}

	public void setAltitudeMode(AltitudeMode altitudeMode) {
		this.altitudeMode = altitudeMode;
	}

	public AltitudeMode getAltitudeMode() {
		return altitudeMode;
	}

	public void setAltitudeOffsetMode(AltitudeOffsetMode altitudeOffsetMode) {
		this.altitudeOffsetMode = altitudeOffsetMode;
	}

	public AltitudeOffsetMode getAltitudeOffsetMode() {
		return altitudeOffsetMode;
	}

	public void setAltitudeOffsetValue(double altitudeOffsetValue) {
		this.altitudeOffsetValue = altitudeOffsetValue;
	}

	public double getAltitudeOffsetValue() {
		return altitudeOffsetValue;
	}

	public void setCallGElevationService(boolean callGElevationService) {
		this.callGElevationService = callGElevationService;
	}

	public boolean isCallGElevationService() {
		return callGElevationService;
	}

	public void setAutoTileSideLength(double autoTileSideLength) {
		this.autoTileSideLength = autoTileSideLength;
	}

	public double getAutoTileSideLength() {
		return autoTileSideLength;
	}

	public void setWriteJSONFile(boolean writeJSONFile) {
		this.writeJSONFile = writeJSONFile;
	}

	public boolean isWriteJSONFile() {
		return writeJSONFile;
	}

	public void setOneFilePerObject(boolean oneFilePerObject) {
		this.oneFilePerObject = oneFilePerObject;
	}

	public boolean isOneFilePerObject() {
		return oneFilePerObject;
	}

	public void setSingleObjectRegionSize(double singleObjectRegionSize) {
		this.singleObjectRegionSize = singleObjectRegionSize;
	}

	public double getSingleObjectRegionSize() {
		return singleObjectRegionSize;
	}

	public void setViewRefreshMode(String viewRefreshMode) {
		this.viewRefreshMode = viewRefreshMode;
	}

	public String getViewRefreshMode() {
		return viewRefreshMode;
	}

	public void setViewRefreshTime(double viewRefreshTime) {
		this.viewRefreshTime = viewRefreshTime;
	}

	public double getViewRefreshTime() {
		return viewRefreshTime;
	}

	public void setUseOriginalZCoords(boolean useOriginalZCoords) {
		this.useOriginalZCoords = useOriginalZCoords;
	}

	public boolean isUseOriginalZCoords() {
		return useOriginalZCoords;
	}

	public void setBuildingBalloon(Balloon buildingBalloon) {
		this.buildingBalloon = buildingBalloon;
	}

	public Balloon getBuildingBalloon() {
		return buildingBalloon;
	}

	public void setWaterBodyBalloon(Balloon waterBodyBalloon) {
		this.waterBodyBalloon = waterBodyBalloon;
	}

	public Balloon getWaterBodyBalloon() {
		return waterBodyBalloon;
	}

	public void setLandUseBalloon(Balloon landUseBalloon) {
		this.landUseBalloon = landUseBalloon;
	}

	public Balloon getLandUseBalloon() {
		return landUseBalloon;
	}

	public void setCityObjectGroupBalloon(Balloon cityObjectGroupBalloon) {
		this.cityObjectGroupBalloon = cityObjectGroupBalloon;
	}

	public Balloon getCityObjectGroupBalloon() {
		return cityObjectGroupBalloon;
	}

	public void setVegetationBalloon(Balloon vegetationBalloon) {
		this.vegetationBalloon = vegetationBalloon;
	}

	public Balloon getVegetationBalloon() {
		return vegetationBalloon;
	}

	public void setGenericCityObjectDisplayForms(
			List<DisplayForm> genericCityObjectDisplayForms) {
		this.genericCityObjectDisplayForms = genericCityObjectDisplayForms;
	}

	public List<DisplayForm> getGenericCityObjectDisplayForms() {
		return genericCityObjectDisplayForms;
	}

	public void setGenericCityObjectColladaOptions(
			ColladaOptions genericCityObjectColladaOptions) {
		this.genericCityObjectColladaOptions = genericCityObjectColladaOptions;
	}

	public ColladaOptions getGenericCityObjectColladaOptions() {
		return genericCityObjectColladaOptions;
	}

	public void setGenericCityObjectBalloon(Balloon genericCityObjectBalloon) {
		this.genericCityObjectBalloon = genericCityObjectBalloon;
	}

	public Balloon getGenericCityObjectBalloon() {
		return genericCityObjectBalloon;
	}

	public void setCityFurnitureDisplayForms(
			List<DisplayForm> cityFurnitureDisplayForms) {
		this.cityFurnitureDisplayForms = cityFurnitureDisplayForms;
	}

	public List<DisplayForm> getCityFurnitureDisplayForms() {
		return cityFurnitureDisplayForms;
	}

	public void setCityFurnitureColladaOptions(
			ColladaOptions cityFurnitureColladaOptions) {
		this.cityFurnitureColladaOptions = cityFurnitureColladaOptions;
	}

	public ColladaOptions getCityFurnitureColladaOptions() {
		return cityFurnitureColladaOptions;
	}

	public void setCityFurnitureBalloon(Balloon cityFurnitureBalloon) {
		this.cityFurnitureBalloon = cityFurnitureBalloon;
	}

	public Balloon getCityFurnitureBalloon() {
		return cityFurnitureBalloon;
	}

	public void setTransportationDisplayForms(List<DisplayForm> transportationDisplayForms) {
		this.transportationDisplayForms = transportationDisplayForms;
	}

	public List<DisplayForm> getTransportationDisplayForms() {
		return transportationDisplayForms;
	}

	public void setTransportationColladaOptions(ColladaOptions transportationColladaOptions) {
		this.transportationColladaOptions = transportationColladaOptions;
	}

	public ColladaOptions getTransportationColladaOptions() {
		return transportationColladaOptions;
	}

	public void setTransportationBalloon(Balloon transportationBalloon) {
		this.transportationBalloon = transportationBalloon;
	}

	public Balloon getTransportationBalloon() {
		return transportationBalloon;
	}

	public List<DisplayForm> getReliefDisplayForms() {
		return reliefDisplayForms;
	}

	public void setReliefDisplayForms(List<DisplayForm> reliefDisplayForms) {
		this.reliefDisplayForms = reliefDisplayForms;
	}

	public ColladaOptions getReliefColladaOptions() {
		return reliefColladaOptions;
	}

	public void setReliefColladaOptions(ColladaOptions reliefColladaOptions) {
		this.reliefColladaOptions = reliefColladaOptions;
	}

	public Balloon getReliefBalloon() {
		return reliefBalloon;
	}

	public void setReliefBalloon(Balloon reliefBalloon) {
		this.reliefBalloon = reliefBalloon;
	}

	public boolean isWriteJSONPFile() {
		return writeJSONPFile;
	}

	public void setWriteJSONPFile(boolean writeJSONPFile) {
		this.writeJSONPFile = writeJSONPFile;
	}

	public String getCallbackNameJSONP() {
		return callbackNameJSONP;
	}

	public void setCallbackNameJSONP(String callbackNameJSONP) {
		this.callbackNameJSONP = callbackNameJSONP;
	}
	
	public Internal getInternal() {
		return internal;
	}

	public void setInternal(Internal internal) {
		this.internal = internal;
	}

}

