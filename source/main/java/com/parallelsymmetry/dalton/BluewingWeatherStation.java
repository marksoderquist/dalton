package com.parallelsymmetry.dalton;

import javax.measure.DecimalMeasure;
import javax.measure.unit.NonSI;

public class BluewingWeatherStation extends WeatherStation {

	@Override
	public void weatherDataEvent( WeatherDataEvent event ) {
		// Air pressure adjustment
		Double pressure = event.getValue( WeatherDatumIdentifier.PRESSURE );
		pressure = 0.75 * pressure + 7.6925;
		WeatherDatum pressureDatum = new WeatherDatum( WeatherDatumIdentifier.PRESSURE, DecimalMeasure.valueOf( pressure, NonSI.INCH_OF_MERCURY ) );
		event.add( pressureDatum );
		super.weatherDataEvent( event );
	}

}
