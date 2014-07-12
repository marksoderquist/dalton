package com.parallelsymmetry.dalton;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

public class WeatherDataEvent {

	public enum Type {
		READ
	}

	private Type type;

	private Date timestamp;

	private Collection<WeatherDatum> data;

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

}
