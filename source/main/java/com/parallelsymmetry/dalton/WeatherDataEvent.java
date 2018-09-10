package com.parallelsymmetry.dalton;

import javax.measure.DecimalMeasure;
import javax.measure.unit.SI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WeatherDataEvent {

	private Map<WeatherDatumIdentifier, WeatherDatum> map;

	public WeatherDataEvent( WeatherDatum... data ) {
		this.map = new ConcurrentHashMap<>();
		add( new WeatherDatum( WeatherDatumIdentifier.TIMESTAMP, DecimalMeasure.valueOf( System.currentTimeMillis(), SI.MILLI( SI.SECOND ) ) ) );
		add( data );
	}

	public void add( WeatherDatum... data ) {
		for( WeatherDatum datum : data ) {
			if( datum == null ) continue;
			this.map.put( datum.getIdentifier(), datum );
		}
	}

	public long getTimestamp() {
		return (Long)map.get( WeatherDatumIdentifier.TIMESTAMP ).getMeasure().getValue();
	}

	public WeatherDatum get( WeatherDatumIdentifier identifier ) {
		return map.get( identifier );
	}

	@SuppressWarnings( "unchecked" )
	public <V> V getValue( WeatherDatumIdentifier identifier ) {
		WeatherDatum datum = map.get( identifier );
		if( datum == null ) return null;
		return (V)datum.getMeasure().getValue();
	}

}
