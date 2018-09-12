package com.parallelsymmetry.dalton;

import com.parallelsymmetry.utility.log.Log;

import javax.measure.DecimalMeasure;
import javax.measure.unit.NonSI;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class WeatherStation {

	private Set<WeatherDataPublisher> publishers;

	private TimedEventBuffer oneMinuteBuffer;

	private TimedEventBuffer twoMinuteBuffer;

	private TimedEventBuffer fiveMinuteBuffer;

	private TimedEventBuffer tenMinuteBuffer;

	private TimedEventBuffer oneHourBuffer;

	private TimedEventBuffer threeHourBuffer;

	public WeatherStation() {
		publishers = new CopyOnWriteArraySet<>();

		oneMinuteBuffer = new TimedEventBuffer( 60000 );
		twoMinuteBuffer = new TimedEventBuffer( 120000 );
		fiveMinuteBuffer = new TimedEventBuffer( 300000 );
		tenMinuteBuffer = new TimedEventBuffer( 600000 );
		oneHourBuffer = new TimedEventBuffer( 3600000 );
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
		// Add the event to the statistics buffers
		oneMinuteBuffer.post( event );
		twoMinuteBuffer.post( event );
		fiveMinuteBuffer.post( event );
		tenMinuteBuffer.post( event );
		oneHourBuffer.post( event );
		threeHourBuffer.post( event );

		// One minute statistics
		event.add( new WeatherDatum( WeatherDatumIdentifier.WIND_SPEED_1_MIN_MIN, DecimalMeasure.valueOf( oneMinuteBuffer.getMinimum( WeatherDatumIdentifier.WIND_SPEED ), NonSI.MILES_PER_HOUR ) ) );
		event.add( new WeatherDatum( WeatherDatumIdentifier.WIND_SPEED_1_MIN_AVG, DecimalMeasure.valueOf( oneMinuteBuffer.getAverage( WeatherDatumIdentifier.WIND_SPEED ), NonSI.MILES_PER_HOUR ) ) );
		event.add( new WeatherDatum( WeatherDatumIdentifier.WIND_SPEED_1_MIN_MAX, DecimalMeasure.valueOf( oneMinuteBuffer.getMaximum( WeatherDatumIdentifier.WIND_SPEED ), NonSI.MILES_PER_HOUR ) ) );

		// Two minute statistics
		event.add( new WeatherDatum( WeatherDatumIdentifier.WIND_SPEED_2_MIN_MIN, DecimalMeasure.valueOf( twoMinuteBuffer.getMinimum( WeatherDatumIdentifier.WIND_SPEED ), NonSI.MILES_PER_HOUR ) ) );
		event.add( new WeatherDatum( WeatherDatumIdentifier.WIND_SPEED_2_MIN_AVG, DecimalMeasure.valueOf( twoMinuteBuffer.getAverage( WeatherDatumIdentifier.WIND_SPEED ), NonSI.MILES_PER_HOUR ) ) );
		event.add( new WeatherDatum( WeatherDatumIdentifier.WIND_SPEED_2_MIN_MAX, DecimalMeasure.valueOf( twoMinuteBuffer.getMaximum( WeatherDatumIdentifier.WIND_SPEED ), NonSI.MILES_PER_HOUR ) ) );
		event.add( new WeatherDatum( WeatherDatumIdentifier.WIND_DIRECTION_2_MIN_AVG, DecimalMeasure.valueOf( TimedEventBuffer.getAngleInDegrees( twoMinuteBuffer.getAverageVector( WeatherDatumIdentifier.WIND_SPEED,
				WeatherDatumIdentifier.WIND_DIRECTION ) ), NonSI.DEGREE_ANGLE ) ) );

		// Five minute statistics
		event.add( new WeatherDatum( WeatherDatumIdentifier.WIND_SPEED_5_MIN_MIN, DecimalMeasure.valueOf( fiveMinuteBuffer.getMinimum( WeatherDatumIdentifier.WIND_SPEED ), NonSI.MILES_PER_HOUR ) ) );
		event.add( new WeatherDatum( WeatherDatumIdentifier.WIND_SPEED_5_MIN_AVG, DecimalMeasure.valueOf( fiveMinuteBuffer.getAverage( WeatherDatumIdentifier.WIND_SPEED ), NonSI.MILES_PER_HOUR ) ) );
		event.add( new WeatherDatum( WeatherDatumIdentifier.WIND_SPEED_5_MIN_MAX, DecimalMeasure.valueOf( fiveMinuteBuffer.getMaximum( WeatherDatumIdentifier.WIND_SPEED ), NonSI.MILES_PER_HOUR ) ) );

		// Ten minute statistics
		event.add( new WeatherDatum( WeatherDatumIdentifier.WIND_SPEED_10_MIN_MIN, DecimalMeasure.valueOf( tenMinuteBuffer.getMinimum( WeatherDatumIdentifier.WIND_SPEED ), NonSI.MILES_PER_HOUR ) ) );
		event.add( new WeatherDatum( WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG, DecimalMeasure.valueOf( tenMinuteBuffer.getAverage( WeatherDatumIdentifier.WIND_SPEED ), NonSI.MILES_PER_HOUR ) ) );
		event.add( new WeatherDatum( WeatherDatumIdentifier.WIND_SPEED_10_MIN_MAX, DecimalMeasure.valueOf( tenMinuteBuffer.getMaximum( WeatherDatumIdentifier.WIND_SPEED ), NonSI.MILES_PER_HOUR ) ) );
		event.add( new WeatherDatum( WeatherDatumIdentifier.WIND_DIRECTION_10_MIN_AVG, DecimalMeasure.valueOf( TimedEventBuffer.getAngleInDegrees( tenMinuteBuffer.getAverageVector( WeatherDatumIdentifier.WIND_SPEED, WeatherDatumIdentifier.WIND_DIRECTION ) ), NonSI.DEGREE_ANGLE ) ) );

		double temperatureTrend = oneHourBuffer.getTrendPerHour( WeatherDatumIdentifier.TEMPERATURE );
		double humidityTrend = threeHourBuffer.getTrendPerHour( WeatherDatumIdentifier.HUMIDITY );
		double pressureTrend = threeHourBuffer.getTrendPerHour( WeatherDatumIdentifier.PRESSURE );
		double windSpeedTrend = oneHourBuffer.getTrendPerHour( WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG );

		event.add( new WeatherDatum( WeatherDatumIdentifier.TEMPERATURE_TREND, DecimalMeasure.valueOf( temperatureTrend, NonSI.FAHRENHEIT.divide( NonSI.HOUR ) ) ) );
		event.add( new WeatherDatum( WeatherDatumIdentifier.WIND_SPEED_TREND, DecimalMeasure.valueOf( windSpeedTrend, NonSI.MILES_PER_HOUR.divide( NonSI.HOUR ) ) ) );
		event.add( new WeatherDatum( WeatherDatumIdentifier.HUMIDITY_TREND, DecimalMeasure.valueOf( humidityTrend, NonSI.PERCENT.divide( NonSI.HOUR ) ) ) );
		event.add( new WeatherDatum( WeatherDatumIdentifier.PRESSURE_TREND, DecimalMeasure.valueOf( pressureTrend, NonSI.INCH_OF_MERCURY.divide( NonSI.HOUR ) ) ) );

		double t = event.getValue( WeatherDatumIdentifier.TEMPERATURE );
		double h = event.getValue( WeatherDatumIdentifier.HUMIDITY );
		double w = event.getValue( WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG );

		event.add( new WeatherDatum( WeatherDatumIdentifier.DEW_POINT, DecimalMeasure.valueOf( WeatherUtil.calculateDewPoint( t, h ), NonSI.FAHRENHEIT ) ) );
		event.add( new WeatherDatum( WeatherDatumIdentifier.WIND_CHILL, DecimalMeasure.valueOf( WeatherUtil.calculateWindChill( t, w ), NonSI.FAHRENHEIT ) ) );
		event.add( new WeatherDatum( WeatherDatumIdentifier.HEAT_INDEX, DecimalMeasure.valueOf( WeatherUtil.calculateHeatIndex( t, h ), NonSI.FAHRENHEIT ) ) );

		// Publish data to publishers
		for( WeatherDataPublisher publisher : publishers ) {
			try {
				publisher.publish( this, event );
			} catch( Throwable throwable ) {
				Log.write( throwable );
			}
		}

		// Log summary
		StringBuilder message = new StringBuilder( "Publishing metrics: " );
		message.append( " T=" ).append( event.getValue( WeatherDatumIdentifier.TEMPERATURE ) );
		message.append( " H=" ).append( event.getValue( WeatherDatumIdentifier.HUMIDITY ) );
		message.append( " P=" ).append( event.getValue( WeatherDatumIdentifier.PRESSURE ) );
		message.append( " W=" ).append( event.getValue( WeatherDatumIdentifier.WIND_SPEED ) );
		Log.write( message );
	}

}
