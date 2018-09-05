package com.parallelsymmetry.dalton;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WeatherDataEvent {

	public enum Type {
		READ
	}

	private Type type;

	private Date timestamp;

	private Collection<WeatherDatum> data;

	private Map<WeatherDatumIdentifier, WeatherDatum> map;

	public WeatherDataEvent( WeatherDatum... data ) {
		this( Type.READ, data );
	}

	public WeatherDataEvent( Type type, WeatherDatum... data ) {
		this( type, new Date(), data );
	}

	public WeatherDataEvent( Type type, Date timestamp, WeatherDatum... data ) {
		this.type = type;
		this.timestamp = timestamp;
		this.data = Arrays.asList( data );
		this.map = new ConcurrentHashMap<>();
		for( WeatherDatum datum : data ) {
			this.map.put( datum.getIdentifier(), datum );
		}
	}

	public Type getType() {
		return type;
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
