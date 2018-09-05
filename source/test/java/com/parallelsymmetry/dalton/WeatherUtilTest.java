package com.parallelsymmetry.dalton;

import junit.framework.TestCase;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class WeatherUtilTest extends TestCase {

	public void testCalculateDewPoint() {
		assertThat( WeatherUtil.calculateDewPoint( 32, 100 ), is( 32.0 ) );
		assertThat( WeatherUtil.calculateDewPoint( 60, 100 ), is( 60.000000000000014 ) );
		assertThat( WeatherUtil.calculateDewPoint( 90, 100 ), is( 90.0 ) );

		assertThat( WeatherUtil.calculateDewPoint( 32, 50 ), is( 12.850148660037606 ) );
		assertThat( WeatherUtil.calculateDewPoint( 60, 50 ), is( 38.70094537781329 ) );
		assertThat( WeatherUtil.calculateDewPoint( 90, 50 ), is( 66.27683376137043 ) );
	}

}
