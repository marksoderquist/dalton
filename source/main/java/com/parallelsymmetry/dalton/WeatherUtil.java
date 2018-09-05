package com.parallelsymmetry.dalton;

import com.parallelsymmetry.utility.math.Statistics;

import java.util.Deque;

public class WeatherUtil {

	public static double calculateDewPoint( double t, double h ) {
		double b = 18.678;
		double c = 470.3;
		double g = Math.log( h / 100f ) + ( b * t / ( c + t ) );
		double dp = ( c * g ) / ( b - g );
		return (double)dp;
	}

	public static double calculateWindChill( double t, double w ) {
		if( w <= 3 || t >= 50 ) return t;

		return 35.74f + 0.6215f * t - 35.75f * Math.pow( w, 0.16 ) + 0.4275f * t * Math.pow( w, 0.16 );
	}

	public static double calculateHeatIndex( double t, double h ) {
		if( t < 80 || h < 40 ) return t;

		double c1 = -42.379f;
		double c2 = 2.04901523f;
		double c3 = 10.14333127f;
		double c4 = -0.22475541f;
		double c5 = -6.83783e-3f;
		double c6 = -5.481717e-10f;
		double c7 = 1.22874e-3f;
		double c8 = 8.5282e-4f;
		double c9 = -1.99e-6f;

		double t2 = t * t;
		double h2 = h * h;

		return c1 + c2 * t + c3 * h + c4 * t * h + c5 * t2 + c6 * h2 + c7 * t2 * h + c8 * t * h2 + c9 * t2 * h2;
	}

	/**
	 * Determine if the instantaneous wind velocity is a gust. A gust is a
	 * velocity greater than 10 MPH above the mean velocity.
	 *
	 * @param wind
	 * @param buffer
	 * @return
	 */
	public static boolean isGust( double wind, Deque<WeatherDataEvent> buffer ) {
		// Collect the wind data from the buffer.
		int index = 0;
		double[] values = new double[buffer.size()];
		for( WeatherDataEvent event : buffer ) {
			for( WeatherDatum datum : event.getData() ) {
				if( WeatherDatumIdentifier.WIND_SPEED_CURRENT == datum.getIdentifier() ) {
					values[index] = (Double)datum.getMeasure().getValue();
					index++;
				}
			}
		}

		// Calculate the wind average.
		double mean = Statistics.mean( values );

		return wind - mean > 10;
	}
}
