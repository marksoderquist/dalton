package com.parallelsymmetry.dalton;

import com.parallelsymmetry.utility.log.Log;

import javax.measure.DecimalMeasure;
import javax.measure.Measure;
import javax.measure.quantity.Quantity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class WeatherStation {

	private Map<WeatherDatumIdentifier, Measure<? extends Number, ? extends Quantity>> data;

	private Set<WeatherDataPublisher> publishers;

	private TimedEventBuffer oneMinuteBuffer;

	private TimedEventBuffer twoMinuteBuffer;

	private TimedEventBuffer fiveMinuteBuffer;

	private TimedEventBuffer tenMinuteBuffer;

	private TimedEventBuffer threeHourBuffer;

	public WeatherStation() {
		data = new ConcurrentHashMap<>();
		publishers = new CopyOnWriteArraySet<>();

		oneMinuteBuffer = new TimedEventBuffer( 60000 );
		twoMinuteBuffer = new TimedEventBuffer( 120000 );
		fiveMinuteBuffer = new TimedEventBuffer( 300000 );
		tenMinuteBuffer = new TimedEventBuffer( 600000 );
		threeHourBuffer = new TimedEventBuffer( 10800000 );
	}

	public TimedEventBuffer getOneMinuteBuffer() {
		return oneMinuteBuffer;
	}

	public TimedEventBuffer getTwoMinuteBuffer() {
		return twoMinuteBuffer;
	}

	public TimedEventBuffer getFiveMinuteBuffer() {
		return fiveMinuteBuffer;
	}

	public TimedEventBuffer getTenMinuteBuffer() {
		return tenMinuteBuffer;
	}

	public TimedEventBuffer getThreeHourBuffer() {
		return threeHourBuffer;
	}

	public void addPublisher( WeatherDataPublisher publisher ) {
		publishers.add( publisher );
	}

	public void removePublisher( WeatherDataPublisher publisher ) {
		publishers.remove( publisher );
	}

	public void weatherDataEvent( WeatherDataEvent event ) {
		// Update statistics buffers
		oneMinuteBuffer.post( event );
		twoMinuteBuffer.post( event );
		fiveMinuteBuffer.post( event );
		tenMinuteBuffer.post( event );
		threeHourBuffer.post( event );

		data.put( WeatherDatumIdentifier.TIMESTAMP, DecimalMeasure.valueOf( event.getTimestamp(), SI.MILLI( SI.SECOND ) ) );

		// Store standard event data
		for( WeatherDatum datum : event.getData() ) {
			data.put( datum.getIdentifier(), datum.getMeasure() );
		}

		// FIXME Should derived values be added to the event that got put in the statistics?
		// That way those values can be used further on to derive even more values?

		// One minute statistics
		data.put( WeatherDatumIdentifier.WIND_SPEED_1_MIN_MIN, DecimalMeasure.valueOf( oneMinuteBuffer.getMinimum( WeatherDatumIdentifier.WIND_SPEED ), NonSI.MILES_PER_HOUR ) );
		data.put( WeatherDatumIdentifier.WIND_SPEED_1_MIN_AVG, DecimalMeasure.valueOf( oneMinuteBuffer.getAverage( WeatherDatumIdentifier.WIND_SPEED ), NonSI.MILES_PER_HOUR ) );
		data.put( WeatherDatumIdentifier.WIND_SPEED_1_MIN_MAX, DecimalMeasure.valueOf( oneMinuteBuffer.getMaximum( WeatherDatumIdentifier.WIND_SPEED ), NonSI.MILES_PER_HOUR ) );

		// Two minute statistics
		data.put( WeatherDatumIdentifier.WIND_SPEED_2_MIN_MIN, DecimalMeasure.valueOf( twoMinuteBuffer.getMinimum( WeatherDatumIdentifier.WIND_SPEED ), NonSI.MILES_PER_HOUR ) );
		data.put( WeatherDatumIdentifier.WIND_SPEED_2_MIN_AVG, DecimalMeasure.valueOf( twoMinuteBuffer.getAverage( WeatherDatumIdentifier.WIND_SPEED ), NonSI.MILES_PER_HOUR ) );
		data.put( WeatherDatumIdentifier.WIND_SPEED_2_MIN_MAX, DecimalMeasure.valueOf( twoMinuteBuffer.getMaximum( WeatherDatumIdentifier.WIND_SPEED ), NonSI.MILES_PER_HOUR ) );

		// Five minute statistics
		data.put( WeatherDatumIdentifier.WIND_SPEED_5_MIN_MIN, DecimalMeasure.valueOf( fiveMinuteBuffer.getMinimum( WeatherDatumIdentifier.WIND_SPEED ), NonSI.MILES_PER_HOUR ) );
		data.put( WeatherDatumIdentifier.WIND_SPEED_5_MIN_AVG, DecimalMeasure.valueOf( fiveMinuteBuffer.getAverage( WeatherDatumIdentifier.WIND_SPEED ), NonSI.MILES_PER_HOUR ) );
		data.put( WeatherDatumIdentifier.WIND_SPEED_5_MIN_MAX, DecimalMeasure.valueOf( fiveMinuteBuffer.getMaximum( WeatherDatumIdentifier.WIND_SPEED ), NonSI.MILES_PER_HOUR ) );

		// Ten minute statistics
		data.put( WeatherDatumIdentifier.WIND_SPEED_10_MIN_MIN, DecimalMeasure.valueOf( tenMinuteBuffer.getMinimum( WeatherDatumIdentifier.WIND_SPEED ), NonSI.MILES_PER_HOUR ) );
		data.put( WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG, DecimalMeasure.valueOf( tenMinuteBuffer.getAverage( WeatherDatumIdentifier.WIND_SPEED ), NonSI.MILES_PER_HOUR ) );
		data.put( WeatherDatumIdentifier.WIND_SPEED_10_MIN_MAX, DecimalMeasure.valueOf( tenMinuteBuffer.getMaximum( WeatherDatumIdentifier.WIND_SPEED ), NonSI.MILES_PER_HOUR ) );

		double temperatureTrend = fiveMinuteBuffer.getTrend( WeatherDatumIdentifier.TEMPERATURE ) * 12.0;
		double humidityTrend = threeHourBuffer.getTrend( WeatherDatumIdentifier.HUMIDITY ) / 3.0;
		double pressureTrend = threeHourBuffer.getTrend( WeatherDatumIdentifier.PRESSURE ) / 3.0;
		double windSpeedTrend = tenMinuteBuffer.getTrend( WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG ) * 6.0;

		data.put( WeatherDatumIdentifier.TEMPERATURE_TREND, DecimalMeasure.valueOf( temperatureTrend, NonSI.FAHRENHEIT.divide( NonSI.HOUR ) ) );
		data.put( WeatherDatumIdentifier.WIND_SPEED_TREND, DecimalMeasure.valueOf( windSpeedTrend, NonSI.MILES_PER_HOUR.divide( NonSI.HOUR ) ) );
		data.put( WeatherDatumIdentifier.HUMIDITY_TREND, DecimalMeasure.valueOf( humidityTrend, NonSI.PERCENT.divide( NonSI.HOUR ) ) );
		data.put( WeatherDatumIdentifier.PRESSURE_TREND, DecimalMeasure.valueOf( pressureTrend, NonSI.INCH_OF_MERCURY.divide( NonSI.HOUR ) ) );

		double t = (Double)data.get( WeatherDatumIdentifier.TEMPERATURE ).getValue();
		double h = (Double)data.get( WeatherDatumIdentifier.HUMIDITY ).getValue();
		double w = (Double)data.get( WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG ).getValue();

		// Calculate dew point
		data.put( WeatherDatumIdentifier.DEW_POINT, DecimalMeasure.valueOf( WeatherUtil.calculateDewPoint( t, h ), NonSI.FAHRENHEIT ) );

		// Calculate wind chill
		data.put( WeatherDatumIdentifier.WIND_CHILL, DecimalMeasure.valueOf( WeatherUtil.calculateWindChill( t, w ), NonSI.FAHRENHEIT ) );

		// Calculate heat index
		data.put( WeatherDatumIdentifier.HEAT_INDEX, DecimalMeasure.valueOf( WeatherUtil.calculateHeatIndex( t, h ), NonSI.FAHRENHEIT ) );

		// Publish data to publishers
		for( WeatherDataPublisher publisher : publishers ) {
			try {
				publisher.publish( this, data );
			} catch( Throwable throwable ) {
				Log.write( throwable );
			}
		}

		// Log summary
		StringBuilder message = new StringBuilder( "Publishing metrics: " );
		message.append( " T=" ).append( data.get( WeatherDatumIdentifier.TEMPERATURE ).getValue() );
		message.append( " H=" ).append( data.get( WeatherDatumIdentifier.HUMIDITY ).getValue() );
		message.append( " P=" ).append( data.get( WeatherDatumIdentifier.PRESSURE ).getValue() );
		message.append( " W=" ).append( data.get( WeatherDatumIdentifier.WIND_SPEED ).getValue() );
		Log.write( message );

		// Log details
		for( WeatherDatumIdentifier identifier : data.keySet() ) {
			Log.write( Log.DETAIL, identifier, " = ", data.get( identifier ) );
		}
	}

}
