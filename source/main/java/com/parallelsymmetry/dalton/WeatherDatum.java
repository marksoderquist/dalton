package com.parallelsymmetry.dalton;

import javax.measure.Measure;

public class WeatherDatum {

	private WeatherDatumIdentifier identifier;

	private Measure<?, ?> measure;

	public WeatherDatum( WeatherDatumIdentifier identifier, Measure<?, ?> measure ) {
		this.identifier = identifier;
		this.measure = measure;
	}

	public WeatherDatumIdentifier getIdentifier() {
		return identifier;
	}

	public Measure<?, ?> getMeasure() {
		return measure;
	}

	@Override
	public String toString() {
		return identifier.name() + " = " + measure.toString();
	}
	
}
