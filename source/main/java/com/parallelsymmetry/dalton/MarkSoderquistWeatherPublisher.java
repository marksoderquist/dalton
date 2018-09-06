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
	public int publish( WeatherStation station, Map<WeatherDatumIdentifier, Measure<? extends Number, ? extends Quantity>> data ) throws IOException {
		Map<String, String> headers = new HashMap<>();
		headers.put( "content-type", "application/json" );
		headers.put( "Authorization", "Basic ZGFsdG9uOkRvNUpwTW84ejVoU3hVaTQ=" );
		return rest( "PUT", "http://mark.soderquist.net/weather/api/station?id=bluewing", headers, generatePayload( data ) ).getCode();
	}

	public byte[] generatePayload( Map<WeatherDatumIdentifier, Measure<? extends Number, ? extends Quantity>> data ) throws IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();

		JsonGenerator generator = new JsonFactory().createGenerator( stream );
		generator.writeStartObject();
		writeLongField(data, generator, "timestamp", WeatherDatumIdentifier.TIMESTAMP );
		writeDoubleField(data, generator,"temperature",  WeatherDatumIdentifier.TEMPERATURE );
		writeDoubleField(data, generator,"pressure",  WeatherDatumIdentifier.PRESSURE );
		writeDoubleField(data, generator,"humidity",  WeatherDatumIdentifier.HUMIDITY );

		writeDoubleField(data, generator,"dewPoint",  WeatherDatumIdentifier.DEW_POINT );
		writeDoubleField(data, generator,"windChill",  WeatherDatumIdentifier.WIND_CHILL );
		writeDoubleField(data, generator,"heatIndex",  WeatherDatumIdentifier.HEAT_INDEX );
		writeDoubleField(data, generator,"pressureTrend",  WeatherDatumIdentifier.PRESSURE_TREND );

		writeDoubleField(data, generator,"windDirection",  WeatherDatumIdentifier.WIND_DIRECTION );
		writeDoubleField(data, generator,"wind",  WeatherDatumIdentifier.WIND_SPEED_CURRENT );

		writeDoubleField(data, generator,"windTenMinMax",  WeatherDatumIdentifier.WIND_SPEED_10_MIN_MAX );
		writeDoubleField(data, generator,"windTenMinAvg",  WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG );
		writeDoubleField(data, generator,"windTenMinMin",  WeatherDatumIdentifier.WIND_SPEED_10_MIN_MIN );

		writeDoubleField(data, generator,"windTwoMinMax",  WeatherDatumIdentifier.WIND_SPEED_2_MIN_MAX );
		writeDoubleField(data, generator,"windTwoMinAvg",  WeatherDatumIdentifier.WIND_SPEED_2_MIN_AVG );
		writeDoubleField(data, generator,"windTwoMinMin",  WeatherDatumIdentifier.WIND_SPEED_2_MIN_MIN );

		writeDoubleField(data, generator,"rainTotalDaily",  WeatherDatumIdentifier.RAIN_TOTAL_DAILY );
		writeDoubleField(data, generator,"rainRate",  WeatherDatumIdentifier.RAIN_RATE );

		generator.writeEndObject();
		generator.close();

		return stream.toByteArray();
	}

	private void writeLongField( Map<WeatherDatumIdentifier, Measure<? extends Number, ? extends Quantity>> data, JsonGenerator generator, String name, WeatherDatumIdentifier identifier ) throws IOException {
		Measure<? extends Number, ? extends Quantity> measure = data.get( identifier );
		if( measure == null ) return;
		generator.writeNumberField( name, (Long)measure.getValue() );
	}

	private void writeDoubleField( Map<WeatherDatumIdentifier, Measure<? extends Number, ? extends Quantity>> data, JsonGenerator generator, String name, WeatherDatumIdentifier identifier ) throws IOException {
		Measure<? extends Number, ? extends Quantity> measure = data.get( identifier );
		if( measure == null ) return;
		generator.writeNumberField( name, (Double)measure.getValue() );
	}

}
