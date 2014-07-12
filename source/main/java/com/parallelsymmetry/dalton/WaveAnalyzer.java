package com.parallelsymmetry.dalton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.parallelsymmetry.utility.FileUtil;

public class WaveAnalyzer {

	private static final double PULSE_WIDTH = 1.0 / 4800.0;

	private static final int START_BIT_COUNT = 0;

	private static final double THRESHOLD = 1.5;

	private File file;

	private List<Datum> data;

	private double startBitTime;

	private double nextBitTime;

	private boolean currentBit;

	private int lineIndex;

	public WaveAnalyzer( File file ) {
		this.file = file;
	}

	private void process() throws Exception {
		List<String> lines = FileUtil.loadAsLines( file );

		int start = 18;
		int length = lines.size();
		data = new ArrayList<Datum>( length - start );

		for( int index = start; index < length; index++ ) {
			data.add( parseDatum( index - start, lines.get( index ) ) );
		}

		//System.out.println( "Data points loaded: " + data.size() );

		int startBitIndex = findStartBitIndex();
		double startBitTime = data.get( startBitIndex ).getTime();

		lineIndex = startBitIndex;
		nextBitTime = startBitTime + 0.5 * PULSE_WIDTH;

		for( int index = 0; index < START_BIT_COUNT; index++ ) {
			getNextBit();
		}

		int bit = -1;
		while( ( bit = getNextBit() ) > -1 ) {
			System.out.print( bit );
			lineIndex++;
		}
		System.out.println();
	}

	private int getNextBit() {
		Datum datum = null;
		while( lineIndex < data.size() && ( datum = data.get( lineIndex ) ).getTime() < nextBitTime ) {
			lineIndex++;
		}
		if( lineIndex >= data.size() ) return -1;

		nextBitTime = nextBitTime + PULSE_WIDTH;

		return data.get( lineIndex - 1 ).getValue() < THRESHOLD ? 0 : 1;
	}

	private Datum parseDatum( int index, String string ) {
		String[] chunks = string.split( "," );
		return new Datum( index, Double.parseDouble( chunks[0] ), Double.parseDouble( chunks[1] ), Double.parseDouble( chunks[2] ) );
	}

	private int findStartBitIndex() {
		for( Datum datum : data ) {
			if( datum.getTime() > 0 ) return datum.getIndex();
		}

		return -1;
	}

	public static final void main( String[] commands ) {
		if( commands.length == 0 ) {
			help();
			return;
		}

		for( String command : commands ) {
			try {
				new WaveAnalyzer( new File( command ) ).process();
			} catch( Exception exception ) {
				exception.printStackTrace( System.err );
			}
		}
	}

	public static final void help() {
		System.out.println( "Usage: java WaveAnalyzer [file...]" );
	}

	private class Datum {

		private int index;

		public double time;

		public double value1;

		public double value2;

		public Datum( int index, double time, double value1, double value2 ) {
			this.index = index;
			this.time = time;
			this.value1 = value1;
			this.value2 = value2;
		}

		public int getIndex() {
			return index;
		}

		public double getTime() {
			return time;
		}

		public double getValue() {
			return 0.5 * ( value1 + value2 );
		}

		public String toString() {
			return "Index: " + index + "  time: " + getTime() + "  value: " + getValue();
		}

	}

}
