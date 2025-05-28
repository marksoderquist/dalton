package com.parallelsymmetry.dalton;

import com.parallelsymmetry.service.Service;
import com.parallelsymmetry.utility.Parameters;

public class Program extends Service {

	private static final String NET_SODERQUIST_MARK_WEATHER_AUTHENTICATION = "/products/net.soderquist.mark.weather/authentication";

	private final WeatherStation station;

	private final DavisReader reader;

	public static void main( String[] commands ) {
		new Program().process( commands );
	}

	public Program() {
		station = new BluewingWeatherStation();
		station.addPublisher( new MarkSoderquistNetPublisher( getSettings().get( NET_SODERQUIST_MARK_WEATHER_AUTHENTICATION, null ) ) );
		// This is being sent to Perform from the mark.soderquist.net weather server
		//station.addPublisher( new PerformWeatherPublisher( this ) );
		station.addPublisher( new WeatherUndergroundPublisher( this ) );

		reader = new DavisReader();
		reader.addWeatherStation( station );
	}

	public WeatherStation getWeatherStation() {
		return station;
	}

	@Override
	protected void startService( Parameters parameters ) throws Exception {
		reader.start();
	}

	@Override
	protected void process( Parameters parameters, boolean peer ) throws Exception {}

	@Override
	protected void stopService( Parameters parameters ) throws Exception {
		reader.stop();
	}

}
