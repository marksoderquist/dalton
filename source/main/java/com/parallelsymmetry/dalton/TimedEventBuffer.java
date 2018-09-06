package com.parallelsymmetry.dalton;

import java.util.Deque;
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

	public double getMinimum( WeatherDatumIdentifier identifier ) {
		double minimum = Double.MAX_VALUE;

		for( WeatherDataEvent event : buffer ) {
			Double value = (Double)event.get( identifier ).getMeasure().getValue();
			if( value != null && value < minimum ) minimum = value;
		}

		return minimum;
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

	public double getTrend( WeatherDatumIdentifier identifier ) {
		if( buffer.size() < 2 ) return 0;

		double trend = 0;

		double first = (Double)buffer.peekFirst().get( identifier ).getMeasure().getValue();
		double last = (Double)buffer.peekLast().get( identifier ).getMeasure().getValue();
		trend = first - last;

		return trend;
	}

	public double getMaximum( WeatherDatumIdentifier identifier ) {
		double maximum = Double.MIN_VALUE;

		for( WeatherDataEvent event : buffer ) {
			Double value = (Double)event.get( identifier ).getMeasure().getValue();
			if( value != null && value > maximum ) maximum = value;
		}

		return 0;
	}

	private void trimEvents() {
		long mostRecentEventTime = buffer.peekFirst().getTimestamp().getTime();
		WeatherDataEvent last = buffer.peekLast();

		while( mostRecentEventTime - last.getTimestamp().getTime() > timeout ) {
			buffer.pollLast();
			last = buffer.peekLast();
		}
	}

}