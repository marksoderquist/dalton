package com.parallelsymmetry.dalton;

import javax.measure.Measure;
import javax.measure.quantity.Quantity;

public class WeatherDatum {

	private WeatherDatumIdentifier identifier;

	private Measure<? extends Number, ? extends Quantity> measure;

	public WeatherDatum( WeatherDatumIdentifier identifier, Measure<? extends Number, ? extends Quantity> measure ) {
		this.identifier = identifier;
		this.measure = measure;
	}

	public WeatherDatumIdentifier getIdentifier() {
		return identifier;
	}

	public Measure<? extends Number, ? extends Quantity> getMeasure() {
		return measure;
	}

	@Override
	public String toString() {
		return identifier.name() + " = " + measure.toString();
	}

}
