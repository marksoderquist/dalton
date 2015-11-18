package com.parallelsymmetry.dalton;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.measure.DecimalMeasure;
import javax.measure.unit.NonSI;

import com.parallelsymmetry.utility.EnumerationIterator;
import com.parallelsymmetry.utility.IoPump;
import com.parallelsymmetry.utility.TextUtil;
import com.parallelsymmetry.utility.ThreadUtil;
import com.parallelsymmetry.utility.agent.Worker;
import com.parallelsymmetry.utility.comm.SerialAgent;
import com.parallelsymmetry.utility.log.Log;

/**
 * Davis weather station reader. This reader is derived from the <a href=
 * "http://www.davisnet.com/support/weather/download/VantageSerialProtocolDocs_v261.pdf"
 * >Davis weather station serial protocol</a>.
 * <p>
 * This reader uses RXTX as the serial library.In order to use RXTX there are
 * three things that need to be configured:
 * <ol>
 * <li>The RXTX shared library must be found on the java.library.path</li>
 * <li>The RXTX java library must be found on the java class path</li>
 * <li>The user running the program must have read access to the ports</li>
 * </ol>
 * On Debian and its derivatives the user should be assigned to the dialout
 * group to have read access to the ports.
 * 
 * @author mvsoder
 */
public class DavisReader extends Worker {

	private boolean useConsole = false;

	private String port = "/dev/ttyUSB0";

	private Collection<WeatherDataListener> listeners;

	private int pollInterval = 2500;

	public DavisReader() {
		setInterruptOnStop( true );
		listeners = new CopyOnWriteArraySet<WeatherDataListener>();
	}

	public int getPollInterval() {
		return pollInterval;
	}

	public void setPollInterval( int pollInterval ) {
		this.pollInterval = pollInterval;
	}

	@Override
	public void run() {
		listPortIdentifiers();

		IoPump reader = null;
		IoPump console = null;

		try {
			SerialAgent agent = new SerialAgent( "Davis Reader", port, 19200, SerialPort.DATABITS_8, SerialPort.PARITY_NONE, SerialPort.STOPBITS_1 );
			agent.setReconnectDelay( 5000 );
			agent.setStopOnException( true );
			agent.start();

			Log.write( Log.INFO, agent.getName(), " running on port ", port );

			if( useConsole ) {
				reader = new IoPump( agent.getInputStream(), new WatcherOutputStream( System.out ) );
				reader.start();

				console = new IoPump( System.in, agent.getOutputStream() );
				console.start();
			} else {
				while( isExecutable() ) {
					getData( agent );
					ThreadUtil.pause( pollInterval );
				}
			}
		} catch( Exception exception ) {
			exception.printStackTrace( System.err );
		} finally {
			if( reader != null ) reader.stop();
			if( console != null ) console.stop();
		}
	}

	public void addWeatherDataListener( WeatherDataListener listener ) {
		listeners.add( listener );
	}

	public void removeWeatherDataListener( WeatherDataListener listener ) {
		listeners.remove( listener );
	}

	@SuppressWarnings( "unchecked" )
	private void listPortIdentifiers() {
		for( CommPortIdentifier identifier : new EnumerationIterator<CommPortIdentifier>( CommPortIdentifier.getPortIdentifiers() ) ) {
			Log.write( "Port: ", identifier.getName() );
		}
	}

	// Does not work to detect if receiver is disconnected.
	private boolean isReceiverConnected( SerialAgent agent ) throws IOException {
		PrintStream output = new PrintStream( agent.getOutputStream() );
		output.println( "RECEIVERS" );

		byte[] buffer = new byte[8];
		BufferedInputStream input = new BufferedInputStream( agent.getInputStream() );

		int read = input.read( buffer );
		int count = read;
		while( count < 7 ) {
			read = input.read( buffer );
			count += read;
		}

		boolean connected = ( buffer[6] & 1 ) != 0;

		Log.write();
		Log.write( "Read: ", TextUtil.toPrintableString( buffer, 0, 7 ) );
		Log.write( "Connected: ", connected );

		return connected;
	}

	private void clearData( SerialAgent agent ) throws IOException {
		PrintStream output = new PrintStream( agent.getOutputStream() );
		output.println( "CLRDATA" );

		byte[] buffer = new byte[8];
		BufferedInputStream input = new BufferedInputStream( agent.getInputStream() );

		int read = input.read( buffer );
		int count = read;
		while( count < 1 ) {
			read = input.read( buffer );
			count += read;
		}
	}

	private void getData( SerialAgent agent ) throws IOException {
		//boolean connected = isReceiverConnected( agent );
		//clearData( agent );

		PrintStream output = new PrintStream( agent.getOutputStream() );
		output.println( "LOOP 1" );

		byte[] buffer = new byte[128];
		BufferedInputStream input = new BufferedInputStream( agent.getInputStream() );

		int read = input.read( buffer );
		int count = read;
		while( count < 100 ) {
			read = input.read( buffer );
			count += read;
		}

		System.arraycopy( buffer, 1, buffer, 0, 99 );

		// Barometer trend
		BarometerTrend pressureTrend = parseBarometerTrend( buffer[3] );

		// Barometer
		int barRaw = getUnsignedByte( buffer[7] ) + ( getUnsignedByte( buffer[8] ) << 8 );
		float pressure = barRaw == 0x7fff ? Float.NaN : barRaw / 1000.0f;

		// Inside temperature
		int tempInsideRaw = getUnsignedByte( buffer[9] ) + ( getUnsignedByte( buffer[10] ) << 8 );
		float tempInside = tempInsideRaw == 0x7fff ? Float.NaN : tempInsideRaw / 10.0f;

		// Inside humidity
		int humidInsideRaw = getUnsignedByte( buffer[11] );
		float humidInside = humidInsideRaw == 0xff ? Float.NaN : humidInsideRaw;

		// Outside temperature
		int tempOutsideRaw = getUnsignedByte( buffer[12] ) + ( ( buffer[13] ) << 8 );
		float tempOutside = tempOutsideRaw == 0x7fff ? Float.NaN : tempOutsideRaw / 10.0f;

		// Wind speed
		int windSpeedRaw = getUnsignedByte( buffer[14] );
		float windSpeed = windSpeedRaw == 0xff ? Float.NaN : windSpeedRaw;

		// Wind speed 10 minute average
		int windSpeedTenMinAvgRaw = getUnsignedByte( buffer[15] );
		float windSpeedTenMinAvg = windSpeedTenMinAvgRaw == 0xff ? Float.NaN : windSpeedTenMinAvgRaw;

		// Wind direction
		int windDirectionRaw = getUnsignedByte( buffer[16] ) + ( getUnsignedByte( buffer[17] ) << 8 );
		float windDirection = windDirectionRaw == 0x7fff ? Float.NaN : windDirectionRaw;

		// Outside humidity
		int humidOutsideRaw = getUnsignedByte( buffer[33] );
		float humidOutside = humidOutsideRaw == 0xff ? Float.NaN : humidOutsideRaw;

		// Rain rate
		int rainRateRaw = getUnsignedByte( buffer[41] ) + ( getUnsignedByte( buffer[42] ) << 8 );
		float rainRate = rainRateRaw == 0xffff ? Float.NaN : rainRateRaw / 100.0f;

		// Daily rain total
		int rainTotalDailyRaw = getUnsignedByte( buffer[50] ) + ( getUnsignedByte( buffer[51] ) << 8 );
		float rainTotalDaily = rainTotalDailyRaw == 0x7fff ? Float.NaN : rainTotalDailyRaw / 100.0f;

		//		Log.write();
		//		Log.write( "Read: ", TextUtil.toPrintableString( buffer, 0, 99 ) );
		//		Log.write( "Pressure: ", TextUtil.toPrintableString( buffer, 7, 2 ), " ", pressure );
		//		Log.write( "Pressure trend: ", TextUtil.toPrintableString( buffer, 3, 1 ), " ", pressureTrend.name() );
		//		Log.write( "Temp in: ", TextUtil.toPrintableString( buffer, 9, 2 ), " ", tempInside );
		//		Log.write( "Humid in: ", TextUtil.toPrintableString( buffer, 1, 1 ), " ", humidInside + "%" );
		//		Log.write( "Temp out: ", TextUtil.toPrintableString( buffer, 12, 2 ), " ", tempOutside );
		//		Log.write( "Wind speed: ", TextUtil.toPrintableString( buffer, 14, 1 ), " ", windSpeed );
		//		Log.write( "Wind speed 10 min. avg.: ", TextUtil.toPrintableString( buffer, 15, 1 ), " ", windSpeedTenMinAvg );
		//		Log.write( "Wind direction: ", TextUtil.toPrintableString( buffer, 16, 2 ), " ", windDirection );
		//
		//		Log.write( "Humid out: ", TextUtil.toPrintableString( buffer, 33, 1 ), " ", humidOutside );
		//		Log.write( "Rain rate: ", TextUtil.toPrintableString( buffer, 41, 2 ), " ", rainRate );
		//		Log.write( "Rain total daily: ", TextUtil.toPrintableString( buffer, 50, 2 ), " ", rainTotalDaily );

		WeatherDatum temperatureDatum = new WeatherDatum( WeatherDatumIdentifier.TEMPERATURE, DecimalMeasure.valueOf( tempOutside, NonSI.FAHRENHEIT ) );
		WeatherDatum pressureDatum = new WeatherDatum( WeatherDatumIdentifier.PRESSURE, DecimalMeasure.valueOf( pressure, NonSI.INCH_OF_MERCURY ) );
		WeatherDatum humidityDatum = new WeatherDatum( WeatherDatumIdentifier.HUMIDITY, DecimalMeasure.valueOf( humidOutside, NonSI.PERCENT ) );

		WeatherDatum windSpeedDatum = new WeatherDatum( WeatherDatumIdentifier.WIND_SPEED_INSTANT, DecimalMeasure.valueOf( windSpeed, NonSI.MILES_PER_HOUR ) );
		WeatherDatum windDirectionDatum = new WeatherDatum( WeatherDatumIdentifier.WIND_DIRECTION, DecimalMeasure.valueOf( windDirection, NonSI.DEGREE_ANGLE ) );
		WeatherDatum windSpeedTenMinAvgDatum = new WeatherDatum( WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG, DecimalMeasure.valueOf( windSpeedTenMinAvg, NonSI.MILES_PER_HOUR ) );

		WeatherDatum rainRateDatum = new WeatherDatum( WeatherDatumIdentifier.RAIN_RATE, DecimalMeasure.valueOf( rainRate, NonSI.INCH.divide( NonSI.HOUR ) ) );
		WeatherDatum rainTotalDailyDatum = new WeatherDatum( WeatherDatumIdentifier.RAIN_TOTAL_DAILY, DecimalMeasure.valueOf( rainTotalDaily, NonSI.INCH ) );

		WeatherDatum temperatureInsideDatum = new WeatherDatum( WeatherDatumIdentifier.TEMPERATURE_INSIDE, DecimalMeasure.valueOf( tempInside, NonSI.FAHRENHEIT ) );
		WeatherDatum humidityInsideDatum = new WeatherDatum( WeatherDatumIdentifier.HUMIDITY_INSIDE, DecimalMeasure.valueOf( humidInside, NonSI.PERCENT ) );

		fireWeatherEvent( new WeatherDataEvent( temperatureDatum, pressureDatum, humidityDatum, windSpeedDatum, windDirectionDatum, rainRateDatum, rainTotalDailyDatum, temperatureInsideDatum, humidityInsideDatum, windSpeedTenMinAvgDatum ) );
	}

	private void fireWeatherEvent( WeatherDataEvent event ) {
		for( WeatherDataListener listener : listeners ) {
			listener.weatherDataEvent( event );
		}
	}

	private int getUnsignedByte( byte value ) {
		int result = (int)value;
		if( result < 0 ) result += 256;
		return result;
	}

	private BarometerTrend parseBarometerTrend( byte data ) {
		switch( data ) {
			case -60: {
				return BarometerTrend.FALLING_FAST;
			}
			case -20: {
				return BarometerTrend.FALLING_SLOW;
			}
			case 0: {
				return BarometerTrend.STEADY;
			}
			case 20: {
				return BarometerTrend.RISING_SLOW;
			}
			case 60: {
				return BarometerTrend.RISING_FAST;
			}
		}
		return BarometerTrend.UNKNOWN;
	}

	private class WatcherOutputStream extends OutputStream {

		private PrintStream output;

		public WatcherOutputStream( OutputStream output ) {
			this.output = new PrintStream( output );
		}

		@Override
		public void write( int data ) throws IOException {
			output.print( TextUtil.toPrintableString( (byte)data ) );
		}

	}

}
