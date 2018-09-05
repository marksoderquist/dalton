package com.parallelsymmetry.dalton;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.parallelsymmetry.utility.log.Log;

import javax.measure.DecimalMeasure;
import javax.measure.Measure;
import javax.measure.unit.NonSI;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class WeatherStation implements WeatherDataListener {

	public static final String WUNDERGROUND_DATE_FORMAT = "yyyy-MM-dd+HH'%3A'mm'%3A'ss";

	private WeatherReader reader;

	private Map<WeatherDatumIdentifier, Measure<?, ?>> data;

	private TimedEventBuffer oneMinuteBuffer;

	private TimedEventBuffer twoMinuteBuffer;

	private TimedEventBuffer fiveMinuteBuffer;

	private TimedEventBuffer tenMinuteBuffer;

	private TimedEventBuffer threeHourBuffer;

	private Deque<WeatherDataEvent> oldTwoMinuteBuffer;

	private Deque<WeatherDataEvent> oldFiveMinuteBuffer;

	private Deque<WeatherDataEvent> oldTenMinuteBuffer;

	private Deque<WeatherDataEvent> oldThreeHourBuffer;

	private WeatherUndergroundPublisher weatherUndergroundPublisher;

	public WeatherStation( WeatherReader reader ) {
		this.reader = reader;
		data = new ConcurrentHashMap<>();

		oneMinuteBuffer = new TimedEventBuffer( 60000 );
		twoMinuteBuffer = new TimedEventBuffer( 120000 );
		fiveMinuteBuffer = new TimedEventBuffer( 300000 );
		tenMinuteBuffer = new TimedEventBuffer( 600000 );
		threeHourBuffer = new TimedEventBuffer( 10800000 );

		oldTwoMinuteBuffer = new ConcurrentLinkedDeque<>();
		oldFiveMinuteBuffer = new ConcurrentLinkedDeque<>();
		oldTenMinuteBuffer = new ConcurrentLinkedDeque<>();
		oldThreeHourBuffer = new ConcurrentLinkedDeque<>();

		weatherUndergroundPublisher = new WeatherUndergroundPublisher( reader, this );
	}

//	public Map<WeatherDatumIdentifier, Measure<?, ?>> getData() {
//		return data;
//	}
//
//	public Deque<WeatherDataEvent> getOneMinuteBuffer() {
//		return oneMinuteBuffer.getDeque();
//	}
//
//	public Deque<WeatherDataEvent> getTwoMinuteBuffer() {
//		return oldTwoMinuteBuffer;
//	}
//
//	public Deque<WeatherDataEvent> getFiveMinuteBuffer() {
//		return oldFiveMinuteBuffer;
//	}
//
//	public Deque<WeatherDataEvent> getTenMinuteBuffer() {
//		return oldTenMinuteBuffer;
//	}
//
//	public Deque<WeatherDataEvent> getThreeHourBuffer() {
//		return oldThreeHourBuffer;
//	}
//
//	public Measure<?, ?> getMeasure( WeatherDatumIdentifier datum ) {
//		return data.get( datum );
//	}

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

			update1MinStatistics( event );
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
			weatherUndergroundPublisher.publish( data );
		} catch( Throwable throwable ) {
			Log.write( throwable );
		}

		try {
			updateMarkSoderquistNetWeather();
		} catch( Throwable throwable ) {
			Log.write( throwable );
		}
	}

	private void update1MinStatistics(WeatherDataEvent event) {
		oneMinuteBuffer.post( event );
		double windMin = oneMinuteBuffer.getMinimum( WeatherDatumIdentifier.WIND_SPEED_CURRENT);
		double windMax = oneMinuteBuffer.getMaximum( WeatherDatumIdentifier.WIND_SPEED_CURRENT);
		double windAvg = oneMinuteBuffer.getAverage( WeatherDatumIdentifier.WIND_SPEED_CURRENT );
		data.put( WeatherDatumIdentifier.WIND_SPEED_1_MIN_MIN, DecimalMeasure.valueOf( windMin, NonSI.MILES_PER_HOUR ) );
		data.put( WeatherDatumIdentifier.WIND_SPEED_1_MIN_AVG, DecimalMeasure.valueOf( windAvg, NonSI.MILES_PER_HOUR ) );
		data.put( WeatherDatumIdentifier.WIND_SPEED_1_MIN_MAX, DecimalMeasure.valueOf( windMax, NonSI.MILES_PER_HOUR ) );
	}

	private void update2MinStatistics( WeatherDataEvent event ) {
		twoMinuteBuffer.post( event );
		double windMin = twoMinuteBuffer.getMinimum( WeatherDatumIdentifier.WIND_SPEED_CURRENT);
		double windMax = twoMinuteBuffer.getMaximum( WeatherDatumIdentifier.WIND_SPEED_CURRENT);
		double windAvg = twoMinuteBuffer.getAverage( WeatherDatumIdentifier.WIND_SPEED_CURRENT );
		data.put( WeatherDatumIdentifier.WIND_SPEED_2_MIN_MIN, DecimalMeasure.valueOf( windMin, NonSI.MILES_PER_HOUR ) );
		data.put( WeatherDatumIdentifier.WIND_SPEED_2_MIN_AVG, DecimalMeasure.valueOf( windAvg, NonSI.MILES_PER_HOUR ) );
		data.put( WeatherDatumIdentifier.WIND_SPEED_2_MIN_MAX, DecimalMeasure.valueOf( windMax, NonSI.MILES_PER_HOUR ) );
	}

	private void update5MinStatistics( WeatherDataEvent event ) {
		fiveMinuteBuffer.post( event );
		double windMin = fiveMinuteBuffer.getMinimum( WeatherDatumIdentifier.WIND_SPEED_CURRENT);
		double windMax = fiveMinuteBuffer.getMaximum( WeatherDatumIdentifier.WIND_SPEED_CURRENT);
		double windAvg = fiveMinuteBuffer.getAverage( WeatherDatumIdentifier.WIND_SPEED_CURRENT );
		data.put( WeatherDatumIdentifier.WIND_SPEED_5_MIN_MIN, DecimalMeasure.valueOf( windMin, NonSI.MILES_PER_HOUR ) );
		data.put( WeatherDatumIdentifier.WIND_SPEED_5_MIN_AVG, DecimalMeasure.valueOf( windAvg, NonSI.MILES_PER_HOUR ) );
		data.put( WeatherDatumIdentifier.WIND_SPEED_5_MIN_MAX, DecimalMeasure.valueOf( windMax, NonSI.MILES_PER_HOUR ) );
	}

	private void update10MinStatistics( WeatherDataEvent event ) {
		tenMinuteBuffer.post( event );
		double windMin = tenMinuteBuffer.getMinimum( WeatherDatumIdentifier.WIND_SPEED_CURRENT);
		double windMax = tenMinuteBuffer.getMaximum( WeatherDatumIdentifier.WIND_SPEED_CURRENT);
		double windAvg = tenMinuteBuffer.getAverage( WeatherDatumIdentifier.WIND_SPEED_CURRENT );
		data.put( WeatherDatumIdentifier.WIND_SPEED_10_MIN_MIN, DecimalMeasure.valueOf( windMin, NonSI.MILES_PER_HOUR ) );
		data.put( WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG, DecimalMeasure.valueOf( windAvg, NonSI.MILES_PER_HOUR ) );
		data.put( WeatherDatumIdentifier.WIND_SPEED_10_MIN_MAX, DecimalMeasure.valueOf( windMax, NonSI.MILES_PER_HOUR ) );
	}

	private void update3HourStatistics( WeatherDataEvent event ) {
		threeHourBuffer.post( event );
		double temperatureTrend = threeHourBuffer.getTrend( WeatherDatumIdentifier.TEMPERATURE ) / 3.0;
		double humidityTrend = threeHourBuffer.getTrend( WeatherDatumIdentifier.HUMIDITY ) / 3.0;
		double pressureTrend = threeHourBuffer.getTrend( WeatherDatumIdentifier.PRESSURE ) / 3.0;
		data.put( WeatherDatumIdentifier.TEMPERATURE_TREND, DecimalMeasure.valueOf( temperatureTrend, NonSI.FAHRENHEIT.divide( NonSI.HOUR ) ) );
		data.put( WeatherDatumIdentifier.HUMIDITY_TREND, DecimalMeasure.valueOf( humidityTrend, NonSI.PERCENT.divide( NonSI.HOUR ) ) );
		data.put( WeatherDatumIdentifier.PRESSURE_TREND, DecimalMeasure.valueOf( pressureTrend, NonSI.INCH_OF_MERCURY.divide( NonSI.HOUR ) ) );
	}

	private int updateMarkSoderquistNetWeather() throws IOException {
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

		return rest( "PUT", "http://mark.soderquist.net/weather/api/station?id=bluewing", headers, stream.toByteArray() ).getCode();
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
