package com.parallelsymmetry.dalton;

import junit.framework.TestCase;

public class WeatherUtilTest extends TestCase {

	public void testCalculateDewPoint() {
		assertEquals( 32f, WeatherUtil.calculateDewPoint( 32, 100 ) );
		assertEquals( 60f, WeatherUtil.calculateDewPoint( 60, 100 ) );
		assertEquals( 90f, WeatherUtil.calculateDewPoint( 90, 100 ) );

		assertEquals( 12.850148f, WeatherUtil.calculateDewPoint( 32, 50 ) );
		assertEquals( 38.700947f, WeatherUtil.calculateDewPoint( 60, 50 ) );
		assertEquals( 66.27683f, WeatherUtil.calculateDewPoint( 90, 50 ) );
	}

}
