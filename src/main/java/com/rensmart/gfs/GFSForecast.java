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
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

public class GFSForecast extends SystemAPI {
	public static final String VERSION = "3.2.3 build 0001 Beta";
	private boolean serverMode = false;
	private DatabaseAPI database;

	public static void main(String[] args) throws Exception {
		(new GFSForecast()).downloadForecast("page", "queue", 6, "gfs");
	}

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
		return "3.2.0 build 0004 Alpha";
	}

	public void downloadForecast(String downloadPageName, String queueName, int cycleRuntime, String table) throws Exception {
		if (this.serverMode && this.database == null) {
			this.logMessage(queueName, "No database set. Stopping.");
			throw new Exception("No database set in server mode");
		} else {
			Date now = new Date();
			int year = now.getYear() + 1900;
			int month = now.getMonth() + 1;
			int date = now.getDate();

			for(int hour = 3; hour <= 192; hour += 3) {
				GFSGrid grid = this.downloadForecast(cycleRuntime, year, month, date, hour, downloadPageName, queueName);
				if (this.database == null) {
					this.logMessage(queueName, "NOT inserting into database. Database is null.");
				} else {
					this.logMessage(queueName, "Inserting into database");
					GFSCell[][] gfsData = grid.getData();

					for(int i = 0; i < gfsData.length; ++i) {
						for(int j = 0; j < gfsData[i].length; ++j) {
							GFSCell cell = gfsData[i][j];
							if (cell != null) {
								String sql = null;

								try {
									sql = "insert into " + table + " (hour,latitude,longitude,solar_flux,wind_speed,wind_direction,temperature,minimum_temperature,maximum_temperature,relative_humidity) values (" + hour + "," + cell.getLatitude() + "," + cell.getLongitude() + "," + cell.getSolarFlux() + "," + cell.getWindSpeed() + "," + cell.getWindDirection() + "," + cell.getTemperature() + "," + cell.getTemperatureMinimum() + "," + cell.getTemperatureMaximum() + "," + cell.getRelativeHumidity() + ")";
									sql = sql + " ON CONFLICT (hour,latitude,longitude) DO ";
									sql = sql + "update set hour=" + hour + ",latitude=" + cell.getLatitude() + ",longitude=" + cell.getLongitude() + ",solar_flux=" + cell.getSolarFlux() + ",wind_speed=" + cell.getWindSpeed() + ",wind_direction=" + cell.getWindDirection() + ",temperature=" + cell.getTemperature() + ",minimum_temperature=" + cell.getTemperatureMinimum() + ",maximum_temperature=" + cell.getTemperatureMaximum() + ",relative_humidity=" + cell.getRelativeHumidity();
									this.database.execute("rensmart-weather", sql);
								} catch (Exception var17) {
									this.logMessage(queueName, var17.getMessage());
								}
							}
						}
					}

					this.logMessage(queueName, "GFS @" + cycleRuntime + " for " + hour + "hr Entries:" + grid.getEntries() + " Records:" + gfsData.length);
				}
			}

		}
	}

	private GFSGrid downloadForecast(int cycleRuntime, int year, int month, int date, int hour, String downloadPageName, String queueName) throws Exception {
		try {
			Thread.sleep(6000L);
		} catch (Exception var18) {
			;
		}

		String cycleTimeString = StringHelper.padLeftToFitSize("" + cycleRuntime, '0', 2);
		String hourString = StringHelper.padLeftToFitSize("" + hour, '0', 3);
		String dateString = StringHelper.padLeftToFitSize("" + date, '0', 2);
		String monthString = StringHelper.padLeftToFitSize("" + month, '0', 2);
		String dateStamp = year + monthString + dateString + cycleTimeString;
		GFSGrid grid = new GFSGrid(61.0D, 49.5D, -12.0D, 2.0D);
		URL remoteUrl = this.createRemoteURLFromTemplate("http://nomads.ncep.noaa.gov/cgi-bin/filter_gfs_0p25.pl?file=gfs.tCYCLE_RUN_TIMEz.pgrb2.0p25.fHOUR&lev_10_m_above_ground=on&var_UGRD=on&var_VGRD=on&dir=", cycleTimeString, hourString, dateStamp);
		URL windURL = this.download(remoteUrl, downloadPageName, "wind-data-" + cycleRuntime + "-" + hour + "-.gfs", queueName);
		remoteUrl = this.createRemoteURLFromTemplate("http://nomads.ncep.noaa.gov/cgi-bin/filter_gfs_0p25.pl?file=gfs.tCYCLE_RUN_TIMEz.pgrb2.0p25.fHOUR&lev_surface=on&var_DSWRF=on&dir=", cycleTimeString, hourString, dateStamp);
		URL solarURL = this.download(remoteUrl, downloadPageName, "solar-data-" + cycleRuntime + "-" + hour + "-.gfs", queueName);
		remoteUrl = this.createRemoteURLFromTemplate("http://nomads.ncep.noaa.gov/cgi-bin/filter_gfs_0p25.pl?file=gfs.tCYCLE_RUN_TIMEz.pgrb2.0p25.fHOUR&lev_2_m_above_ground=on&var_RH=on&var_TMAX=on&var_TMIN=on&var_TMP=on&dir=", cycleTimeString, hourString, dateStamp);
		URL temperatureURL = this.download(remoteUrl, downloadPageName, "temperature-data-" + cycleRuntime + "-" + hour + "-.gfs", queueName);
		this.processForecast(queueName, grid, windURL.toExternalForm(), solarURL.toExternalForm(), temperatureURL.toExternalForm());
		return grid;
	}

	private URL createRemoteURLFromTemplate(String template, String cycleTimeString, String hourString, String dateStamp) throws MalformedURLException {
		String dir = "/gfs." + dateStamp;
		return new URL(template.replace("CYCLE_RUN_TIME", cycleTimeString).replace("HOUR", "" + hourString) + dir);
	}

	private URL download(URL url, String downloadPageName, String fileName, String queueName) throws Exception {
		this.logMessage(queueName, "downloading " + url + " to " + downloadPageName + "/" + fileName);
		if (this.serverMode && downloadPageName != null) {
			URLConnection connection = url.openConnection();
			connection.setRequestProperty("User-Agent", "OpenForum Wiki");
			OutputStream oStream = this.getController().getFileManager().getAttachmentOutputStream(downloadPageName, fileName, this.getController().getSystemLogin());
			FileHelper.copyInputStreamToOutputStream(connection.getInputStream(), oStream);
			oStream.flush();
			oStream.close();
			Resource resource = this.getController().getFileManager().getFile(downloadPageName, fileName, this.getController().getSystemLogin());
			return this.getController().getFileManager().getResourceStore(this.getController().getSystemLogin()).getResourceURL(resource);
		} else {
			File temp = new File("./gfs-data/" + fileName);
			if (!temp.exists()) {
				URLConnection connection = url.openConnection();
				FileHelper.copyInputStreamToFile(connection.getInputStream(), new File(temp.getAbsolutePath()));
				this.logMessage(queueName, "Saved to " + temp.getAbsolutePath());
			}

			return temp.toURI().toURL();
		}
	}

	public void processForecast(String queueName, GFSGrid grid, String windURL, String solarURL, String temperatureAndHumidityURL) throws Exception {
		Variable windU = null;
		Variable windV = null;
		Variable time = null;
		NetcdfDataset gid = NetcdfDataset.openDataset(windURL);
		this.logMessage(queueName, "Processing " + ((Attribute)gid.getGlobalAttributes().get(5)).getStringValue());
		this.logMessage(queueName, "info:" + gid.getDetailInfo());
		List<Variable> variables = gid.getReferencedFile().getVariables();
		Iterator variablesIterator = variables.iterator();

		Variable variable;
		while(variablesIterator.hasNext()) {
			variable = (Variable)variablesIterator.next();
			this.logMessage(queueName, "Wind Variable Name:" + variable.getName());
			if (variable.getName().equals("U-component_of_wind")) {
				windU = variable;
			} else if (variable.getName().equals("V-component_of_wind")) {
				windV = variable;
			} else if (!variable.getName().equals("lat") && variable.getName().equals("lon")) {
				;
			}
		}

		Variable solarFlux = null;
		gid = NetcdfDataset.openDataset(solarURL);
		variables = gid.getReferencedFile().getVariables();
		variablesIterator = variables.iterator();

		while(variablesIterator.hasNext()) {
			variable = (Variable)variablesIterator.next();
			this.logMessage(queueName, "Solar Variable Name:" + variable.getName());
			if (variable.getName().equals("Downward_Short-Wave_Rad_Flux")) {
				solarFlux = variable;
			} else if (variable.getName().equals("time")) {
				time = variable;
			}
		}

		Variable minimumTemperature = null;
		Variable maximumTemperature = null;
		Variable temperature = null;
		Variable relativeHumidity = null;
		gid = NetcdfDataset.openDataset(temperatureAndHumidityURL);
		variables = gid.getReferencedFile().getVariables();
		variablesIterator = variables.iterator();

		while(variablesIterator.hasNext()) {
			variable = (Variable)variablesIterator.next();
			this.logMessage(queueName, "Temperature Variable Name:" + variable.getName());
			if (variable.getName().equals("Temperature")) {
				temperature = variable;
			} else if (variable.getName().equals("Minimum_temperature")) {
				minimumTemperature = variable;
			} else if (variable.getName().equals("Maximum_temperature")) {
				maximumTemperature = variable;
			} else if (variable.getName().equals("Relative_humidity")) {
				relativeHumidity = variable;
			}
		}

		Array timeData = time.read();
		grid.setTimeStamp(timeData.getInt(0) + " " + time.getUnitsString());
		Array uData = windU.read();
		Array vData = windV.read();
		Array solarFluxData = solarFlux.read();
		Array minimumTemperatureData = minimumTemperature.read();
		Array relativeHumidityData = relativeHumidity.read();
		Array maximumTemperatureData = maximumTemperature.read();
		Array temperatureData = temperature.read();
		int index = 0;
		int entries = 0;

		for(int lat = 0; lat < 721; ++lat) {
			double latitude = 90.0D - (double)lat * 0.25D;

			for(int lon = 0; lon < 1440; ++lon) {
				double longitude = (double)lon * 0.25D;
				if (grid.getCell(latitude, longitude, true) == null) {
					++index;
				} else {
					grid.setCellWindData(latitude, longitude, (double)uData.getFloat(index), (double)vData.getFloat(index));
					grid.setCellSolarData(latitude, longitude, (double)solarFluxData.getFloat(index));
					grid.setCellTemperatureData(latitude, longitude, (double)temperatureData.getFloat(index), (double)minimumTemperatureData.getFloat(index), (double)maximumTemperatureData.getFloat(index), (double)relativeHumidityData.getFloat(index));
					++entries;
					++index;
				}
			}
		}

		gid.close();
		this.logMessage(queueName, "Processing complete. " + entries + " cell entries added.");
	}

	private void logMessage(String queueName, String message) {
		if (this.serverMode) {
			this.getController().getQueueManager().getQueue(queueName).postMessage(message, "GFSForecast");
		} else {
			System.out.println(message);
		}

	}
}
