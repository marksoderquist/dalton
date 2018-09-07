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
		assertThat( (Double)collector.getEvents().get( 0 ).get( WeatherDatumIdentifier.TEMPERATURE ).getValue(), is( 60.0 ) );
		assertThat( (Double)collector.getEvents().get( 0 ).get( WeatherDatumIdentifier.PRESSURE ).getValue(), is( 29.92 ) );
		assertThat( (Double)collector.getEvents().get( 0 ).get( WeatherDatumIdentifier.HUMIDITY ).getValue(), is( 25.0 ) );

		assertThat( (Double)collector.getEvents().get( 0 ).get( WeatherDatumIdentifier.WIND_SPEED ).getValue(), is( 10.0 ) );
		assertThat( (Double)collector.getEvents().get( 0 ).get( WeatherDatumIdentifier.WIND_DIRECTION ).getValue(), is( 0.0 ) );
		assertThat( (Double)collector.getEvents().get( 0 ).get( WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG ).getValue(), is( 10.0 ) );

		assertThat( (Double)collector.getEvents().get( 0 ).get( WeatherDatumIdentifier.RAIN_RATE ).getValue(), is( 0.0 ) );
		assertThat( (Double)collector.getEvents().get( 0 ).get( WeatherDatumIdentifier.RAIN_TOTAL_DAILY ).getValue(), is( 0.09 ) );

		assertThat( (Double)collector.getEvents().get( 0 ).get( WeatherDatumIdentifier.TEMPERATURE_INSIDE ).getValue(), is( 72.0 ) );
		assertThat( (Double)collector.getEvents().get( 0 ).get( WeatherDatumIdentifier.HUMIDITY_INSIDE ).getValue(), is( 40.0 ) );
	}

}
