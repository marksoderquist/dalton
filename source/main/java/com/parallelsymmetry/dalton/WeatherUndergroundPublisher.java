package com.parallelsymmetry.dalton;

import com.parallelsymmetry.utility.DateUtil;
import com.parallelsymmetry.utility.TextUtil;
import com.parallelsymmetry.utility.log.Log;

import javax.measure.Measure;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Deque;
import java.util.Map;
import java.util.TimeZone;

/**
 * @author ecco
 */
public class WeatherUndergroundPublisher {

    private WeatherReader reader;

    private boolean windGustAvailable;

    private long windGustTime;

    private float windGustValue;

    private float windGustDirection;

    public WeatherUndergroundPublisher(WeatherReader reader ) {
        this.reader = reader;
        this.windGustTime = windGustTimeReset();
    }

    public int publish( Map<WeatherDatumIdentifier, Measure<?, ?>> data, Deque<WeatherDataEvent> tenMinuteBuffer) throws IOException {
        // Example:
        // http://rtupdate.wunderground.com/weatherstation/updateweatherstation.php?ID=KCASANFR5&PASSWORD=XXXXXX&dateutc=2000-01-01+10%3A32%3A35&winddir=230&windspeedmph=12&windgustmph=12&tempf=70&rainin=0&baromin=29.1&dewptf=68.2&humidity=90&weather=&clouds=&softwaretype=vws%20versionxx&action=updateraw&realtime=1&rtfreq=2.5

        //	winddir - [0-360 instantaneous wind direction]
        //	windspeedmph - [mph instantaneous wind speed]
        //	windgustmph - [mph current wind gust, using software specific time period]
        //	windgustdir - [0-360 using software specific time period]
        //	windspdmph_avg2m  - [mph 2 minute average wind speed mph]
        //	winddir_avg2m - [0-360 2 minute average wind direction]
        //	windgustmph_10m - [mph past 10 minutes wind gust mph ]
        //	windgustdir_10m - [0-360 past 10 minutes wind gust direction]

        StringBuilder builder = new StringBuilder( "http://rtupdate.wunderground.com/weatherstation/updateweatherstation.php" );
        builder.append( "?ID=KUTRIVER9" );
        builder.append( "&PASSWORD=qWest73wun" );
        builder.append( "&action=updateraw" );
        builder.append( "&realtime=1&rtfreq=2.5" );
        builder.append( "&dateutc=" );
        builder.append( DateUtil.format( new Date(), WeatherStation.WUNDERGROUND_DATE_FORMAT, TimeZone.getTimeZone( "UTC" ) ) );

        // Prepare basic data.
        add( data, builder, WeatherDatumIdentifier.TEMPERATURE, "tempf", "0.0" );
        add( data, builder, WeatherDatumIdentifier.PRESSURE, "baromin", "0.00" );
        add( data, builder, WeatherDatumIdentifier.HUMIDITY, "humidity", "0" );
        add( data, builder, WeatherDatumIdentifier.DEW_POINT, "dewptf", "0.0" );

        // Prepare wind data.
        add( data, builder, WeatherDatumIdentifier.WIND_DIRECTION, "winddir", "0" );
        add( data, builder, WeatherDatumIdentifier.WIND_SPEED_10_MIN_AVG, "windspeedmph", "0.0" );

        float ws = (Float) data.get(WeatherDatumIdentifier.WIND_SPEED_CURRENT).getValue();
        float wd = (Float) data.get(WeatherDatumIdentifier.WIND_DIRECTION).getValue();
        if( WeatherUtil.isGust(ws, tenMinuteBuffer) && ws > windGustValue ) {
            windGustValue = ws;
            windGustDirection = wd;
            windGustAvailable = true;
        }
        if( System.currentTimeMillis() >= windGustTime ) {
            if(windGustAvailable) {
                add(builder, windGustValue, "windgustmph", "0");
                add(builder, windGustDirection, "windgustdir", "0");
            }
            windGustAvailable = false;
            windGustTimeReset();
        }

        // Prepare rain data.
        add( data, builder, WeatherDatumIdentifier.RAIN_RATE, "rainin", "0.00" );
        add( data, builder, WeatherDatumIdentifier.RAIN_TOTAL_DAILY, "dailyrainin", "0.00" );

        // Prepare software data.
        builder.append( "&softwaretype=dalton" );
        String release = reader.getCard().getRelease().toHumanString( DateUtil.DEFAULT_TIME_ZONE );
        builder.append( URLEncoder.encode( " " + release, TextUtil.DEFAULT_ENCODING ) );

        Log.write( Log.TRACE, builder.toString() );
        Response response = rest( "GET", builder.toString() );

        return response.getCode();
    }

    private long windGustTimeReset() {
        return windGustTime = System.currentTimeMillis() + 300000;
    }

    private void add( Map<WeatherDatumIdentifier, Measure<?, ?>> data,StringBuilder builder, WeatherDatumIdentifier identifier, String key, String format ) {
        Measure<?, ?> measure = data.get( identifier );
        if( measure == null ) return;
        add( builder, (Float)measure.getValue(), key, format );
    }

    private void add( StringBuilder builder, float value, String key, String format ) {
        builder.append( "&" );
        builder.append( key );
        builder.append( "=" );
        if( format == null ) {
            builder.append( value );
        } else {
            DecimalFormat formatter = new DecimalFormat( format );
            builder.append( formatter.format( value ) );
        }
    }

    private Response rest(String method, String url ) throws IOException {
        return rest( method, url, null );
    }

    private Response rest(String method, String url, byte[] request ) throws IOException {
        return rest( method, url, null, request );
    }

    private Response rest(String method, String url, Map<String, String> headers, byte[] request ) throws IOException {
        String USER_AGENT = "Mozilla/5.0";

        // Set up the request.
        HttpURLConnection connection = (HttpURLConnection)new URL( url ).openConnection();
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
            try {
                connection.getOutputStream().write( request );
            } finally {
                connection.getOutputStream().close();
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
            while( ( inputLine = input.readLine() ) != null ) {
                response.append( inputLine );
            }
            input.close();

            return new Response( responseCode, response.toString() );
        } finally {
            if( connection != null ) connection.disconnect();
        }
    }

    private String format( Map<WeatherDatumIdentifier, Measure<?, ?>> data,WeatherDatumIdentifier identifier, String format ) {
        Measure<?, ?> measure = data.get( identifier );
        if( measure == null ) return null;
        return format( (Float)measure.getValue(), format );
    }

    private String format( float value, String format ) {
        return new DecimalFormat( format ).format( value );
    }

    private class Response {

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
