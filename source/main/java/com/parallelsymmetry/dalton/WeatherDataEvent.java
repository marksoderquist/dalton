package com.parallelsymmetry.dalton;

import javax.measure.DecimalMeasure;
import javax.measure.unit.SI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WeatherDataEvent {

	private Date timestamp;

	private Collection<WeatherDatum> data;

	private Map<WeatherDatumIdentifier, WeatherDatum> map;

	public WeatherDataEvent( WeatherDatum... data ) {
		this( System.currentTimeMillis(), data );
	}

	public WeatherDataEvent( long timestamp, WeatherDatum... data ) {
		this.map = new ConcurrentHashMap<>();
		this.data = new ArrayList<>();
		map.put( WeatherDatumIdentifier.TIMESTAMP, new WeatherDatum( WeatherDatumIdentifier.TIMESTAMP, DecimalMeasure.valueOf( timestamp, SI.MILLI( SI.SECOND ) ) ) );
		for( WeatherDatum datum : data ) {
			if( datum == null ) continue;
			this.map.put( datum.getIdentifier(), datum );
			this.data.add( datum );
		}
	}

	public long getTimestamp() {
		return (Long)map.get( WeatherDatumIdentifier.TIMESTAMP ).getMeasure().getValue();
	}

	public WeatherDatum get( WeatherDatumIdentifier identifier ) {
		return map.get( identifier );
	}

	public Collection<WeatherDatum> getData() {
		return data;
	}

}
