package com.knobtviker.android.things.contrib.driver.bme680;

/**
 * Created by bojan on 08/01/2018.
 */

public class Calibration {

    public final int[] temperature = new int[3];

    public final int[] pressure = new int[10];

    public final int[] humidity = new int[7];

    public final int[] gasHeater = new int[3];
}
