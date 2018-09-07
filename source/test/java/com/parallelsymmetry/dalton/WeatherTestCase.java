package com.parallelsymmetry.dalton;

import junit.framework.TestCase;

import javax.measure.DecimalMeasure;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class WeatherTestCase extends TestCase {

	protected WeatherDataEvent generateEvent( Long timestamp, Double tempOutside, Double pressure, Double humidOutside, Double windSpeed, Double windDirection, Double windSpeedTenMinAvg, Double rainRate, Double rainTotalDaily, Double tempInside, Double humidInside ) {
		WeatherDatum timestampDatum = new WeatherDatum( WeatherDatumIdentifier.TIMESTAMP, DecimalMeasure.valueOf( timestamp == null ? System.currentTimeMillis() : timestamp, SI.MILLI( SI.SECOND ) ) );

		WeatherDatum temperatureDatum = new WeatherDatum( WeatherDatumIdentifier.TEMPERATURE, DecimalMeasure.valueOf( tempOutside, NonSI.FAHRENHEIT ) );
		WeatherDatum pressureDatum = new WeatherDatum( WeatherDatumIdentifier.PRESSURE, DecimalMeasure.valueOf( pressure, NonSI.INCH_OF_MERCURY ) );
		WeatherDatum humidityDatum = new WeatherDatum( WeatherDatumIdentifier.HUMIDITY, DecimalMeasure.valueOf( humidOutside, NonSI.PERCENT ) );

		WeatherDatum windSpeedDatum = new WeatherDatum( WeatherDatumIdentifier.WIND_SPEED, DecimalMeasure.valueOf( windSpeed, NonSI.MILES_PER_HOUR ) );
		WeatherDatum windDirectionDatum = new WeatherDatum( WeatherDatumIdentifier.WIND_DIRECTION, DecimalMeasure.valueOf( windDirection, NonSI.DEGREE_ANGLE ) );
		WeatherDatum windSpeedTenMinAvgDatum = new WeatherDatum( WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG, DecimalMeasure.valueOf( windSpeedTenMinAvg, NonSI.MILES_PER_HOUR ) );

		WeatherDatum rainRateDatum = new WeatherDatum( WeatherDatumIdentifier.RAIN_RATE, DecimalMeasure.valueOf( rainRate, NonSI.INCH.divide( NonSI.HOUR ) ) );
		WeatherDatum rainTotalDailyDatum = new WeatherDatum( WeatherDatumIdentifier.RAIN_TOTAL_DAILY, DecimalMeasure.valueOf( rainTotalDaily, NonSI.INCH ) );

		WeatherDatum temperatureInsideDatum = new WeatherDatum( WeatherDatumIdentifier.TEMPERATURE_INSIDE, DecimalMeasure.valueOf( tempInside, NonSI.FAHRENHEIT ) );
		WeatherDatum humidityInsideDatum = new WeatherDatum( WeatherDatumIdentifier.HUMIDITY_INSIDE, DecimalMeasure.valueOf( humidInside, NonSI.PERCENT ) );

		return new WeatherDataEvent( timestampDatum, temperatureDatum, pressureDatum, humidityDatum, windSpeedDatum, windDirectionDatum, rainRateDatum, rainTotalDailyDatum, temperatureInsideDatum, humidityInsideDatum, windSpeedTenMinAvgDatum );
	}

	protected class WeatherDataCollector implements WeatherDataPublisher {

		private List<WeatherDataEvent> events;

		public WeatherDataCollector() {
			events = new CopyOnWriteArrayList<>();
		}

		public List<WeatherDataEvent> getEvents() {
			return events;
		}

		@Override
		public int publish( WeatherStation station, WeatherDataEvent event ) throws IOException {
			events.add( event );
			return 200;
		}
	}

}
