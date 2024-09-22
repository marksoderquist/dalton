package com.parallelsymmetry.dalton;

import org.junit.jupiter.api.Test;

import javax.measure.DecimalMeasure;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import static org.assertj.core.api.Assertions.assertThat;

public class PerformWeatherPublisherTest {

	@Test
	public void testValue() {
		Program program = new Program();
		WeatherStation station = new WeatherStation();
		PerformWeatherPublisher publisher = new PerformWeatherPublisher( program );
		long timestamp = System.currentTimeMillis();

		// Populate the weather data
		WeatherDataEvent event = new WeatherDataEvent();
		event.add( new WeatherDatum( WeatherDatumIdentifier.TIMESTAMP, DecimalMeasure.valueOf( timestamp, SI.MILLI( SI.SECOND ) ) ) );
		event.add( new WeatherDatum( WeatherDatumIdentifier.TEMPERATURE, DecimalMeasure.valueOf( 60.0, NonSI.FAHRENHEIT ) ) );
		event.add( new WeatherDatum( WeatherDatumIdentifier.HUMIDITY, DecimalMeasure.valueOf( 25.0, NonSI.FAHRENHEIT ) ) );
		event.add( new WeatherDatum( WeatherDatumIdentifier.PRESSURE, DecimalMeasure.valueOf( 29.92, NonSI.FAHRENHEIT ) ) );

		// Generate the comparison string
		StringBuilder builder = new StringBuilder();
		builder.append( "https://perform.southbranchcontrols.com/api/stations/39/data/feed" );
		builder.append( "?accesskey=b8f4c216-c54f-4c2f-bc33-5ab120accca3" );
		builder.append( "&vbit-0=1" );
		builder.append( "&mem-0=" ).append( timestamp );
		builder.append( "&mem-10=4633641066610819072" );
		builder.append( "&mem-11=4629114948985311724" );
		builder.append( "&mem-12=4627730092099895296" );

		// Test generated payload
		assertThat( publisher.generateRequest( station, event ) ).contains( builder.toString() );
	}

}
