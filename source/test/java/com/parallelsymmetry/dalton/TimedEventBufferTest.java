package com.parallelsymmetry.dalton;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TimedEventBufferTest extends WeatherTestCase {

	@Test
	public void testGetAverage() {
		TimedEventBuffer buffer = generateBuffer();
		assertThat( buffer.getAverage( WeatherDatumIdentifier.TEMPERATURE ), is( 60.9 ) );
		assertThat( buffer.getAverage( WeatherDatumIdentifier.WIND_SPEED ), is( 10.0 ) );
		assertThat( buffer.getEvents().size(), is( 6 ) );
	}

	@Test
	public void testGetMinimum() {
		TimedEventBuffer buffer = generateBuffer();
		assertThat( buffer.getMinimum( WeatherDatumIdentifier.TEMPERATURE ), is( 60.4 ) );
		assertThat( buffer.getMinimum( WeatherDatumIdentifier.WIND_SPEED ), is( 9.0 ) );
		assertThat( buffer.getEvents().size(), is( 6 ) );
	}

	@Test
	public void testGetMaximum() {
		TimedEventBuffer buffer = generateBuffer();
		assertThat( buffer.getMaximum( WeatherDatumIdentifier.TEMPERATURE ), is( 61.4 ) );
		assertThat( buffer.getMaximum( WeatherDatumIdentifier.WIND_SPEED ), is( 11.0 ) );
		assertThat( buffer.getEvents().size(), is( 6 ) );
	}

	@Test
	public void testGetTrendPerHour() {
		TimedEventBuffer buffer = generateBuffer();
		assertThat( buffer.getTrendPerHour( WeatherDatumIdentifier.TEMPERATURE ), is( 360.00000000000034 ) );
		assertThat( buffer.getTrendPerHour( WeatherDatumIdentifier.WIND_SPEED ), is( 0.0 ) );
		assertThat( buffer.getEvents().size(), is( 6 ) );
	}

	private TimedEventBuffer generateBuffer() {
		TimedEventBuffer buffer = new TimedEventBuffer( 10000 );

		long time= System.currentTimeMillis();
		buffer.post( generateEvent( time + 0000L, 60.0, 29.92, 25.0, 10.0, 0.0, 10.0, 0.0, 0.09, 72.0, 40.0 ) );
		buffer.post( generateEvent( time + 2000L, 60.2, 29.92, 25.0, 10.0, 0.0, 10.0, 0.0, 0.09, 72.0, 40.0 ) );
		buffer.post( generateEvent( time + 4000L, 60.4, 29.92, 25.0, 9.0, 0.0, 10.0, 0.0, 0.09, 72.0, 40.0 ) );
		buffer.post( generateEvent( time + 6000L, 60.6, 29.92, 25.0, 10.0, 0.0, 10.0, 0.0, 0.09, 72.0, 40.0 ) );
		buffer.post( generateEvent( time + 8000L, 60.8, 29.92, 25.0, 11.0, 0.0, 10.0, 0.0, 0.09, 72.0, 40.0 ) );
		buffer.post( generateEvent( time + 10000L, 61.0, 29.92, 25.0, 11.0, 0.0, 10.0, 0.0, 0.09, 72.0, 40.0 ) );
		buffer.post( generateEvent( time + 12000L, 61.2, 29.92, 25.0, 10.0, 0.0, 10.0, 0.0, 0.09, 72.0, 40.0 ) );
		buffer.post( generateEvent( time + 14000L, 61.4, 29.92, 25.0, 9.0, 0.0, 10.0, 0.0, 0.09, 72.0, 40.0 ) );

		return buffer;
	}
}
