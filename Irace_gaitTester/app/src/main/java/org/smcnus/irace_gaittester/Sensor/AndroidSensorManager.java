package org.smcnus.irace_gaittester.Sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;

import org.smcnus.irace_gaittester.Helpers.DateTime;
import org.smcnus.irace_gaittester.Models.SensorInstance;

public class AndroidSensorManager extends GaitSensorManager implements SensorEventListener{

    private static final String TAG             = AndroidSensorManager.class.getSimpleName();
    public static final String PREFIX_FILENAME  = TAG;

    private static final String ACCEL_THREAD        = TAG + "accel_thread";
    private static final String GYRO_THREAD         = TAG + "gyro_thread";
    private static final String COMPASS_THREAD      = TAG + "compass_thread";

    private static final String ACCEL_STORE_THREAD  = "accel_store_thread";
    private static final String GYRO_STORE_THREAD   = "gyro_store_thread";
    private static final String COMPASS_STORE_THREAD = "compass_store_thread";

    public static final int SENSOR_SAMPLING         = 10;

    private boolean isTimerRunning = false;

    private SensorManager sensorManager;
    private Sensor sensorAccelerometer;
    private Sensor sensorGyroscope;
    private Sensor sensorMagnometer;

    private HandlerThread accelThread;
    private Handler accelHandler;

    private HandlerThread gyroThread;
    private Handler gyroHandler;

    private HandlerThread compassThread;
    private Handler compassHandler;

    // sensor store
    private HandlerThread accelStoreThread;
    private Handler accelStoreHandler;
    private Runnable accelStoreRunnable;

    private HandlerThread gyroStoreThread;
    private Handler gyroStoreHandler;
    private Runnable gyroStoreRunnable;

    private HandlerThread compassStoreThread;
    private Handler compassStoreHandler;
    private Runnable compassStoreRunnable;

    public AndroidSensorManager(Context context) {
        super(context);
        initializeSensorManager();
    }

    @Override
    public void initializeSensorThreads() {
        initializeAccelerometerThread();
        initializeGyroscopeThread();
        initializeCompassThread();

        initializeSensorStoreThreads();
    }

    @Override
    public void startSensors() {
        this.isTimerRunning = true;
        registerSensors();

        startSensorStoreThreads();
    }

    @Override
    public void pauseSensors() {
        this.isTimerRunning = true;
        unregisterSensors();

        stopSensorStoreThreads();
    }

    @Override
    public void destroySensorThreads() {
        destroyAccelerometerThread();
        destroyGyroscopeThread();
        destroyCompassThread();

        destroySensorStoreThreads();
    }


    /*
    * callback methods
    * */

    @Override
    public void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();
        double[] values;

        switch(type) {
            case SensorInstance.ACCELEROMETER:
                values = getFilteredAccelValues(event);
                addAccelDataToStore(values);
                break;

            case SensorInstance.GYROSCOPE:
                values = getFilteredGyroValues(event);
                addGyroDataToStore(values);
                break;

            case SensorInstance.MAGNOMETER:
                values = getFilteredCompassValues(event);
                addCompassDataToStore(values);
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    /*
    * filtering
    * */

    private double[] getFilteredAccelValues(SensorEvent event) {
        float[] values = event.values;

        double accelX = values[0];
        double accelY = values[1];
        double accelZ = values[2];

        //TODO: filter and calibrate the accelerometer values

        double[] accels = {accelX, accelY, accelZ};

        return accels;
    }

    private double[] getFilteredGyroValues(SensorEvent event) {
        float[] values = event.values;

        double gyroX = values[0];
        double gyroY = values[1];
        double gyroZ = values[2];

        //TODO: filter and calibrate the accelerometer values

        double[] gyros = {gyroX, gyroY, gyroZ};

        return gyros;
    }

    private double[] getFilteredCompassValues(SensorEvent event) {
        float[] values = event.values;

        double compassX = values[0];
        double compassY = values[1];
        double compassZ = values[2];

        //TODO: filter and calibrate the accelerometer values

        double[] compass = {compassX, compassY, compassZ};

        return compass;
    }


    /*
    * adding to store
    * */

    private void addAccelDataToStore(final double[] values) {
        addSensorDataToStore(values, SensorInstance.ACCELEROMETER);
    }

    private void addGyroDataToStore(final double[] values) {
        addSensorDataToStore(values, SensorInstance.GYROSCOPE);
    }

    private void addCompassDataToStore(final double[] values) {
        addSensorDataToStore(values, SensorInstance.MAGNOMETER);
    }

    private void addSensorDataToStore(final double[] values, int type) {
        sensorStore.updateCurrentSensorData(values, type);
    }


    /*
    * initialize
    * */

    private void initializeSensorManager() {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        initializeSensors();
    }

    private void initializeSensors() {
        initializeAccelerometer();
        initializeGyroscope();
        initializeMagnometer();
    }

    private void initializeAccelerometer() {
        sensorAccelerometer = sensorManager.getDefaultSensor(SensorInstance.ACCELEROMETER);
    }

    private void initializeGyroscope() {
        sensorGyroscope = sensorManager.getDefaultSensor(SensorInstance.GYROSCOPE);
    }

    private void initializeMagnometer() {
        sensorMagnometer = sensorManager.getDefaultSensor(SensorInstance.MAGNOMETER);
    }


    /*
    * sensor threads
    * */

    private void initializeAccelerometerThread() {
        accelThread = new HandlerThread(ACCEL_THREAD);
        accelThread.start();
        accelHandler = new Handler(accelThread.getLooper());
    }

    private void initializeGyroscopeThread() {
        gyroThread = new HandlerThread(GYRO_THREAD);
        gyroThread.start();
        gyroHandler = new Handler(gyroThread.getLooper());
    }

    private void initializeCompassThread() {
        compassThread = new HandlerThread(COMPASS_THREAD);
        compassThread.start();
        compassHandler = new Handler(compassThread.getLooper());
    }


    /*
    * registering sensors
    * */

    private void registerSensors() {
        registerAccelerometer();
        registerGyroscope();
        registerMagnometer();
    }

    private void registerAccelerometer() {
        sensorManager.registerListener(this, sensorAccelerometer, 10, accelHandler);
    }

    private void registerGyroscope() {
        sensorManager.registerListener(this, sensorGyroscope, 10, gyroHandler);
    }

    private void registerMagnometer() {
        sensorManager.registerListener(this, sensorMagnometer, 10, compassHandler);
    }


    /*
    * unregistering sensors
    * */

    private void unregisterSensors() {
        sensorManager.unregisterListener(this);
    }


    /*
    * destory threads
    * */

    private void destroyAccelerometerThread() {
        accelThread.quit();
        accelHandler = null;
    }

    private void destroyGyroscopeThread() {
        gyroThread.quit();
        gyroHandler = null;
    }

    private void destroyCompassThread() {
        compassThread.quit();
        compassHandler = null;
    }

    /*
    * Sensor store handler
    * */

    private void initializeSensorStoreThreads() {
        initializeAccelStoreThread();
        initializeGyroStoreThread();
        initializeCompassStoreThread();
    }

    private void initializeAccelStoreThread() {
        initializeAccelStoreHandler();
        initializeAccelStoreRunnable();
    }

    private void initializeGyroStoreThread() {
        initializeGyroStoreHandler();
        initializeGyroStoreRunnable();
    }

    private void initializeCompassStoreThread() {
        initializeCompassStoreHandler();
        initializeCompassStoreRunnable();
    }

    private void initializeAccelStoreHandler() {
        accelStoreThread = new HandlerThread(ACCEL_STORE_THREAD);
        accelStoreThread.start();
        accelStoreHandler = new Handler(accelStoreThread.getLooper());
    }

    private void initializeAccelStoreRunnable() {
        accelStoreRunnable = new Runnable() {
            @Override
            public void run() {
                if(isTimerRunning) {
                    pushAccelerometerData();
                    accelStoreHandler.postDelayed(accelStoreRunnable, SENSOR_SAMPLING);
                }
            }
        };

    }

    private void initializeGyroStoreHandler() {
        gyroStoreThread = new HandlerThread(GYRO_STORE_THREAD);
        gyroStoreThread.start();
        gyroStoreHandler = new Handler(gyroStoreThread.getLooper());
    }

    private void initializeGyroStoreRunnable() {
        gyroStoreRunnable = new Runnable() {
            @Override
            public void run() {
                if(isTimerRunning) {
                    pushGyroscopeData();
                    gyroStoreHandler.postDelayed(gyroStoreRunnable, SENSOR_SAMPLING);
                }
            }
        };

    }

    private void initializeCompassStoreHandler() {
        compassStoreThread = new HandlerThread(COMPASS_STORE_THREAD);
        compassStoreThread.start();
        compassStoreHandler = new Handler(compassStoreThread.getLooper());
    }

    private void initializeCompassStoreRunnable() {
        compassStoreRunnable = new Runnable() {
            @Override
            public void run() {
                if(isTimerRunning) {
                    pushCompassData();
                    compassStoreHandler.postDelayed(compassStoreRunnable, SENSOR_SAMPLING);
                }
            }
        };

    }

    private void startSensorStoreThreads() {
        startAccelStoreThread();
        startGyroStoreThread();
        startCompassStoreThread();
    }

    private void startAccelStoreThread() {
        accelStoreHandler.postDelayed(accelStoreRunnable, SENSOR_SAMPLING);
    }

    private void startGyroStoreThread() {
        gyroStoreHandler.postDelayed(gyroStoreRunnable, SENSOR_SAMPLING);
    }

    private void startCompassStoreThread() {
        compassStoreHandler.postDelayed(compassStoreRunnable, SENSOR_SAMPLING);
    }

    private void stopSensorStoreThreads() {
        stopAccelStoreThread();
        stopGyroStoreThread();
        stopCompassStoreThread();
    }

    private void stopAccelStoreThread() {
        accelStoreHandler.removeCallbacks(accelStoreRunnable);
    }

    private void stopGyroStoreThread() {
        gyroStoreHandler.removeCallbacks(gyroStoreRunnable);
    }

    private void stopCompassStoreThread() {
        compassStoreHandler.removeCallbacks(compassStoreRunnable);
    }

    private void destroySensorStoreThreads() {
        destroyAccelStoreThread();
        destroyGyroStoreThread();
        destroyCompassStoreThread();
    }

    private void destroyAccelStoreThread() {
        accelStoreThread.quit();
        accelStoreHandler = null;
        accelStoreRunnable = null;
    }

    private void destroyGyroStoreThread() {
        gyroStoreThread.quit();
        gyroStoreHandler = null;
        gyroStoreRunnable = null;
    }

    private void destroyCompassStoreThread() {
        compassStoreThread.quit();
        compassStoreHandler = null;
        compassStoreRunnable = null;
    }

    private void pushAccelerometerData() {
        pushSensorData(SensorInstance.ACCELEROMETER);
    }

    private void pushGyroscopeData() {
        pushSensorData(SensorInstance.GYROSCOPE);
    }

    private void pushCompassData() {
        pushSensorData(SensorInstance.MAGNOMETER);
    }

    private void pushSensorData(int type) {
        double[] values = sensorStore.getCurrentSensorData(type);
        String timestamp = String.valueOf(DateTime.getIntegerCurrentTimestamp());

        if(values != null && values.length == 3) {
            SensorInstance currentData = new SensorInstance(0, 0, timestamp, 0, values[0], values[1],
                    values[2], type);

            sensorStore.addSensorData(currentData);
        }
    }

}
