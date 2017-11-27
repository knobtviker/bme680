package com.knobtviker.android.things.contrib.driver.bme680;

/**
 * Created by bojan on 16/11/2017.
 */

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.TimeUnit;

/**
 * Driver for the Bosch BME 680 sensor.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class BME680 implements AutoCloseable {
    private static final String TAG = BME680.class.getSimpleName();

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

    // Registers
    private static final int BME680_REG_ID = 0xD0;
    private static final int BME680_REG_SOFT_RESET = 0xe0;

    // Sensor configuration registers
    private static final int BME680_CONF_HEAT_CTRL_ADDR = 0x70;
    private static final int BME680_CONF_ODR_RUN_GAS_NBC_ADDR = 0x71;
    private static final int BME680_CONF_OS_H_ADDR = 0x72;
    private static final int BME680_MEM_PAGE_ADDR = 0xf3;
    private static final int BME680_CONF_T_P_MODE_ADDR = 0x74;
    private static final int BME680_CONF_ODR_FILT_ADDR = 0x75;

    // field_x related defines
    private static final int BME680_FIELD0_ADDR = 0x1d;
    private static final int BME680_FIELD_LENGTH = 15;
    private static final int BME680_FIELD_ADDR_OFFSET = 17;

    // Commands
    private static final int BME680_CMD_SOFT_RESET = 0xb6;

    // BME680 coefficients related defines
    private static final int COEFF_ADDR1_LEN = 25;
    private static final int COEFF_ADDR2_LEN = 16;

    // Coefficient's address
    private static final int COEFF_ADDR1 = 0x89;
    private static final int COEFF_ADDR2 = 0xe1;

    // Other coefficient's address
    private static final int BME680_ADDR_RES_HEAT_VAL_ADDR = 0x00;
    private static final int BME680_ADDR_RES_HEAT_RANGE_ADDR = 0x02;
    private static final int BME680_ADDR_RANGE_SW_ERR_ADDR = 0x04;
    private static final int BME680_ADDR_SENS_CONF_START = 0x5A;
    private static final int BME680_ADDR_GAS_CONF_START = 0x64;

    // Mask definitions
    private static final int BME680_GAS_MEAS_MSK = 0x30;
    private static final int BME680_NBCONV_MSK = 0X0F;
    private static final int BME680_FILTER_MSK = 0X1C;
    private static final int BME680_OST_MSK = 0XE0;
    private static final int BME680_OSP_MSK = 0X1C;
    private static final int BME680_OSH_MSK = 0X07;
    private static final int BME680_HCTRL_MSK = 0x08;
    private static final int BME680_RUN_GAS_MSK = 0x10;
    private static final int BME680_MODE_MSK = 0x03;
    private static final int BME680_RHRANGE_MSK = 0x30;
    private static final int BME680_RSERROR_MSK = 0xf0;
    private static final int BME680_NEW_DATA_MSK = 0x80;
    private static final int BME680_GAS_INDEX_MSK = 0x0f;
    private static final int BME680_GAS_RANGE_MSK = 0x0f;
    private static final int BME680_GASM_VALID_MSK = 0x20;
    private static final int BME680_HEAT_STAB_MSK = 0x10;
    private static final int BME680_MEM_PAGE_MSK = 0x10;
    private static final int BME680_SPI_RD_MSK = 0x80;
    private static final int BME680_SPI_WR_MSK = 0x7f;
    private static final int BME680_BIT_H1_DATA_MSK = 0x0F;

    // Bit position definitions for sensor settings
    private static final int GAS_MEAS_POS = 4;
    private static final int FILTER_POS = 2;
    private static final int OST_POS = 5;
    private static final int OSP_POS = 2;
    private static final int OSH_POS = 0;
    private static final int RUN_GAS_POS = 4;
    private static final int MODE_POS = 0;
    private static final int NBCONV_POS = 0;

    // Array Index to Field data mapping for Calibration Data
    private static final int BME680_T2_LSB_REG = 1;
    private static final int BME680_T2_MSB_REG = 2;
    private static final int BME680_T3_REG = 3;
    private static final int BME680_P1_LSB_REG = 5;
    private static final int BME680_P1_MSB_REG = 6;
    private static final int BME680_P2_LSB_REG = 7;
    private static final int BME680_P2_MSB_REG = 8;
    private static final int BME680_P3_REG = 9;
    private static final int BME680_P4_LSB_REG = 11;
    private static final int BME680_P4_MSB_REG = 12;
    private static final int BME680_P5_LSB_REG = 13;
    private static final int BME680_P5_MSB_REG = 14;
    private static final int BME680_P7_REG = 15;
    private static final int BME680_P6_REG = 16;
    private static final int BME680_P8_LSB_REG = 19;
    private static final int BME680_P8_MSB_REG = 20;
    private static final int BME680_P9_LSB_REG = 21;
    private static final int BME680_P9_MSB_REG = 22;
    private static final int BME680_P10_REG = 23;
    private static final int BME680_H2_MSB_REG = 25;
    private static final int BME680_H2_LSB_REG = 26;
    private static final int BME680_H1_LSB_REG = 26;
    private static final int BME680_H1_MSB_REG = 27;
    private static final int BME680_H3_REG = 28;
    private static final int BME680_H4_REG = 29;
    private static final int BME680_H5_REG = 30;
    private static final int BME680_H6_REG = 31;
    private static final int BME680_H7_REG = 32;
    private static final int BME680_T1_LSB_REG = 33;
    private static final int BME680_T1_MSB_REG = 34;
    private static final int BME680_GH2_LSB_REG = 35;
    private static final int BME680_GH2_MSB_REG = 36;
    private static final int BME680_GH1_REG = 37;
    private static final int BME680_GH3_REG = 38;

    private static final int BME680_HUM_REG_SHIFT_VAL = 4;
    private static final int BME680_RESET_PERIOD_MILISECONDS = 10;
    private static final int BME680_POLL_PERIOD_MILISECONDS = 10;

    private I2cDevice mDevice;
    private final int[] mTempCalibrationData = new int[3];
    private final int[] mPressureCalibrationData = new int[10];
    private final int[] mHumidityCalibrationData = new int[7];
    private final int[] mGasHeaterCalibrationData = new int[3];
    private int mHeaterResistanceRange;
    private int mHeaterResistanceValue;
    private int mErrorRange;

    // Look up tables for the possible gas range values
    final long lookupTable1[] = {2147483647L, 2147483647L, 2147483647L, 2147483647L,
        2147483647L, 2126008810L, 2147483647L, 2130303777L, 2147483647L,
        2147483647L, 2143188679L, 2136746228L, 2147483647L, 2126008810L,
        2147483647L, 2147483647L};

    final long lookupTable2[] = {4096000000L, 2048000000L, 1024000000L, 512000000L,
        255744255L, 127110228L, 64000000L, 32258064L,
        16016016L, 8000000L, 4000000L, 2000000L,
        1000000L, 500000L, 250000L, 125000L};

    private boolean mEnabled = false;
    private int mChipId;
    private int mPowerMode;
    private final GasSettings gasSettings;
    private final SensorSettings sensorSettings;
    private int ambientTemperature;

    /**
     * Create a new BME680 sensor driver connected on the given bus.
     *
     * @param bus I2C bus the sensor is connected to.
     * @throws IOException
     */
    public BME680(@NonNull final String bus) throws IOException {
        this(bus, DEFAULT_I2C_ADDRESS);
    }

    /**
     * Create a new BME680 sensor driver connected on the given bus and address.
     *
     * @param bus     I2C bus the sensor is connected to.
     * @param address I2C address of the sensor.
     * @throws IOException
     */
    public BME680(@NonNull final String bus, final int address) throws IOException {
        final PeripheralManagerService pioService = new PeripheralManagerService();
        final I2cDevice device = pioService.openI2cDevice(bus, address);
        sensorSettings= new SensorSettings();
        gasSettings = new GasSettings();
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
    /*package*/  BME680(I2cDevice device) throws IOException {
        sensorSettings = new SensorSettings();
        gasSettings = new GasSettings();
        connect(device);
    }

    /**
     * Close the driver and the underlying device.
     */
    @Override
    public void close() throws IOException {
        if (mDevice != null) {
            try {
                mDevice.close();
            } finally {
                mDevice = null;
            }
        }
    }

    private void connect(I2cDevice device) throws IOException {
        mDevice = device;

        mChipId = mDevice.readRegByte(BME680_REG_ID);

        softReset();

        setPowerMode(MODE_SLEEP);

        // Read calibration data in 2 parts and concat them into 1 array
        final byte[] mCalibrationArray = calibrate();

        // Read temperature calibration data (3 words)
        mTempCalibrationData[0] = concatBytes(mCalibrationArray[BME680_T1_MSB_REG], mCalibrationArray[BME680_T1_LSB_REG]) & 0xffff;
        mTempCalibrationData[1] = concatBytes(mCalibrationArray[BME680_T2_MSB_REG], mCalibrationArray[BME680_T2_LSB_REG]);
        mTempCalibrationData[2] = (short) mCalibrationArray[BME680_T3_REG];

        // Read pressure calibration data (10 words)
        mPressureCalibrationData[0] = concatBytes(mCalibrationArray[BME680_P1_MSB_REG], mCalibrationArray[BME680_P1_LSB_REG]) & 0xffff;
        mPressureCalibrationData[1] = concatBytes(mCalibrationArray[BME680_P2_MSB_REG], mCalibrationArray[BME680_P2_LSB_REG]);
        mPressureCalibrationData[2] = (short) mCalibrationArray[BME680_P3_REG];
        mPressureCalibrationData[3] = concatBytes(mCalibrationArray[BME680_P4_MSB_REG], mCalibrationArray[BME680_P4_LSB_REG]);
        mPressureCalibrationData[4] = concatBytes(mCalibrationArray[BME680_P5_MSB_REG], mCalibrationArray[BME680_P5_LSB_REG]);
        mPressureCalibrationData[5] = (short) mCalibrationArray[BME680_P6_REG];
        mPressureCalibrationData[6] = (short) mCalibrationArray[BME680_P7_REG];
        mPressureCalibrationData[7] = concatBytes(mCalibrationArray[BME680_P8_MSB_REG], mCalibrationArray[BME680_P8_LSB_REG]);
        mPressureCalibrationData[8] = concatBytes(mCalibrationArray[BME680_P9_MSB_REG], mCalibrationArray[BME680_P9_LSB_REG]);
        mPressureCalibrationData[9] = (short) mCalibrationArray[BME680_P10_REG]; //this is really uint8_t

        // Read humidity calibration data (7 words)
        mHumidityCalibrationData[0] = (((mCalibrationArray[BME680_H1_MSB_REG] & 0xffff) << BME680_HUM_REG_SHIFT_VAL) | (mCalibrationArray[BME680_H1_LSB_REG] & BME680_BIT_H1_DATA_MSK)) & 0xffff;
        mHumidityCalibrationData[1] = (((mCalibrationArray[BME680_H2_MSB_REG] & 0xffff) << BME680_HUM_REG_SHIFT_VAL) | (mCalibrationArray[BME680_H2_LSB_REG] >> BME680_HUM_REG_SHIFT_VAL)) & 0xffff;
        mHumidityCalibrationData[2] = (short) mCalibrationArray[BME680_H3_REG];
        mHumidityCalibrationData[3] = (short) mCalibrationArray[BME680_H4_REG];
        mHumidityCalibrationData[4] = (short) mCalibrationArray[BME680_H5_REG];
        mHumidityCalibrationData[5] = (short) mCalibrationArray[BME680_H6_REG]; //this is really uint8_t
        mHumidityCalibrationData[6] = (short) mCalibrationArray[BME680_H7_REG];

        // Read gas heater calibration data (3 words)
        mGasHeaterCalibrationData[0] = (short) mCalibrationArray[BME680_GH1_REG];
        mGasHeaterCalibrationData[1] = concatBytes(mCalibrationArray[BME680_GH2_MSB_REG], mCalibrationArray[BME680_GH2_LSB_REG]);
        mGasHeaterCalibrationData[2] = (short) mCalibrationArray[BME680_GH3_REG];

        // Read other heater calibration data
        mHeaterResistanceRange = (short) ((mDevice.readRegByte(BME680_ADDR_RES_HEAT_RANGE_ADDR) & BME680_RHRANGE_MSK) / 16);
        mHeaterResistanceValue = (short) mDevice.readRegByte(BME680_ADDR_RES_HEAT_VAL_ADDR);
        mErrorRange = ((short) mDevice.readRegByte(BME680_ADDR_RANGE_SW_ERR_ADDR) & (short) BME680_RSERROR_MSK) / 16;

        setTemperatureOversample(OVERSAMPLING_8X);
        setHumidityOversample(OVERSAMPLING_2X);
        setPressureOversample(OVERSAMPLING_4X);
        setFilter(FILTER_SIZE_3);
        setGasStatus(ENABLE_GAS);
    }

    // Initiate a soft reset
    private void softReset() throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        mDevice.writeRegByte(BME680_REG_SOFT_RESET, (byte) BME680_CMD_SOFT_RESET);
        try {
            TimeUnit.MILLISECONDS.sleep(BME680_RESET_PERIOD_MILISECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    // Set power mode
    @SuppressWarnings("PointlessBitwiseExpression")
    private void setPowerMode(@Mode final int mode) throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        int regCtrl = mDevice.readRegByte(BME680_CONF_T_P_MODE_ADDR) & 0xff;
        regCtrl &= ~BME680_MODE_MSK;
        regCtrl |= mode << MODE_POS;
        mDevice.writeRegByte(BME680_CONF_T_P_MODE_ADDR, (byte) (regCtrl));

        this.mPowerMode = mode;

        while (getPowerMode() != this.mPowerMode) {
            try {
                TimeUnit.MILLISECONDS.sleep(BME680_POLL_PERIOD_MILISECONDS);
            } catch (InterruptedException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }

    // Get power mode
    private int getPowerMode() throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        this.mPowerMode = mDevice.readRegByte(BME680_CONF_T_P_MODE_ADDR) & BME680_MODE_MSK;

        return this.mPowerMode;
    }

    // Read calibration array
    private byte[] calibrate() throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        final byte[] mCalibrationDataPart1 = new byte[COEFF_ADDR1_LEN];
        final byte[] mCalibrationDataPart2 = new byte[COEFF_ADDR2_LEN];
        mDevice.readRegBuffer(COEFF_ADDR1, mCalibrationDataPart1, COEFF_ADDR1_LEN);
        mDevice.readRegBuffer(COEFF_ADDR2, mCalibrationDataPart2, COEFF_ADDR2_LEN);
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(mCalibrationDataPart1);
        outputStream.write(mCalibrationDataPart2);

        return outputStream.toByteArray();
    }

    // Set temperature oversampling
    // A higher oversampling value means more stable sensor readings, with less noise and jitter.
    // However each step of oversampling adds about 2ms to the latency,
    // causing a slower response time to fast transients.
    private void setTemperatureOversample(@Oversampling final int value) throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        int regCtrl = mDevice.readRegByte(BME680_CONF_T_P_MODE_ADDR) & 0xff;
        regCtrl &= ~BME680_OST_MSK;
        regCtrl |= value << OST_POS;
        mDevice.writeRegByte(BME680_CONF_T_P_MODE_ADDR, (byte) (regCtrl));

        sensorSettings.oversamplingTemperature = value;
    }

    //  Get temperature oversampling
    private int getTemperatureOversample() throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        return (mDevice.readRegByte(BME680_CONF_T_P_MODE_ADDR) & BME680_OST_MSK) >> OST_POS;
    }

    // Set humidity oversampling
    // A higher oversampling value means more stable sensor readings, with less noise and jitter.
    // However each step of oversampling adds about 2ms to the latency,
    // causing a slower response time to fast transients.
    @SuppressWarnings("PointlessBitwiseExpression")
    private void setHumidityOversample(@Oversampling final int value) throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        int regCtrl = mDevice.readRegByte(BME680_CONF_OS_H_ADDR) & 0xff;
        regCtrl &= ~BME680_OSH_MSK;
        regCtrl |= value << OSH_POS;
        mDevice.writeRegByte(BME680_CONF_OS_H_ADDR, (byte) (regCtrl));

        sensorSettings.oversamplingHumidity = value;
    }

    //  Get humidity oversampling
    @SuppressWarnings("PointlessBitwiseExpression")
    private int getHumidityOversample() throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        return (mDevice.readRegByte(BME680_CONF_OS_H_ADDR) & BME680_OSH_MSK) >> OSH_POS;
    }

    // Set pressure oversampling
    // A higher oversampling value means more stable sensor readings, with less noise and jitter.
    // However each step of oversampling adds about 2ms to the latency,
    // causing a slower response time to fast transients.
    private void setPressureOversample(@Oversampling final int value) throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        int regCtrl = mDevice.readRegByte(BME680_CONF_T_P_MODE_ADDR) & 0xff;
        regCtrl &= ~BME680_OSP_MSK;
        regCtrl |= value << OSP_POS;
        mDevice.writeRegByte(BME680_CONF_T_P_MODE_ADDR, (byte) (regCtrl));

        sensorSettings.oversamplingPressure = value;
    }

    //  Get pressure oversampling
    private int getPressureOversample() throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        return (mDevice.readRegByte(BME680_CONF_T_P_MODE_ADDR) & BME680_OSP_MSK) >> OSP_POS;
    }

    // Set IIR filter size
    // Optionally remove short term fluctuations from the temperature and pressure readings,
    // increasing their resolution but reducing their bandwidth.
    // Enabling the IIR filter does not slow down the time a reading takes,
    // but will slow down the BME680s response to changes in temperature and pressure.
    // When the IIR filter is enabled, the temperature and pressure resolution is effectively 20bit.
    // When it is disabled, it is 16bit + oversampling-1 bits.
    private void setFilter(@Filter final int value) throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        int regCtrl = mDevice.readRegByte(BME680_CONF_ODR_FILT_ADDR) & 0xff;
        regCtrl &= ~BME680_FILTER_MSK;
        regCtrl |= value << FILTER_POS;
        mDevice.writeRegByte(BME680_CONF_ODR_FILT_ADDR, (byte) (regCtrl));

        sensorSettings.filter = value;
    }

    //  Get IIR filter size
    private int getFilter() throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        return (mDevice.readRegByte(BME680_CONF_ODR_FILT_ADDR) & BME680_FILTER_MSK) >> FILTER_POS;
    }

//    // Set current gas sensor conversion profile: 0 to 9. Select one of the 10 configured heating durations/set points.
    private void selectGasHeaterProfile(final int value) {
//        if value > NBCONV_MAX or value < NBCONV_MIN:
//    raise ValueError("Profile '{}' should be between {} and {}".format(value, NBCONV_MIN, NBCONV_MAX))
//
//    self.gas_settings.nb_conv = value
//      self._set_bits(CONF_ODR_RUN_GAS_NBC_ADDR, NBCONV_MSK, NBCONV_POS, value)
    }

    // Get gas sensor conversion profile: 0 to 9
    private int getGasHeaterProfile() throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        return mDevice.readRegByte(BME680_CONF_ODR_RUN_GAS_NBC_ADDR) & BME680_NBCONV_MSK;
    }

    // Enable/disable gas sensor
    private void setGasStatus(@GasMeasure final int value) throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        int regCtrl = mDevice.readRegByte(BME680_CONF_ODR_RUN_GAS_NBC_ADDR) & 0xff;
        regCtrl &= ~BME680_RUN_GAS_MSK;
        regCtrl |= value << RUN_GAS_POS;
        mDevice.writeRegByte(BME680_CONF_ODR_RUN_GAS_NBC_ADDR, (byte) (regCtrl));

        gasSettings.runGas = value;
    }

    // Get the current gas status
    private int getGasStatus() throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        return (mDevice.readRegByte(BME680_CONF_ODR_RUN_GAS_NBC_ADDR) & BME680_RUN_GAS_MSK) >> RUN_GAS_POS;
    }

    // Get sensor data
    private Data getSensorData() throws IOException {
        setPowerMode(MODE_FORCED);

        final Data data = new Data();

        int attempts = 10;
        do {
            final byte[] buffer = new byte[BME680_FIELD_LENGTH];
            mDevice.readRegBuffer(BME680_FIELD0_ADDR, buffer, BME680_FIELD_LENGTH);

            data.status = buffer[0] & BME680_NEW_DATA_MSK;
            data.gas_index = buffer[0] & BME680_GAS_INDEX_MSK;
            data.meas_index = buffer[1];

            // read the raw data from the sensor
            final int pressure = (buffer[2] << 12) | (buffer[3] << 4) | (buffer[4] >> 4);
            final int temperature = (buffer[5] << 12) | (buffer[6] << 4) | (buffer[7] >> 4);
            final int humidity = (buffer[8] << 8) | buffer[9];
            final int gas_resistance = (buffer[13] << 2) | (buffer[14] >> 6);
            final int gas_range = buffer[14] & BME680_GAS_RANGE_MSK;

            data.status |= buffer[14] & BME680_GASM_VALID_MSK;
            data.status |= buffer[14] & BME680_HEAT_STAB_MSK;

            data.heat_stable = (data.status & BME680_HEAT_STAB_MSK) > 0;

            if ((data.status & BME680_NEW_DATA_MSK) == 0) {
                data.temperature = compensateTemperature(temperature);
                data.pressure = compensatePressure(pressure, data.temperature);
                data.humidity = compensateHumidity(humidity, data.temperature);
                data.gas_resistance = compensateGasResistance(gas_resistance, gas_range);
                ambientTemperature = temperature;
                break;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(BME680_POLL_PERIOD_MILISECONDS);
            } catch (InterruptedException e) {
                Log.e(TAG, e.getMessage(), e);
            }

            attempts--;
        } while (attempts > 0);

        return data;
    }

    //TODO: Check if can be replaced with mDevice.readRegWord()
    private int concatBytes(final byte msb, final byte lsb) {
        return (msb << 8) | lsb;
    }

    private int compensateTemperature(final int temperature) {
        final int var1 = (temperature >> 3) - (mTempCalibrationData[0] << 1);
        final int var2 = (var1 * mTempCalibrationData[1]) >> 11;
        int var3 = ((var1 >> 1) * (var1 >> 1)) >> 12;
        var3 = ((var3) * (mTempCalibrationData[2] << 4)) >> 14;

        return ((((var2 + var3) * 5) + 128) >> 8);
    }

    @SuppressWarnings({"ConstantConditions", "NumericOverflow"})
    private int compensatePressure(final int pressure, final int temperature) {
        int var1 = ((temperature) >> 1) - 64000;
        int var2 = ((((var1 >> 2) * (var1 >> 2)) >> 11) * mPressureCalibrationData[5]) >> 2;
        var2 = var2 + ((var1 * mPressureCalibrationData[4]) << 1);
        var2 = (var2 >> 2) + (mPressureCalibrationData[3] << 16);
        var1 = (((((var1 >> 2) * (var1 >> 2)) >> 13) * ((mPressureCalibrationData[2] << 5)) >> 3) + ((mPressureCalibrationData[1] * var1) >> 1));
        var1 = var1 >> 18;

        var1 = ((32768 + var1) * mPressureCalibrationData[0]) >> 15;
        int calculatedPressure = 1048576 - pressure;
        calculatedPressure = ((calculatedPressure - (var2 >> 12)) * (3125));

        if (calculatedPressure >= (1 << 31)) {
            calculatedPressure = ((calculatedPressure / var1) << 1);
        } else {
            calculatedPressure = ((calculatedPressure << 1) / var1);
        }

        var1 = (mPressureCalibrationData[8] * (((calculatedPressure >> 3) * (calculatedPressure >> 3)) >> 13)) >> 12;
        var2 = ((calculatedPressure >> 2) * mPressureCalibrationData[7]) >> 13;
        int var3 = ((calculatedPressure >> 8) * (calculatedPressure >> 8) * (calculatedPressure >> 8) * mPressureCalibrationData[9]) >> 17;

        calculatedPressure = (calculatedPressure) + ((var1 + var2 + var3 + (mPressureCalibrationData[6] << 7)) >> 4);

        return calculatedPressure;
    }

    @SuppressWarnings("PointlessArithmeticExpression")
    private int compensateHumidity(final int humidity, final int temperature) {
        final int temp_scaled = ((temperature * 5) + 128) >> 8;
        final int var1 = (humidity - ((mHumidityCalibrationData[0] * 16))) - (((temp_scaled * mHumidityCalibrationData[2]) / (100)) >> 1);
        final int var2 = (mHumidityCalibrationData[1] * (((temp_scaled * mHumidityCalibrationData[3]) / (100)) + (((temp_scaled * ((temp_scaled * mHumidityCalibrationData[4]) / (100))) >> 6) / (100)) + (1 * 16384))) >> 10;
        final int var3 = var1 * var2;
        int var4 = mHumidityCalibrationData[5] << 7;
        var4 = ((var4) + ((temp_scaled * mHumidityCalibrationData[6]) / (100))) >> 4;
        final int var5 = ((var3 >> 14) * (var3 >> 14)) >> 10;
        final int var6 = (var4 * var5) >> 1;
        final int calc_hum = (((var3 + var6) >> 10) * (1000)) >> 12;

        return Math.min(Math.max(calc_hum, 0), 100000);
    }

    private int compensateGasResistance(final int gas_resistance, final int gas_range) {
        final long var1 = (1340 + (5 * (long) mErrorRange)) * lookupTable1[gas_range] >> 16;
        final long var2 = ((((long) gas_resistance << 15) - (long) (16777216)) + var1);
        final long var3 = ((lookupTable2[gas_range] * var1) >> 9);

        return (int) ((var3 + (var2 >> 1)) / var2);
    }

    private int calculateHeaterResistance(final int temperature) {
        final int normalizedTemperature = Math.min(Math.max(temperature, 200), 400);

        final int var1 = ((ambientTemperature * mGasHeaterCalibrationData[2]) / 1000) * 256;
        final int var2 = (mGasHeaterCalibrationData[2] + 784) * (((((mGasHeaterCalibrationData[1] + 154009) * normalizedTemperature * 5) / 100) + 3276800) / 10);
        final int var3 = var1 + (var2 / 2);
        final int var4 = (var3 / (mHeaterResistanceRange + 4));
        final int var5 = (131 * mHeaterResistanceValue) + 65536;
        final int heatr_res_x100 = ((var4 / var5) - 250) * 34;
        return (short) ((heatr_res_x100 + 50) / 100);
    }

    private int calculateHeaterDuration(int duration) {
        int factor = 0;
        short newDuration;

        if (duration >= 0xfc0) {
            newDuration = 0xff; /* Max duration*/
        } else {
            while (duration > 0x3F) {
                duration = duration / 4;
                factor += 1;
            }
            newDuration = (short) (duration + (factor * 64));
        }

        return newDuration;
    }
}
