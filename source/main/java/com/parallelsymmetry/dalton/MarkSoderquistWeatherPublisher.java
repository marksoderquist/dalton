package com.parallelsymmetry.dalton;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import javax.measure.Measure;
import javax.measure.quantity.Quantity;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MarkSoderquistWeatherPublisher extends HttpPublisher {

	@Override
	public int publish( Map<WeatherDatumIdentifier, Measure<? extends Number, ? extends Quantity>> data ) throws IOException {
		Map<String, String> headers = new HashMap<>();
		headers.put( "content-type", "application/json" );
		headers.put( "Authorization", "Basic ZGFsdG9uOkRvNUpwTW84ejVoU3hVaTQ=" );
		return rest( "PUT", "http://mark.soderquist.net/weather/api/station?id=bluewing", headers, generatePayload( data ) ).getCode();
	}

	public byte[] generatePayload( Map<WeatherDatumIdentifier, Measure<? extends Number, ? extends Quantity>> data ) throws IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();

		JsonGenerator generator = new JsonFactory().createGenerator( stream );
		generator.writeStartObject();
		generator.writeNumberField( "timestamp", System.currentTimeMillis() );
		generator.writeNumberField( "temperature", (Float)data.get( WeatherDatumIdentifier.TEMPERATURE ).getValue() );
		generator.writeNumberField( "pressure", (Float)data.get( WeatherDatumIdentifier.PRESSURE ).getValue() );
		generator.writeNumberField( "humidity", (Float)data.get( WeatherDatumIdentifier.HUMIDITY ).getValue() );

		generator.writeNumberField( "dewPoint", (Float)data.get( WeatherDatumIdentifier.DEW_POINT ).getValue() );
		generator.writeNumberField( "windChill", (Float)data.get( WeatherDatumIdentifier.WIND_CHILL ).getValue() );
		generator.writeNumberField( "heatIndex", (Float)data.get( WeatherDatumIdentifier.HEAT_INDEX ).getValue() );
		generator.writeNumberField( "pressureTrend", (Float)data.get( WeatherDatumIdentifier.PRESSURE_TREND ).getValue() );

		generator.writeNumberField( "windDirection", (Float)data.get( WeatherDatumIdentifier.WIND_DIRECTION ).getValue() );
		generator.writeNumberField( "wind", (Float)data.get( WeatherDatumIdentifier.WIND_SPEED_CURRENT ).getValue() );

		generator.writeNumberField( "windTenMinMax", (Float)data.get( WeatherDatumIdentifier.WIND_SPEED_10_MIN_MAX ).getValue() );
		generator.writeNumberField( "windTenMinAvg", (Float)data.get( WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG ).getValue() );
		generator.writeNumberField( "windTenMinMin", (Float)data.get( WeatherDatumIdentifier.WIND_SPEED_10_MIN_MIN ).getValue() );

		generator.writeNumberField( "windTwoMinMax", (Float)data.get( WeatherDatumIdentifier.WIND_SPEED_2_MIN_MAX ).getValue() );
		generator.writeNumberField( "windTwoMinAvg", (Float)data.get( WeatherDatumIdentifier.WIND_SPEED_2_MIN_AVG ).getValue() );
		generator.writeNumberField( "windTwoMinMin", (Float)data.get( WeatherDatumIdentifier.WIND_SPEED_2_MIN_MIN ).getValue() );

		generator.writeNumberField( "rainTotalDaily", (Float)data.get( WeatherDatumIdentifier.RAIN_TOTAL_DAILY ).getValue() );
		generator.writeNumberField( "rainRate", (Float)data.get( WeatherDatumIdentifier.RAIN_RATE ).getValue() );

		generator.writeEndObject();
		generator.close();

		return stream.toByteArray();
	}

}
