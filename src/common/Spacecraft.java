package common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import telemetry.BitArrayLayout;
import telemetry.LayoutLoadException;
import telemetry.LookUpTable;

public abstract class Spacecraft {
	public Properties properties; // Java properties file for user defined values
	public String propertiesFileName;
	
	public static String SPACECRAFT_DIR = "spacecraft";
	public static final int ERROR_IDX = -1;
	
	public static final int FOX1A = 1;
	public static final int FOX1B = 2;
	public static final int FOX1C = 3;
	public static final int FOX1D = 4;
	public static final int FOX1E = 5;
	public static final int FUN_CUBE1 = 100;
	public static final int FUN_CUBE2 = 101;
	
	// Layout Types
	public static final String DEBUG_LAYOUT = "DEBUG";
	public static final String REAL_TIME_LAYOUT = "RT";
	public static final String MAX_LAYOUT = "MAX";
	public static final String MIN_LAYOUT = "MIN";
	public static final String RAD_LAYOUT = "RAD";
	public static final String RAD2_LAYOUT = "RAD2";
	public static final String HERCI_HS_LAYOUT = "HERCI";
	public static final String HERCI_HS_HEADER_LAYOUT = "RAD2";
	public static final String HERCI_HS_PKT_LAYOUT = "RAD3";
	
	public static final String RSSI_LOOKUP = "RSSI";
	public static final String IHU_VBATT_LOOKUP = "IHU_VBATT";
	public static final String IHU_TEMP_LOOKUP = "IHU_TEMP";

	
	// Model Versions
	public static final int EM = 0;
	public static final int FM = 1;
	public static final int FS = 2;
	
	// Flattened ENUM for spacecraft name
	public static String[] modelNames = {
			"Engineering Model",
			"Flight Model",
			"Flight Spare"
	};
		
	public static String[] models = {
			"EM",
			"FM",
			"FS"
	};
	
	public int foxId = 1;
	public int catalogNumber = 0;
	public String series = "Fox";
	public String name = "Fox-1A";
	public String description = "";
	public int model;
	public int telemetryDownlinkFreqkHz = 145980;
	public int minFreqBoundkHz = 145970;
	public int maxFreqBoundkHz = 145990;
	
	public boolean telemetryMSBfirst = true;
	public boolean ihuLittleEndian = true;
	
	public int numberOfLayouts = 4;
	public String[] layoutFilename;
	//public String[] layoutName;
	public BitArrayLayout[] layout;
	 	
	public int numberOfLookupTables = 3;
	public String[] lookupTableFilename;
	//public String[] lookupTableName;
	public LookUpTable[] lookupTable;
	
	public String measurementsFileName;
	public String passMeasurementsFileName;
	public BitArrayLayout measurementLayout;
	public BitArrayLayout passMeasurementLayout;
	
	// User Config
	public boolean track = true; // default is we track a satellite
	
	public Spacecraft(String fileName ) throws FileNotFoundException, LayoutLoadException {
		properties = new Properties();
		propertiesFileName = fileName;
		
		
		
	}
	
	public boolean isFox1() {
		if (foxId < 10) return true;
		return false;
	}
	
	public int getLayoutIdxByName(String name) {
		for (int i=0; i<numberOfLayouts; i++)
			if (layout[i].name.equalsIgnoreCase(name))
				return i;
		return ERROR_IDX;
	}
	public int getLookupIdxByName(String name) {
		for (int i=0; i<numberOfLookupTables; i++)
			if (lookupTable[i].name.equalsIgnoreCase(name))
				return i;
		return ERROR_IDX;
	}
	
	public BitArrayLayout getLayoutByName(String name) {
		int i = getLayoutIdxByName(name);
		if (i != ERROR_IDX)
				return layout[i];
		return null;
	}

	public LookUpTable getLookupTableByName(String name) {
		int i = getLookupIdxByName(name);
		if (i != ERROR_IDX)
				return lookupTable[i];
		return null;
	}

	public String getLayoutFileNameByName(String name) {
		int i = getLayoutIdxByName(name);
		if (i != ERROR_IDX)
				return layoutFilename[i];
		return null;
	}

	public String getLookupTableFileNameByName(String name) {
		int i = getLookupIdxByName(name);
		if (i != ERROR_IDX)
				return lookupTableFilename[i];
		return null;
	}

	protected void load() throws LayoutLoadException {
		// try to load the properties from a file
		try {
			FileInputStream f=new FileInputStream(Config.currentDir + File.separator + SPACECRAFT_DIR + File.separator +propertiesFileName);
			properties.load(f);
		} catch (IOException e) {
			throw new LayoutLoadException("Could not load spacecraft files: " + Config.currentDir + File.separator + SPACECRAFT_DIR + File.separator +propertiesFileName);
		}
		try {
			foxId = Integer.parseInt(getProperty("foxId"));
			catalogNumber = Integer.parseInt(getProperty("catalogNumber"));			
			name = getProperty("name");
			description = getProperty("description");
			model = Integer.parseInt(getProperty("model"));
			telemetryDownlinkFreqkHz = Integer.parseInt(getProperty("telemetryDownlinkFreqkHz"));			
			minFreqBoundkHz = Integer.parseInt(getProperty("minFreqBoundkHz"));
			maxFreqBoundkHz = Integer.parseInt(getProperty("maxFreqBoundkHz"));
			numberOfLayouts = Integer.parseInt(getProperty("numberOfLayouts"));
			layoutFilename = new String[numberOfLayouts];
			//layoutName = new String[numberOfLayouts];
			layout = new BitArrayLayout[numberOfLayouts];
			for (int i=0; i < numberOfLayouts; i++) {
				layoutFilename[i] = getProperty("layout"+i+".filename");
				layout[i] = new BitArrayLayout(layoutFilename[i]);
				layout[i].name = getProperty("layout"+i+".name");
				layout[i].parentLayout = getOptionalProperty("layout"+i+".parentLayout");
			}
			
			numberOfLookupTables = Integer.parseInt(getProperty("numberOfLookupTables"));
			lookupTableFilename = new String[numberOfLookupTables];
			//lookupTableName = new String[numberOfLookupTables];
			lookupTable = new LookUpTable[numberOfLookupTables];
			for (int i=0; i < numberOfLookupTables; i++) {
				lookupTableFilename[i] = getProperty("lookupTable"+i+".filename");
				lookupTable[i] = new LookUpTable(lookupTableFilename[i]);
				lookupTable[i].name = getProperty("lookupTable"+i);
			}
			
			
			String t = getOptionalProperty("track");
			if (t == null) 
				track = true;
			else 
				track = Boolean.parseBoolean(t);
		} catch (NumberFormatException nf) {
			nf.printStackTrace(Log.getWriter());
			throw new LayoutLoadException("Corrupt data found when loading Spacecraft file: " + Config.currentDir + File.separator + SPACECRAFT_DIR + File.separator +propertiesFileName );
		} catch (NullPointerException nf) {
			nf.printStackTrace(Log.getWriter());
			throw new LayoutLoadException("Missing data value when loading Spacecraft file: " + Config.currentDir + File.separator + SPACECRAFT_DIR + File.separator +propertiesFileName );		
		} catch (FileNotFoundException e) {
			e.printStackTrace(Log.getWriter());
			throw new LayoutLoadException("File not found loading Spacecraft file: " + Config.currentDir + File.separator + SPACECRAFT_DIR + File.separator +propertiesFileName );
		}
	}
	
	protected String getOptionalProperty(String key) throws LayoutLoadException {
		String value = properties.getProperty(key);
		if (value == null) {
			return null;
		}
		return value;
	}

	protected String getProperty(String key) throws LayoutLoadException {
		String value = properties.getProperty(key);
		if (value == null) {
			throw new LayoutLoadException("Missing data value: " + key + " when loading Spacecraft file: \n" + Config.currentDir + File.separator + SPACECRAFT_DIR + File.separator +propertiesFileName );
//			throw new NullPointerException();
		}
		return value;
	}
	protected void store() {
		try {
			properties.store(new FileOutputStream(Config.currentDir + File.separator + SPACECRAFT_DIR + File.separator + propertiesFileName), "Fox 1 Telemetry Decoder Properties");
		} catch (FileNotFoundException e1) {
			Log.errorDialog("ERROR", "Could not write spacecraft file. Check permissions on run directory or on the file");
			e1.printStackTrace(Log.getWriter());
		} catch (IOException e1) {
			Log.errorDialog("ERROR", "Error writing spacecraft file");
			e1.printStackTrace(Log.getWriter());
		}
	}
	protected void save() {
		properties.setProperty("foxId", Integer.toString(foxId));
		properties.setProperty("catalogNumber", Integer.toString(catalogNumber));
		properties.setProperty("name", name);
		properties.setProperty("description", description);
		properties.setProperty("model", Integer.toString(model));
		properties.setProperty("telemetryDownlinkFreqkHz", Integer.toString(telemetryDownlinkFreqkHz));
		properties.setProperty("minFreqBoundkHz", Integer.toString(minFreqBoundkHz));
		properties.setProperty("maxFreqBoundkHz", Integer.toString(maxFreqBoundkHz));
		properties.setProperty("maxFreqBoundkHz", Integer.toString(maxFreqBoundkHz));
		properties.setProperty("track", Boolean.toString(track));
	}
	
	public String getIdString() {
		String id = "??";
		id = Integer.toString(foxId);

		return id;
	}
	
	public String toString() {
		return name;
	}
	
}
