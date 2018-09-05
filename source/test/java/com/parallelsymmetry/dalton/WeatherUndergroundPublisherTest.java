package com.parallelsymmetry.dalton;

import com.parallelsymmetry.utility.DateUtil;
import org.junit.Test;

import java.util.Date;
import java.util.TimeZone;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class WeatherUndergroundPublisherTest {

	@Test
	public void testWundergroundDateFormat() {
		assertThat( DateUtil.format( new Date( 0 ), WeatherUndergroundPublisher.WUNDERGROUND_DATE_FORMAT, TimeZone.getTimeZone( "UTC" ) ), is( "1970-01-01+00%3A00%3A00" ) );
	}

}
