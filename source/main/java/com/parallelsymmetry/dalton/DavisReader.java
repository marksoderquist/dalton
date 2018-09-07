package com.parallelsymmetry.dalton;

import com.parallelsymmetry.utility.EnumerationIterator;
import com.parallelsymmetry.utility.TextUtil;
import com.parallelsymmetry.utility.ThreadUtil;
import com.parallelsymmetry.utility.agent.Worker;
import com.parallelsymmetry.utility.log.Log;
import purejavacomm.CommPortIdentifier;
import purejavacomm.SerialPort;

import javax.measure.DecimalMeasure;
import javax.measure.unit.NonSI;
import java.io.*;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeoutException;

/**
 * Davis weather station reader. This reader is derived from the <a href= "http://www.davisnet.com/support/weather/download/VantageSerialProtocolDocs_v261.pdf" >Davis weather station serial protocol</a>.
 * <p>
 * This reader uses purejavacomm as the serial library. In order to use purejavacomm, the user
 * running the program must have read access to the ports. On Debian and its derivatives the
 * user should be assigned to the dialout group to have read access to the ports.
 *
 * @author mvsoder
 */
public class DavisReader extends Worker implements WeatherDataReader {

	private static final int POLL_INTERVAL = 1500;

	private static final int READ_INTERVAL = 500;

	private boolean useConsole = false;

	private String port = "/dev/ttyUSB0";

	private Collection<WeatherStation> stations;

	private Thread pollingThread;

	private long lastPoll;

	private Timer timer;

	private SerialPort serialPort;

	private static final long DEAD_MAN_LIMIT = 10000;

	public DavisReader() {
		timer = new Timer( true );
		setInterruptOnStop( true );
		stations = new CopyOnWriteArraySet<>();
	}

	@Override
	public void run() {
		while( isExecutable() ) {
			try {
				pollingThread = Thread.currentThread();

				// Open the serial port and read from it
				Log.write( Log.DEBUG, "Open the serial port: " + port );
				CommPortIdentifier identifier = CommPortIdentifier.getPortIdentifier( port );
				serialPort = (SerialPort)identifier.open( getName(), 0 );
				serialPort.setSerialPortParams( 19200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE );

				getData( serialPort.getOutputStream(), serialPort.getInputStream() );

				lastPoll = System.currentTimeMillis();
			} catch( Throwable throwable ) {
				Log.write( Log.ERROR, throwable );
			} finally {
				if( serialPort != null ) {
					Log.write( Log.DEBUG, "Close the serial port: " + serialPort.getName() );
					serialPort.close();
				}
				ThreadUtil.pause( POLL_INTERVAL );
			}
		}
	}

//	public void runX() {
//		pollingThread = Thread.currentThread();
//		listPortIdentifiers();
//
//		IoPump reader = null;
//		IoPump console = null;
//
//		try {
//			SerialAgent agent = new SerialAgent( "Davis Reader", port, 19200, SerialPort.DATABITS_8, SerialPort.PARITY_NONE, SerialPort.STOPBITS_1 );
//			agent.setReconnectDelay( 5000 );
//			agent.setStopOnException( true );
//			agent.start();
//
//			Log.write( Log.INFO, agent.getName(), " running on port ", port );
//
//			if( useConsole ) {
//				reader = new IoPump( agent.getInputStream(), new WatcherOutputStream( System.out ) );
//				reader.start();
//
//				console = new IoPump( System.in, agent.getOutputStream() );
//				console.start();
//			} else {
//				while( isExecutable() ) {
//					try {
//						agent.start();
//						getData( agent );
//						lastPoll = System.currentTimeMillis();
//					} catch( TimeoutException exception ) {
//						Log.write( exception );
//					} finally {
//						agent.stop();
//					}
//					ThreadUtil.pause( pollInterval );
//				}
//			}
//		} catch( Throwable throwable ) {
//			Log.write( throwable );
//		} finally {
//			if( reader != null ) reader.stop();
//			if( console != null ) console.stop();
//			Log.write( "David Reader exiting..." );
//		}
//	}

	@Override
	public void addWeatherStation( WeatherStation station ) {
		stations.add( station );
	}

	@Override
	public void removeWeatherStation( WeatherStation station ) {
		stations.remove( station );
	}

	@Override
	protected void startWorker() throws Exception {
		timer.scheduleAtFixedRate( new DeadManSwitch(), DEAD_MAN_LIMIT, DEAD_MAN_LIMIT );
	}

	@SuppressWarnings( "unchecked" )
	private void listPortIdentifiers() {
		for( CommPortIdentifier identifier : new EnumerationIterator<>( CommPortIdentifier.getPortIdentifiers() ) ) {
			Log.write( "Port: ", identifier.getName() );
		}
	}

//	private void clearData( SerialAgent agent ) throws IOException {
//		PrintStream output = new PrintStream( agent.getOutputStream() );
//		output.println( "CLRDATA" );
//
//		byte[] buffer = new byte[ 8 ];
//		BufferedInputStream input = new BufferedInputStream( agent.getInputStream() );
//
//		int read = 0;
//		int count = read;
//		while( count < 1 && (read = input.read( buffer )) > -1 ) count += read;
//	}

	private int read( InputStream input, byte[] data, int offset, int length, long timeout ) throws Exception {
		int read;
		int count = 0;
		int safe = length - count;
		boolean timeoutOccurred = false;
		long timeLimit = System.currentTimeMillis() + timeout;

		while( input.available() > 0 && (read = input.read( data, offset + count, safe )) > -1 && !timeoutOccurred ) {
			count += read;
			safe = length - count;
			timeoutOccurred = System.currentTimeMillis() > timeLimit;
		}

		if( timeoutOccurred ) throw new TimeoutException( "Timeout reading from station: " + timeout );

		return count;
	}

//	private void getData( SerialAgent agent ) throws Exception {
//		getData( agent.getOutputStream(), agent.getInputStream() );
//	}

	private void getData( OutputStream outputStream, InputStream inputStream ) throws Exception {
		PrintStream output = new PrintStream( outputStream );
		BufferedInputStream input = new BufferedInputStream( inputStream );

		// Send the command
		output.println( "LOOP 1" );
		output.flush();

		// Wait for the station to process
		// Otherwise there are no bytes available
		try {
			Thread.sleep( READ_INTERVAL );
		} catch( InterruptedException exception ) {
			return;
		}

		byte[] buffer = new byte[ 100 ];
		int read = read( input, buffer, 0, 100, 4000 );
		Log.write( Log.DEBUG, "Bytes read in getData: ", read );
		if( read <= 0 ) return;

		// Shift the buffer left on byte
		System.arraycopy( buffer, 1, buffer, 0, 99 );

		// Check the CRC to ensure good data
		int crc = new DavisCRC().update( buffer, 0, 99 ).value();
		if( crc != 0 ) {
			Log.write( Log.WARN, "CRC failure: ", crc );
			return;
		}

		// Barometer trend
		BarometerTrend pressureTrend = parseBarometerTrend( buffer[ 3 ] );

		// Barometer
		int barRaw = getUnsignedByte( buffer[ 7 ] ) + (getUnsignedByte( buffer[ 8 ] ) << 8);
		double pressure = barRaw == 0x7fff ? Float.NaN : barRaw / 1000.0f;

		// Inside temperature
		int tempInsideRaw = getUnsignedByte( buffer[ 9 ] ) + (getUnsignedByte( buffer[ 10 ] ) << 8);
		double tempInside = tempInsideRaw == 0x7fff ? Float.NaN : tempInsideRaw / 10.0f;

		// Inside humidity
		int humidInsideRaw = getUnsignedByte( buffer[ 11 ] );
		double humidInside = humidInsideRaw == 0xff ? Float.NaN : humidInsideRaw;

		// Outside temperature
		int tempOutsideRaw = getUnsignedByte( buffer[ 12 ] ) + ((buffer[ 13 ]) << 8);
		double tempOutside = tempOutsideRaw == 0x7fff ? Float.NaN : tempOutsideRaw / 10.0f;

		// Wind speed
		int windSpeedRaw = getUnsignedByte( buffer[ 14 ] );
		double windSpeed = windSpeedRaw == 0xff ? Float.NaN : windSpeedRaw;

		// Wind speed 10 minute average
		int windSpeedTenMinAvgRaw = getUnsignedByte( buffer[ 15 ] );
		double windSpeedTenMinAvg = windSpeedTenMinAvgRaw == 0xff ? Float.NaN : windSpeedTenMinAvgRaw;

		// Wind direction
		int windDirectionRaw = getUnsignedByte( buffer[ 16 ] ) + (getUnsignedByte( buffer[ 17 ] ) << 8);
		double windDirection = windDirectionRaw == 0x7fff ? Float.NaN : windDirectionRaw;

		// Outside humidity
		int humidOutsideRaw = getUnsignedByte( buffer[ 33 ] );
		double humidOutside = humidOutsideRaw == 0xff ? Float.NaN : humidOutsideRaw;

		// Rain rate
		int rainRateRaw = getUnsignedByte( buffer[ 41 ] ) + (getUnsignedByte( buffer[ 42 ] ) << 8);
		double rainRate = rainRateRaw == 0xffff ? Float.NaN : rainRateRaw / 100.0f;

		// Daily rain total
		int rainTotalDailyRaw = getUnsignedByte( buffer[ 50 ] ) + (getUnsignedByte( buffer[ 51 ] ) << 8);
		double rainTotalDaily = rainTotalDailyRaw == 0x7fff ? Float.NaN : rainTotalDailyRaw / 100.0f;

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

		WeatherDatum windSpeedDatum = new WeatherDatum( WeatherDatumIdentifier.WIND_SPEED, DecimalMeasure.valueOf( windSpeed, NonSI.MILES_PER_HOUR ) );
		WeatherDatum windDirectionDatum = new WeatherDatum( WeatherDatumIdentifier.WIND_DIRECTION, DecimalMeasure.valueOf( windDirection, NonSI.DEGREE_ANGLE ) );
		WeatherDatum windSpeedTenMinAvgDatum = new WeatherDatum( WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG, DecimalMeasure.valueOf( windSpeedTenMinAvg, NonSI.MILES_PER_HOUR ) );

		WeatherDatum rainRateDatum = new WeatherDatum( WeatherDatumIdentifier.RAIN_RATE, DecimalMeasure.valueOf( rainRate, NonSI.INCH.divide( NonSI.HOUR ) ) );
		WeatherDatum rainTotalDailyDatum = new WeatherDatum( WeatherDatumIdentifier.RAIN_TOTAL_DAILY, DecimalMeasure.valueOf( rainTotalDaily, NonSI.INCH ) );

		WeatherDatum temperatureInsideDatum = new WeatherDatum( WeatherDatumIdentifier.TEMPERATURE_INSIDE, DecimalMeasure.valueOf( tempInside, NonSI.FAHRENHEIT ) );
		WeatherDatum humidityInsideDatum = new WeatherDatum( WeatherDatumIdentifier.HUMIDITY_INSIDE, DecimalMeasure.valueOf( humidInside, NonSI.PERCENT ) );

		fireWeatherEvent( new WeatherDataEvent( temperatureDatum, pressureDatum, humidityDatum, windSpeedDatum, windDirectionDatum, rainRateDatum, rainTotalDailyDatum, temperatureInsideDatum, humidityInsideDatum, windSpeedTenMinAvgDatum ) );
	}

	private void fireWeatherEvent( WeatherDataEvent event ) {
		for( WeatherStation station : stations ) {
			station.weatherDataEvent( event );
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

	private class DeadManSwitch extends TimerTask {

		@Override
		public void run() {
			if( (System.currentTimeMillis() - lastPoll) > DEAD_MAN_LIMIT ) {
				Log.write( Log.ERROR, "Hung polling thread detected" );
				if( serialPort != null ) serialPort.close();
				if( pollingThread != null ) pollingThread.interrupt();
			}
		}

	}

}
