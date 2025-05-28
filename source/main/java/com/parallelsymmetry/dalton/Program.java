package com.parallelsymmetry.dalton;

import com.parallelsymmetry.service.Service;
import com.parallelsymmetry.utility.Parameters;
import com.parallelsymmetry.utility.setting.Settings;

public class Program extends Service {

	private final WeatherStation station;

	private final DavisReader reader;

	public static void main( String[] commands ) {
		new Program().process( commands );
	}

	public Program() {
		Settings products = getSettings().getNode( "products" );
		Settings markSoderquistNetPublisherSettings = products.getNode( "mark.soderquist.net.weather" );
		markSoderquistNetPublisherSettings.put( "tryhere", "with this value" );

		station = new BluewingWeatherStation();
		station.addPublisher( new MarkSoderquistNetPublisher(markSoderquistNetPublisherSettings.get( "authentication", null )) );
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
