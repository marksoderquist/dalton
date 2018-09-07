package com.parallelsymmetry.dalton;

public class WeatherUtil {

	public static double calculateDewPoint( double t, double h ) {
		double b = 18.678;
		double c = 470.3;
		double g = Math.log( h / 100f ) + (b * t / (c + t));
		return (c * g) / (b - g);
	}

	public static double calculateWindChill( double t, double w ) {
		if( w <= 3 || t >= 50 ) return t;

		return 35.74f + 0.6215f * t - 35.75f * Math.pow( w, 0.16 ) + 0.4275f * t * Math.pow( w, 0.16 );
	}

	public static double calculateHeatIndex( double t, double h ) {
		if( t < 80 || h < 40 ) return t;

		double c1 = -42.379;
		double c2 = 2.04901523;
		double c3 = 10.14333127;
		double c4 = -0.22475541;
		double c5 = -6.83783e-3;
		double c6 = -5.481717e-10;
		double c7 = 1.22874e-3;
		double c8 = 8.5282e-4;
		double c9 = -1.99e-6;

		double t2 = t * t;
		double h2 = h * h;

		return c1 + c2 * t + c3 * h + c4 * t * h + c5 * t2 + c6 * h2 + c7 * t2 * h + c8 * t * h2 + c9 * t2 * h2;
	}

	/**
	 * Determine if the instantaneous wind velocity is a gust. A gust is a
	 * velocity greater than 10 MPH above the mean velocity.
	 *
	 * @param wind The wind speed
	 * @param buffer The weather data event buffer
	 * @return If the wind speed is a gust relative to the buffer data
	 */
	public static boolean isGust( double wind, TimedEventBuffer buffer ) {
		return isGust( wind, buffer.getAverage( WeatherDatumIdentifier.WIND_SPEED ) );
	}

	public static boolean isGust( double wind, double windAverage ) {
		return wind - windAverage > 10;
	}

}
