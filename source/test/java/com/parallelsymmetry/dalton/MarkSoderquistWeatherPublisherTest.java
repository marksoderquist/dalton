package com.parallelsymmetry.dalton;

import com.parallelsymmetry.utility.TextUtil;
import org.junit.jupiter.api.Test;

import javax.measure.DecimalMeasure;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import static org.assertj.core.api.Assertions.assertThat;

public class MarkSoderquistWeatherPublisherTest {

	@Test
	public void testPayload() throws Exception {
		MarkSoderquistWeatherPublisher publisher = new MarkSoderquistWeatherPublisher();
		long timestamp = System.currentTimeMillis();

		// Populate the weather data
		WeatherDataEvent event = new WeatherDataEvent();
		event.add( new WeatherDatum( WeatherDatumIdentifier.TIMESTAMP, DecimalMeasure.valueOf( timestamp, SI.MILLI( SI.SECOND ) ) ) );
		event.add( new WeatherDatum( WeatherDatumIdentifier.TEMPERATURE, DecimalMeasure.valueOf( 60.0, NonSI.FAHRENHEIT ) ) );
		event.add( new WeatherDatum( WeatherDatumIdentifier.HUMIDITY, DecimalMeasure.valueOf( 25.0, NonSI.FAHRENHEIT ) ) );
		event.add( new WeatherDatum( WeatherDatumIdentifier.PRESSURE, DecimalMeasure.valueOf( 29.92, NonSI.FAHRENHEIT ) ) );

		StringBuilder builder = new StringBuilder();
		builder.append( "{" );
		builder.append( "\"unitSystem\":\"IMPERIAL\",");
		builder.append( "\"timestamp\":" ).append( timestamp );
		builder.append( ",\"temperature\":60.0" );
		builder.append( ",\"pressure\":29.92" );
		builder.append( ",\"humidity\":25.0" );
		builder.append( "}" );

		assertThat( new String( publisher.generatePayload( event ), TextUtil.DEFAULT_ENCODING ) ).isEqualTo( builder.toString() );
	}

}
