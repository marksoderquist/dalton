package com.parallelsymmetry.dalton;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BluewingWeatherStationTest extends WeatherTestCase {

	@Test
	public void testWeatherDataEvent() {
		WeatherStation station = new BluewingWeatherStation();
		WeatherDataCollector collector = new WeatherDataCollector();

		station.addPublisher( collector );

		station.weatherDataEvent( generateEvent( null, 60.0, 29.92, 25.0, 10.0, 0.0, 10.0, 0.0, 0.09, 72.0, 40.0 ) );

		assertThat( collector.getEvents().size() ).isEqualTo( 1 );
		assertThat( (Double)collector.getEvents().get( 0 ).getValue( WeatherDatumIdentifier.TEMPERATURE ) ).isEqualTo( 60.0 );
		assertThat( (Double)collector.getEvents().get( 0 ).getValue( WeatherDatumIdentifier.PRESSURE ) ).isEqualTo( 30.1325 );
		assertThat( (Double)collector.getEvents().get( 0 ).getValue( WeatherDatumIdentifier.HUMIDITY ) ).isEqualTo( 25.0 );

		assertThat( (Double)collector.getEvents().get( 0 ).getValue( WeatherDatumIdentifier.WIND_SPEED ) ).isEqualTo( 10.0 );
		assertThat( (Double)collector.getEvents().get( 0 ).getValue( WeatherDatumIdentifier.WIND_DIRECTION ) ).isEqualTo( 0.0 );
		assertThat( (Double)collector.getEvents().get( 0 ).getValue( WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG ) ).isEqualTo( 10.0 );

		assertThat( (Double)collector.getEvents().get( 0 ).getValue( WeatherDatumIdentifier.RAIN_RATE ) ).isEqualTo( 0.0 );
		assertThat( (Double)collector.getEvents().get( 0 ).getValue( WeatherDatumIdentifier.RAIN_TOTAL_DAILY ) ).isEqualTo( 0.09 );

		assertThat( (Double)collector.getEvents().get( 0 ).getValue( WeatherDatumIdentifier.TEMPERATURE_INSIDE ) ).isEqualTo( 72.0 );
		assertThat( (Double)collector.getEvents().get( 0 ).getValue( WeatherDatumIdentifier.HUMIDITY_INSIDE ) ).isEqualTo( 40.0 );
	}

}
