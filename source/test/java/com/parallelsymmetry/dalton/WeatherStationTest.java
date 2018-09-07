package com.parallelsymmetry.dalton;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class WeatherStationTest extends WeatherTestCase {

	@Test
	public void testWeatherDataEvent() {
		WeatherStation station = new WeatherStation();
		WeatherDataCollector collector = new WeatherDataCollector();

		station.addPublisher( collector );

		station.weatherDataEvent( generateEvent( null, 60.0, 29.92, 25.0, 10.0, 0.0, 10.0, 0.0, 0.09, 72.0, 40.0 ) );

		assertThat( collector.getEvents().size(), is( 1 ) );
		assertThat( (Double)collector.getEvents().get( 0 ).getValue( WeatherDatumIdentifier.TEMPERATURE ), is( 60.0 ) );
		assertThat( (Double)collector.getEvents().get( 0 ).getValue( WeatherDatumIdentifier.PRESSURE ), is( 29.92 ) );
		assertThat( (Double)collector.getEvents().get( 0 ).getValue( WeatherDatumIdentifier.HUMIDITY ), is( 25.0 ) );

		assertThat( (Double)collector.getEvents().get( 0 ).getValue( WeatherDatumIdentifier.WIND_SPEED ), is( 10.0 ) );
		assertThat( (Double)collector.getEvents().get( 0 ).getValue( WeatherDatumIdentifier.WIND_DIRECTION ), is( 0.0 ) );
		assertThat( (Double)collector.getEvents().get( 0 ).getValue( WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG ), is( 10.0 ) );

		assertThat( (Double)collector.getEvents().get( 0 ).getValue( WeatherDatumIdentifier.RAIN_RATE ), is( 0.0 ) );
		assertThat( (Double)collector.getEvents().get( 0 ).getValue( WeatherDatumIdentifier.RAIN_TOTAL_DAILY ), is( 0.09 ) );

		assertThat( (Double)collector.getEvents().get( 0 ).getValue( WeatherDatumIdentifier.TEMPERATURE_INSIDE ), is( 72.0 ) );
		assertThat( (Double)collector.getEvents().get( 0 ).getValue( WeatherDatumIdentifier.HUMIDITY_INSIDE ), is( 40.0 ) );
	}

}
