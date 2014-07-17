package com.parallelsymmetry.dalton;

import java.util.Date;
import java.util.TimeZone;

import com.parallelsymmetry.utility.DateUtil;

import junit.framework.TestCase;

public class WeatherStationTest extends TestCase {

	public void testWundergroundDateFormat() {
		assertEquals( "1970-01-01+00%3A00%3A00", DateUtil.format( new Date( 0 ), WeatherStation.WUNDERGROUND_DATE_FORMAT, TimeZone.getTimeZone( "UTC" ) ) );
	}
	
}
