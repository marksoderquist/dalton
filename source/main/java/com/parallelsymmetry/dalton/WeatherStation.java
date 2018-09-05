package com.parallelsymmetry.dalton;

import com.parallelsymmetry.utility.log.Log;

import javax.measure.DecimalMeasure;
import javax.measure.Measure;
import javax.measure.unit.NonSI;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArraySet;

public class WeatherStation {

	private Map<WeatherDatumIdentifier, Measure<?, ?>> data;

	private Set<WeatherDataPublisher> publishers;

	private TimedEventBuffer oneMinuteBuffer;

	private TimedEventBuffer twoMinuteBuffer;

	private TimedEventBuffer fiveMinuteBuffer;

	private TimedEventBuffer tenMinuteBuffer;

	private TimedEventBuffer threeHourBuffer;

	private Deque<WeatherDataEvent> oldTwoMinuteBuffer;

	private Deque<WeatherDataEvent> oldFiveMinuteBuffer;

	private Deque<WeatherDataEvent> oldTenMinuteBuffer;

	private Deque<WeatherDataEvent> oldThreeHourBuffer;

	// TODO Move this out to Program
	//private WeatherUndergroundPublisher weatherUndergroundPublisher;

	public WeatherStation() {
		data = new ConcurrentHashMap<>();
		publishers = new CopyOnWriteArraySet<>();

		oneMinuteBuffer = new TimedEventBuffer( 60000 );
		twoMinuteBuffer = new TimedEventBuffer( 120000 );
		fiveMinuteBuffer = new TimedEventBuffer( 300000 );
		tenMinuteBuffer = new TimedEventBuffer( 600000 );
		threeHourBuffer = new TimedEventBuffer( 10800000 );

		oldTwoMinuteBuffer = new ConcurrentLinkedDeque<>();
		oldFiveMinuteBuffer = new ConcurrentLinkedDeque<>();
		oldTenMinuteBuffer = new ConcurrentLinkedDeque<>();
		oldThreeHourBuffer = new ConcurrentLinkedDeque<>();

		//weatherUndergroundPublisher = new WeatherUndergroundPublisher( reader, this );
	}

	public void addPublisher( WeatherDataPublisher publisher ) {
		publishers.add( publisher );
	}

	public void removePublisher( WeatherDataPublisher publisher ) {
		publishers.remove( publisher );
	}

	public void weatherDataEvent( WeatherDataEvent event ) {
		for( WeatherDatum datum : event.getData() ) {
			data.put( datum.getIdentifier(), datum.getMeasure() );
		}

		try {
			double t = (Double)data.get( WeatherDatumIdentifier.TEMPERATURE ).getValue();
			double h = (Double)data.get( WeatherDatumIdentifier.HUMIDITY ).getValue();
			double w = (Double)data.get( WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG ).getValue();

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

		StringBuilder message = new StringBuilder( "Publishing metrics: " );
		message.append( " T=" ).append(data.get( WeatherDatumIdentifier.TEMPERATURE ).getValue() );
		message.append( " H=" ).append(data.get( WeatherDatumIdentifier.HUMIDITY ).getValue() );
		message.append( " P=" ).append(data.get( WeatherDatumIdentifier.PRESSURE ).getValue() );
		message.append( " W=" ).append(data.get( WeatherDatumIdentifier.WIND_SPEED_CURRENT ).getValue() );
		Log.write( message );

		for( WeatherDataPublisher publisher : publishers ) {
			try {
				publisher.publish( data );
			} catch( Throwable throwable ) {
				Log.write( throwable );
			}
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

//	private int updateMarkSoderquistNetWeather() throws IOException {
//		ByteArrayOutputStream stream = new ByteArrayOutputStream();
//
//		JsonGenerator generator = new JsonFactory().createGenerator( stream );
//		generator.writeStartObject();
//		generator.writeNumberField( "timestamp", System.currentTimeMillis() );
//		generator.writeNumberField( "temperature", (Float)data.get( WeatherDatumIdentifier.TEMPERATURE ).getValue() );
//		generator.writeNumberField( "pressure", (Float)data.get( WeatherDatumIdentifier.PRESSURE ).getValue() );
//		generator.writeNumberField( "humidity", (Float)data.get( WeatherDatumIdentifier.HUMIDITY ).getValue() );
//
//		generator.writeNumberField( "dewPoint", (Float)data.get( WeatherDatumIdentifier.DEW_POINT ).getValue() );
//		generator.writeNumberField( "windChill", (Float)data.get( WeatherDatumIdentifier.WIND_CHILL ).getValue() );
//		generator.writeNumberField( "heatIndex", (Float)data.get( WeatherDatumIdentifier.HEAT_INDEX ).getValue() );
//		generator.writeNumberField( "pressureTrend", (Float)data.get( WeatherDatumIdentifier.PRESSURE_TREND ).getValue() );
//
//		generator.writeNumberField( "windDirection", (Float)data.get( WeatherDatumIdentifier.WIND_DIRECTION ).getValue() );
//		generator.writeNumberField( "wind", (Float)data.get( WeatherDatumIdentifier.WIND_SPEED_CURRENT ).getValue() );
//
//		generator.writeNumberField( "windTenMinMax", (Float)data.get( WeatherDatumIdentifier.WIND_SPEED_10_MIN_MAX ).getValue() );
//		generator.writeNumberField( "windTenMinAvg", (Float)data.get( WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG ).getValue() );
//		generator.writeNumberField( "windTenMinMin", (Float)data.get( WeatherDatumIdentifier.WIND_SPEED_10_MIN_MIN ).getValue() );
//
//		generator.writeNumberField( "windTwoMinMax", (Float)data.get( WeatherDatumIdentifier.WIND_SPEED_2_MIN_MAX ).getValue() );
//		generator.writeNumberField( "windTwoMinAvg", (Float)data.get( WeatherDatumIdentifier.WIND_SPEED_2_MIN_AVG ).getValue() );
//		generator.writeNumberField( "windTwoMinMin", (Float)data.get( WeatherDatumIdentifier.WIND_SPEED_2_MIN_MIN ).getValue() );
//
//		generator.writeNumberField( "rainTotalDaily", (Float)data.get( WeatherDatumIdentifier.RAIN_TOTAL_DAILY ).getValue() );
//		generator.writeNumberField( "rainRate", (Float)data.get( WeatherDatumIdentifier.RAIN_RATE ).getValue() );
//
//		generator.writeEndObject();
//		generator.close();
//
//		Map<String, String> headers = new HashMap<>();
//		headers.put( "content-type", "application/json" );
//		headers.put( "Authorization", "Basic ZGFsdG9uOkRvNUpwTW84ejVoU3hVaTQ=" );
//
//		return rest( "PUT", "http://mark.soderquist.net/weather/api/station?id=bluewing", headers, stream.toByteArray() ).getCode();
//	}

}
