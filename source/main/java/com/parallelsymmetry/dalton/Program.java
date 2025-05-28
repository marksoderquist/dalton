package com.parallelsymmetry.dalton;

import com.parallelsymmetry.service.Service;
import com.parallelsymmetry.utility.Parameters;
import com.parallelsymmetry.utility.setting.Settings;
import lombok.Getter;

import java.io.Reader;

public class Program extends Service {

	@Getter
	private final DavisReader reader;

	@Getter
	private final WeatherStation station;

	public static void main( String[] commands ) {
		new Program().process( commands );
	}

	public Program() {
		reader = new DavisReader();
		station = new BluewingWeatherStation();
	}

	@Override
	protected void startService( Parameters parameters ) throws Exception {
		Settings markSoderquistNetPublisherSettings = getSettings().getNode( "products/mark.soderquist.net.weather" );
		markSoderquistNetPublisherSettings.put( "tryhere", "with this value" );
		markSoderquistNetPublisherSettings.flush();

		station.addPublisher( new MarkSoderquistNetPublisher(markSoderquistNetPublisherSettings.get( "authorization", null )) );
		// This is being sent to Perform from the mark.soderquist.net weather server
		//station.addPublisher( new PerformWeatherPublisher( this ) );
		station.addPublisher( new WeatherUndergroundPublisher( this ) );

		reader.addWeatherStation( station );

		reader.start();
	}

	@Override
	protected void process( Parameters parameters, boolean peer ) throws Exception {}

	@Override
	protected void stopService( Parameters parameters ) throws Exception {
		reader.stop();
	}

}
