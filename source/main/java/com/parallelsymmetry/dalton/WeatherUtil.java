package com.parallelsymmetry.dalton;

public class WeatherUtil {

	public static float calculateDewPoint( float t, float h ) {
		double b = 18.678;
		double c = 470.3;
		double g = Math.log( h / 100f ) + ( b * t / ( c + t ) );
		double dp = ( c * g ) / ( b - g );
		return (float)dp;
	}
	
}
