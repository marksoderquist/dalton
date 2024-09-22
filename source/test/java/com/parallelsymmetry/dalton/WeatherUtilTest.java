package com.parallelsymmetry.dalton;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WeatherUtilTest {

	@Test
	public void testCalculateDewPoint() {
		assertThat( WeatherUtil.calculateDewPoint( 32, 100 ) ).isEqualTo( 32.0 );
		assertThat( WeatherUtil.calculateDewPoint( 60, 100 ) ).isEqualTo( 60.000000000000014 );
		assertThat( WeatherUtil.calculateDewPoint( 90, 100 ) ).isEqualTo( 90.0 );

		assertThat( WeatherUtil.calculateDewPoint( 32, 50 ) ).isEqualTo( 12.850148660037606 );
		assertThat( WeatherUtil.calculateDewPoint( 60, 50 ) ).isEqualTo( 38.70094537781329 );
		assertThat( WeatherUtil.calculateDewPoint( 90, 50 ) ).isEqualTo( 66.27683376137043 );
	}

}
