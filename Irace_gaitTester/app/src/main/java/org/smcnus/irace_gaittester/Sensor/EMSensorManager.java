package org.smcnus.irace_gaittester.Sensor;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.ubhave.dataformatter.DataFormatter;
import com.ubhave.dataformatter.json.JSONFormatter;
import com.ubhave.datahandler.except.DataHandlerException;
import com.ubhave.sensormanager.ESException;
import com.ubhave.sensormanager.ESSensorManager;
import com.ubhave.sensormanager.config.pull.PullSensorConfig;
import com.ubhave.sensormanager.data.SensorData;
import com.ubhave.sensormanager.sensors.SensorUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smcnus.irace_gaittester.Models.SensorInstance;

public class EMSensorManager extends GaitSensorManager {

    private static final String TAG             = EMSensorManager.class.getSimpleName();
    public static final String PREFIX_FILENAME  = TAG;

    private static final String ACCEL_THREAD        = TAG + "accel_thread";
    private static final String GYRO_THREAD         = TAG + "gyro_thread";
    private static final String COMPASS_THREAD      = TAG + "compass_thread";

    private static final String X_AXIS      = "xAxis";
    private static final String Y_AXIS      = "yAxis";
    private static final String Z_AXIS      = "zAxis";
    private static final String TIMESTAMP   = "sensorTimeStamps";

    private ESSensorManager esSensorManager;

    private HandlerThread accelThread;
    private Handler accelHandler;
    private Runnable accelRunnable;

    private HandlerThread gyroThread;
    private Handler gyroHandler;
    private Runnable gyroRunnable;

    private HandlerThread compassThread;
    private Handler compassHandler;
    private Runnable compassRunnable;

    private boolean isTimerRunning = false;

    /*
    * Overridden methods
    * */

    public EMSensorManager(Context context) {
        super(context);
        initializeSensorManager();
    }

    @Override
    public void initializeSensorThreads() {
        initializeAccelerometerThread();
        initializeGyroscopeThread();
        initializeCompassThread();
    }

    @Override
    public void startSensors() {
        this.isTimerRunning = true;
        startAccelerometerSensor();
        startGyroscopeSensor();
        startCompassSensor();
    }

    @Override
    public void pauseSensors() {
        this.isTimerRunning = false;
    }

    @Override
    public void destroySensorThreads() {
        destroyAccelerometerThread();
        destroyGyroscopeThread();
        destroyCompassThread();
    }


    /*
    * initialization - config sensors
    * */

    private void initializeSensorManager() {
        try {
            esSensorManager = ESSensorManager.getSensorManager(context);
            configSensors();
        } catch (ESException e) {
            e.printStackTrace();
        }
    }

    private void configSensors() throws ESException{
        configAccelerometerSensor();
        configGyroSensor();
        configCompassSensor();
    }

    private void configAccelerometerSensor() throws ESException{
        esSensorManager.setSensorConfig(SensorUtils.SENSOR_TYPE_ACCELEROMETER,
                PullSensorConfig.SENSE_WINDOW_LENGTH_MILLIS, 100L);
        esSensorManager.setSensorConfig(SensorUtils.SENSOR_TYPE_ACCELEROMETER,
                PullSensorConfig.POST_SENSE_SLEEP_LENGTH_MILLIS, 10L);
    }

    private void configGyroSensor() throws ESException {
        esSensorManager.setSensorConfig(SensorUtils.SENSOR_TYPE_GYROSCOPE,
                PullSensorConfig.SENSE_WINDOW_LENGTH_MILLIS, 100L);
        esSensorManager.setSensorConfig(SensorUtils.SENSOR_TYPE_GYROSCOPE,
                PullSensorConfig.POST_SENSE_SLEEP_LENGTH_MILLIS, 10L);
    }

    private void configCompassSensor() throws ESException {
        esSensorManager.setSensorConfig(SensorUtils.SENSOR_TYPE_MAGNETIC_FIELD,
                PullSensorConfig.SENSE_WINDOW_LENGTH_MILLIS, 100L);
        esSensorManager.setSensorConfig(SensorUtils.SENSOR_TYPE_MAGNETIC_FIELD,
                PullSensorConfig.POST_SENSE_SLEEP_LENGTH_MILLIS, 10L);
    }


    /*
    * initiatization methods
    * */

    private void initializeAccelerometerThread() {
        initializeAccelerometerHandler();
        initializeAccelerometerRunnable();
    }

    private void initializeAccelerometerHandler() {
        accelThread = new HandlerThread(ACCEL_THREAD);
        accelThread.start();
        accelHandler = new Handler(accelThread.getLooper());
    }

    private void initializeAccelerometerRunnable() {
        accelRunnable = new Runnable() {
            @Override
            public void run() {
                if(isTimerRunning) {
                    storeSensorData(SensorUtils.SENSOR_TYPE_ACCELEROMETER);
                    accelHandler.post(accelRunnable);
                }
            }
        };
    }

    private void startAccelerometerSensor() {
        accelHandler.post(accelRunnable);
    }


    private void initializeGyroscopeThread() {
        initializeGyroscopeHandler();
        initializeGyroscopeRunnable();
    }

    private void initializeGyroscopeHandler() {
        gyroThread = new HandlerThread(GYRO_THREAD);
        gyroThread.start();
        gyroHandler = new Handler(gyroThread.getLooper());
    }

    private void initializeGyroscopeRunnable() {
        gyroRunnable = new Runnable() {
            @Override
            public void run() {
                if(isTimerRunning) {
                    storeSensorData(SensorUtils.SENSOR_TYPE_GYROSCOPE);
                    gyroHandler.post(gyroRunnable);
                }
            }
        };
    }

    private void startGyroscopeSensor() {
        gyroHandler.post(gyroRunnable);
    }

    private void initializeCompassThread() {
        initializeCompassHandler();
        initializeCompassRunnable();
    }

    private void initializeCompassHandler() {
        compassThread = new HandlerThread(COMPASS_THREAD);
        compassThread.start();
        compassHandler = new Handler(compassThread.getLooper());
    }

    private void initializeCompassRunnable() {
        compassRunnable = new Runnable() {
            @Override
            public void run() {
                if(isTimerRunning) {
                    storeSensorData(SensorUtils.SENSOR_TYPE_MAGNETIC_FIELD);
                    compassHandler.post(compassRunnable);
                }
            }
        };
    }

    private void startCompassSensor() {
        compassHandler.post(compassRunnable);
    }


    /*
    * destorying threads
    * */

    private void destroyAccelerometerThread() {
        accelThread.quit();
        accelHandler = null;
        accelRunnable = null;
    }

    private void destroyGyroscopeThread() {
        gyroThread.quit();
        gyroHandler = null;
        gyroRunnable = null;
    }

    private void destroyCompassThread() {
        compassThread.quit();
        compassHandler = null;
        compassRunnable = null;
    }


    /*
    * help methods
    * */

    private void storeSensorData(int sensorType){

        try {
            Log.d(TAG, "hello from the other side " + getDataFromSensor(sensorType).toString());

            JSONObject json = getDataFromSensor(sensorType);
            JSONArray dataX = json.getJSONArray(X_AXIS);
            JSONArray dataY = json.getJSONArray(Y_AXIS);
            JSONArray dataZ = json.getJSONArray(Z_AXIS);
            JSONArray timestamps = json.getJSONArray(TIMESTAMP);

            if(dataX.length() == dataY.length() && dataY.length() == dataZ.length() &&
                    dataZ.length() == timestamps.length() ) {
                for(int i=0; i<dataX.length(); i++) {
                    double x = (double) dataX.get(i);
                    double y = (double) dataY.get(i);
                    double z = (double) dataZ.get(i);
                    long timestamp = (long) timestamps.get(i);
                    int type = convertSensorType(sensorType);
                    SensorInstance instance = new SensorInstance(0, 0, String.valueOf(timestamp), 0,
                            x, y, z, type);

                    sensorStore.addSensorData(instance);
                }

            } else {
                Log.e(TAG, "JSON Data has wrong format " + json.toString());
            }


        } catch(ESException e ) {
            Log.e(TAG, e.getErrorCode() + " " + e.getMessage());

        } catch (DataHandlerException e) {
            Log.e(TAG, e.getErrorCode() + " " + e.getMessage());

        } catch (JSONException e) {
            Log.e(TAG,  "JSONException " + e.getMessage());
        }
    }

    private JSONObject getDataFromSensor(int sensorType) throws DataHandlerException, ESException {
        SensorData data = esSensorManager.getDataFromSensor(sensorType);
        JSONFormatter formatter = DataFormatter.getJSONFormatter(context, data.getSensorType());
        return formatter.toJSON(data);
    }

    private int convertSensorType(int sensorType) {
        int type = SensorInstance.ACCELEROMETER;
        switch(sensorType) {
            case SensorUtils.SENSOR_TYPE_ACCELEROMETER:
                type = SensorInstance.ACCELEROMETER;
                break;
            case SensorUtils.SENSOR_TYPE_GYROSCOPE:
                type = SensorInstance.GYROSCOPE;
                break;
            case SensorUtils.SENSOR_TYPE_MAGNETIC_FIELD:
                type = SensorInstance.MAGNOMETER;
        }

        return type;
    }
}
