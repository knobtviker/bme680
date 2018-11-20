package com.knobtviker.android.things.contrib.community.driver.bme680;


import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class Bme680Test {

    // copy of the method in Bme680.java
    private static int concatBytes(final int msb, final int lsb, final boolean isSigned) {
        if (isSigned) {
            return (msb << 8) | (lsb & 0xff); // keep the sign of msb but not of lsb
        } else {
            return ((msb & 0xff) << 8) | (lsb & 0xff);
        }
    }

    /*
Example calibration data provided by c-driver:
----------------------------------------------

par_gh1: -31
par_gh2: -13193
par_gh3: 18
par_h1: 846
par_h2: 997
par_h3: 0
par_h4: 45
par_h5: 20
par_h6: 120
par_h7: -100
par_p1: 36236
par_p10: 30
par_p2: -10417
par_p3: 88
par_p4: 6248
par_p5: -28
par_p6: 30
par_p7: 45
par_p8: -3060 -> 62476 at Android driver
par_p9: -2427 -> -123 at Android driver
par_t1: 26353
par_t2: 26261 -> -107 at Android driver
par_t3: 3
range_sw_err: 0
res_heat_range: 1
res_heat_val: 49
     */

    @Test
    public void should_calculate_the_right_value_T1() {
        byte msb = 102;
        byte lsb = -15; // 241
        int i = concatBytes(msb, lsb, false);

        assertThat(i, CoreMatchers.equalTo(26353));
    }

    @Test
    public void should_calculate_the_right_value_T2() {
        byte msb = 102;
        byte lsb = -107; // 149
        int i = concatBytes(msb, lsb, true);

        assertThat(i, CoreMatchers.equalTo(26261));
    }

    @Test
    public void should_calculate_the_right_value_P1() {
        byte msb = -115; // 141
        byte lsb = -116; // 140
        int i = concatBytes(msb, lsb, false);

        assertThat(i, CoreMatchers.equalTo(36236));
    }

    @Test
    public void should_calculate_the_right_value_P2() {
        byte msb = -41; // 215
        byte lsb = 79;
        int i = concatBytes(msb, lsb, true);

        assertThat(i, CoreMatchers.equalTo(-10417));
    }

    @Test
    public void should_calculate_the_right_value_P4() {
        byte msb = 24;
        byte lsb = 104;
        int i = concatBytes(msb, lsb, true);  // should be 'true' according to c-driver

        assertThat(i, CoreMatchers.equalTo(6248));
    }

    @Test
    public void should_calculate_the_right_value_P5() {
        byte msb = -1; // 255
        byte lsb = -28; // 228
        int i = concatBytes(msb, lsb, true);

        assertThat(i, CoreMatchers.equalTo(-28));
    }

    @Test
    public void should_calculate_the_right_value_P8() {
        byte msb = -12; // 244
        byte lsb = 12;
        int i = concatBytes(msb, lsb, true); // should be 'true' according to c-driver

        assertThat(i, CoreMatchers.equalTo(-3060));
    }

    @Test
    public void should_calculate_the_right_value_P9() {
        byte msb = -10; // 246
        byte lsb = -123; // 133
        int i = concatBytes(msb, lsb, true);

        assertThat(i, CoreMatchers.equalTo(-2427));
    }

    @Test
    public void should_calculate_the_right_value_GH2() {
        byte msb = -52; // 204
        byte lsb = 119;
        int i = concatBytes(msb, lsb, true);

        assertThat(i, CoreMatchers.equalTo(-13193));
    }

}