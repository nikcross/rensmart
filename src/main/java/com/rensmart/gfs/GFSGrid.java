package com.rensmart.gfs;

import java.util.Date;

public class GFSGrid {
	private double topLatitude;
	private double bottomLatitude;
	private double westLongitude;
	private double eastLongitude;
	private GFSCell[][] grid;
	private int gridWidth;
	private int gridHeight;
	private String timeStamp = null;
	private int entries = 0;

	public GFSGrid(double topLatitude, double bottomLatitude, double westLongitude, double eastLongitude) {
		if (westLongitude < 0.0D) {
			westLongitude += 360.0D;
		}

		if (eastLongitude < 0.0D) {
			eastLongitude += 360.0D;
		}

		if (westLongitude > eastLongitude) {
			eastLongitude += 360.0D;
		}

		this.gridWidth = (int)((eastLongitude - westLongitude) * 4.0D) + 1;
		this.gridHeight = (int)((topLatitude - bottomLatitude) * 4.0D) + 1;
		this.grid = new GFSCell[this.gridWidth][this.gridHeight];
		this.topLatitude = topLatitude;
		this.bottomLatitude = bottomLatitude;
		this.westLongitude = westLongitude;
		this.eastLongitude = eastLongitude;
	}

	public void setCellWindData(double latitude, double longitude, double windSpeedU, double windSpeedV) {
		GFSCell cell = this.getCell(latitude, longitude, true);
		if (cell != null) {
			double windSpeed = Math.pow(windSpeedU * windSpeedU + windSpeedV * windSpeedV, 0.5D);
			double windDirection = Math.cos(windSpeedU / windSpeed) * 180.0D / 3.141592653589793D;
			windDirection = 360.0D - (windDirection + 90.0D);
			cell.setWindSpeed(windSpeed);
			cell.setWindDirection(windDirection);
		}

	}

	public void setCellSolarData(double latitude, double longitude, double solarFlux) {
		GFSCell cell = this.getCell(latitude, longitude, true);
		if (cell != null) {
			cell.setSolarFlux(solarFlux);
		}

	}

	public void setCellTemperatureData(double latitude, double longitude, double temperature, double minTemperature, double maxTemperature, double relativeHumidity) {
		GFSCell cell = this.getCell(latitude, longitude, true);
		if (cell != null) {
			cell.setTemperature(temperature - 273.15D);
			cell.setTemperatureMinimum(minTemperature - 273.15D);
			cell.setTemperatureMaximum(maxTemperature - 273.15D);
			cell.setRelativeHumidity(relativeHumidity);
		}

	}

	public int getEntries() {
		return this.entries;
	}

	public double getTopLatitude() {
		return this.topLatitude;
	}

	public void setTopLatitude(double topLatitude) {
		this.topLatitude = topLatitude;
	}

	public double getBottomLatitude() {
		return this.bottomLatitude;
	}

	public void setBottomLatitude(double bottomLatitude) {
		this.bottomLatitude = bottomLatitude;
	}

	public double getWestLongitude() {
		return this.westLongitude;
	}

	public void setWestLongitude(double westLongitude) {
		this.westLongitude = westLongitude;
	}

	public double getEastLongitude() {
		return this.eastLongitude;
	}

	public void setEastLongitude(double eastLongitude) {
		this.eastLongitude = eastLongitude;
	}

	public GFSCell getCell(double latitude, double longitude) {
		return this.getCell(latitude, longitude, false);
	}

	public GFSCell getCell(double latitude, double longitude, boolean createIfNotFound) {
		double latitudeOffset = latitude - this.bottomLatitude;
		double longitudeOffset = longitude;
		if (longitude < 180.0D) {
			longitudeOffset = longitude + 360.0D;
		}

		longitudeOffset -= this.westLongitude;
		int y = (int)(latitudeOffset * 4.0D);
		int x = (int)(longitudeOffset * 4.0D);
		if (x >= 0 && y >= 0 && x < this.grid.length - 1 && y < this.grid[x].length - 1) {
			if (this.grid[x][y] == null) {
				if (!createIfNotFound) {
					return null;
				}

				GFSCell cell = new GFSCell();
				cell.setLatitude(latitude);
				cell.setLongitude(longitude);
				this.grid[x][y] = cell;
				++this.entries;
			}

			return this.grid[x][y];
		} else {
			return null;
		}
	}

	public GFSCell[][] getData() {
		return this.grid;
	}

	public String toString() {
		StringBuffer data = new StringBuffer(this.timeStamp + "\n");
		data.append("Run Time: " + this.getRunTime() + "\n");
		data.append("--\t");
		double dLon = (this.eastLongitude - this.westLongitude) / (double)this.grid[0].length;

		for(int i = 0; i < this.grid[0].length; ++i) {
			data.append(this.westLongitude + (double)i * dLon + "\t");
		}

		data.append("\n");
		double dLat = (this.topLatitude - this.bottomLatitude) / (double)this.grid.length;

		for(int i = 0; i < this.grid.length; ++i) {
			data.append(this.topLatitude - (double)i * dLat + "\t");

			for(int j = 0; j < this.grid[i].length; ++j) {
				if (this.grid[i][j] != null) {
					data.append(this.grid[i][j].getSolarFlux() + "\t");
				} else {
					data.append("--\t");
				}
			}

			data.append("\n");
		}

		return data.toString();
	}

	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}

	public String getTimeStamp() {
		return this.timeStamp;
	}

	public Date getRunTime() {
		String time = this.timeStamp.split(" ")[3];
		int year = Integer.parseInt(time.substring(0, 4)) - 1900;
		int month = Integer.parseInt(time.substring(5, 7)) - 1;
		int date = Integer.parseInt(time.substring(8, 10));
		int hours = Integer.parseInt(time.substring(11, 13));
		Date runTime = new Date(year, month, date, hours, 0);
		return runTime;
	}
}
