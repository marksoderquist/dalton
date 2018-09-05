package com.parallelsymmetry.dalton;

import com.parallelsymmetry.utility.log.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public abstract class HttpPublisher implements WeatherDataPublisher {

	protected Response rest( String method, String url ) throws IOException {
		return rest( method, url, null );
	}

	protected Response rest( String method, String url, byte[] request ) throws IOException {
		return rest( method, url, null, request );
	}

	protected Response rest( String method, String url, Map<String, String> headers, byte[] request ) throws IOException {
		String USER_AGENT = "Mozilla/5.0";

		Log.write( Log.TRACE, "Sending to: " + url );

		// Set up the request.
		HttpURLConnection connection = (HttpURLConnection)new URL( url ).openConnection();
		connection.setConnectTimeout( 5000 );
		connection.setReadTimeout( 5000 );
		connection.setRequestMethod( method );
		connection.setRequestProperty( "User-Agent", USER_AGENT );
		connection.setAllowUserInteraction( false );
		if( headers != null ) {
			for( String key : headers.keySet() ) {
				connection.setRequestProperty( key, headers.get( key ) );
			}
		}
		if( request != null ) {
			connection.setDoOutput( true );
			OutputStream output = connection.getOutputStream();
			try {
				output.write( request );
			} finally {
				if( output != null ) output.close();
			}
		}

		// Handle the response.
		try {
			// Get the response code.
			int responseCode = connection.getResponseCode();

			// Read the response.
			String inputLine;
			StringBuilder response = new StringBuilder();
			BufferedReader input = new BufferedReader( new InputStreamReader( connection.getInputStream() ) );
			while( (inputLine = input.readLine()) != null ) {
				response.append( inputLine );
			}
			input.close();

			return new Response( responseCode, response.toString() );
		} finally {
			if( connection != null ) connection.disconnect();
		}
	}

	protected class Response {

		private int code;

		private String content;

		public Response( int code, String content ) {
			this.code = code;
			this.content = content;
		}

		public int getCode() {
			return code;
		}

		public String getContent() {
			return content;
		}

	}

}
