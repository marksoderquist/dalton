package com.parallelsymmetry.dalton;

import com.parallelsymmetry.utility.log.Log;

import javax.measure.DecimalMeasure;
import javax.measure.Measure;
import javax.measure.quantity.Quantity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArraySet;

public class WeatherStation {

	private Map<WeatherDatumIdentifier, Measure<? extends Number, ? extends Quantity>> data;

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
	}

	public void addPublisher( WeatherDataPublisher publisher ) {
		publishers.add( publisher );
	}

	public void removePublisher( WeatherDataPublisher publisher ) {
		publishers.remove( publisher );
	}

	public void weatherDataEvent( WeatherDataEvent event ) {
		data.put( WeatherDatumIdentifier.TIMESTAMP, DecimalMeasure.valueOf( event.getTimestamp().getTime(), SI.MILLI( SI.SECOND ) ) );

		// Store event data
		for( WeatherDatum datum : event.getData() ) {
			data.put( datum.getIdentifier(), datum.getMeasure() );
		}

		double t = (Double)data.get( WeatherDatumIdentifier.TEMPERATURE ).getValue();
		double h = (Double)data.get( WeatherDatumIdentifier.HUMIDITY ).getValue();
		double w = (Double)data.get( WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG ).getValue();

		// Calculate dew point
		data.put( WeatherDatumIdentifier.DEW_POINT, DecimalMeasure.valueOf( WeatherUtil.calculateDewPoint( t, h ), NonSI.FAHRENHEIT ) );

		// Calculate wind chill
		data.put( WeatherDatumIdentifier.WIND_CHILL, DecimalMeasure.valueOf( WeatherUtil.calculateWindChill( t, w ), NonSI.FAHRENHEIT ) );

		// Calculate heat index
		data.put( WeatherDatumIdentifier.HEAT_INDEX, DecimalMeasure.valueOf( WeatherUtil.calculateHeatIndex( t, h ), NonSI.FAHRENHEIT ) );

		// Update statistics
		update1MinStatistics( event );
		update2MinStatistics( event );
		update5MinStatistics( event );
		update10MinStatistics( event );
		update3HourStatistics( event );

		// Publish data to publishers
		for( WeatherDataPublisher publisher : publishers ) {
			try {
				publisher.publish( data );
			} catch( Throwable throwable ) {
				Log.write( throwable );
			}
		}

		// Log summary
		StringBuilder message = new StringBuilder( "Publishing metrics: " );
		message.append( " T=" ).append( data.get( WeatherDatumIdentifier.TEMPERATURE ).getValue() );
		message.append( " H=" ).append( data.get( WeatherDatumIdentifier.HUMIDITY ).getValue() );
		message.append( " P=" ).append( data.get( WeatherDatumIdentifier.PRESSURE ).getValue() );
		message.append( " W=" ).append( data.get( WeatherDatumIdentifier.WIND_SPEED_CURRENT ).getValue() );
		Log.write( message );

		// Log details
		for( WeatherDatumIdentifier identifier : data.keySet() ) {
			Log.write( Log.DETAIL, identifier, " = ", data.get( identifier ) );
		}
	}

	private void update1MinStatistics( WeatherDataEvent event ) {
		oneMinuteBuffer.post( event );
		double windMin = oneMinuteBuffer.getMinimum( WeatherDatumIdentifier.WIND_SPEED_CURRENT );
		double windMax = oneMinuteBuffer.getMaximum( WeatherDatumIdentifier.WIND_SPEED_CURRENT );
		double windAvg = oneMinuteBuffer.getAverage( WeatherDatumIdentifier.WIND_SPEED_CURRENT );
		data.put( WeatherDatumIdentifier.WIND_SPEED_1_MIN_MIN, DecimalMeasure.valueOf( windMin, NonSI.MILES_PER_HOUR ) );
		data.put( WeatherDatumIdentifier.WIND_SPEED_1_MIN_AVG, DecimalMeasure.valueOf( windAvg, NonSI.MILES_PER_HOUR ) );
		data.put( WeatherDatumIdentifier.WIND_SPEED_1_MIN_MAX, DecimalMeasure.valueOf( windMax, NonSI.MILES_PER_HOUR ) );
	}

	private void update2MinStatistics( WeatherDataEvent event ) {
		twoMinuteBuffer.post( event );
		double windMin = twoMinuteBuffer.getMinimum( WeatherDatumIdentifier.WIND_SPEED_CURRENT );
		double windMax = twoMinuteBuffer.getMaximum( WeatherDatumIdentifier.WIND_SPEED_CURRENT );
		double windAvg = twoMinuteBuffer.getAverage( WeatherDatumIdentifier.WIND_SPEED_CURRENT );
		data.put( WeatherDatumIdentifier.WIND_SPEED_2_MIN_MIN, DecimalMeasure.valueOf( windMin, NonSI.MILES_PER_HOUR ) );
		data.put( WeatherDatumIdentifier.WIND_SPEED_2_MIN_AVG, DecimalMeasure.valueOf( windAvg, NonSI.MILES_PER_HOUR ) );
		data.put( WeatherDatumIdentifier.WIND_SPEED_2_MIN_MAX, DecimalMeasure.valueOf( windMax, NonSI.MILES_PER_HOUR ) );
	}

	private void update5MinStatistics( WeatherDataEvent event ) {
		fiveMinuteBuffer.post( event );
		double windMin = fiveMinuteBuffer.getMinimum( WeatherDatumIdentifier.WIND_SPEED_CURRENT );
		double windMax = fiveMinuteBuffer.getMaximum( WeatherDatumIdentifier.WIND_SPEED_CURRENT );
		double windAvg = fiveMinuteBuffer.getAverage( WeatherDatumIdentifier.WIND_SPEED_CURRENT );
		data.put( WeatherDatumIdentifier.WIND_SPEED_5_MIN_MIN, DecimalMeasure.valueOf( windMin, NonSI.MILES_PER_HOUR ) );
		data.put( WeatherDatumIdentifier.WIND_SPEED_5_MIN_AVG, DecimalMeasure.valueOf( windAvg, NonSI.MILES_PER_HOUR ) );
		data.put( WeatherDatumIdentifier.WIND_SPEED_5_MIN_MAX, DecimalMeasure.valueOf( windMax, NonSI.MILES_PER_HOUR ) );
	}

	private void update10MinStatistics( WeatherDataEvent event ) {
		tenMinuteBuffer.post( event );
		double windMin = tenMinuteBuffer.getMinimum( WeatherDatumIdentifier.WIND_SPEED_CURRENT );
		double windMax = tenMinuteBuffer.getMaximum( WeatherDatumIdentifier.WIND_SPEED_CURRENT );
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

}
