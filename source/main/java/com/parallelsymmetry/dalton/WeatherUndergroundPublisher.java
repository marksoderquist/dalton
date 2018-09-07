package com.parallelsymmetry.dalton;

import com.parallelsymmetry.utility.DateUtil;
import com.parallelsymmetry.utility.TextUtil;

import javax.measure.Measure;
import javax.measure.quantity.Quantity;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

/**
 * @author ecco
 */
public class WeatherUndergroundPublisher extends HttpPublisher {

	public static final String WUNDERGROUND_DATE_FORMAT = "yyyy-MM-dd+HH'%3A'mm'%3A'ss";

	private Program program;

	public WeatherUndergroundPublisher( Program program ) {
		this.program = program;
	}

	public int publish( WeatherStation station, Map<WeatherDatumIdentifier, Measure<? extends Number, ? extends Quantity>> data ) throws IOException {
		return rest( "GET", generatePayload( station, data ) ).getCode();
	}

	public String generatePayload( WeatherStation station, Map<WeatherDatumIdentifier, Measure<? extends Number, ? extends Quantity>> data ) throws IOException {
		// Example:
		// http://rtupdate.wunderground.com/weatherstation/updateweatherstation.php?ID=KCASANFR5&PASSWORD=XXXXXX&dateutc=2000-01-01+10%3A32%3A35&winddir=230&windspeedmph=12&windgustmph=12&tempf=70&rainin=0&baromin=29.1&dewptf=68.2&humidity=90&weather=&clouds=&softwaretype=vws%20versionxx&action=updateraw&realtime=1&rtfreq=2.5

		//	winddir - [0-360 instantaneous wind direction]
		//	windspeedmph - [mph instantaneous wind speed]
		//	windgustmph - [mph current wind gust, using software specific time period]
		//	windgustdir - [0-360 using software specific time period]
		//	windspdmph_avg2m  - [mph 2 minute average wind speed mph]
		//	winddir_avg2m - [0-360 2 minute average wind direction]
		//	windgustmph_10m - [mph past 10 minutes wind gust mph ]
		//	windgustdir_10m - [0-360 past 10 minutes wind gust direction]

		long timestamp = (Long)data.get( WeatherDatumIdentifier.TIMESTAMP ).getValue();

		StringBuilder builder = new StringBuilder( "http://rtupdate.wunderground.com/weatherstation/updateweatherstation.php" );
		builder.append( "?ID=KUTRIVER9" );
		builder.append( "&PASSWORD=effea03f" );
		builder.append( "&action=updateraw" );
		builder.append( "&realtime=1&rtfreq=2.5" );
		builder.append( "&dateutc=" );
		builder.append( DateUtil.format( new Date( timestamp ), WUNDERGROUND_DATE_FORMAT, TimeZone.getTimeZone( "UTC" ) ) );

		// Prepare basic data
		add( data, builder, WeatherDatumIdentifier.TEMPERATURE, "tempf", "0.0" );
		add( data, builder, WeatherDatumIdentifier.PRESSURE, "baromin", "0.00" );
		add( data, builder, WeatherDatumIdentifier.HUMIDITY, "humidity", "0" );
		add( data, builder, WeatherDatumIdentifier.DEW_POINT, "dewptf", "0.0" );

		// Prepare wind data
		add( data, builder, WeatherDatumIdentifier.WIND_DIRECTION, "winddir", "0" );
		add( data, builder, WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG, "windspeedmph", "0.0" );

		// Prepare wind gust data
		// This uses the five minute data because Weather Underground only uses one value every five minutes
		double wa = station.getFiveMinuteBuffer().getAverage( WeatherDatumIdentifier.WIND_SPEED );
		double wx = station.getFiveMinuteBuffer().getMaximum( WeatherDatumIdentifier.WIND_SPEED );
		if( WeatherUtil.isGust( wx, wa ))add( builder, wx, "windgustmph", "0" );

		// Prepare rain data
		add( data, builder, WeatherDatumIdentifier.RAIN_RATE, "rainin", "0.00" );
		add( data, builder, WeatherDatumIdentifier.RAIN_TOTAL_DAILY, "dailyrainin", "0.00" );

		// Prepare software data
		builder.append( "&softwaretype=dalton" );
		String release = program.getCard().getRelease().toHumanString( DateUtil.DEFAULT_TIME_ZONE );
		builder.append( URLEncoder.encode( " " + release, TextUtil.DEFAULT_ENCODING ) );

		return builder.toString();
	}

	private void add( Map<WeatherDatumIdentifier, Measure<? extends Number, ? extends Quantity>> data, StringBuilder builder, WeatherDatumIdentifier identifier, String key, String format ) {
		Measure<? extends Number, ? extends Quantity> measure = data.get( identifier );
		if( measure == null ) return;
		add( builder, (Double)measure.getValue(), key, format );
	}

	private void add( StringBuilder builder, double value, String key, String format ) {
		builder.append( "&" );
		builder.append( key );
		builder.append( "=" );
		if( format == null ) {
			builder.append( value );
		} else {
			DecimalFormat formatter = new DecimalFormat( format );
			builder.append( formatter.format( value ) );
		}
	}

}
