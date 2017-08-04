package com.rensmart.gfs;

public class GFSCell {
	private double latitude;
	private double longitude;
	private double windSpeed;
	private double windDirection;
	private double solarFlux;
	private double temperature;
	private double temperatureMinimum;
	private double temperatureMaximum;
	private double relativeHumidity;
	private double humidity;

	public GFSCell() {
	}

	public double getLatitude() {
		return this.latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return this.longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public double getWindSpeed() {
		return this.windSpeed;
	}

	public void setWindSpeed(double windSpeed) {
		this.windSpeed = windSpeed;
	}

	public double getWindDirection() {
		return this.windDirection;
	}

	public void setWindDirection(double windDirection) {
		this.windDirection = windDirection;
	}

	public double getSolarFlux() {
		return this.solarFlux;
	}

	public void setSolarFlux(double solarFlux) {
		this.solarFlux = solarFlux;
	}

	public double getRelativeHumidity() {
		return this.relativeHumidity;
	}

	public void setRelativeHumidity(double relativeHumidity) {
		this.relativeHumidity = relativeHumidity;
	}

	public double getTemperature() {
		return this.temperature;
	}

	public void setTemperature(double temperature) {
		this.temperature = temperature;
	}

	public double getTemperatureMinimum() {
		return this.temperatureMinimum;
	}

	public void setTemperatureMinimum(double temperatureMinimum) {
		this.temperatureMinimum = temperatureMinimum;
	}

	public double getTemperatureMaximum() {
		return this.temperatureMaximum;
	}

	public void setTemperatureMaximum(double temperatureMaximum) {
		this.temperatureMaximum = temperatureMaximum;
	}

	public double getHumidity() {
		return this.humidity;
	}

	public void setHumidity(double humidity) {
		this.humidity = humidity;
	}
}