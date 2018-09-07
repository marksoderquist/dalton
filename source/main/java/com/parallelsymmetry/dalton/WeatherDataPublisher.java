package com.parallelsymmetry.dalton;

import java.io.IOException;

public interface WeatherDataPublisher {

	int publish( WeatherStation station, WeatherDataEvent event ) throws IOException;

}
