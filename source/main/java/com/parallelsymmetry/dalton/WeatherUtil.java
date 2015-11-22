package com.parallelsymmetry.dalton;

public class WeatherUtil {

	public static float calculateDewPoint( float t, float h ) {
		double b = 18.678;
		double c = 470.3;
		double g = Math.log( h / 100f ) + ( b * t / ( c + t ) );
		double dp = ( c * g ) / ( b - g );
		return (float)dp;
	}

	public static float calculateWindChill( float t, float w ) {
		if( w <= 3 || t >= 50 ) return t;
		
		return 35.74f + 0.6215f * t - 35.75f * (float)Math.pow( w, 0.16 ) + 0.4275f * t * (float)Math.pow( w, 0.16 );
	}

	public static float calculateHeatIndex( float t, float h ) {
		if( t < 80 || h < 40 ) return t;

		float c1 = -42.379f;
		float c2 = 2.04901523f;
		float c3 = 10.14333127f;
		float c4 = -0.22475541f;
		float c5 = -6.83783e-3f;
		float c6 = -5.481717e-10f;
		float c7 = 1.22874e-3f;
		float c8 = 8.5282e-4f;
		float c9 = -1.99e-6f;

		float t2 = t * t;
		float h2 = h * h;

		return c1 + c2 * t + c3 * h + c4 * t * h + c5 * t2 + c6 * h2 + c7 * t2 * h + c8 * t * h2 + c9 * t2 * h2;
	}

}
