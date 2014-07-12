package com.parallelsymmetry.dalton;

import java.util.Set;

import com.parallelsymmetry.service.Service;
import com.parallelsymmetry.utility.Parameters;

public class WeatherReader extends Service {

	private WeatherStation station;

	private DavisReader reader;

	public static final void main( String[] commands ) {
		new WeatherReader().process( commands );
	}

	public WeatherReader() {
		station = new WeatherStation();
		reader = new DavisReader();
		reader.addWeatherDataListener( station );
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
