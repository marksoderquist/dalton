package com.parallelsymmetry.dalton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import javax.measure.DecimalMeasure;
import javax.measure.Measure;
import javax.measure.unit.NonSI;

import com.parallelsymmetry.utility.DateUtil;
import com.parallelsymmetry.utility.TextUtil;
import com.parallelsymmetry.utility.log.Log;

public class WeatherStation implements WeatherDataListener {

	public static final String WUNDERGROUND_DATE_FORMAT = "yyyy-MM-dd+HH'%3A'mm'%3A'ss";

	private WeatherReader reader;

	private Map<WeatherDatumIdentifier, Measure<?, ?>> data;

	public WeatherStation( WeatherReader reader ) {
		this.reader = reader;
		data = new ConcurrentHashMap<WeatherDatumIdentifier, Measure<?, ?>>();
	}

	public Measure<?, ?> getMeasure( WeatherDatumIdentifier datum ) {
		return data.get( datum );
	}

	public float getTemperature() {
		Measure<?, ?> measure = getMeasure( WeatherDatumIdentifier.TEMPERATURE );
		if( measure == null ) return Float.NaN;
		return ( (Float)measure.getValue() ).floatValue();
	}

	@Override
	public void weatherDataEvent( WeatherDataEvent event ) {
		for( WeatherDatum datum : event.getData() ) {
			data.put( datum.getIdentifier(), datum.getMeasure() );
		}

		// Calculate dew point.
		float t = (Float)data.get( WeatherDatumIdentifier.TEMPERATURE ).getValue();
		float h = (Float)data.get( WeatherDatumIdentifier.HUMIDITY ).getValue();
		data.put( WeatherDatumIdentifier.DEW_POINT, DecimalMeasure.valueOf( WeatherUtil.calculateDewPoint( t, h ), NonSI.FAHRENHEIT ) );

		try {
			updateMarkSoderquistNet();
		} catch( IOException exception ) {
			Log.write( exception );
		}

		try {
			updateWunderground();
		} catch( IOException exception ) {
			Log.write( exception );
		}
	}

	private int updateMarkSoderquistNet() throws IOException {
		//http://emerald:8080/weather/station?id=21&ts=2348923&t=42.1&h=57&p=29.92
		StringBuilder builder = new StringBuilder( "http://emerald:8080/weather/wxstation?id=0" );

		builder.append( "&ts=" );
		builder.append( System.currentTimeMillis() );

		builder.append( "&t=" );
		builder.append( data.get( WeatherDatumIdentifier.TEMPERATURE ).getValue() );
		builder.append( "&p=" );
		builder.append( data.get( WeatherDatumIdentifier.PRESSURE ).getValue() );
		builder.append( "&h=" );
		builder.append( data.get( WeatherDatumIdentifier.HUMIDITY ).getValue() );
		builder.append( "&dp=" );
		builder.append( data.get( WeatherDatumIdentifier.DEW_POINT ).getValue() );

		builder.append( "&wd=" );
		builder.append( data.get( WeatherDatumIdentifier.WIND_DIRECTION ).getValue() );
		builder.append( "&wi=" );
		builder.append( data.get( WeatherDatumIdentifier.WIND_SPEED_INSTANT ).getValue() );
		builder.append( "&ws=" );
		builder.append( data.get( WeatherDatumIdentifier.WIND_SPEED_SUSTAIN ).getValue() );

		builder.append( "&rr=" );
		builder.append( data.get( WeatherDatumIdentifier.RAIN_RATE ).getValue() );
		builder.append( "&rd=" );
		builder.append( data.get( WeatherDatumIdentifier.RAIN_TOTAL_DAILY ).getValue() );

		Log.write( Log.TRACE, builder.toString() );
		Response response = sendGet( builder.toString() );

		return response.getCode();
	}

	private int updateWunderground() throws IOException {
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

		StringBuilder builder = new StringBuilder( "http://rtupdate.wunderground.com/weatherstation/updateweatherstation.php" );
		builder.append( "?ID=KUTRIVER9" );
		builder.append( "&PASSWORD=qWest73wun" );
		builder.append( "&action=updateraw" );
		builder.append( "&realtime=1&rtfreq=2.5" );
		builder.append( "&dateutc=" );
		builder.append( DateUtil.format( new Date(), WeatherStation.WUNDERGROUND_DATE_FORMAT, TimeZone.getTimeZone( "UTC" ) ) );
		builder.append( "&tempf=" );
		builder.append( data.get( WeatherDatumIdentifier.TEMPERATURE ).getValue() );
		builder.append( "&baromin=" );
		builder.append( data.get( WeatherDatumIdentifier.PRESSURE ).getValue() );
		builder.append( "&humidity=" );
		builder.append( data.get( WeatherDatumIdentifier.HUMIDITY ).getValue() );
		builder.append( "&dewptf=" );
		builder.append( data.get( WeatherDatumIdentifier.DEW_POINT ).getValue() );

		builder.append( "&winddir=" );
		builder.append( data.get( WeatherDatumIdentifier.WIND_DIRECTION ).getValue() );
		builder.append( "&windspeedmph=" );
		builder.append( data.get( WeatherDatumIdentifier.WIND_SPEED_SUSTAIN ).getValue() );
		builder.append( "&windspdmph_avg2m=" );
		builder.append( data.get( WeatherDatumIdentifier.WIND_SPEED_SUSTAIN ).getValue() );

		builder.append( "&rainin=" );
		builder.append( data.get( WeatherDatumIdentifier.RAIN_RATE ).getValue() );
		builder.append( "&dailyrainin=" );
		builder.append( data.get( WeatherDatumIdentifier.RAIN_TOTAL_DAILY ).getValue() );

		builder.append( "&softwaretype=dalton" );
		String release = reader.getCard().getRelease().toHumanString( DateUtil.DEFAULT_TIME_ZONE );
		builder.append( URLEncoder.encode( " " + release, TextUtil.DEFAULT_ENCODING ) );

		Log.write( Log.TRACE, builder.toString() );
		Response response = sendGet( builder.toString() );

		return response.getCode();
	}

	private Response sendGet( String url ) throws IOException {
		String USER_AGENT = "Mozilla/5.0";

		HttpURLConnection connection = (HttpURLConnection)new URL( url ).openConnection();
		connection.setRequestMethod( "GET" );
		connection.setRequestProperty( "User-Agent", USER_AGENT );

		// Get the response code.
		int responseCode = connection.getResponseCode();

		// Read the response.
		String inputLine;
		StringBuilder content = new StringBuilder();
		BufferedReader input = new BufferedReader( new InputStreamReader( connection.getInputStream() ) );
		while( ( inputLine = input.readLine() ) != null ) {
			content.append( inputLine );
		}
		input.close();

		Response response = new Response( responseCode, content.toString() );

		return response;
	}

	private class Response {

		private int code;

		private String content;

		public Response( int code, String content ) {
			this.code = code;
			this.content = content;
		}

		public int getCode() {
			return code;
		}

		public String getContent() {
			return content;
		}

	}

}
