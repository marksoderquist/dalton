package com.parallelsymmetry.dalton;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TimedEventBufferTest extends WeatherTestCase {

	@Test
	public void testGetAverage() {
		TimedEventBuffer buffer = generateBuffer();
		assertThat( buffer.getAverage( WeatherDatumIdentifier.TEMPERATURE ) ).isEqualTo( 60.9 );
		assertThat( buffer.getAverage( WeatherDatumIdentifier.WIND_SPEED ) ).isEqualTo( 10.0 );
		assertThat( buffer.getEvents().size() ).isEqualTo( 6 );
	}

	@Test
	public void testGetMinimum() {
		TimedEventBuffer buffer = generateBuffer();
		assertThat( buffer.getMinimum( WeatherDatumIdentifier.TEMPERATURE ) ).isEqualTo( 60.4 );
		assertThat( buffer.getMinimum( WeatherDatumIdentifier.WIND_SPEED ) ).isEqualTo( 9.0 );
		assertThat( buffer.getEvents().size() ).isEqualTo( 6 );
	}

	@Test
	public void testGetMaximum() {
		TimedEventBuffer buffer = generateBuffer();
		assertThat( buffer.getMaximum( WeatherDatumIdentifier.TEMPERATURE ) ).isEqualTo( 61.4 );
		assertThat( buffer.getMaximum( WeatherDatumIdentifier.WIND_SPEED ) ).isEqualTo( 11.0 );
		assertThat( buffer.getEvents().size() ).isEqualTo( 6 );
	}

	@Test
	public void testGetTrendPerHour() {
		TimedEventBuffer buffer = generateBuffer();
		assertThat( buffer.getTrendPerHour( WeatherDatumIdentifier.TEMPERATURE ) ).isEqualTo( 360.00000000000034 );
		assertThat( buffer.getTrendPerHour( WeatherDatumIdentifier.WIND_SPEED ) ).isEqualTo( 0.0 );
		assertThat( buffer.getEvents().size() ).isEqualTo( 6 );
	}

	@Test
	public void testWindAverage() {
		TimedEventBuffer buffer = generateWindBuffer();
		assertThat( TimedEventBuffer.getAngleInDegrees( buffer.getAverageVector( WeatherDatumIdentifier.WIND_SPEED, WeatherDatumIdentifier.WIND_DIRECTION ) ) ).isEqualTo( 350.0 );
	}

	private TimedEventBuffer generateBuffer() {
		TimedEventBuffer buffer = new TimedEventBuffer( 10000 );

		long time = System.currentTimeMillis();
		// The first two values fall out of the buffer
		buffer.post( generateEvent( time + 0000L, 60.0, 29.92, 25.0, 10.0, 0.0, 10.0, 0.0, 0.09, 72.0, 40.0 ) );
		buffer.post( generateEvent( time + 2000L, 60.2, 29.92, 25.0, 10.0, 330.0, 10.0, 0.0, 0.09, 72.0, 40.0 ) );
		buffer.post( generateEvent( time + 4000L, 60.4, 29.92, 25.0, 9.0, 340.0, 10.0, 0.0, 0.09, 72.0, 40.0 ) );
		buffer.post( generateEvent( time + 6000L, 60.6, 29.92, 25.0, 10.0, 350.0, 10.0, 0.0, 0.09, 72.0, 40.0 ) );
		buffer.post( generateEvent( time + 8000L, 60.8, 29.92, 25.0, 11.0, 0.0, 10.0, 0.0, 0.09, 72.0, 40.0 ) );
		buffer.post( generateEvent( time + 10000L, 61.0, 29.92, 25.0, 11.0, 10.0, 10.0, 0.0, 0.09, 72.0, 40.0 ) );
		buffer.post( generateEvent( time + 12000L, 61.2, 29.92, 25.0, 10.0, 340.0, 10.0, 0.0, 0.09, 72.0, 40.0 ) );
		buffer.post( generateEvent( time + 14000L, 61.4, 29.92, 25.0, 9.0, 350.0, 10.0, 0.0, 0.09, 72.0, 40.0 ) );

		return buffer;
	}

	private TimedEventBuffer generateWindBuffer() {
		TimedEventBuffer buffer = new TimedEventBuffer( 10000 );

		long time = System.currentTimeMillis();
		// The first two values fall out of the buffer
		buffer.post( generateWindEvent( time + 0000L, 10.0, 0.0 ) );
		buffer.post( generateWindEvent( time + 2000L, 14.0, 330.0 ) );
		buffer.post( generateWindEvent( time + 4000L, 8.0, 340.0 ) );
		buffer.post( generateWindEvent( time + 6000L, 8.0, 0.0 ) );
		buffer.post( generateWindEvent( time + 8000L, 12.0, 345.0 ) );
		buffer.post( generateWindEvent( time + 10000L, 8.0, 350.0 ) );
		buffer.post( generateWindEvent( time + 12000L, 14.0, 350.0 ) );
		buffer.post( generateWindEvent( time + 14000L, 12.0, 355.0 ) );

		return buffer;
	}

}
