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
		if( w > 0 && t < 50 ) return 35.74f
			+ 0.6215f * t
			- 35.75f * (float)Math.pow( w, 0.16 )
			+ 0.4275f * t * (float)Math.pow( w, 0.16 );
		return t;
	}

	public static float calculateHeatIndex( float t, float h ) {
		return t;
	}

}
