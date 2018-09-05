package com.parallelsymmetry.dalton;

import com.parallelsymmetry.utility.DateUtil;
import com.parallelsymmetry.utility.TextUtil;
import org.junit.Test;

import javax.measure.DecimalMeasure;
import javax.measure.Measure;
import javax.measure.quantity.Quantity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class WeatherUndergroundPublisherTest {

	@Test
	public void testDateFormat() {
		assertThat( DateUtil.format( new Date( 0 ), WeatherUndergroundPublisher.WUNDERGROUND_DATE_FORMAT, TimeZone.getTimeZone( "UTC" ) ), is( "1970-01-01+00%3A00%3A00" ) );
	}

	@Test
	public void testPayload() throws Exception {
		Program program = new Program();
		WeatherUndergroundPublisher publisher = new WeatherUndergroundPublisher( program );
		long timestamp = System.currentTimeMillis();
		String release = program.getCard().getRelease().toHumanString( DateUtil.DEFAULT_TIME_ZONE );

		// Populate the weather data
		Map<WeatherDatumIdentifier, Measure<? extends Number, ? extends Quantity>> data = new HashMap<>();
		data.put( WeatherDatumIdentifier.TIMESTAMP, DecimalMeasure.valueOf( timestamp, SI.MILLI( SI.SECOND ) ) );
		data.put( WeatherDatumIdentifier.TEMPERATURE, DecimalMeasure.valueOf( 60.0, NonSI.FAHRENHEIT ) );
		data.put( WeatherDatumIdentifier.HUMIDITY, DecimalMeasure.valueOf( 25.0, NonSI.FAHRENHEIT ) );
		data.put( WeatherDatumIdentifier.PRESSURE, DecimalMeasure.valueOf( 29.92, NonSI.FAHRENHEIT ) );

		// Generate the comparison string
		StringBuilder builder = new StringBuilder();
		builder.append( "http://rtupdate.wunderground.com/weatherstation/updateweatherstation.php" );
		builder.append( "?ID=KUTRIVER9" );
		builder.append( "&PASSWORD=effea03f" );
		builder.append( "&action=updateraw" );
		builder.append( "&realtime=1" );
		builder.append( "&rtfreq=2.5" );
		builder.append( "&dateutc=" ).append( DateUtil.format( new Date( timestamp ), WeatherUndergroundPublisher.WUNDERGROUND_DATE_FORMAT, TimeZone.getTimeZone( "UTC" ) ) );
		builder.append( "&tempf=60.0" );
		builder.append( "&baromin=29.92" );
		builder.append( "&humidity=25" );
		builder.append( "&softwaretype=dalton" ).append( URLEncoder.encode( " " + release, TextUtil.DEFAULT_ENCODING ) );

		// Test generated payload
		assertThat( publisher.generatePayload( data ), is( builder.toString() ) );
	}

}
