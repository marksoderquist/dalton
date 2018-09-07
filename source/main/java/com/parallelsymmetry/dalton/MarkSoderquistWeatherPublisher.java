package com.parallelsymmetry.dalton;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MarkSoderquistWeatherPublisher extends HttpPublisher {

	@Override
	public int publish( WeatherStation station, WeatherDataEvent event ) throws IOException {
		Map<String, String> headers = new HashMap<>();
		headers.put( "content-type", "application/json" );
		headers.put( "Authorization", "Basic ZGFsdG9uOkRvNUpwTW84ejVoU3hVaTQ=" );
		return rest( "PUT", "http://mark.soderquist.net/weather/api/station?id=bluewing", headers, generatePayload( event ) ).getCode();
	}

	public byte[] generatePayload( WeatherDataEvent event ) throws IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();

		JsonGenerator generator = new JsonFactory().createGenerator( stream );
		generator.writeStartObject();

		writeLongField( event, generator, "timestamp", WeatherDatumIdentifier.TIMESTAMP );
		writeDoubleField( event, generator, "temperature", WeatherDatumIdentifier.TEMPERATURE );
		writeDoubleField( event, generator, "pressure", WeatherDatumIdentifier.PRESSURE );
		writeDoubleField( event, generator, "humidity", WeatherDatumIdentifier.HUMIDITY );

		writeDoubleField( event, generator, "dewPoint", WeatherDatumIdentifier.DEW_POINT );
		writeDoubleField( event, generator, "windChill", WeatherDatumIdentifier.WIND_CHILL );
		writeDoubleField( event, generator, "heatIndex", WeatherDatumIdentifier.HEAT_INDEX );

		writeDoubleField( event, generator, "wind", WeatherDatumIdentifier.WIND_SPEED );
		writeDoubleField( event, generator, "windSpeed", WeatherDatumIdentifier.WIND_SPEED );
		writeDoubleField( event, generator, "windDirection", WeatherDatumIdentifier.WIND_DIRECTION );

		writeDoubleField( event, generator, "rainTotalDaily", WeatherDatumIdentifier.RAIN_TOTAL_DAILY );
		writeDoubleField( event, generator, "rainRate", WeatherDatumIdentifier.RAIN_RATE );

		writeDoubleField( event, generator, "temperatureTrend", WeatherDatumIdentifier.TEMPERATURE_TREND );
		writeDoubleField( event, generator, "humidityTrend", WeatherDatumIdentifier.HUMIDITY_TREND );
		writeDoubleField( event, generator, "pressureTrend", WeatherDatumIdentifier.PRESSURE_TREND );
		writeDoubleField( event, generator, "windSpeedTrend", WeatherDatumIdentifier.WIND_SPEED_TREND );

		writeDoubleField( event, generator, "windTenMinMax", WeatherDatumIdentifier.WIND_SPEED_10_MIN_MAX );
		writeDoubleField( event, generator, "windTenMinAvg", WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG );
		writeDoubleField( event, generator, "windTenMinMin", WeatherDatumIdentifier.WIND_SPEED_10_MIN_MIN );

		writeDoubleField( event, generator, "windTwoMinMax", WeatherDatumIdentifier.WIND_SPEED_2_MIN_MAX );
		writeDoubleField( event, generator, "windTwoMinAvg", WeatherDatumIdentifier.WIND_SPEED_2_MIN_AVG );
		writeDoubleField( event, generator, "windTwoMinMin", WeatherDatumIdentifier.WIND_SPEED_2_MIN_MIN );

		writeDoubleField( event, generator, "windDirectionTenMinAvg", WeatherDatumIdentifier.WIND_DIRECTION_10_MIN_AVG );
		writeDoubleField( event, generator, "windDirectionTwoMinAvg", WeatherDatumIdentifier.WIND_DIRECTION_2_MIN_AVG );

		generator.writeEndObject();
		generator.close();

		return stream.toByteArray();
	}

	private void writeLongField( WeatherDataEvent event, JsonGenerator generator, String name, WeatherDatumIdentifier identifier ) throws IOException {
		Long value = event.getValue( identifier );
		if( value == null ) return;
		generator.writeNumberField( name, value );
	}

	private void writeDoubleField( WeatherDataEvent event, JsonGenerator generator, String name, WeatherDatumIdentifier identifier ) throws IOException {
		Double value = event.getValue( identifier );
		if( value == null ) return;
		generator.writeNumberField( name, value );
	}

}
