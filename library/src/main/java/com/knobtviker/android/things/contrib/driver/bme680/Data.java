package com.knobtviker.android.things.contrib.driver.bme680;

/**
 * Created by bojan on 27/11/2017.
 */

public class Data {

    // Contains new_data, gasm_valid & heat_stab
    public byte status;

    public boolean heaterStable = false;

    // The index of the heater profile used
    public int gasIndex = -1;

    // Measurement index to track order
    public byte measureIndex = -1;

    // Temperature in degree celsius x100
    public float temperature;

    // Pressure in Pascal
    public float pressure;

    // Humidity in % relative humidity x1000
    public float humidity;

    // Gas resistance in Ohms
    public int gasResistance = 0;

    // Indoor air quality score index
    public float airQualityScore = 0.0f;
}
