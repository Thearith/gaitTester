package org.smcnus.irace_gaittester.Service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.content.LocalBroadcastManager;
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
import org.smcnus.irace_gaittester.FileLogger.FileLogger;
import org.smcnus.irace_gaittester.Helpers.DateTime;
import org.smcnus.irace_gaittester.Models.SensorInstance;
import org.smcnus.irace_gaittester.Sensor.EMSensorManager;
import org.smcnus.irace_gaittester.SensorStore.SensorStore;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class GaitAnalyzer extends Service {

    private static final String TAG                 = GaitAnalyzer.class.getSimpleName() + " hello";

    // Handler Message Types
    public static final int MSG_START_SESSION       = 0;
    public static final int MSG_STOP_SESSION        = 3;

    // Handler Broadcast Receiver Message Types
    public static final String PEDOMETER_BROADCAST  = "pedometer_broadcast";
    public static final String MSG_TIME_LAPSED      = PEDOMETER_BROADCAST + "time_lapsed";
    public static final String MSG_MIN_MAX          = PEDOMETER_BROADCAST + "min_max_accel";
    public static final String MIN                  = MSG_MIN_MAX + " min";
    public static final String MAX                  = MSG_MIN_MAX + " max";
    public static final String MSG_NUM_STEPS        = PEDOMETER_BROADCAST + "num_steps";
    public static final String MSG_PEAK_DIFF        = PEDOMETER_BROADCAST + "peak_diff";

    public static final int COUNTDOWN_TIMER         = 1000;

    private static final String TIMER_THREAD        = "timer_thread";
    private static final String ACCEL_THREAD        = TAG + "accel_thread";
    private static final String GYRO_THREAD         = TAG + "gyro_thread";
    private static final String COMPASS_THREAD      = TAG + "compass_thread";

    private static final String X_AXIS              = "xAxis";
    private static final String Y_AXIS              = "yAxis";
    private static final String Z_AXIS              = "zAxis";
    private static final String TIMESTAMP           = "sensorTimeStamps";

    private static final int MAXIMA_DIRECTION       = 1;
    private static final int MINIMA_DIRECTION       = 1 - MAXIMA_DIRECTION;

    // calibration
    private static final double ACCEL_LOW_PASS      = 0.0f;
    private static final double ACCEL_HIGH_PASS     = 6.5f;
    private static final double ACCEL_THRESHOLD     = 6.0f;
    private static final double PEAK_THRESHOLD      = 14.318913822716906 * 3.0f / 4.0f;
    private static final int TIME_THRESHOLD         = 2000;
    private static final double HIGH_PASS           = 0.8;


    // state variables
    private int timeLapsed;

    // Countdown timer
    private HandlerThread timerThread;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private boolean isTimerRunning = true;

    // storage
    private FileLogger fileLogger;
    private SensorStore sensorStore;

    // pedometer
    private ArrayList<SensorInstance> accels = new ArrayList<>();
    private int numSteps = 0;

    private double gravityX = 0.0f;
    private double gravityY = 0.0f;
    private double gravityZ = 0.0f;

    private boolean isFirstTime = true;

    private double sampleNew;
    private double accelMaxima = 0.0f;
    private double accelMinima = 0.0f;

    private int peakDirection;
    private long timeWindow;
    private double peakThreshold = 0.0f;

    private double peakDiffAvg = 0.0f;
    private int peakDiffNum = 0;

    // sensors
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


    static class IncomingHandler extends Handler {

        private final WeakReference<GaitAnalyzer> mService;

        IncomingHandler(GaitAnalyzer service) {
            mService = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            GaitAnalyzer service = mService.get();
            switch (msg.what) {
                case MSG_START_SESSION:
                    service.startNewSession();
                    break;
                case MSG_STOP_SESSION:
                    service.stopSession();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    final Messenger mMessenger = new Messenger(new IncomingHandler(this));

    /*
    * Broadcast methods
    * */

    private void broadcastTimeLapsed() {
        Intent intent = new Intent(MSG_TIME_LAPSED);
        intent.putExtra(MSG_TIME_LAPSED, timeLapsed);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastMinMaxAccel(double min, double max) {
        Intent intent = new Intent(MSG_MIN_MAX);
        intent.putExtra(MIN, min);
        intent.putExtra(MAX, max);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastNumSteps() {
        Intent intent = new Intent(MSG_NUM_STEPS);
        intent.putExtra(MSG_NUM_STEPS, numSteps);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        Log.d(TAG, "found a step");
    }

    private void broadCastPeakDiffAverage() {
        Intent intent = new Intent(MSG_PEAK_DIFF);
        intent.putExtra(MSG_PEAK_DIFF, peakDiffAvg);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    /*
    * Overridden Service methods
    * */

    @Override
    public void onCreate() {
        Log.d(TAG, "Pedometer Service is started in background thread");

        initializeSensorManager();
        initializeTimerThread();

        initializeStorage();
        initializeSensorStore();

        initializeSensorThreads();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        destroyTimerThread();
        destroySensorThreads();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }


    /*
    * Session methods
    * */

    private void startNewSession() {
        startCountdownTimer();
    }

    private void stopSession() {
        stopCountdownTimer();
        writeSensorLogsToFile(EMSensorManager.PREFIX_FILENAME);

        double[] minMax = sensorStore.getMinMaxAccelY();
        broadcastMinMaxAccel(minMax[0], minMax[1]);

        peakDiffAvg /= (double) peakDiffNum;
        broadCastPeakDiffAverage();
        Log.d(TAG, "peak difference average is: " + peakDiffAvg);
    }


    /*
    * Timer handler
    * */

    private void initializeTimerThread() {
        initializeTimeHandler();
        initializeTimeRunnable();
    }

    private void initializeTimeHandler() {
        timerThread = new HandlerThread(TIMER_THREAD);
        timerThread.start();
        timerHandler = new Handler(timerThread.getLooper());
    }

    private void initializeTimeRunnable() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if(isTimerRunning) {
                    incrementTimeLapsed();
                    broadcastTimeLapsed();
                    timerHandler.postDelayed(timerRunnable, COUNTDOWN_TIMER);
                }
            }
        };
    }

    private void startCountdownTimer() {
        initializeTimeLapsed();

        isTimerRunning = true;
        startSensors();
        timerHandler.postDelayed(timerRunnable, COUNTDOWN_TIMER);
    }

    private void stopCountdownTimer() {
        isTimerRunning = false;
        pauseSensors();
        timerHandler.removeCallbacks(timerRunnable);
    }

    private void destroyTimerThread() {
        timerThread.quit();
        timerHandler = null;
        timerRunnable = null;
    }


    /*
    * Pedometer
    * */

    private void updateAllSteps() {
        for (int index = 0; index<accels.size(); index++)
            updateStep(accels.get(index));

        accels.clear();
    }

    private void updateStep(SensorInstance accel) {

        double accelVal = accel.getDataY();

        if(!isFirstTime) {

            if(Math.abs(sampleNew - accelVal) >= ACCEL_THRESHOLD) {
                sampleNew = accelVal;

                if(peakDirection == MINIMA_DIRECTION) {

                    if(accelVal > accelMaxima) {
                        accelMaxima = accelVal;
                    } else {
                        if(accelMaxima - accelVal >= peakThreshold) {
                            if (DateTime.getIntegerCurrentTimestamp() - timeWindow >= TIME_THRESHOLD) {
                                Log.d(TAG, "minima: " + accelMinima);
                                // a step has been detected
                                numSteps += 1;

                                peakDirection = MAXIMA_DIRECTION;
                                accelMinima = accelVal;
                                timeWindow = DateTime.getIntegerCurrentTimestamp();
                                peakThreshold = PEAK_THRESHOLD;

                                peakDiffAvg += (accelMaxima - accelMinima);
                                peakDiffNum++;

                                broadcastNumSteps();
                            }
                        }
                    }
                } else if(peakDirection == MAXIMA_DIRECTION) {
                    if(accelVal < accelMinima) {
                        accelMinima = accelVal;
                    } else {
                        if(accelVal - accelMinima >= peakThreshold) {
                            if (DateTime.getIntegerCurrentTimestamp() - timeWindow >= TIME_THRESHOLD) {
                                Log.d(TAG, "maxima: " + accelMaxima);
                                // a step has been detected
                                numSteps += 1;

                                peakDirection = MINIMA_DIRECTION;
                                accelMaxima = accelVal;
                                timeWindow = DateTime.getIntegerCurrentTimestamp();
                                peakThreshold = PEAK_THRESHOLD;

                                peakDiffAvg += (accelMaxima - accelMinima);
                                peakDiffNum++;

                                broadcastNumSteps();
                            }
                        }

                    }
                }

            }

        } else {
            isFirstTime = false;
            peakDirection = MINIMA_DIRECTION;
            peakThreshold = PEAK_THRESHOLD / 2;

            accelMaxima = accelVal;
            sampleNew = accelVal;

            timeWindow = Long.parseLong(accel.getTimestamp());
        }
    }


    /*
    * Sensor methods
    * */

    private void initializeSensorThreads() {
        initializeAccelerometerThread();
        initializeGyroscopeThread();
        initializeCompassThread();
    }

    public void startSensors() {
        this.isTimerRunning = true;
        startAccelerometerSensor();
        startGyroscopeSensor();
        startCompassSensor();
    }


    public void pauseSensors() {
        this.isTimerRunning = false;
    }

    public void destroySensorThreads() {
        destroyAccelerometerThread();
        destroyGyroscopeThread();
        destroyCompassThread();
    }


    private void initializeSensorManager() {
        try {
            esSensorManager = ESSensorManager.getSensorManager(this);
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
                PullSensorConfig.SENSE_WINDOW_LENGTH_MILLIS, 500L);
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
                    updateAllSteps();
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
    * Storage
    * */

    private void initializeStorage() {
        fileLogger = FileLogger.getInstance(this);
    }

    private void initializeSensorStore() {
        sensorStore = new SensorStore();
    }

    private void writeSensorLogsToFile(String prefixFileName) {
        writeAccelLogsToFile(prefixFileName);
        writeGyroLogsToFile(prefixFileName);
        writeCompassLogsToFile(prefixFileName);
    }

    private void storeSensorData(int sensorType){

        try {

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

                    // gravity
                    gravityX = HIGH_PASS * gravityX + (1 - HIGH_PASS) * x;
                    gravityY = HIGH_PASS * gravityY + (1 - HIGH_PASS) * y;
                    gravityZ = HIGH_PASS * gravityZ + (1 - HIGH_PASS) * z;
                    x = x - gravityX;
                    y = y - gravityY;
                    z = z - gravityZ;

                    long timestamp = (long) timestamps.get(i);
                    int type = convertSensorType(sensorType);
                    SensorInstance instance = new SensorInstance(0, 0, String.valueOf(timestamp), 0,
                            x, y, z, type);

                    sensorStore.addSensorData(instance);
                    if(sensorType == SensorUtils.SENSOR_TYPE_ACCELEROMETER)
                        accels.add(instance);
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

    private void writeAccelLogsToFile(String prefixFileName) {
        ArrayList<SensorInstance> accels = sensorStore.getSensorList(SensorInstance.ACCELEROMETER);

        for(int index=0; index<accels.size(); index++)
            fileLogger.addAccelLogToFile(accels.get(index));

        fileLogger.writeLogsToFile(prefixFileName + " Accel ");
    }

    private void writeGyroLogsToFile(String prefixFileName) {
        ArrayList<SensorInstance> gyros = sensorStore.getSensorList(SensorInstance.GYROSCOPE);

        for(int index=0; index<gyros.size(); index++)
            fileLogger.addGyroLogToFile(gyros.get(index));

        fileLogger.writeLogsToFile(prefixFileName + " Gyro ");
    }

    private void writeCompassLogsToFile(String prefixFileName) {
        ArrayList<SensorInstance> compasses = sensorStore.getSensorList(SensorInstance.MAGNOMETER);

        for(int index=0; index<compasses.size(); index++)
            fileLogger.addCompassLogToFile(compasses.get(index));

        fileLogger.writeLogsToFile(prefixFileName + " Compass ");
    }


    /*
    * help methods
    * */

    private JSONObject getDataFromSensor(int sensorType) throws DataHandlerException, ESException {
        SensorData data = esSensorManager.getDataFromSensor(sensorType);
        JSONFormatter formatter = DataFormatter.getJSONFormatter(this, data.getSensorType());
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


    /*
    * State variables
    * */

    private void initializeTimeLapsed() {
        timeLapsed = 0;
    }

    private void incrementTimeLapsed() {
        timeLapsed += DateTime.MILLISECOND_RATE;
    }
}
