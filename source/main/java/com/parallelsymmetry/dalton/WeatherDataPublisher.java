package com.parallelsymmetry.dalton;

import javax.measure.Measure;
import java.io.IOException;
import java.util.Map;

public interface WeatherDataPublisher {

	int publish( Map<WeatherDatumIdentifier, Measure<?, ?>> data ) throws IOException;

}
