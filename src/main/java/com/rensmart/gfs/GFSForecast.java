package com.rensmart.gfs;

import java.io.File;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.onestonesoup.core.FileHelper;
import org.onestonesoup.core.StringHelper;
import org.onestonesoup.openforum.controller.OpenForumController;
import org.onestonesoup.openforum.filemanager.Resource;
import org.onestonesoup.openforum.jdbc.DatabaseAPI;
import org.onestonesoup.openforum.plugin.SystemAPI;
import ucar.ma2.Array;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

public class GFSForecast extends SystemAPI {
	public static final String VERSION = "1.001 build 018 Alpha";
	private boolean serverMode = false;
	public static final String HOST = "http://nomads.ncep.noaa.gov/cgi-bin/filter_gfs_0p25.pl";
	public static final String HOST_FILE = "pgrb2.0p25.fHOUR";
	public static final String WEST = "WEST";
	public static final String WEST_REGION = "&leftlon=348&rightlon=359.5&toplat=61&bottomlat=49.5";
	public static final String EAST = "EAST";
	public static final String EAST_REGION = "&leftlon=0&rightlon=2&toplat=61&bottomlat=49.5";
	public static final String WIND_DOWNLOAD_URL = "http://nomads.ncep.noaa.gov/cgi-bin/filter_gfs_0p25.pl?file=gfs.tCYCLE_RUN_TIMEz.pgrb2.0p25.fHOUR&lev_10_m_above_ground=on&var_UGRD=on&var_VGRD=on&subregion=REGION&dir=";
	public static final String SOLAR_DOWNLOAD_URL = "http://nomads.ncep.noaa.gov/cgi-bin/filter_gfs_0p25.pl?file=gfs.tCYCLE_RUN_TIMEz.pgrb2.0p25.fHOUR&lev_surface=on&var_DSWRF=on&subregion=REGION&dir=";
	private static final String TEMPERATURE_AND_HUMIDITY_DOWNLOAD_URL = "http://nomads.ncep.noaa.gov/cgi-bin/filter_gfs_0p25.pl?file=gfs.tCYCLE_RUN_TIMEz.pgrb2.0p25.fHOUR&lev_2_m_above_ground=on&var_RH=on&var_TMAX=on&var_TMIN=on&var_TMP=on&subregion=REGION&dir=";
	private DatabaseAPI database;

	public GFSForecast() {
	}

	public void setDatabase(DatabaseAPI database) {
		this.database = database;
	}

	public void setController(OpenForumController controller) {
		super.setController(controller);

		try {
			this.database = (DatabaseAPI)controller.getApi("/OpenForum/AddOn/SQL");
		} catch (Throwable var3) {
			var3.printStackTrace();
		}

		this.serverMode = true;
	}

	public boolean isServerMode() {
		return this.serverMode;
	}

	public void setServerMode(boolean serverMode) {
		this.serverMode = serverMode;
	}

	public String getVersion() {
		return "1.001 build 018 Alpha";
	}

	public void downloadForecast(String pageName, int cycleRuntime) throws Exception {
		Date now = new Date();
		int year = now.getYear() + 1900;
		int month = now.getMonth() + 1;
		int date = now.getDate();

		for(int hour = 3; hour <= 192; hour += 3) {
			GFSGrid grid = this.downloadForecast(cycleRuntime, year, month, date, hour, pageName);
			if(this.database != null) {
				GFSCell[][] gfsData = grid.getData();

				for(int i = 0; i < gfsData.length; ++i) {
					for(int j = 0; j < gfsData[i].length; ++j) {
						GFSCell cell = gfsData[i][j];
						if(cell != null) {
							String sql = null;
							boolean found = true;

							try {
								sql = "select * from gfs  where hour=" + hour + " " + "and latitude=" + cell.getLatitude() + " " + "and longitude=" + cell.getLongitude();
								String data = this.database.query("rensmart-weather", sql);
								if(data.indexOf("rowCount: \"0\"") != -1) {
									found = false;
								}
							} catch (Exception var17) {
								this.logMessage(pageName, var17.getMessage());
							}

							if(found) {
								try {
									sql = "update gfs set hour=" + hour + ",latitude=" + cell.getLatitude() + ",longitude=" + cell.getLongitude() + ",solar_flux=" + cell.getSolarFlux() + ",wind_speed=" + cell.getWindSpeed() + ",wind_direction=" + cell.getWindDirection() + ",temperature=" + cell.getTemperature() + ",minimum_temperature=" + cell.getTemperatureMinimum() + ",maximum_temperature=" + cell.getTemperatureMaximum() + ",relative_humidity=" + cell.getRelativeHumidity() + " where hour=" + hour + " " + "and latitude=" + cell.getLatitude() + " " + "and longitude=" + cell.getLongitude();
									this.database.execute("rensmart-weather", sql);
								} catch (Exception var16) {
									this.logMessage(pageName, var16.getMessage());
								}
							} else {
								try {
									sql = "insert into gfs (hour,latitude,longitude,solar_flux,wind_speed,wind_direction,temperature,minimum_temperature,maximum_temperature,relative_humidity) values (" + hour + "," + cell.getLatitude() + "," + cell.getLongitude() + "," + cell.getSolarFlux() + "," + cell.getWindSpeed() + "," + cell.getWindDirection() + "," + cell.getTemperature() + "," + cell.getTemperatureMinimum() + "," + cell.getTemperatureMaximum() + "," + cell.getRelativeHumidity() + ")";
									this.database.execute("rensmart-weather", sql);
								} catch (Exception var18) {
									this.logMessage(pageName, var18.getMessage());
								}
							}
						}
					}
				}
			}
		}

	}

	private GFSGrid downloadForecast(int cycleRuntime, int year, int month, int date, int hour, String pageName) throws Exception {
		try {
			Thread.sleep(6000L);
		} catch (Exception var17) {
			;
		}

		String cycleTimeString = StringHelper.padLeftToFitSize("" + cycleRuntime, '0', 2);
		String hourString = StringHelper.padLeftToFitSize("" + hour, '0', 3);
		String dateString = StringHelper.padLeftToFitSize("" + date, '0', 2);
		String monthString = StringHelper.padLeftToFitSize("" + month, '0', 2);
		String dateStamp = year + monthString + dateString + cycleTimeString;
		GFSGrid grid = new GFSGrid(61.0D, 49.5D, 348.0D, 362.0D);
		URL remoteUrl = this.createRemoteURLFromTemplate("http://nomads.ncep.noaa.gov/cgi-bin/filter_gfs_0p25.pl?file=gfs.tCYCLE_RUN_TIMEz.pgrb2.0p25.fHOUR&lev_10_m_above_ground=on&var_UGRD=on&var_VGRD=on&subregion=REGION&dir=", cycleTimeString, hourString, dateStamp, "&leftlon=348&rightlon=359.5&toplat=61&bottomlat=49.5");
		URL windURL = this.download(remoteUrl, pageName, "wind-data-" + cycleRuntime + "-" + hour + "-" + "WEST" + ".gfs");
		remoteUrl = this.createRemoteURLFromTemplate("http://nomads.ncep.noaa.gov/cgi-bin/filter_gfs_0p25.pl?file=gfs.tCYCLE_RUN_TIMEz.pgrb2.0p25.fHOUR&lev_surface=on&var_DSWRF=on&subregion=REGION&dir=", cycleTimeString, hourString, dateStamp, "&leftlon=348&rightlon=359.5&toplat=61&bottomlat=49.5");
		URL solarURL = this.download(remoteUrl, pageName, "solar-data-" + cycleRuntime + "-" + hour + "-" + "WEST" + ".gfs");
		remoteUrl = this.createRemoteURLFromTemplate("http://nomads.ncep.noaa.gov/cgi-bin/filter_gfs_0p25.pl?file=gfs.tCYCLE_RUN_TIMEz.pgrb2.0p25.fHOUR&lev_2_m_above_ground=on&var_RH=on&var_TMAX=on&var_TMIN=on&var_TMP=on&subregion=REGION&dir=", cycleTimeString, hourString, dateStamp, "&leftlon=348&rightlon=359.5&toplat=61&bottomlat=49.5");
		URL temperatureURL = this.download(remoteUrl, pageName, "temperature-data-" + cycleRuntime + "-" + hour + "-" + "WEST" + ".gfs");
		this.processForecast(pageName, grid, windURL.toExternalForm(), solarURL.toExternalForm(), temperatureURL.toExternalForm(), "WEST");
		remoteUrl = this.createRemoteURLFromTemplate("http://nomads.ncep.noaa.gov/cgi-bin/filter_gfs_0p25.pl?file=gfs.tCYCLE_RUN_TIMEz.pgrb2.0p25.fHOUR&lev_10_m_above_ground=on&var_UGRD=on&var_VGRD=on&subregion=REGION&dir=", cycleTimeString, hourString, dateStamp, "&leftlon=0&rightlon=2&toplat=61&bottomlat=49.5");
		windURL = this.download(remoteUrl, pageName, "wind-data-" + cycleRuntime + "-" + hour + "-" + "EAST" + ".gfs");
		remoteUrl = this.createRemoteURLFromTemplate("http://nomads.ncep.noaa.gov/cgi-bin/filter_gfs_0p25.pl?file=gfs.tCYCLE_RUN_TIMEz.pgrb2.0p25.fHOUR&lev_surface=on&var_DSWRF=on&subregion=REGION&dir=", cycleTimeString, hourString, dateStamp, "&leftlon=0&rightlon=2&toplat=61&bottomlat=49.5");
		solarURL = this.download(remoteUrl, pageName, "solar-data-" + cycleRuntime + "-" + hour + "-" + "EAST" + ".gfs");
		remoteUrl = this.createRemoteURLFromTemplate("http://nomads.ncep.noaa.gov/cgi-bin/filter_gfs_0p25.pl?file=gfs.tCYCLE_RUN_TIMEz.pgrb2.0p25.fHOUR&lev_2_m_above_ground=on&var_RH=on&var_TMAX=on&var_TMIN=on&var_TMP=on&subregion=REGION&dir=", cycleTimeString, hourString, dateStamp, "&leftlon=0&rightlon=2&toplat=61&bottomlat=49.5");
		temperatureURL = this.download(remoteUrl, pageName, "temperature-data-" + cycleRuntime + "-" + hour + "-" + "EAST" + ".gfs");
		this.processForecast(pageName, grid, windURL.toExternalForm(), solarURL.toExternalForm(), temperatureURL.toExternalForm(), "EAST");
		return grid;
	}

	private URL createRemoteURLFromTemplate(String template, String cycleTimeString, String hourString, String dateStamp, String region) throws MalformedURLException {
		String dir = "/gfs." + dateStamp;
		return new URL(template.replace("CYCLE_RUN_TIME", cycleTimeString).replace("HOUR", "" + hourString).replace("REGION", region) + dir);
	}

	private URL download(URL url, String pageName, String fileName) throws Exception {
		this.logMessage(pageName, "downloading " + url + " to " + pageName + "/" + fileName);
		if(this.serverMode && pageName != null) {
			URLConnection connection = url.openConnection();
			connection.setRequestProperty("User-Agent", "OpenForum Wiki");
			OutputStream oStream = this.getController().getFileManager().getAttachmentOutputStream(pageName, fileName, this.getController().getSystemLogin());
			FileHelper.copyInputStreamToOutputStream(connection.getInputStream(), oStream);
			oStream.flush();
			oStream.close();
			Resource resource = this.getController().getFileManager().getFile(pageName, fileName, this.getController().getSystemLogin());
			return this.getController().getFileManager().getResourceStore(this.getController().getSystemLogin()).getResourceURL(resource);
		} else {
			File temp = new File("./gfs-data/" + fileName);
			if(!temp.exists()) {
				URLConnection connection = url.openConnection();
				FileHelper.copyInputStreamToFile(connection.getInputStream(), new File(temp.getAbsolutePath()));
				this.logMessage("", "Saved to " + temp.getAbsolutePath());
			}

			return temp.toURI().toURL();
		}
	}

	public void processForecast(String pageName, GFSGrid grid, String windURL, String solarURL, String temperatureAndHumidityURL, String region) throws Exception {
		Variable windU = null;
		Variable windV = null;
		Variable time = null;
		this.logMessage(pageName, "--processing--");
		NetcdfDataset gid = NetcdfDataset.openDataset(windURL);
		this.logMessage(pageName, "info:" + gid.getDetailInfo());
		List<Variable> variables = gid.getReferencedFile().getVariables();
		Iterator var12 = variables.iterator();

		Variable minimumTemperature;
		while(var12.hasNext()) {
			minimumTemperature = (Variable)var12.next();
			this.logMessage(pageName, "Wind Name:" + minimumTemperature.getName());
			if(minimumTemperature.getName().equals("U-component_of_wind")) {
				windU = minimumTemperature;
			} else if(minimumTemperature.getName().equals("V-component_of_wind")) {
				windV = minimumTemperature;
			}
		}

		Variable solarFlux = null;
		gid = NetcdfDataset.openDataset(solarURL);
		variables = gid.getReferencedFile().getVariables();
		Iterator var32 = variables.iterator();

		Variable variable;
		while(var32.hasNext()) {
			variable = (Variable)var32.next();
			this.logMessage(pageName, "Solar Name:" + variable.getName());
			if(variable.getName().equals("Downward_Short-Wave_Rad_Flux")) {
				solarFlux = variable;
			} else if(variable.getName().equals("time")) {
				time = variable;
			}
		}

		minimumTemperature = null;
		variable = null;
		Variable maximumTemperature = null;
		Variable temperature = null;
		gid = NetcdfDataset.openDataset(temperatureAndHumidityURL);
		variables = gid.getReferencedFile().getVariables();
		Iterator var17 = variables.iterator();

		while(var17.hasNext()) {
			Variable variable2 = (Variable)var17.next();
			this.logMessage(pageName, "Temp Name:" + variable.getName());
			if(variable2.getName().equals("Temperature")) {
				temperature = variable2;
			} else if(variable2.getName().equals("Minimum_temperature")) {
				minimumTemperature = variable2;
			} else if(variable2.getName().equals("Maximum_temperature")) {
				maximumTemperature = variable2;
			} else if(variable2.getName().equals("Relative_humidity")) {
				variable = variable2;
			}
		}

		Array timeData = time.read();
		grid.setTimeStamp(timeData.getInt(0) + " " + time.getUnitsString());
		Array uData = windU.read();
		Array vData = windV.read();
		Array solarFluxData = solarFlux.read();
		Array minimumTemperatureData = minimumTemperature.read();
		Array relativeHumidityData = variable.read();
		Array maximumTemperatureData = maximumTemperature.read();
		Array temperatureData = temperature.read();

		for(int i = 0; (long)i < uData.getSize(); ++i) {
			double windSpeedU = (double)uData.getFloat(i);
			double windSpeedV = (double)vData.getFloat(i);
			LatLong location = grid.getGFSCellLocation(i, region);
			this.logMessage(pageName, "processing lat:" + location.getLatitude() + " lon:" + location.getLongitude());
			grid.setCellWindData(location.getLatitude(), location.getLongitude(), windSpeedU, windSpeedV);
			grid.setCellSolarData(location.getLatitude(), location.getLongitude(), (double)solarFluxData.getFloat(i));
			grid.setCellTemperatureData(location.getLatitude(), location.getLongitude(), (double)temperatureData.getFloat(i), (double)minimumTemperatureData.getFloat(i), (double)maximumTemperatureData.getFloat(i), (double)relativeHumidityData.getFloat(i));
		}

		gid.close();
		this.logMessage(pageName, "--processing complete--");
	}

	private void logMessage(String pageName, String message) {
		if(this.serverMode) {
			this.getController().getQueueManager().getQueue("/OpenForum/System.debug").postMessage(message, "Admin");
		} else {
			System.out.println(message);
		}

	}
}