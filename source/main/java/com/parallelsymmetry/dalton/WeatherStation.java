package com.parallelsymmetry.dalton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Deque;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import javax.measure.DecimalMeasure;
import javax.measure.Measure;
import javax.measure.unit.NonSI;

import com.parallelsymmetry.utility.DateUtil;
import com.parallelsymmetry.utility.TextUtil;
import com.parallelsymmetry.utility.log.Log;
import com.parallelsymmetry.utility.math.Statistics;

public class WeatherStation implements WeatherDataListener {

	public static final String WUNDERGROUND_DATE_FORMAT = "yyyy-MM-dd+HH'%3A'mm'%3A'ss";

	private WeatherReader reader;

	private Map<WeatherDatumIdentifier, Measure<?, ?>> data;

	private Deque<WeatherDataEvent> twoMinuteWindBuffer;

	private Deque<WeatherDataEvent> tenMinuteWindBuffer;

	public WeatherStation( WeatherReader reader ) {
		this.reader = reader;
		data = new ConcurrentHashMap<WeatherDatumIdentifier, Measure<?, ?>>();

		twoMinuteWindBuffer = new ConcurrentLinkedDeque<WeatherDataEvent>();
		tenMinuteWindBuffer = new ConcurrentLinkedDeque<WeatherDataEvent>();
	}

	public float getTemperature() {
		Measure<?, ?> measure = getMeasure( WeatherDatumIdentifier.TEMPERATURE );
		if( measure == null ) return Float.NaN;
		return ( (Float)measure.getValue() ).floatValue();
	}

	public Measure<?, ?> getMeasure( WeatherDatumIdentifier datum ) {
		return data.get( datum );
	}

	@Override
	public void weatherDataEvent( WeatherDataEvent event ) {
		for( WeatherDatum datum : event.getData() ) {
			data.put( datum.getIdentifier(), datum.getMeasure() );
		}

		try {
			// Calculate dew point.
			float t = (Float)data.get( WeatherDatumIdentifier.TEMPERATURE ).getValue();
			float h = (Float)data.get( WeatherDatumIdentifier.HUMIDITY ).getValue();
			data.put( WeatherDatumIdentifier.DEW_POINT, DecimalMeasure.valueOf( WeatherUtil.calculateDewPoint( t, h ), NonSI.FAHRENHEIT ) );

			// TODO Calculate wind chill.

			// TODO Calculate pressure trend.
			//		WeatherDatum pressureTrendDatum = new WeatherDatum( WeatherDatumIdentifier.PRESSURE_TREND, DecimalMeasure.valueOf( pressureTrend, NonSI.BAR.divide( NonSI.HOUR ) ) );

			//postDataEvent( event, twoMinuteWindBuffer, 120000 );
			//postDataEvent( event, tenMinuteWindBuffer, 600000 );

			update2MinStatistics( event );
			update10MinStatistics( event );
		} catch( Exception exception ) {
			Log.write( exception );
		}

		for( WeatherDatumIdentifier identifier : data.keySet() ) {
			Log.write( Log.DETAIL, identifier, " = ", data.get( identifier ) );
		}

		try {
			updateMarkSoderquistNet();
		} catch( Exception exception ) {
			Log.write( exception );
		}

		try {
			updateWunderground();
		} catch( Exception exception ) {
			Log.write( exception );
		}
	}

	private void postDataEvent( WeatherDataEvent event, Deque<WeatherDataEvent> buffer, long timeout ) {
		buffer.push( event );

		WeatherDataEvent last = buffer.peekLast();
		while( event.getTimestamp().getTime() - last.getTimestamp().getTime() > timeout ) {
			buffer.pollLast();
			last = buffer.peekLast();
		}

	}

	private void update2MinStatistics( WeatherDataEvent event ) {
		Deque<WeatherDataEvent> buffer = twoMinuteWindBuffer;
		postDataEvent( event, buffer, 120000 );

		int windCount = 0;
		float windMin = Float.MAX_VALUE;
		float windMax = Float.MIN_VALUE;
		float windTotal = 0;

		for( WeatherDataEvent bufferEvent : buffer ) {
			for( WeatherDatum datum : bufferEvent.getData() ) {
				if( WeatherDatumIdentifier.WIND_SPEED_INSTANT == datum.getIdentifier() ) {
					windCount++;
					float value = (Float)datum.getMeasure().getValue();
					windTotal += value;
					if( value < windMin ) windMin = value;
					if( value > windMax ) windMax = value;
				}
			}
		}

		float windAvg = windTotal / windCount;

		data.put( WeatherDatumIdentifier.WIND_SPEED_2_MIN_MIN, DecimalMeasure.valueOf( windMin, NonSI.MILES_PER_HOUR ) );
		data.put( WeatherDatumIdentifier.WIND_SPEED_2_MIN_AVG, DecimalMeasure.valueOf( windAvg, NonSI.MILES_PER_HOUR ) );
		data.put( WeatherDatumIdentifier.WIND_SPEED_2_MIN_MAX, DecimalMeasure.valueOf( windMax, NonSI.MILES_PER_HOUR ) );
	}

	private void update10MinStatistics( WeatherDataEvent event ) {
		Deque<WeatherDataEvent> buffer = tenMinuteWindBuffer;
		postDataEvent( event, buffer, 600000 );

		int windCount = 0;
		float windMin = Float.MAX_VALUE;
		float windMax = Float.MIN_VALUE;
		float windTotal = 0;

		for( WeatherDataEvent bufferEvent : buffer ) {
			for( WeatherDatum datum : bufferEvent.getData() ) {
				if( WeatherDatumIdentifier.WIND_SPEED_INSTANT == datum.getIdentifier() ) {
					windCount++;
					float value = (Float)datum.getMeasure().getValue();
					windTotal += value;
					if( value < windMin ) windMin = value;
					if( value > windMax ) windMax = value;
				}
			}
		}

		float windAvg = windTotal / windCount;

		data.put( WeatherDatumIdentifier.WIND_SPEED_10_MIN_MIN, DecimalMeasure.valueOf( windMin, NonSI.MILES_PER_HOUR ) );
		data.put( WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG, DecimalMeasure.valueOf( windAvg, NonSI.MILES_PER_HOUR ) );
		data.put( WeatherDatumIdentifier.WIND_SPEED_10_MIN_MAX, DecimalMeasure.valueOf( windMax, NonSI.MILES_PER_HOUR ) );
	}

	/**
	 * Determine if the instantaneous wind velocity is a gust. A gust is a
	 * velocity greater than 10 MPH above the mean velocity.
	 * 
	 * @param wind
	 * @param buffer
	 * @return
	 */
	private boolean isGust( float wind, Deque<WeatherDataEvent> buffer ) {
		// Collect the wind data from the buffer.
		int index = 0;
		double[] values = new double[buffer.size()];
		for( WeatherDataEvent event : buffer ) {
			for( WeatherDatum datum : event.getData() ) {
				if( WeatherDatumIdentifier.WIND_SPEED_INSTANT == datum.getIdentifier() ) {
					values[index] = (Float)datum.getMeasure().getValue();
					index++;
				}
			}
		}

		// Calculate the wind average.
		double mean = Statistics.mean( values );

		return wind - mean > 10;
	}

	private int updateMarkSoderquistNet() throws IOException {
		StringBuilder builder = new StringBuilder( "http://ruby:8080/weather/wxstation?id=0" );

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

		add( builder, WeatherDatumIdentifier.WIND_SPEED_2_MIN_MIN, "wmin2", "0.0" );
		add( builder, WeatherDatumIdentifier.WIND_SPEED_2_MIN_AVG, "wavg2", "0.0" );
		add( builder, WeatherDatumIdentifier.WIND_SPEED_2_MIN_MAX, "wmax2", "0.0" );
		add( builder, WeatherDatumIdentifier.WIND_SPEED_10_MIN_MIN, "wmin10", "0.0" );
		add( builder, WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG, "wavg10", "0.0" );
		add( builder, WeatherDatumIdentifier.WIND_SPEED_10_MIN_MAX, "wmax10", "0.0" );

		builder.append( "&rr=" );
		builder.append( data.get( WeatherDatumIdentifier.RAIN_RATE ).getValue() );
		builder.append( "&rd=" );
		builder.append( data.get( WeatherDatumIdentifier.RAIN_TOTAL_DAILY ).getValue() );

		Log.write( Log.TRACE, builder.toString() );
		Response response = rest( "GET", builder.toString() );

		return response.getCode();
	}

	private void add( StringBuilder builder, WeatherDatumIdentifier identifier, String key, String format ) {
		Measure<?, ?> measure = data.get( identifier );
		if( measure == null ) return;

		builder.append( "&" );
		builder.append( key );
		builder.append( "=" );
		if( format == null ) {
			builder.append( measure.getValue() );
		} else {
			DecimalFormat formatter = new DecimalFormat( format );
			builder.append( formatter.format( measure.getValue() ) );
		}
	}

	/**
	 * This method to send data to the Weather Underground was developed using
	 * instructions from:
	 * <a href="http://wiki.wunderground.com/index.php/PWS_-_Upload_Protocol" >
	 * http://wiki.wunderground.com/index.php/PWS_-_Upload_Protocol</a>
	 * 
	 * @return
	 * @throws IOException
	 */
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
		builder.append( data.get( WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG ).getValue() );

		// Calculate wind data.
		float w = (Float)data.get( WeatherDatumIdentifier.WIND_SPEED_INSTANT ).getValue();
		if( isGust( w, tenMinuteWindBuffer ) ) {
			builder.append( "&windgustmph=" );
			builder.append( w );
			builder.append( "&windgustdir=" );
			builder.append( data.get( WeatherDatumIdentifier.WIND_DIRECTION ).getValue() );
		}

		builder.append( "&rainin=" );
		builder.append( data.get( WeatherDatumIdentifier.RAIN_RATE ).getValue() );
		builder.append( "&dailyrainin=" );
		builder.append( data.get( WeatherDatumIdentifier.RAIN_TOTAL_DAILY ).getValue() );

		builder.append( "&softwaretype=dalton" );
		String release = reader.getCard().getRelease().toHumanString( DateUtil.DEFAULT_TIME_ZONE );
		builder.append( URLEncoder.encode( " " + release, TextUtil.DEFAULT_ENCODING ) );

		Log.write( Log.TRACE, builder.toString() );
		Response response = rest( "GET", builder.toString() );

		return response.getCode();
	}

	private Response rest( String method, String url ) throws IOException {
		String USER_AGENT = "Mozilla/5.0";

		HttpURLConnection connection = (HttpURLConnection)new URL( url ).openConnection();
		connection.setRequestMethod( method );
		connection.setRequestProperty( "User-Agent", USER_AGENT );

		try {
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

			return new Response( responseCode, content.toString() );
		} finally {
			if( connection != null ) connection.disconnect();
		}
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
