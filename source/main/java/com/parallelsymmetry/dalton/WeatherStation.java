package com.parallelsymmetry.dalton;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.measure.Measure;

import com.parallelsymmetry.utility.log.Log;

public class WeatherStation implements WeatherDataListener {

	//	Log.write( "Bar: ", TextUtil.toPrintableString( buffer, 7, 2 ), " ", bar );
	//	Log.write( "Bar trend: ", TextUtil.toPrintableString( buffer, 3, 1 ), " ", barTrend.name() );
	//	Log.write( "Temp in: ", TextUtil.toPrintableString( buffer, 9, 2 ), " ", tempInside );
	//	Log.write( "Humid in: ", TextUtil.toPrintableString( buffer, 1, 1 ), " ", humidInside + "%" );
	//	Log.write( "Temp out: ", TextUtil.toPrintableString( buffer, 12, 2 ), " ", tempOutside );
	//	Log.write( "Wind speed: ", TextUtil.toPrintableString( buffer, 14, 1 ), " ", windSpeed );
	//	Log.write( "Wind speed 10 min. avg.: ", TextUtil.toPrintableString( buffer, 15, 1 ), " ", windSpeedTenMinAvg );
	//	Log.write( "Wind direction: ", TextUtil.toPrintableString( buffer, 16, 2 ), " ", windDirection );
	//
	//	Log.write( "Humid out: ", TextUtil.toPrintableString( buffer, 33, 1 ), " ", humidOutside );
	//	Log.write( "Rain rate: ", TextUtil.toPrintableString( buffer, 41, 2 ), " ", rainRate );

	private Map<WeatherDatumIdentifier, Measure<?, ?>> data;

	public WeatherStation() {
		data = new ConcurrentHashMap<WeatherDatumIdentifier, Measure<?, ?>>();
	}

	public Measure<?, ?> getMeasure( WeatherDatumIdentifier datum ) {
		return data.get( datum );
	}

	public float getTemperature() {
		Measure<?, ?> measure = getMeasure( WeatherDatumIdentifier.TEMPERATURE );
		if( measure == null ) return Float.NaN;
		return ( (Float)measure.getValue() ).floatValue();
	}

	@Override
	public void weatherDataEvent( WeatherDataEvent event ) {
		for( WeatherDatum datum : event.getData() ) {
			data.put( datum.getIdentifier(), datum.getMeasure() );
		}

		//http://emerald:8080/weather/station?id=21&ts=2348923&t=42.1&h=57&p=29.92
		StringBuilder builder = new StringBuilder( "http://emerald:8080/weather/wxstation?id=0" );

		builder.append( "&ts=" );
		builder.append( System.currentTimeMillis() );

		builder.append( "&t=" );
		builder.append( data.get( WeatherDatumIdentifier.TEMPERATURE ).getValue() );
		builder.append( "&h=" );
		builder.append( data.get( WeatherDatumIdentifier.HUMIDITY ).getValue() );
		builder.append( "&p=" );
		builder.append( data.get( WeatherDatumIdentifier.PRESSURE ).getValue() );

		builder.append( "&wd=" );
		builder.append( data.get( WeatherDatumIdentifier.WIND_DIRECTION ).getValue() );
		builder.append( "&wi=" );
		builder.append( data.get( WeatherDatumIdentifier.WIND_SPEED_INSTANT ).getValue() );
		builder.append( "&ws=" );
		builder.append( data.get( WeatherDatumIdentifier.WIND_SPEED_SUSTAIN ).getValue() );

		builder.append( "&rr=" );
		builder.append( data.get( WeatherDatumIdentifier.RAIN_RATE ).getValue() );
		builder.append( "&rd=" );
		builder.append( data.get( WeatherDatumIdentifier.RAIN_TOTAL_DAILY ).getValue() );

		Log.write( Log.TRACE, builder.toString() );
		try {
			sendGet( builder.toString() );
		} catch( Exception exception ) {
			Log.write( exception );
		}
	}

	private int sendGet( String url ) throws Exception {
		String USER_AGENT = "Mozilla/5.0";

		HttpURLConnection connection = (HttpURLConnection)new URL( url ).openConnection();
		connection.setRequestMethod( "GET" );
		connection.setRequestProperty( "User-Agent", USER_AGENT );

		// Get the response code.
		int responseCode = connection.getResponseCode();

		// Read the response.
		String inputLine;
		StringBuffer response = new StringBuffer();
		BufferedReader input = new BufferedReader( new InputStreamReader( connection.getInputStream() ) );
		while( ( inputLine = input.readLine() ) != null ) {
			response.append( inputLine );
		}
		input.close();

		return responseCode;
	}

}
