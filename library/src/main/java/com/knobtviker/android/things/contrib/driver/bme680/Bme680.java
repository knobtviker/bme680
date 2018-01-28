package com.knobtviker.android.things.contrib.driver.bme680;

/**
 * Created by bojan on 16/11/2017.
 */

import android.os.SystemClock;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Driver for the Bosch BME 680 sensor.
 */
@SuppressWarnings({"unused", "WeakerAccess", "FieldCanBeLocal"})
public class Bme680 implements AutoCloseable {
    private static final String TAG = Bme680.class.getSimpleName();

    /**
     * Chip vendor for the BME680
     */
    public static final String CHIP_VENDOR = "Bosch";

    /**
     * Chip name for the BME680
     */
    public static final String CHIP_NAME = "BME680";

    /**
     * Chip sensor type for the BME680 indoor air quality sensor
     */
    public static final String CHIP_SENSOR_TYPE_IAQ = "android.sensor.indoor_air_quality";

    /**
     * Chip ID for the BME680
     */
    public static final int CHIP_ID_BME680 = 0x61;

    /**
     * Default I2C address for the sensor.
     */
    public static final int DEFAULT_I2C_ADDRESS = 0x76;

    /**
     * Alternative I2C address for the sensor.
     */
    public static final int ALTERNATIVE_I2C_ADDRESS = 0x77;

    @Deprecated
    public static final int I2C_ADDRESS = DEFAULT_I2C_ADDRESS;

    // Sensor constants from the datasheet.
    /**
     * Mininum temperature in Celsius the sensor can measure.
     */
    public static final float MIN_TEMP_C = -40f;
    /**
     * Maximum temperature in Celsius the sensor can measure.
     */
    public static final float MAX_TEMP_C = 85f;
    /**
     * Minimum pressure in hPa the sensor can measure.
     */
    public static final float MIN_PRESSURE_HPA = 300f;
    /**
     * Maximum pressure in hPa the sensor can measure.
     */
    public static final float MAX_PRESSURE_HPA = 1100f;
    /**
     * Minimum humidity in percentage the sensor can measure.
     */
    public static final float MIN_HUMIDITY_PERCENT = 0f;
    /**
     * Maximum humidity in percentage the sensor can measure.
     */
    public static final float MAX_HUMIDITY_PERCENT = 100f;
    /**
     * Minimum humidity in percentage the sensor can measure.
     */
    public static final float MIN_GAS_PERCENT = 10f;
    /**
     * Maximum humidity in percentage the sensor can measure.
     */
    public static final float MAX_GAS_PERCENT = 95f;
    /**
     * Maximum power consumption in micro-amperes when measuring temperature.
     */
    public static final float MAX_POWER_CONSUMPTION_TEMP_UA = 350f;
    /**
     * Maximum power consumption in micro-amperes when measuring pressure.
     */
    public static final float MAX_POWER_CONSUMPTION_PRESSURE_UA = 714f;
    /**
     * Maximum power consumption in micro-amperes when measuring pressure.
     */
    public static final float MAX_POWER_CONSUMPTION_HUMIDITY_UA = 340f;
    /**
     * Maximum power consumption in micro-amperes when measuring volatile gases.
     */
    public static final float MAX_POWER_CONSUMPTION_GAS_UA = 13f; //12f
    //TODO: Fix this fake data from BME280
    /**
     * Maximum frequency of the measurements.
     */
    public static final float MAX_FREQ_HZ = 181f;
    /**
     * Minimum frequency of the measurements.
     */
    public static final float MIN_FREQ_HZ = 23.1f;

    /**
     * Power mode.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({MODE_SLEEP, MODE_FORCED})
    public @interface Mode {
    }

    public static final int MODE_SLEEP = 0;
    public static final int MODE_FORCED = 1;

    /**
     * Oversampling multiplier.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({OVERSAMPLING_SKIPPED, OVERSAMPLING_1X, OVERSAMPLING_2X, OVERSAMPLING_4X, OVERSAMPLING_8X, OVERSAMPLING_16X})
    public @interface Oversampling {
    }

    public static final int OVERSAMPLING_SKIPPED = 0;
    public static final int OVERSAMPLING_1X = 1;
    public static final int OVERSAMPLING_2X = 2;
    public static final int OVERSAMPLING_4X = 3;
    public static final int OVERSAMPLING_8X = 4;
    public static final int OVERSAMPLING_16X = 5;

    /**
     * IIR filter size.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FILTER_SIZE_NONE, FILTER_SIZE_1, FILTER_SIZE_3, FILTER_SIZE_7, FILTER_SIZE_15, FILTER_SIZE_31, FILTER_SIZE_63, FILTER_SIZE_127})
    public @interface Filter {
    }

    public static final int FILTER_SIZE_NONE = 0;
    public static final int FILTER_SIZE_1 = 1;
    public static final int FILTER_SIZE_3 = 2;
    public static final int FILTER_SIZE_7 = 3;
    public static final int FILTER_SIZE_15 = 4;
    public static final int FILTER_SIZE_31 = 5;
    public static final int FILTER_SIZE_63 = 6;
    public static final int FILTER_SIZE_127 = 7;

    /**
     * Heater control.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ENABLE_HEATER, DISABLE_HEATER})
    public @interface HeaterControl {
    }

    public static final int ENABLE_HEATER = 0;
    public static final int DISABLE_HEATER = 0x08;

    /**
     * Gas measurement.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({DISABLE_GAS, ENABLE_GAS})
    public @interface GasMeasure {
    }

    public static final int DISABLE_GAS = 0;
    public static final int ENABLE_GAS = 1;

    /**
     * Gas heater profile.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PROFILE_0, PROFILE_1, PROFILE_2, PROFILE_3, PROFILE_4, PROFILE_5, PROFILE_6, PROFILE_7, PROFILE_8, PROFILE_9})
    public @interface HeaterProfile {
    }

    /**
     * Gas heater duration.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntRange(from = 1, to = 4032)
    public @interface HeaterDuration {
    }

    public static final int PROFILE_0 = 0;
    public static final int PROFILE_1 = 1;
    public static final int PROFILE_2 = 2;
    public static final int PROFILE_3 = 3;
    public static final int PROFILE_4 = 4;
    public static final int PROFILE_5 = 5;
    public static final int PROFILE_6 = 6;
    public static final int PROFILE_7 = 7;
    public static final int PROFILE_8 = 8;
    public static final int PROFILE_9 = 9;

    /**
     * Settings selector.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({BME680_OST_SEL, BME680_OSP_SEL, BME680_OSH_SEL, BME680_GAS_MEAS_SEL, BME680_FILTER_SEL, BME680_HCNTRL_SEL, BME680_RUN_GAS_SEL, BME680_NBCONV_SEL, BME680_GAS_SENSOR_SEL})
    public @interface Settings {
    }

    public static final int BME680_OST_SEL = 1;
    public static final int BME680_OSP_SEL = 2;
    public static final int BME680_OSH_SEL = 4;
    public static final int BME680_GAS_MEAS_SEL = 8;
    public static final int BME680_FILTER_SEL = 16;
    public static final int BME680_HCNTRL_SEL = 32;
    public static final int BME680_RUN_GAS_SEL = 64;
    public static final int BME680_NBCONV_SEL = 128;
    public static final int BME680_GAS_SENSOR_SEL = (BME680_GAS_MEAS_SEL | BME680_RUN_GAS_SEL | BME680_NBCONV_SEL);

    // Registers
    private static final int BME680_REGISTER_ID = 0xD0;
    private static final int BME680_REGISTER_SOFT_RESET = 0xe0;

    // Sensor configuration registers
    private static final int BME680_CONFIG_HEATER_CONTROL_ADDRESS = 0x70;
    private static final int BME680_CONFIG_ODR_RUN_GAS_NBC_ADDRESS = 0x71;
    private static final int BME680_CONFIG_OS_H_ADDRESS = 0x72;
    private static final int BME680_MEM_PAGE_ADDRESS = 0xf3;
    private static final int BME680_CONFIG_T_P_MODE_ADDRESS = 0x74;
    private static final int BME680_CONFIG_ODR_FILTER_ADDRESS = 0x75;

    // field_x related defines
    private static final int BME680_FIELD0_ADDRESS = 0x1d;
    private static final int BME680_FIELD_LENGTH = 15;
    private static final int BME680_FIELD_ADDRESS_OFFSET = 17;

    // Heater settings
    private static final int BME680_RESISTANCE_HEAT0_ADDRESS = 0x5a;
    private static final int BME680_GAS_WAIT0_ADDRESS = 0x64;

    // Commands
    private static final int BME680_COMMAND_SOFT_RESET = 0xb6;

    // BME680 coefficients related defines
    private static final int BME680_COEFFICIENT_ADDRESS1_LEN = 25;
    private static final int BME680_COEFFICIENT_ADDRESS2_LEN = 16;

    // Coefficient's address
    private static final int BME680_COEFFICIENT_ADDRESS1 = 0x89;
    private static final int BME680_COEFFICIENT_ADDRESS2 = 0xe1;

    // Other coefficient's address
    private static final int BME680_ADDRESS_RESISTANCE_HEAT_VALUE_ADDRESS = 0x00;
    private static final int BME680_ADDRESS_RESISTANCE_HEAT_RANGE_ADDRESS = 0x02;
    private static final int BME680_ADDRESS_RANGE_SOFTWARE_ERROR_ADDRESS = 0x04;
    private static final int BME680_ADDRESS_SENSOR_CONFIG_START = 0x5A;
    private static final int BME680_ADDRESS_GAS_CONFIG_START = 0x64;

    // Mask definitions
    private static final int BME680_GAS_MEASURE_MASK = 0x30;
    private static final int BME680_NBCONVERSION_MASK = 0x0F;
    private static final int BME680_FILTER_MASK = 0x1C;
    private static final int BME680_OVERSAMPLING_TEMPERATURE_MASK = 0xE0;
    private static final int BME680_OVERSAMPLING_PRESSURE_MASK = 0x1C;
    private static final int BME680_OVERSAMPLING_HUMIDITY_MASK = 0x07;
    private static final int BME680_HEATER_CONTROL_MASK = 0x08;
    private static final int BME680_RUN_GAS_MASK = 0x10;
    private static final int BME680_MODE_MASK = 0x03;
    private static final int BME680_RHRANGE_MASK = 0x30;
    private static final int BME680_RSERROR_MASK = 0xf0;
    private static final int BME680_NEW_DATA_MASK = 0x80;
    private static final int BME680_GAS_INDEX_MASK = 0x0f;
    private static final int BME680_GAS_RANGE_MASK = 0x0f;
    private static final int BME680_GASM_VALID_MASK = 0x20;
    private static final int BME680_HEAT_STABLE_MASK = 0x10;
    private static final int BME680_MEM_PAGE_MASK = 0x10;
    private static final int BME680_SPI_RD_MASK = 0x80;
    private static final int BME680_SPI_WR_MASK = 0x7f;
    private static final int BME680_BIT_H1_DATA_MASK = 0x0F;

    // Bit position definitions for sensor settings
    private static final int GAS_MEASURE_POSITION = 4;
    private static final int FILTER_POSITION = 2;
    private static final int OVERSAMPLING_TEMPERATURE_POSITION = 5;
    private static final int OVERSAMPLING_PRESSURE_POSITION = 2;
    private static final int OVERSAMPLING_HUMIDITY_POSITION = 0;
    private static final int RUN_GAS_POSITION = 4;
    private static final int MODE_POSITION = 0;
    private static final int NBCONVERSION_POSITION = 0;

    // Array Index to Field data mapping for Calibration Data
    private static final int BME680_T2_LSB_REGISTER = 1;
    private static final int BME680_T2_MSB_REGISTER = 2;
    private static final int BME680_T3_REGISTER = 3;
    private static final int BME680_P1_LSB_REGISTER = 5;
    private static final int BME680_P1_MSB_REGISTER = 6;
    private static final int BME680_P2_LSB_REGISTER = 7;
    private static final int BME680_P2_MSB_REGISTER = 8;
    private static final int BME680_P3_REGISTER = 9;
    private static final int BME680_P4_LSB_REGISTER = 11;
    private static final int BME680_P4_MSB_REGISTER = 12;
    private static final int BME680_P5_LSB_REGISTER = 13;
    private static final int BME680_P5_MSB_REGISTER = 14;
    private static final int BME680_P7_REGISTER = 15;
    private static final int BME680_P6_REGISTER = 16;
    private static final int BME680_P8_LSB_REGISTER = 19;
    private static final int BME680_P8_MSB_REGISTER = 20;
    private static final int BME680_P9_LSB_REGISTER = 21;
    private static final int BME680_P9_MSB_REGISTER = 22;
    private static final int BME680_P10_REGISTER = 23;
    private static final int BME680_H2_MSB_REGISTER = 25;
    private static final int BME680_H2_LSB_REGISTER = 26;
    private static final int BME680_H1_LSB_REGISTER = 26;
    private static final int BME680_H1_MSB_REGISTER = 27;
    private static final int BME680_H3_REGISTER = 28;
    private static final int BME680_H4_REGISTER = 29;
    private static final int BME680_H5_REGISTER = 30;
    private static final int BME680_H6_REGISTER = 31;
    private static final int BME680_H7_REGISTER = 32;
    private static final int BME680_T1_LSB_REGISTER = 33;
    private static final int BME680_T1_MSB_REGISTER = 34;
    private static final int BME680_GH2_LSB_REGISTER = 35;
    private static final int BME680_GH2_MSB_REGISTER = 36;
    private static final int BME680_GH1_REGISTER = 37;
    private static final int BME680_GH3_REGISTER = 38;

    private static final int BME680_HUMIDITY_REGISTER_SHIFT_VALUE = 4;
    private static final int BEE680_RESET_PERIOD_MILLISECONDS = 10;
    private static final int BME680_POLL_PERIOD_MILLISECONDS = 10;

    private I2cDevice device;
    private int heaterResistanceRange;
    private int heaterResistanceValue;
    private int errorRange;

    // Look up tables for the possible gas range values
    final long GAS_RANGE_LOOKUP_TABLE_1[] = {
        2147483647L, 2147483647L, 2147483647L, 2147483647L, 2147483647L, 2126008810L, 2147483647L,
        2130303777L, 2147483647L, 2147483647L, 2143188679L, 2136746228L, 2147483647L, 2126008810L,
        2147483647L, 2147483647L
    };

    final long GAS_RANGE_LOOKUP_TABLE_2[] = {
        4096000000L, 2048000000L, 1024000000L, 512000000L, 255744255L, 127110228L, 64000000L,
        32258064L, 16016016L, 8000000L, 4000000L, 2000000L, 1000000L, 500000L, 250000L, 125000L
    };

    private int DATA_READ_ATTEMPTS = 10;
    private int DATA_GAS_BURN_IN = 50;

    private boolean enabled = false;
    private int chipId;
    private int powerMode;
    private Calibration calibration;
    private GasSettings gasSettings;
    private SensorSettings sensorSettings;
    private Data data;
    private LinkedBlockingQueue<Long> gasResistanceData = new LinkedBlockingQueue<>(DATA_GAS_BURN_IN);
    private int temperatureFine;
    private long ambientTemperature;

    /**
     * Create a new BME680 sensor driver connected on the given bus.
     *
     * @param bus I2C bus the sensor is connected to.
     * @throws IOException
     */
    public Bme680(@NonNull final String bus) throws IOException {
        this(bus, DEFAULT_I2C_ADDRESS);
    }

    /**
     * Create a new BME680 sensor driver connected on the given bus and address.
     *
     * @param bus     I2C bus the sensor is connected to.
     * @param address I2C address of the sensor.
     * @throws IOException
     */
    public Bme680(@NonNull final String bus, final int address) throws IOException {
        final PeripheralManagerService pioService = new PeripheralManagerService();
        final I2cDevice device = pioService.openI2cDevice(bus, address);
        try {
            connect(device);
        } catch (IOException | RuntimeException e) {
            try {
                close();
            } catch (IOException | RuntimeException ignored) {
            }
            throw e;
        }
    }

    /**
     * Create a new BME680 sensor driver connected to the given I2c device.
     *
     * @param device I2C device of the sensor.
     * @throws IOException
     */
    /*package*/  Bme680(I2cDevice device) throws IOException {
        connect(device);
    }

    /**
     * Close the driver and the underlying device.
     */
    @Override
    public void close() throws IOException {
        if (device != null) {
            try {
                device.close();
            } finally {
                sensorSettings = null;
                gasSettings = null;
                device = null;
            }
        }
    }

    private void connect(I2cDevice device) throws IOException {
        calibration = new Calibration();
        sensorSettings = new SensorSettings();
        gasSettings = new GasSettings();
        data = new Data();

        prefillGasDataResistance();

        this.device = device;

        softReset();

        chipId = this.device.readRegByte(BME680_REGISTER_ID);
        if (chipId != CHIP_ID_BME680) {
            throw new IllegalStateException("Bosch BME680 not found");
        }

        setPowerMode(MODE_SLEEP);

        // Read calibration data in 2 parts and concat them into 1 array
        final byte[] mCalibrationArray = readCalibrationData();

        // Read temperature calibration data (3 words)
        calibration.temperature[0] = concatBytes(mCalibrationArray[BME680_T1_MSB_REGISTER], mCalibrationArray[BME680_T1_LSB_REGISTER], false);
        calibration.temperature[1] = concatBytes(mCalibrationArray[BME680_T2_MSB_REGISTER], mCalibrationArray[BME680_T2_LSB_REGISTER], true);
        calibration.temperature[2] = mCalibrationArray[BME680_T3_REGISTER];

        // Read pressure calibration data (10 words)
        calibration.pressure[0] = concatBytes(mCalibrationArray[BME680_P1_MSB_REGISTER], mCalibrationArray[BME680_P1_LSB_REGISTER], false);
        calibration.pressure[1] = concatBytes(mCalibrationArray[BME680_P2_MSB_REGISTER], mCalibrationArray[BME680_P2_LSB_REGISTER], true);
        calibration.pressure[2] = mCalibrationArray[BME680_P3_REGISTER];
        calibration.pressure[3] = concatBytes(mCalibrationArray[BME680_P4_MSB_REGISTER], mCalibrationArray[BME680_P4_LSB_REGISTER], false);
        calibration.pressure[4] = concatBytes(mCalibrationArray[BME680_P5_MSB_REGISTER], mCalibrationArray[BME680_P5_LSB_REGISTER], true);
        calibration.pressure[5] = mCalibrationArray[BME680_P6_REGISTER];
        calibration.pressure[6] = mCalibrationArray[BME680_P7_REGISTER];
        calibration.pressure[7] = concatBytes(mCalibrationArray[BME680_P8_MSB_REGISTER], mCalibrationArray[BME680_P8_LSB_REGISTER], false);
        calibration.pressure[8] = concatBytes(mCalibrationArray[BME680_P9_MSB_REGISTER], mCalibrationArray[BME680_P9_LSB_REGISTER], true);
        calibration.pressure[9] = mCalibrationArray[BME680_P10_REGISTER] & 0xFF;

        // Read humidity calibration data (7 words)
        calibration.humidity[0] = (((mCalibrationArray[BME680_H1_MSB_REGISTER] & 0xffff) << BME680_HUMIDITY_REGISTER_SHIFT_VALUE) | (mCalibrationArray[BME680_H1_LSB_REGISTER] & BME680_BIT_H1_DATA_MASK)) & 0xffff;
        calibration.humidity[1] = (((mCalibrationArray[BME680_H2_MSB_REGISTER] & 0xffff) << BME680_HUMIDITY_REGISTER_SHIFT_VALUE) | (mCalibrationArray[BME680_H2_LSB_REGISTER] >> BME680_HUMIDITY_REGISTER_SHIFT_VALUE)) & 0xffff;
        calibration.humidity[2] = mCalibrationArray[BME680_H3_REGISTER];
        calibration.humidity[3] = mCalibrationArray[BME680_H4_REGISTER];
        calibration.humidity[4] = mCalibrationArray[BME680_H5_REGISTER];
        calibration.humidity[5] = mCalibrationArray[BME680_H6_REGISTER] & 0xFF;
        calibration.humidity[6] = mCalibrationArray[BME680_H7_REGISTER];

        // Read gas heater calibration data (3 words)
        calibration.gasHeater[0] = mCalibrationArray[BME680_GH1_REGISTER];
        calibration.gasHeater[1] = concatBytes(mCalibrationArray[BME680_GH2_MSB_REGISTER], mCalibrationArray[BME680_GH2_LSB_REGISTER], true);
        calibration.gasHeater[2] = mCalibrationArray[BME680_GH3_REGISTER];

        // Read other heater calibration data
        heaterResistanceRange = ((this.device.readRegByte(BME680_ADDRESS_RESISTANCE_HEAT_RANGE_ADDRESS) & BME680_RHRANGE_MASK) & 0xFF) / 16;
        heaterResistanceValue = this.device.readRegByte(BME680_ADDRESS_RESISTANCE_HEAT_VALUE_ADDRESS);
        errorRange = ((this.device.readRegByte(BME680_ADDRESS_RANGE_SOFTWARE_ERROR_ADDRESS) & 0xFF) & (BME680_RSERROR_MASK & 0xFF)) / 16;

        setTemperatureOversample(OVERSAMPLING_SKIPPED);
        setHumidityOversample(OVERSAMPLING_SKIPPED);
        setPressureOversample(OVERSAMPLING_SKIPPED);
        setFilter(FILTER_SIZE_NONE);

        setGasStatus(DISABLE_GAS);
    }

    // Initiate a soft reset
    private void softReset() throws IOException {
        if (device == null) {
            throw new IllegalStateException("I2C device not open");
        }

        device.writeRegByte(BME680_REGISTER_SOFT_RESET, (byte) BME680_COMMAND_SOFT_RESET);

        SystemClock.sleep(BEE680_RESET_PERIOD_MILLISECONDS);
    }

    // Set power mode
    public void setPowerMode(@Mode final int value) throws IOException {
        if (device == null) {
            throw new IllegalStateException("I2C device not open");
        }

        setRegByte(BME680_CONFIG_T_P_MODE_ADDRESS, BME680_MODE_MASK, MODE_POSITION, value);

        this.powerMode = value;

        while (this.powerMode != getPowerMode()) {
            SystemClock.sleep(BME680_POLL_PERIOD_MILLISECONDS);
        }
    }

    // Get power mode
    public int getPowerMode() throws IOException {
        if (device == null) {
            throw new IllegalStateException("I2C device not open");
        }

        this.powerMode = device.readRegByte(BME680_CONFIG_T_P_MODE_ADDRESS) & BME680_MODE_MASK;

        return this.powerMode;
    }

    // Read calibration array
    private byte[] readCalibrationData() throws IOException {
        if (device == null) {
            throw new IllegalStateException("I2C device not open");
        }

        final byte[] mCalibrationDataPart1 = new byte[BME680_COEFFICIENT_ADDRESS1_LEN];
        final byte[] mCalibrationDataPart2 = new byte[BME680_COEFFICIENT_ADDRESS2_LEN];
        final byte[] mCalibrationData = new byte[BME680_COEFFICIENT_ADDRESS1_LEN + BME680_COEFFICIENT_ADDRESS2_LEN];
        device.readRegBuffer(BME680_COEFFICIENT_ADDRESS1, mCalibrationDataPart1, BME680_COEFFICIENT_ADDRESS1_LEN);
        device.readRegBuffer(BME680_COEFFICIENT_ADDRESS2, mCalibrationDataPart2, BME680_COEFFICIENT_ADDRESS2_LEN);

        System.arraycopy(mCalibrationDataPart1, 0, mCalibrationData, 0, mCalibrationDataPart1.length);
        System.arraycopy(mCalibrationDataPart2, 0, mCalibrationData, mCalibrationDataPart1.length, mCalibrationDataPart2.length);

        return mCalibrationData;
    }

    // Set temperature oversampling
    // A higher oversampling value means more stable sensor readings, with less noise and jitter.
    // However each step of oversampling adds about 2ms to the latency, causing a slower response time to fast transients.
    public void setTemperatureOversample(@Oversampling final int value) throws IOException {
        if (device == null) {
            throw new IllegalStateException("I2C device not open");
        }

        setRegByte(BME680_CONFIG_T_P_MODE_ADDRESS, BME680_OVERSAMPLING_TEMPERATURE_MASK, OVERSAMPLING_TEMPERATURE_POSITION, value);

        sensorSettings.oversamplingTemperature = value;
    }

    //  Get temperature oversampling
    public int getTemperatureOversample() throws IOException {
        if (device == null) {
            throw new IllegalStateException("I2C device not open");
        }

        return (device.readRegByte(BME680_CONFIG_T_P_MODE_ADDRESS) & BME680_OVERSAMPLING_TEMPERATURE_MASK) >> OVERSAMPLING_TEMPERATURE_POSITION;
    }

    // Set humidity oversampling
    // A higher oversampling value means more stable sensor readings, with less noise and jitter.
    // However each step of oversampling adds about 2ms to the latency, causing a slower response time to fast transients.
    public void setHumidityOversample(@Oversampling final int value) throws IOException {
        if (device == null) {
            throw new IllegalStateException("I2C device not open");
        }

        setRegByte(BME680_CONFIG_OS_H_ADDRESS, BME680_OVERSAMPLING_HUMIDITY_MASK, OVERSAMPLING_HUMIDITY_POSITION, value);

        sensorSettings.oversamplingHumidity = value;
    }

    //  Get humidity oversampling
    @SuppressWarnings("PointlessBitwiseExpression")
    public int getHumidityOversample() throws IOException {
        if (device == null) {
            throw new IllegalStateException("I2C device not open");
        }

        return (device.readRegByte(BME680_CONFIG_OS_H_ADDRESS) & BME680_OVERSAMPLING_HUMIDITY_MASK) >> OVERSAMPLING_HUMIDITY_POSITION;
    }

    // Set pressure oversampling
    // A higher oversampling value means more stable sensor readings, with less noise and jitter.
    // However each step of oversampling adds about 2ms to the latency,
    // causing a slower response time to fast transients.
    public void setPressureOversample(@Oversampling final int value) throws IOException {
        if (device == null) {
            throw new IllegalStateException("I2C device not open");
        }

        setRegByte(BME680_CONFIG_T_P_MODE_ADDRESS, BME680_OVERSAMPLING_PRESSURE_MASK, OVERSAMPLING_PRESSURE_POSITION, value);

        sensorSettings.oversamplingPressure = value;
    }

    //  Get pressure oversampling
    public int getPressureOversample() throws IOException {
        if (device == null) {
            throw new IllegalStateException("I2C device not open");
        }

        return (device.readRegByte(BME680_CONFIG_T_P_MODE_ADDRESS) & BME680_OVERSAMPLING_PRESSURE_MASK) >> OVERSAMPLING_PRESSURE_POSITION;
    }

    // Set IIR filter size
    // Optionally remove short term fluctuations from the temperature and pressure readings,
    // increasing their resolution but reducing their bandwidth.
    // Enabling the IIR filter does not slow down the time a reading takes,
    // but will slow down the BME680s response to changes in temperature and pressure.
    // When the IIR filter is enabled, the temperature and pressure resolution is effectively 20bit.
    // When it is disabled, it is 16bit + oversampling-1 bits.
    public void setFilter(@Filter final int value) throws IOException {
        if (device == null) {
            throw new IllegalStateException("I2C device not open");
        }

        setRegByte(BME680_CONFIG_ODR_FILTER_ADDRESS, BME680_FILTER_MASK, FILTER_POSITION, value);

        sensorSettings.filter = value;
    }

    //  Get IIR filter size
    public int getFilter() throws IOException {
        if (device == null) {
            throw new IllegalStateException("I2C device not open");
        }

        return (device.readRegByte(BME680_CONFIG_ODR_FILTER_ADDRESS) & BME680_FILTER_MASK) >> FILTER_POSITION;
    }

    // Set current gas sensor conversion profile: 0 to 9. Select one of the 10 configured heating durations/set points.
    @SuppressWarnings("PointlessBitwiseExpression")
    public void selectGasHeaterProfile(@HeaterProfile final int value) throws IOException {
        if (device == null) {
            throw new IllegalStateException("I2C device not open");
        }
        if (value > PROFILE_9 || value < PROFILE_0) {
            throw new IllegalStateException(String.format(Locale.getDefault(), "Profile '%d should be between %d and %d", value, PROFILE_0, PROFILE_9));
        }

        setRegByte(BME680_CONFIG_ODR_RUN_GAS_NBC_ADDRESS, BME680_NBCONVERSION_MASK, NBCONVERSION_POSITION, value);

        gasSettings.nbConversion = value;
    }

    // Get gas sensor conversion profile: 0 to 9
    public int getGasHeaterProfile() throws IOException {
        if (device == null) {
            throw new IllegalStateException("I2C device not open");
        }

        return device.readRegByte(BME680_CONFIG_ODR_RUN_GAS_NBC_ADDRESS) & BME680_NBCONVERSION_MASK;
    }

    // Set temperature and duration of gas sensor heater
    // Target temperature in degrees celsius, between 200 and 400
    // Target duration in milliseconds, between 1 and 4032
    // Target profile, between 0 and 9
    public void setGasHeaterProfile(@HeaterProfile final int profile, final int temperature, final int duration) throws IOException {
        if (profile > PROFILE_9 || profile < PROFILE_0) {
            throw new IllegalStateException(String.format(Locale.getDefault(), "Profile '%d should be between %d and %d", profile, PROFILE_0, PROFILE_9));
        }

        setGasHeaterTemperature(profile, temperature);
        setGasHeaterDuration(profile, duration);
    }

    // Set gas sensor heater temperature
    // Target temperature in degrees celsius, between 200 and 400
    // When setting a profile other than 0, make sure to select it with selectGasHeaterProfile.
    public void setGasHeaterTemperature(@HeaterProfile final int profile, final int value) throws IOException {
        if (device == null) {
            throw new IllegalStateException("I2C device not open");
        }
        if (profile > PROFILE_9 || profile < PROFILE_0) {
            throw new IllegalStateException(String.format(Locale.getDefault(), "Profile '%d should be between %d and %d", value, PROFILE_0, PROFILE_9));
        }

        device.writeRegByte(BME680_RESISTANCE_HEAT0_ADDRESS + profile, (byte) calculateHeaterResistance(value));

        gasSettings.heaterTemperature = value;
    }

    // Set gas sensor heater duration
    // Heating durations between 1 ms and 4032 ms can be configured.
    // Approximately 20 - 30 ms are necessary for the heater to reach the intended target temperature.
    // Heating duration in milliseconds.
    // When setting a profile other than 0, make sure to select it with selectGasHeaterProfile.
    public void setGasHeaterDuration(@HeaterProfile final int profile, @HeaterDuration final int value) throws IOException {
        if (device == null) {
            throw new IllegalStateException("I2C device not open");
        }
        if (profile > PROFILE_9 || profile < PROFILE_0) {
            throw new IllegalStateException(String.format(Locale.getDefault(), "Profile '%d should be between %d and %d", value, PROFILE_0, PROFILE_9));
        }

        final int calculatedDuration = calculateHeaterDuration(value);
        device.writeRegByte(BME680_GAS_WAIT0_ADDRESS + profile, (byte) calculatedDuration);

        gasSettings.heaterDuration = calculatedDuration;
    }

    public int getProfileDuration() throws IOException {
        int duration = 0;
        // Calculate oversample measurement cycles
        final int[] oversamplingToCycles = {0, 1, 2, 4, 8, 16};

        int cycles = 0;

        if (sensorSettings.oversamplingTemperature != OVERSAMPLING_SKIPPED) {
            cycles += oversamplingToCycles[sensorSettings.oversamplingTemperature];
        }
        if (sensorSettings.oversamplingPressure != OVERSAMPLING_SKIPPED) {
            cycles += oversamplingToCycles[sensorSettings.oversamplingPressure];
        }
        if (sensorSettings.oversamplingHumidity != OVERSAMPLING_SKIPPED) {
            cycles += oversamplingToCycles[sensorSettings.oversamplingHumidity];
        }

        /// Temperature, pressure and humidity measurement duration calculated in microseconds [us]
        int newDuration = cycles * 1963;
        newDuration += (477 * 4); // Temperature, pressure and humidity switching duration
        newDuration += (477 * 5); // Gas measurement duration
        newDuration += (500); // Get it to the closest whole number
        newDuration /= (1000); // Convert to milisecond [ms]
        newDuration += (1); // Wake up duration of 1ms

        duration = newDuration;

        // Get the gas duration only when the run gas is enabled
        if (gasSettings.runGas == ENABLE_GAS) {
            // The remaining time should be used for heating */
            duration += gasSettings.heaterDuration;
        }

        return duration;
    }

    // Enable/disable gas sensor
    public void setGasStatus(@GasMeasure final int value) throws IOException {
        if (device == null) {
            throw new IllegalStateException("I2C device not open");
        }

        setRegByte(BME680_CONFIG_ODR_RUN_GAS_NBC_ADDRESS, BME680_RUN_GAS_MASK, RUN_GAS_POSITION, value);

        gasSettings.runGas = value;
    }

    // Get the current gas status
    public int getGasStatus() throws IOException {
        if (device == null) {
            throw new IllegalStateException("I2C device not open");
        }

        return (device.readRegByte(BME680_CONFIG_ODR_RUN_GAS_NBC_ADDRESS) & BME680_RUN_GAS_MASK) >> RUN_GAS_POSITION;
    }

    public float readTemperature() throws IOException {
        getSensorData();

        return this.data.temperature;
    }

    public float readPressure() throws IOException {
        getSensorData();

        return this.data.pressure;
    }

    public float readHumidity() throws IOException {
        getSensorData();

        return this.data.humidity;
    }

    public float readGasResistance() throws IOException {
        getSensorData();

        return this.data.gasResistance;
    }

    public float readAirQuality() throws IOException {
        getSensorData();

        return this.data.airQualityScore;
    }

    // Get sensor data
    private void getSensorData() throws IOException {
        setPowerMode(MODE_FORCED);

        final byte status = device.readRegByte(BME680_FIELD0_ADDRESS);

        //if sensor has new data available
        if ((status & BME680_NEW_DATA_MASK) == 0) {

            data.status = status;

            final byte[] buffer = new byte[BME680_FIELD_LENGTH];
            device.readRegBuffer(BME680_FIELD0_ADDRESS, buffer, BME680_FIELD_LENGTH);

            data.status = (byte) (buffer[0] & BME680_NEW_DATA_MASK);
            data.gasIndex = (buffer[0] & BME680_GAS_INDEX_MASK);
            data.measureIndex = buffer[1];

            // read the raw data from the sensor
            final int temperature = ((buffer[5] & 0xff) << 12) | ((buffer[6] & 0xff) << 4) | ((buffer[7] & 0xff) >> 4);
            final int pressure = ((buffer[2] & 0xff) << 12) | ((buffer[3] & 0xff) << 4) | ((buffer[4] & 0xff) >> 4);
            final int humidity = (buffer[8] << 8) | (buffer[9] & 0xff);
            final int gas_resistance = ((buffer[13] & 0xff) << 2) | ((buffer[14] & 0xff) >> 6);
            final int gas_range = buffer[14] & BME680_GAS_RANGE_MASK;

            ambientTemperature = temperature;

            data.status |= buffer[14] & BME680_GASM_VALID_MASK;
            data.status |= buffer[14] & BME680_HEAT_STABLE_MASK;

            data.heaterStable = (data.status & BME680_HEAT_STABLE_MASK) > 0;

            data.temperature = compensateTemperature(temperature) / 100.0f;
            data.pressure = compensatePressure(pressure) / 100.0f;
            data.humidity = compensateHumidity(humidity) / 1000.0f;
            data.gasResistance = compensateGasResistance(gas_resistance, gas_range);
            data.airQualityScore = calculateAirQuality(gas_resistance, data.humidity);
        }
    }

    private int concatBytes(final int msb, final int lsb, final boolean isSigned) {
        if (isSigned) {
            return ((msb << 8) | lsb);
        } else {
            return (((msb & 0xFF) << 8) | (lsb & 0xFF));
        }
    }

    private int compensateTemperature(final int temperature) {
        int var1 = (temperature >> 3) - (calibration.temperature[0] << 1);
        int var2 = (var1 * calibration.temperature[1]) >> 11;
        int var3 = ((var1 >> 1) * (var1 >> 1)) >> 12;
        var3 = ((var3) * (calibration.temperature[2] << 4)) >> 14;
        temperatureFine = var2 + var3;
        return ((temperatureFine * 5) + 128) >> 8;
    }

    @SuppressWarnings({"ConstantConditions", "NumericOverflow"})
    private int compensatePressure(final int pressure) {
        int var1 = (temperatureFine >> 1) - 64000;
        int var2 = ((((var1 >> 2) * (var1 >> 2)) >> 11) * calibration.pressure[5]) >> 2;
        var2 = var2 + ((var1 * calibration.pressure[4]) << 1);
        var2 = (var2 >> 2) + (calibration.pressure[3] << 16);
        var1 = (((((var1 >> 2) * (var1 >> 2)) >> 13) * (calibration.pressure[2] << 5)) >> 3) + ((calibration.pressure[1] * var1) >> 1);
        var1 = var1 >> 18;
        var1 = ((32768 + var1) * calibration.pressure[0]) >> 15;
        int pressure_comp = 1048576 - pressure;
        pressure_comp = (pressure_comp - (var2 >> 12)) * 3125;
        final int var4 = (1 << 31);
        if (pressure_comp >= var4) {
            pressure_comp = ((pressure_comp / var1) << 1);
        } else {
            pressure_comp = ((pressure_comp << 1) / var1);
        }
        var1 = (calibration.pressure[8] * (((pressure_comp >> 3) * (pressure_comp >> 3)) >> 13)) >> 12;
        var2 = ((pressure_comp >> 2) * calibration.pressure[7]) >> 13;
        final int var3 = ((pressure_comp >> 8) * (pressure_comp >> 8) * (pressure_comp >> 8) * calibration.pressure[9]) >> 17;

        pressure_comp = pressure_comp + ((var1 + var2 + var3 + (calibration.pressure[6] << 7)) >> 4);

        return pressure_comp;
    }

    @SuppressWarnings("PointlessArithmeticExpression")
    private long compensateHumidity(final int humidity) {
        final int temp_scaled = ((temperatureFine * 5) + 128) >> 8;
        final int var1 = humidity - calibration.humidity[0] * 16 - (((temp_scaled * calibration.humidity[2]) / 100) >> 1);
        final int var2 = (calibration.humidity[1] * (((temp_scaled * calibration.humidity[3]) / 100) + (((temp_scaled * ((temp_scaled * calibration.humidity[4]) / 100)) >> 6) / 100) + (1 << 14))) >> 10;
        final int var3 = var1 * var2;
        int var4 = calibration.humidity[5] << 7;
        var4 = (var4 + ((temp_scaled * calibration.humidity[6]) / 100)) >> 4;
        final int var5 = ((var3 >> 14) * (var3 >> 14)) >> 10;
        final int var6 = (var4 * var5) >> 1;
        final int calc_hum = (((var3 + var6) >> 10) * 1000) >> 12;

        // Cap at 100%rH
        return Math.min(Math.max(calc_hum, 0), 100000);
    }

    private int compensateGasResistance(final int gas_resistance, final int gas_range) {
        final long var1 = (1340 + (5 * (long) errorRange)) * GAS_RANGE_LOOKUP_TABLE_1[gas_range] >> 16;
        final long var2 = ((((long) gas_resistance << 15) - (long) (16777216)) + var1);
        final long var3 = ((GAS_RANGE_LOOKUP_TABLE_2[gas_range] * var1) >> 9);

        return (int) ((var3 + (var2 >> 1)) / var2);
    }

    private float calculateAirQuality(final long gasResistance, final float humidity) {
        // Set the humidity baseline to 40%, an optimal indoor humidity.
        final float humidityBaseline = 40.0f;
        // This sets the balance between humidity and gas reading in the calculation of airQualityScore (25:75, humidity:gas)
        final float humidityWeighting = 0.25f;

        try {
            gasResistanceData.take();
            gasResistanceData.put(gasResistance);

            //Collect gas resistance burn-in values, then use the average of the last n values to set the upper limit for calculating gasBaseline.
            final int gasBaseline = Math.round(sumGasDataResistance(gasResistanceData) / (float) DATA_GAS_BURN_IN);

            final long gasOffset = gasBaseline - gasResistance;

            final float humidityOffset = humidity - humidityBaseline;

            // Calculate humidityScore as the distance from the humidityBaseline
            final float humidityScore;
            if (humidityOffset > 0) {
                humidityScore = (100.0f - humidityBaseline - humidityOffset) / (100.0f - humidityBaseline) * (humidityWeighting * 100.0f);
            } else {
                humidityScore = (humidityBaseline + humidityOffset) / humidityBaseline * (humidityWeighting * 100.0f);
            }

            // Calculate gasScore as the distance from the gasBaseline
            final float gasScore;
            if (gasOffset > 0) {
                gasScore = (gasResistance / gasBaseline) * (100.0f - (humidityWeighting * 100.0f));
            } else {
                gasScore = 100.0f - (humidityWeighting * 100.0f);
            }

            return humidityScore + gasScore;
        } catch (InterruptedException e) {
            Log.e(TAG, e.getMessage(), e);
            return data.airQualityScore;
        }
    }

    private int calculateHeaterResistance(final int temperature) {
        final int normalizedTemperature = Math.min(Math.max(temperature, 200), 400);

        final long var1 = ((ambientTemperature * calibration.gasHeater[2]) / 1000) * 256;
        final int var2 = (calibration.gasHeater[0] + 784) * (((((calibration.gasHeater[1] + 154009) * normalizedTemperature * 5) / 100) + 3276800) / 10);
        final long var3 = var1 + (var2 / 2);
        final long var4 = (var3 / (heaterResistanceRange + 4));
        final int var5 = (131 * heaterResistanceValue) + 65536;
        final long heater_res_x100 = ((var4 / var5) - 250) * 34;
        return (short) ((heater_res_x100 + 50) / 100);
    }

    private int calculateHeaterDuration(int duration) throws IOException {
        // Calculate oversample measurement cycles
        final int[] oversamplingToCycles = {0, 1, 2, 4, 8, 16};

        int cycles = 0;

        if (sensorSettings.oversamplingTemperature != OVERSAMPLING_SKIPPED) {
            cycles += oversamplingToCycles[sensorSettings.oversamplingTemperature];
        }
        if (sensorSettings.oversamplingPressure != OVERSAMPLING_SKIPPED) {
            cycles += oversamplingToCycles[sensorSettings.oversamplingPressure];
        }
        if (sensorSettings.oversamplingHumidity != OVERSAMPLING_SKIPPED) {
            cycles += oversamplingToCycles[sensorSettings.oversamplingHumidity];
        }

        /// TPH measurement duration calculated in microseconds [us]
        int newDuration = cycles * 1963;
        newDuration += (477 * 4); // TPH switching duration
        newDuration += (477 * 5); // Gas measurement duration
        newDuration += (500); // Get it to the closest whole number
        newDuration /= (1000); // Convert to milisecond [ms]
        newDuration += (1); // Wake up duration of 1ms

        // The remaining time should be used for heating
        return (duration - newDuration);
    }

    private void prefillGasDataResistance() {
        for (int i = 0; i < DATA_GAS_BURN_IN; i++) {
            gasResistanceData.add(0L);
        }
    }

    private long sumGasDataResistance(@NonNull final LinkedBlockingQueue<Long> queue) {
        long sum = 0;

        for (int i = 0; i < DATA_GAS_BURN_IN; i++) {
            final long n = queue.remove();
            sum += n;
            queue.add(n);
        }

        return sum;
    }

    private String bytesToHex(final byte[] bytes) {
        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private void setRegByte(final int address, final int mask, final int position, final int value) throws IOException {
        if (device == null) {
            throw new IllegalStateException("I2C device not open");
        }

        byte regCtrl = device.readRegByte(address);

        regCtrl &= ~mask;
        regCtrl |= (value << position);

        device.writeRegByte(address, regCtrl);
    }
}
