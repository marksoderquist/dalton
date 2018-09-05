package com.parallelsymmetry.dalton;

import junit.framework.TestCase;
import org.junit.Test;

import javax.measure.DecimalMeasure;
import javax.measure.Measure;
import javax.measure.quantity.Quantity;
import javax.measure.unit.NonSI;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class WeatherStationTest extends TestCase {

	@Test
	public void testWeatherDataEvent() {
		WeatherStation station = new WeatherStation();
		WeatherDataCollector collector = new WeatherDataCollector();

		station.addPublisher( collector );

		station.weatherDataEvent( generateDavisWeatherDataEvent( 60, 29.92, 25, 10, 0, 10, 0, 0.09, 72, 40 ) );

		assertThat( collector.getEvents().size(), is( 1 ) );
		assertThat( (Double)collector.getEvents().get( 0 ).get( WeatherDatumIdentifier.TEMPERATURE ).getValue(), is( 60.0 ) );
		assertThat( (Double)collector.getEvents().get( 0 ).get( WeatherDatumIdentifier.PRESSURE ).getValue(), is( 29.92 ) );
		assertThat( (Double)collector.getEvents().get( 0 ).get( WeatherDatumIdentifier.HUMIDITY ).getValue(), is( 25.0 ) );

		assertThat( (Double)collector.getEvents().get( 0 ).get( WeatherDatumIdentifier.WIND_SPEED_CURRENT ).getValue(), is( 10.0 ) );
		assertThat( (Double)collector.getEvents().get( 0 ).get( WeatherDatumIdentifier.WIND_DIRECTION ).getValue(), is( 0.0 ) );
		assertThat( (Double)collector.getEvents().get( 0 ).get( WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG ).getValue(), is( 10.0 ) );

		assertThat( (Double)collector.getEvents().get( 0 ).get( WeatherDatumIdentifier.RAIN_RATE ).getValue(), is( 0.0 ) );
		assertThat( (Double)collector.getEvents().get( 0 ).get( WeatherDatumIdentifier.RAIN_TOTAL_DAILY ).getValue(), is( 0.09 ) );

		assertThat( (Double)collector.getEvents().get( 0 ).get( WeatherDatumIdentifier.TEMPERATURE_INSIDE ).getValue(), is( 72.0 ) );
		assertThat( (Double)collector.getEvents().get( 0 ).get( WeatherDatumIdentifier.HUMIDITY_INSIDE ).getValue(), is( 40.0 ) );
	}

	private WeatherDataEvent generateDavisWeatherDataEvent( double tempOutside, double pressure, double humidOutside, double windSpeed, double windDirection, double windSpeedTenMinAvg, double rainRate, double rainTotalDaily, double tempInside, double humidInside ) {
		WeatherDatum temperatureDatum = new WeatherDatum( WeatherDatumIdentifier.TEMPERATURE, DecimalMeasure.valueOf( tempOutside, NonSI.FAHRENHEIT ) );
		WeatherDatum pressureDatum = new WeatherDatum( WeatherDatumIdentifier.PRESSURE, DecimalMeasure.valueOf( pressure, NonSI.INCH_OF_MERCURY ) );
		WeatherDatum humidityDatum = new WeatherDatum( WeatherDatumIdentifier.HUMIDITY, DecimalMeasure.valueOf( humidOutside, NonSI.PERCENT ) );

		WeatherDatum windSpeedDatum = new WeatherDatum( WeatherDatumIdentifier.WIND_SPEED_CURRENT, DecimalMeasure.valueOf( windSpeed, NonSI.MILES_PER_HOUR ) );
		WeatherDatum windDirectionDatum = new WeatherDatum( WeatherDatumIdentifier.WIND_DIRECTION, DecimalMeasure.valueOf( windDirection, NonSI.DEGREE_ANGLE ) );
		WeatherDatum windSpeedTenMinAvgDatum = new WeatherDatum( WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG, DecimalMeasure.valueOf( windSpeedTenMinAvg, NonSI.MILES_PER_HOUR ) );

		WeatherDatum rainRateDatum = new WeatherDatum( WeatherDatumIdentifier.RAIN_RATE, DecimalMeasure.valueOf( rainRate, NonSI.INCH.divide( NonSI.HOUR ) ) );
		WeatherDatum rainTotalDailyDatum = new WeatherDatum( WeatherDatumIdentifier.RAIN_TOTAL_DAILY, DecimalMeasure.valueOf( rainTotalDaily, NonSI.INCH ) );

		WeatherDatum temperatureInsideDatum = new WeatherDatum( WeatherDatumIdentifier.TEMPERATURE_INSIDE, DecimalMeasure.valueOf( tempInside, NonSI.FAHRENHEIT ) );
		WeatherDatum humidityInsideDatum = new WeatherDatum( WeatherDatumIdentifier.HUMIDITY_INSIDE, DecimalMeasure.valueOf( humidInside, NonSI.PERCENT ) );

		return new WeatherDataEvent( temperatureDatum, pressureDatum, humidityDatum, windSpeedDatum, windDirectionDatum, rainRateDatum, rainTotalDailyDatum, temperatureInsideDatum, humidityInsideDatum, windSpeedTenMinAvgDatum );
	}

	private class WeatherDataCollector implements WeatherDataPublisher {

		private List<Map<WeatherDatumIdentifier, Measure<? extends Number, ? extends Quantity>>> events;

		public WeatherDataCollector() {
			events = new CopyOnWriteArrayList<>();
		}

		public List<Map<WeatherDatumIdentifier, Measure<? extends Number, ? extends Quantity>>> getEvents() {
			return events;
		}

		@Override
		public int publish( Map<WeatherDatumIdentifier, Measure<? extends Number, ? extends Quantity>> data ) throws IOException {
			events.add( data );
			return 200;
		}
	}
}
