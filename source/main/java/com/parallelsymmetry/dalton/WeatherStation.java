package com.parallelsymmetry.dalton;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.parallelsymmetry.utility.DateUtil;
import com.parallelsymmetry.utility.TextUtil;
import com.parallelsymmetry.utility.log.Log;

import javax.measure.DecimalMeasure;
import javax.measure.Measure;
import javax.measure.unit.NonSI;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class WeatherStation implements WeatherDataListener {

	public static final String WUNDERGROUND_DATE_FORMAT = "yyyy-MM-dd+HH'%3A'mm'%3A'ss";

	private WeatherReader reader;

	private Map<WeatherDatumIdentifier, Measure<?, ?>> data;

	private Deque<WeatherDataEvent> twoMinuteBuffer;

	private Deque<WeatherDataEvent> fiveMinuteBuffer;

	private Deque<WeatherDataEvent> tenMinuteBuffer;

	private Deque<WeatherDataEvent> threeHourBuffer;

	private WeatherUndergroundPublisher weatherUndergroundPublisher;

	public WeatherStation( WeatherReader reader ) {
		this.reader = reader;
		data = new ConcurrentHashMap<WeatherDatumIdentifier, Measure<?, ?>>();

		twoMinuteBuffer = new ConcurrentLinkedDeque<WeatherDataEvent>();
		fiveMinuteBuffer = new ConcurrentLinkedDeque<WeatherDataEvent>();
		tenMinuteBuffer = new ConcurrentLinkedDeque<WeatherDataEvent>();
		threeHourBuffer = new ConcurrentLinkedDeque<WeatherDataEvent>();

		weatherUndergroundPublisher = new WeatherUndergroundPublisher( reader, this );
	}

	public Map<WeatherDatumIdentifier, Measure<?, ?>> getData() {
		return data;
	}

	public Deque<WeatherDataEvent> getTwoMinuteBuffer() {
		return twoMinuteBuffer;
	}

	public Deque<WeatherDataEvent> getFiveMinuteBuffer() {
		return fiveMinuteBuffer;
	}

	public Deque<WeatherDataEvent> getTenMinuteBuffer() {
		return tenMinuteBuffer;
	}

	public Deque<WeatherDataEvent> getThreeHourBuffer() {
		return threeHourBuffer;
	}

	public float getTemperature() {
		Measure<?, ?> measure = getMeasure( WeatherDatumIdentifier.TEMPERATURE );
		if( measure == null ) return Float.NaN;
		return ((Float)measure.getValue()).floatValue();
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
			float t = (Float)data.get( WeatherDatumIdentifier.TEMPERATURE ).getValue();
			float h = (Float)data.get( WeatherDatumIdentifier.HUMIDITY ).getValue();
			float w = (Float)data.get( WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG ).getValue();

			// Calculate dew point.
			data.put( WeatherDatumIdentifier.DEW_POINT, DecimalMeasure.valueOf( WeatherUtil.calculateDewPoint( t, h ), NonSI.FAHRENHEIT ) );

			// Calculate wind chill.
			data.put( WeatherDatumIdentifier.WIND_CHILL, DecimalMeasure.valueOf( WeatherUtil.calculateWindChill( t, w ), NonSI.FAHRENHEIT ) );

			// Calculate heat index.
			data.put( WeatherDatumIdentifier.HEAT_INDEX, DecimalMeasure.valueOf( WeatherUtil.calculateHeatIndex( t, h ), NonSI.FAHRENHEIT ) );

			update2MinStatistics( event );
			update5MinStatistics( event );
			update10MinStatistics( event );
			update3HourStatistics( event );
		} catch( Exception exception ) {
			Log.write( exception );
		}

		for( WeatherDatumIdentifier identifier : data.keySet() ) {
			Log.write( Log.DETAIL, identifier, " = ", data.get( identifier ) );
		}

		Log.write( "Publishing metrics: T: " + data.get( WeatherDatumIdentifier.TEMPERATURE ).getValue() + "  W: " + data.get( WeatherDatumIdentifier.WIND_SPEED_2_MIN_AVG ).getValue() );

		try {
			weatherUndergroundPublisher.publish( data, tenMinuteBuffer );
		} catch( Throwable throwable ) {
			Log.write( throwable );
		}

		try {
			updateMarkSoderquistNetWeatherx();
		} catch( Throwable throwable ) {
			Log.write( throwable );
		}

		try {
			updateMarkSoderquistNet();
		} catch( Throwable throwable ) {
			Log.write( throwable );
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
		Deque<WeatherDataEvent> buffer = twoMinuteBuffer;
		postDataEvent( event, buffer, 120000 );

		int windCount = 0;
		float windMin = Float.MAX_VALUE;
		float windMax = Float.MIN_VALUE;
		float windTotal = 0;

		for( WeatherDataEvent bufferEvent : buffer ) {
			for( WeatherDatum datum : bufferEvent.getData() ) {
				if( WeatherDatumIdentifier.WIND_SPEED_CURRENT == datum.getIdentifier() ) {
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

	private void update5MinStatistics( WeatherDataEvent event ) {
		Deque<WeatherDataEvent> buffer = fiveMinuteBuffer;
		postDataEvent( event, buffer, 300000 );

		int windCount = 0;
		float windMin = Float.MAX_VALUE;
		float windMax = Float.MIN_VALUE;
		float windTotal = 0;

		for( WeatherDataEvent bufferEvent : buffer ) {
			for( WeatherDatum datum : bufferEvent.getData() ) {
				if( WeatherDatumIdentifier.WIND_SPEED_CURRENT == datum.getIdentifier() ) {
					windCount++;
					float value = (Float)datum.getMeasure().getValue();
					windTotal += value;
					if( value < windMin ) windMin = value;
					if( value > windMax ) windMax = value;
				}
			}
		}

		float windAvg = windTotal / windCount;

		data.put( WeatherDatumIdentifier.WIND_SPEED_5_MIN_MIN, DecimalMeasure.valueOf( windMin, NonSI.MILES_PER_HOUR ) );
		data.put( WeatherDatumIdentifier.WIND_SPEED_5_MIN_AVG, DecimalMeasure.valueOf( windAvg, NonSI.MILES_PER_HOUR ) );
		data.put( WeatherDatumIdentifier.WIND_SPEED_5_MIN_MAX, DecimalMeasure.valueOf( windMax, NonSI.MILES_PER_HOUR ) );
	}

	private void update10MinStatistics( WeatherDataEvent event ) {
		Deque<WeatherDataEvent> buffer = tenMinuteBuffer;
		postDataEvent( event, buffer, 600000 );

		int windCount = 0;
		float windMin = Float.MAX_VALUE;
		float windMax = Float.MIN_VALUE;
		float windTotal = 0;

		for( WeatherDataEvent bufferEvent : buffer ) {
			for( WeatherDatum datum : bufferEvent.getData() ) {
				if( WeatherDatumIdentifier.WIND_SPEED_CURRENT == datum.getIdentifier() ) {
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

	private void update3HourStatistics( WeatherDataEvent event ) {
		Deque<WeatherDataEvent> buffer = threeHourBuffer;
		postDataEvent( event, buffer, 10800000 );

		float trend = 0;
		if( buffer.size() >= 2 ) {
			float first = getValue( buffer.peekFirst(), WeatherDatumIdentifier.PRESSURE );
			float last = getValue( buffer.peekLast(), WeatherDatumIdentifier.PRESSURE );
			trend = (first - last) / 3f;
		}
		data.put( WeatherDatumIdentifier.PRESSURE_TREND, DecimalMeasure.valueOf( trend, NonSI.INCH_OF_MERCURY.divide( NonSI.HOUR ) ) );
	}

	private static float getValue( WeatherDataEvent event, WeatherDatumIdentifier identifier ) {
		for( WeatherDatum datum : event.getData() ) {
			if( datum.getIdentifier() == identifier ) return (Float)datum.getMeasure().getValue();
		}
		return Float.NaN;
	}

	private int updateMarkSoderquistNet() throws IOException {
		Map<String, String> fields = new HashMap<>();
		fields.put( "timestamp", String.valueOf( System.currentTimeMillis() ) );

		// Prepare basic values.
		fields.put( "temperature", format( WeatherDatumIdentifier.TEMPERATURE, "0.0" ) );
		fields.put( "pressure", format( WeatherDatumIdentifier.PRESSURE, "0.00" ) );
		fields.put( "humidity", format( WeatherDatumIdentifier.HUMIDITY, "0" ) );

		// Prepare derived values.
		fields.put( "dew-point", format( WeatherDatumIdentifier.DEW_POINT, "0.0" ) );
		fields.put( "wind-chill", format( WeatherDatumIdentifier.WIND_CHILL, "0.0" ) );
		fields.put( "heat-index", format( WeatherDatumIdentifier.HEAT_INDEX, "0.0" ) );
		fields.put( "pressure-trend", format( WeatherDatumIdentifier.PRESSURE_TREND, "0.00" ) );

		// Prepare wind values.
		fields.put( "wind-current", format( WeatherDatumIdentifier.WIND_SPEED_CURRENT, "0" ) );
		fields.put( "wind-direction", format( WeatherDatumIdentifier.WIND_DIRECTION, "0" ) );
		fields.put( "wind-10-min-max", format( WeatherDatumIdentifier.WIND_SPEED_10_MIN_MAX, "0" ) );
		fields.put( "wind-10-min-avg", format( WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG, "0.0" ) );
		fields.put( "wind-10-min-min", format( WeatherDatumIdentifier.WIND_SPEED_10_MIN_MIN, "0" ) );
		fields.put( "wind-2-min-max", format( WeatherDatumIdentifier.WIND_SPEED_2_MIN_MAX, "0" ) );
		fields.put( "wind-2-min-avg", format( WeatherDatumIdentifier.WIND_SPEED_2_MIN_AVG, "0.0" ) );
		fields.put( "wind-2-min-min", format( WeatherDatumIdentifier.WIND_SPEED_2_MIN_MIN, "0" ) );

		// Prepare rain values.
		fields.put( "rain-total-daily", format( WeatherDatumIdentifier.RAIN_TOTAL_DAILY, "0.00" ) );
		fields.put( "rain-rate", format( WeatherDatumIdentifier.RAIN_RATE, "0.00" ) );

		Properties properties = new Properties();
		properties.putAll( fields );
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		properties.store( output, null );

		return rest( "PUT", "http://ruby:8080/weather/wxstation", output.toByteArray() ).getCode();
	}

	private int updateMarkSoderquistNetWeatherx() throws IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();

		JsonGenerator generator = new JsonFactory().createGenerator( stream );
		generator.writeStartObject();
		generator.writeNumberField( "timestamp", System.currentTimeMillis() );
		generator.writeNumberField( "temperature", (Float)data.get( WeatherDatumIdentifier.TEMPERATURE ).getValue() );
		generator.writeNumberField( "pressure", (Float)data.get( WeatherDatumIdentifier.PRESSURE ).getValue() );
		generator.writeNumberField( "humidity", (Float)data.get( WeatherDatumIdentifier.HUMIDITY ).getValue() );

		generator.writeNumberField( "dewPoint", (Float)data.get( WeatherDatumIdentifier.DEW_POINT ).getValue() );
		generator.writeNumberField( "windChill", (Float)data.get( WeatherDatumIdentifier.WIND_CHILL ).getValue() );
		generator.writeNumberField( "heatIndex", (Float)data.get( WeatherDatumIdentifier.HEAT_INDEX ).getValue() );
		generator.writeNumberField( "pressureTrend", (Float)data.get( WeatherDatumIdentifier.PRESSURE_TREND ).getValue() );

		generator.writeNumberField( "windDirection", (Float)data.get( WeatherDatumIdentifier.WIND_DIRECTION ).getValue() );
		generator.writeNumberField( "wind", (Float)data.get( WeatherDatumIdentifier.WIND_SPEED_CURRENT ).getValue() );

		generator.writeNumberField( "windTenMinMax", (Float)data.get( WeatherDatumIdentifier.WIND_SPEED_10_MIN_MAX ).getValue() );
		generator.writeNumberField( "windTenMinAvg", (Float)data.get( WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG ).getValue() );
		generator.writeNumberField( "windTenMinMin", (Float)data.get( WeatherDatumIdentifier.WIND_SPEED_10_MIN_MIN ).getValue() );

		generator.writeNumberField( "windTwoMinMax", (Float)data.get( WeatherDatumIdentifier.WIND_SPEED_2_MIN_MAX ).getValue() );
		generator.writeNumberField( "windTwoMinAvg", (Float)data.get( WeatherDatumIdentifier.WIND_SPEED_2_MIN_AVG ).getValue() );
		generator.writeNumberField( "windTwoMinMin", (Float)data.get( WeatherDatumIdentifier.WIND_SPEED_2_MIN_MIN ).getValue() );

		generator.writeNumberField( "rainTotalDaily", (Float)data.get( WeatherDatumIdentifier.RAIN_TOTAL_DAILY ).getValue() );
		generator.writeNumberField( "rainRate", (Float)data.get( WeatherDatumIdentifier.RAIN_RATE ).getValue() );

		generator.writeEndObject();
		generator.close();

		Map<String, String> headers = new HashMap<>();
		headers.put( "content-type", "application/json" );
		headers.put( "Authorization", "Basic ZGFsdG9uOkRvNUpwTW84ejVoU3hVaTQ=" );

		rest( "PUT", "http://ruby:8080/weatherx/station?id=bluewing", headers, stream.toByteArray() ).getCode();
		return rest( "PUT", "http://mark.soderquist.net/weatherx/station?id=bluewing", headers, stream.toByteArray() ).getCode();
	}

	/**
	 * This method to send data to the Weather Underground was developed using instructions from: <a href="http://wiki.wunderground.com/index.php/PWS_-_Upload_Protocol" > http://wiki.wunderground.com/index.php/PWS_-_Upload_Protocol</a>
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

		// Prepare basic data.
		add( builder, WeatherDatumIdentifier.TEMPERATURE, "tempf", "0.0" );
		add( builder, WeatherDatumIdentifier.PRESSURE, "baromin", "0.00" );
		add( builder, WeatherDatumIdentifier.HUMIDITY, "humidity", "0" );
		add( builder, WeatherDatumIdentifier.DEW_POINT, "dewptf", "0.0" );

		// Prepare wind data.
		add( builder, WeatherDatumIdentifier.WIND_DIRECTION, "winddir", "0" );
		add( builder, WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG, "windspeedmph", "0.0" );

		// Prepare wind gust data.
		float w = (Float)data.get( WeatherDatumIdentifier.WIND_SPEED_CURRENT ).getValue();
		if( WeatherUtil.isGust( w, tenMinuteBuffer ) ) {
			add( builder, w, "windgustmph", "0" );
		} else {
			add( builder, WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG, "windgustmph", "0" );
		}
		add( builder, WeatherDatumIdentifier.WIND_DIRECTION, "windgustdir", "0" );

		// Prepare rain data.
		add( builder, WeatherDatumIdentifier.RAIN_RATE, "rainin", "0.00" );
		add( builder, WeatherDatumIdentifier.RAIN_TOTAL_DAILY, "dailyrainin", "0.00" );

		// Prepare software data.
		builder.append( "&softwaretype=dalton" );
		String release = reader.getCard().getRelease().toHumanString( DateUtil.DEFAULT_TIME_ZONE );
		builder.append( URLEncoder.encode( " " + release, TextUtil.DEFAULT_ENCODING ) );

		Log.write( Log.TRACE, builder.toString() );
		Response response = rest( "GET", builder.toString() );

		return response.getCode();
	}

	private void add( StringBuilder builder, WeatherDatumIdentifier identifier, String key, String format ) {
		Measure<?, ?> measure = data.get( identifier );
		if( measure == null ) return;
		add( builder, (Float)measure.getValue(), key, format );
	}

	private void add( StringBuilder builder, float value, String key, String format ) {
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

	Response rest( String method, String url ) throws IOException {
		return rest( method, url, null );
	}

	Response rest( String method, String url, byte[] request ) throws IOException {
		return rest( method, url, null, request );
	}

	Response rest( String method, String url, Map<String, String> headers, byte[] request ) throws IOException {
		String USER_AGENT = "Mozilla/5.0";

		Log.write( Log.TRACE, "Sending to: " + url );

		// Set up the request.
		HttpURLConnection connection = (HttpURLConnection)new URL( url ).openConnection();
		connection.setConnectTimeout( 5000 );
		connection.setReadTimeout( 5000 );
		connection.setRequestMethod( method );
		connection.setRequestProperty( "User-Agent", USER_AGENT );
		connection.setAllowUserInteraction( false );
		if( headers != null ) {
			for( String key : headers.keySet() ) {
				connection.setRequestProperty( key, headers.get( key ) );
			}
		}
		if( request != null ) {
			connection.setDoOutput( true );
			OutputStream output = connection.getOutputStream();
			try {
				output.write( request );
			} finally {
				if( output != null ) output.close();
			}
		}

		// Handle the response.
		try {
			// Get the response code.
			int responseCode = connection.getResponseCode();

			// Read the response.
			String inputLine;
			StringBuilder response = new StringBuilder();
			BufferedReader input = new BufferedReader( new InputStreamReader( connection.getInputStream() ) );
			while( (inputLine = input.readLine()) != null ) {
				response.append( inputLine );
			}
			input.close();

			return new Response( responseCode, response.toString() );
		} finally {
			if( connection != null ) connection.disconnect();
		}
	}

	private String format( WeatherDatumIdentifier identifier, String format ) {
		Measure<?, ?> measure = data.get( identifier );
		if( measure == null ) return null;
		return format( (Float)measure.getValue(), format );
	}

	private String format( float value, String format ) {
		return new DecimalFormat( format ).format( value );
	}

	class Response {

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
