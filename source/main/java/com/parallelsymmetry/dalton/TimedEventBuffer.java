package com.parallelsymmetry.dalton;

import com.parallelsymmetry.utility.math.Statistics;
import javolution.lang.MathLib;
import org.jscience.mathematics.vector.Float64Vector;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedDeque;

public class TimedEventBuffer {

	private Deque<WeatherDataEvent> buffer;

	private long timeout;

	public TimedEventBuffer( long timeout ) {
		this.timeout = timeout;
		buffer = new ConcurrentLinkedDeque<>();
	}

	public void post( WeatherDataEvent event ) {
		buffer.push( event );
		trimEvents();
	}

	public Deque<WeatherDataEvent> getEvents() {
		return new LinkedList<>( buffer );
	}

	public double getAverage( WeatherDatumIdentifier identifier ) {
		double total = 0;
		long count = 0;

		for( WeatherDataEvent event : buffer ) {
			Double value = (Double)event.get( identifier ).getMeasure().getValue();
			if( value != null ) {
				total += value;
				count++;
			}
		}

		return total / count;
	}

	public double getMinimum( WeatherDatumIdentifier identifier ) {
		double minimum = Double.MAX_VALUE;

		for( WeatherDataEvent event : buffer ) {
			Double value = (Double)event.get( identifier ).getMeasure().getValue();
			if( value != null && value < minimum ) minimum = value;
		}

		return minimum;
	}

	public double getMaximum( WeatherDatumIdentifier identifier ) {
		double maximum = Double.MIN_VALUE;

		for( WeatherDataEvent event : buffer ) {
			Double value = (Double)event.get( identifier ).getMeasure().getValue();
			if( value != null && value > maximum ) maximum = value;
		}

		return maximum;
	}

	public double getTrendPerHour( WeatherDatumIdentifier identifier ) {
		return getTrendPerMillisecond( identifier ) * 3600000;
	}

	private double getTrendPerMillisecond( WeatherDatumIdentifier identifier ) {
		if( buffer.size() < 2 ) return 0;

		int index = 0;
		int count = buffer.size();
		double[] times = new double[ count ];
		double[] values = new double[ count ];
		for( WeatherDataEvent event : buffer ) {
			WeatherDatum timeDatum = event.get( WeatherDatumIdentifier.TIMESTAMP );
			WeatherDatum datum = event.get( identifier );
			if( timeDatum == null || datum == null ) continue;
			times[ index ] = timeDatum.getMeasure().getValue().doubleValue();
			values[ index ] = datum.getMeasure().getValue().doubleValue();
			index++;
		}

		return Statistics.leastSquaresSlope( times, values );
	}

	public static double getMagnitude( Float64Vector v ) {
		return v.normValue();
	}

	public static double getAngleInDegrees( Float64Vector v ) {
		double degrees = MathLib.toDegrees( Math.atan2( v.getValue( 1 ), v.getValue( 0 ) ) );
		if( degrees < 0 ) degrees += 360;
		return degrees;
	}

	public Float64Vector getAverageVector( WeatherDatumIdentifier magnitude, WeatherDatumIdentifier direction ) {
		int count = 0;
		double totalX = 0;
		double totalY = 0;

		for( WeatherDataEvent event : buffer ) {
			double m = event.get( magnitude ).getMeasure().getValue().doubleValue();
			double d = event.get( direction ).getMeasure().getValue().doubleValue();

			double a = MathLib.toRadians( d );

			double x = m * Math.sin( a );
			double y = m * Math.cos( a );

			totalX += x;
			totalY += y;
			count++;
		}

		double averageX = totalX / count;
		double averageY = totalY / count;

		return Float64Vector.valueOf( averageX, averageY );
	}

	private void trimEvents() {
		long mostRecentEventTime = buffer.peekFirst().getTimestamp();
		WeatherDataEvent last = buffer.peekLast();

		while( mostRecentEventTime - last.getTimestamp() > timeout ) {
			buffer.pollLast();
			last = buffer.peekLast();
		}
	}

}
