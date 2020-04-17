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

	String generateRequest( WeatherStation station, WeatherDataEvent event ) {
		String stationId = "39";
		String accessKey = "b8f4c216-c54f-4c2f-bc33-5ab120accca3";
		StringBuilder builder = new StringBuilder( "https://perform.southbranchcontrols.com" );
		builder.append( "/api/stations/" ).append( stationId ).append( "/data/feed" );
		builder.append( "?accesskey=" ).append( accessKey );

		// Need to send online flag VBIT-0
		builder.append( "&vbit-0=1" );

		// Timestamp
		addLong( event, builder, WeatherDatumIdentifier.TIMESTAMP, "mem-0" );

		// Basic data
		addDouble( event, builder, WeatherDatumIdentifier.TEMPERATURE, "mem-10" );
		addDouble( event, builder, WeatherDatumIdentifier.PRESSURE, "mem-11" );
		addDouble( event, builder, WeatherDatumIdentifier.HUMIDITY, "mem-12" );
		addDouble( event, builder, WeatherDatumIdentifier.DEW_POINT, "mem-13" );

		addDouble( event, builder, WeatherDatumIdentifier.WIND_CHILL, "mem-15" );
		addDouble( event, builder, WeatherDatumIdentifier.HEAT_INDEX, "mem-16" );

		// Wind data
		addDouble( event, builder, WeatherDatumIdentifier.WIND_SPEED, "mem-20" );
		addDouble( event, builder, WeatherDatumIdentifier.WIND_DIRECTION, "mem-21" );

		// Wind 2-min averaged
		addDouble( event, builder, WeatherDatumIdentifier.WIND_SPEED_2_MIN_AVG, "mem-30" );
		addDouble( event, builder, WeatherDatumIdentifier.WIND_DIRECTION_2_MIN_AVG, "mem-31" );
		addDouble( event, builder, WeatherDatumIdentifier.WIND_SPEED_2_MIN_MAX, "mem-32" );
		addDouble( event, builder, WeatherDatumIdentifier.WIND_SPEED_2_MIN_MIN, "mem-33" );

		// Wind 10-min averaged
		addDouble( event, builder, WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG, "mem-40" );
		addDouble( event, builder, WeatherDatumIdentifier.WIND_DIRECTION_10_MIN_AVG, "mem-41" );
		addDouble( event, builder, WeatherDatumIdentifier.WIND_SPEED_10_MIN_MAX, "mem-42" );
		addDouble( event, builder, WeatherDatumIdentifier.WIND_SPEED_10_MIN_MIN, "mem-43" );

		addDouble( event, builder, WeatherDatumIdentifier.RAIN_RATE, "mem-50" );
		addDouble( event, builder, WeatherDatumIdentifier.RAIN_TOTAL_DAILY, "mem-51" );

		addDouble( event, builder, WeatherDatumIdentifier.TEMPERATURE_TREND, "mem-110" );
		addDouble( event, builder, WeatherDatumIdentifier.PRESSURE_TREND, "mem-111" );
		addDouble( event, builder, WeatherDatumIdentifier.HUMIDITY_TREND, "mem-112" );
		addDouble( event, builder, WeatherDatumIdentifier.WIND_SPEED_TREND, "mem-120" );
		//addDouble( event, builder, WeatherDatumIdentifier.WIND_DIRECTION_TREND, "mem-121" );

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
