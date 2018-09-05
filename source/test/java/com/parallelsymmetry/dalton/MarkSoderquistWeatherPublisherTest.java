package com.parallelsymmetry.dalton;

import com.parallelsymmetry.utility.TextUtil;
import org.junit.Test;

import javax.measure.DecimalMeasure;
import javax.measure.Measure;
import javax.measure.quantity.Quantity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class MarkSoderquistWeatherPublisherTest {

	@Test
	public void testPayload() throws Exception {
		MarkSoderquistWeatherPublisher publisher = new MarkSoderquistWeatherPublisher();
		long timestamp = System.currentTimeMillis();

		// Populate the weather data
		Map<WeatherDatumIdentifier, Measure<? extends Number, ? extends Quantity>> data = new HashMap<>();
		data.put( WeatherDatumIdentifier.TIMESTAMP, DecimalMeasure.valueOf( timestamp, SI.MILLI( SI.SECOND ) ) );
		data.put( WeatherDatumIdentifier.TEMPERATURE, DecimalMeasure.valueOf( 60.0, NonSI.FAHRENHEIT ) );
		data.put( WeatherDatumIdentifier.HUMIDITY, DecimalMeasure.valueOf( 25.0, NonSI.FAHRENHEIT ) );
		data.put( WeatherDatumIdentifier.PRESSURE, DecimalMeasure.valueOf( 29.92, NonSI.FAHRENHEIT ) );

		StringBuilder builder = new StringBuilder();
		builder.append( "{" );
		builder.append( "\"timestamp\":" ).append( timestamp );
		builder.append( ",\"temperature\":60.0" );
		builder.append( ",\"pressure\":29.92" );
		builder.append( ",\"humidity\":25.0");
		builder.append( "}" );

		assertThat( new String( publisher.generatePayload( data ), TextUtil.DEFAULT_ENCODING ), is( builder.toString() ) );
	}

}
