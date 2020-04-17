package com.parallelsymmetry.dalton;

import com.parallelsymmetry.service.Service;
import com.parallelsymmetry.utility.Parameters;

public class Program extends Service {

	private WeatherStation station;

	private DavisReader reader;

	public static final void main( String[] commands ) {
		new Program().process( commands );
	}

	public Program() {
		reader = new DavisReader();
		station = new WeatherStation();
		reader.addWeatherStation( station );
		station.addPublisher( new MarkSoderquistWeatherPublisher() );
		station.addPublisher( new PerformWeatherPublisher( this ) );
		station.addPublisher( new WeatherUndergroundPublisher( this ) );
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
