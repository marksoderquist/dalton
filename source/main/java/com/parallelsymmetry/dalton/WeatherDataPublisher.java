package com.parallelsymmetry.dalton;

import javax.measure.Measure;
import javax.measure.quantity.Quantity;
import java.io.IOException;
import java.util.Map;

public interface WeatherDataPublisher {

	int publish( WeatherStation station, Map<WeatherDatumIdentifier, Measure<? extends Number, ? extends Quantity>> data ) throws IOException;

}
