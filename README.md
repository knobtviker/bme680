[ ![Download](https://api.bintray.com/packages/knobtviker/maven/bme680/images/download.svg) ](https://bintray.com/knobtviker/maven/bme680/_latestVersion)

Bosch BME680 driver for Android Things
======================================

This driver supports Bosch [BME680](https://www.bosch-sensortec.com/bst/products/all_products/bme680) environmental sensors.

How to use the driver
---------------------

### Gradle dependency

To use the `bme680` driver, simply add the line below to your project's `build.gradle`,
where `<version>` matches the last version of the driver available on [jcenter][jcenter].

```
dependencies {
    implementation 'com.knobtviker.android.things.contrib.community.driver:bme680:<version>'
}
```

### Sample usage

```java
import com.knobtviker.android.things.contrib.community.driver.bme680.Bme680;

// Access the environmental sensor:

Bme680 bme680;

try {
    bme680 = new Bme680(i2cBusName);
    // Configure driver oversampling for temperature, humidity or pressure,
    // threshold filter or gas status settings according to your use case
    bme680.setTemperatureOversampling(Bme680.OVERSAMPLING_1X);
    // Ensure the driver is powered and not sleeping before trying to read from it
    bme680.setMode(Bme680.MODE_NORMAL);
} catch (IOException e) {
    // couldn't configure the device...
}

// Read the current data:
try {
    float temperature = bme680.readTemperature();
    float humidty = bme680.readHumidity();
    float pressure = bme680.readPressure();

    // There are other low level air quality methods exposed
    float airQuality = bme680.readAirQuality();
} catch (IOException e) {
    // error reading temperature
}

// Close the environmental sensor when finished:

try {
    // If nothing else needs to read sensor values, consider calling setMode(Bme680.MODE_SLEEP)
    bme680.close();
} catch (IOException e) {
    // error closing sensor
}
```

If you need to read sensor values continuously, you can register the Bme680 with the system and
listen for sensor values using the [Sensor APIs][sensors]:
```java
SensorManager mSensorManager = getSystemService(Context.SENSOR_SERVICE);
SensorEventListener mListener = ...;
Bme680SensorDriver mSensorDriver;

mSensorManager.registerDynamicSensorCallback(new SensorManager.DynamicSensorCallback() {
    @Override
    public void onDynamicSensorConnected(Sensor sensor) {
        if (sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            mSensorManager.registerListener(mListener, sensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        // Indoor air quality sensors are non-standard on Android.
        // Custom private base type has been included
        if (sensor.getType() == Sensor.TYPE_DEVICE_PRIVATE_BASE) {
            // If you have multiple private based sensors catch "android.sensor.indoor_air_quality"
            if (event.sensor.getStringType().equals(Bme680.CHIP_SENSOR_TYPE_IAQ)) {
                mSensorManager.registerListener(mListener, sensor,
                        SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }
});

try {
    mSensorDriver = new Bme680SensorDriver(i2cBusName);
    mSensorDriver.registerTemperatureSensor();
    mSensorDriver.registerHumiditySensor();
    mSensorDriver.registerPressureSensor();
    mSensorDriver.registerGasSensor();

    // Configure temperature offset curve if data looks sligthly off
    // mSensorDriver.setTemperatureOffset(-1);
} catch (IOException e) {
    // Error configuring sensor
}

// Unregister and close the driver when finished:

mSensorManager.unregisterListener(mListener);
mSensorDriver.unregisterTemperatureSensor();
mSensorDriver.unregisterPressureSensor();
mSensorDriver.unregisterHumiditySensor();
mSensorDriver.unregisterGasSensor();
try {
    mSensorDriver.close();
} catch (IOException e) {
    // error closing sensor
}
```
