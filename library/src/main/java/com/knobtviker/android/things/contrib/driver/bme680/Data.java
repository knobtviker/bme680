package com.knobtviker.android.things.contrib.driver.bme680;

/**
 * Created by bojan on 27/11/2017.
 */

class Data {

    // Contains new_data, gasm_valid & heat_stab
    int status = -1;

    boolean heat_stable = false;

    // The index of the heater profile used
    int gas_index = -1;

    // Measurement index to track order
    int meas_index = -1;

    // Temperature in degree celsius x100
    int temperature = -1;

    // Pressure in Pascal
    int pressure = -1;

    // Humidity in % relative humidity x1000
    int humidity = -1;

    // Gas resistance in Ohms
    int gas_resistance = -1;
}
