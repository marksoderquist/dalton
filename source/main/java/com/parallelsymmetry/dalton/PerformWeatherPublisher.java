package com.parallelsymmetry.dalton;

import java.io.IOException;

public class PerformWeatherPublisher extends HttpPublisher {

	private final Program program;

	public PerformWeatherPublisher( Program program ) {
		this.program = program;
	}

	@Override
	public int publish( WeatherStation station, WeatherDataEvent event ) throws IOException {
		return rest( "GET", generateRequest( station, event ) ).getCode();
	}

	private String generateRequest( WeatherStation station, WeatherDataEvent event ) {
		String stationId = "39";
		String accessKey = "b8f4c216-c54f-4c2f-bc33-5ab120accca3";
		StringBuilder builder = new StringBuilder( "https://perform.southbranchcontrols.com" );
		builder.append( "/api/stations/" ).append( stationId ).append( "/data/feed" );
		builder.append( "?accesskey=" ).append( accessKey );

		// Need to send online flag VIN-0
		builder.append( "&vin-0=1" );

		// Timestamp
		addLong( event, builder, WeatherDatumIdentifier.TIMESTAMP, "mem-0" );

		// Prepare basic data
		addDouble( event, builder, WeatherDatumIdentifier.TEMPERATURE, "mem-10" );
		addDouble( event, builder, WeatherDatumIdentifier.PRESSURE, "mem-11" );
		addDouble( event, builder, WeatherDatumIdentifier.HUMIDITY, "mem-12" );
		addDouble( event, builder, WeatherDatumIdentifier.DEW_POINT, "mem-13" );

		// Prepare wind data
		addDouble( event, builder, WeatherDatumIdentifier.WIND_DIRECTION_10_MIN_AVG, "mem-20" );
		addDouble( event, builder, WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG, "mem-21" );

		return builder.toString();
	}

	private void addLong( WeatherDataEvent event, StringBuilder builder, WeatherDatumIdentifier identifier, String key ) {
		Long value = event.getValue( identifier );
		if( value == null ) return;

		builder.append( "&" );
		builder.append( key );
		builder.append( "=" );
		builder.append( value );
	}

	private void addDouble( WeatherDataEvent event, StringBuilder builder, WeatherDatumIdentifier identifier, String key ) {
		Double value = event.getValue( identifier );
		if( value == null ) return;

		builder.append( "&" );
		builder.append( key );
		builder.append( "=" );
		builder.append( Double.doubleToLongBits( value ) );
	}

}
