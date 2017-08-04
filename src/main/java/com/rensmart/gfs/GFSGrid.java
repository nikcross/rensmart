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

	public GFSGrid(double topLatitude, double bottomLatitude, double westLongitude, double eastLongitude) {
		this.topLatitude = topLatitude;
		this.bottomLatitude = bottomLatitude;
		this.westLongitude = westLongitude;
		this.eastLongitude = eastLongitude;
		this.gridWidth = (int)((eastLongitude - westLongitude) * 2.0D) + 1;
		this.gridHeight = (int)((topLatitude - bottomLatitude) * 2.0D) + 1;
		this.grid = new GFSCell[this.gridWidth][this.gridHeight];
	}

	public LatLong getGFSCellLocation(int index, String region) {
		double longitude = 0.0D;
		double latitude = 0.0D;
		if(region == "WEST") {
			longitude = (double)(index % 24) / 2.0D;
			latitude = (double)(index / 24) / 2.0D;
		} else if(region == "EAST") {
			longitude = (double)(index % 5) / 2.0D;
			latitude = (double)(index / 5) / 2.0D;
			longitude += 12.0D;
		}

		LatLong location = new LatLong(latitude + this.bottomLatitude, longitude + this.westLongitude);
		return location;
	}

	public void setCellWindData(double latitude, double longitude, double windSpeedU, double windSpeedV) {
		GFSCell cell = this.getCell(latitude, longitude, true);
		if(cell != null) {
			double windSpeed = Math.pow(windSpeedU * windSpeedU + windSpeedV * windSpeedV, 0.5D);
			double windDirection = Math.cos(windSpeedU / windSpeed) * 180.0D / 3.141592653589793D;
			windDirection = 360.0D - (windDirection + 90.0D);
			cell.setWindSpeed(windSpeed);
			cell.setWindDirection(windDirection);
		}
	}

	public void setCellSolarData(double latitude, double longitude, double solarFlux) {
		GFSCell cell = this.getCell(latitude, longitude, true);
		if(cell != null) {
			cell.setSolarFlux(solarFlux);
		}
	}

	public void setCellTemperatureData(double latitude, double longitude, double temperature, double minTemperature, double maxTemperature, double relativeHumidity) {
		GFSCell cell = this.getCell(latitude, longitude, true);
		if(cell != null) {
			cell.setTemperature(temperature - 273.15D);
			cell.setTemperatureMinimum(minTemperature - 273.15D);
			cell.setTemperatureMaximum(maxTemperature - 273.15D);
			cell.setRelativeHumidity(relativeHumidity);
		}
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

	private GFSCell getCell(double latitude, double longitude, boolean createIfNotFound) {
		int x = (int)((longitude - this.westLongitude) * 2.0D);
		int y = (int)((latitude - this.bottomLatitude) * 2.0D);
		if(x < this.grid.length - 1 && y < this.grid[x].length - 1) {
			if(this.grid[x][y] == null) {
				if(!createIfNotFound) {
					return null;
				}

				System.out.println("Creating for " + latitude + "," + longitude);
				GFSCell cell = new GFSCell();
				cell.setLatitude(latitude);
				cell.setLongitude(longitude);
				this.grid[x][y] = cell;
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
				if(this.grid[i][j] != null) {
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