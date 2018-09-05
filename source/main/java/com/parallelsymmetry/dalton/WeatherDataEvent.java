package com.parallelsymmetry.dalton;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WeatherDataEvent {

	private Date timestamp;

	private Collection<WeatherDatum> data;

	private Map<WeatherDatumIdentifier, WeatherDatum> map;

	public WeatherDataEvent( WeatherDatum... data ) {
		this( new Date(), data );
	}

	public WeatherDataEvent( Date timestamp, WeatherDatum... data ) {
		this.timestamp = timestamp;
		this.data = Arrays.asList( data );
		this.map = new ConcurrentHashMap<>();
		for( WeatherDatum datum : data ) {
			this.map.put( datum.getIdentifier(), datum );
		}
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public Collection<WeatherDatum> getData() {
		return data;
	}

	public WeatherDatum get( WeatherDatumIdentifier identifier ) {
		return map.get( identifier );
	}

}
