package com.knobtviker.android.things.contrib.driver.bme680;

/**
 * Created by bojan on 27/11/2017.
 */

public class Data {

    // Contains new_data, gasm_valid & heat_stab
    public int status = -1;

    public boolean heaterStable = false;

    // The index of the heater profile used
    public int gasIndex = -1;

    // Measurement index to track order
    public int measureIndex = -1;

    // Temperature in degree celsius x100
    public int temperature = -1;

    // Pressure in Pascal
    public int pressure = -1;

    // Humidity in % relative humidity x1000
    public int humidity = -1;

    // Gas resistance in Ohms
    public int gasResistance = -1;
}
