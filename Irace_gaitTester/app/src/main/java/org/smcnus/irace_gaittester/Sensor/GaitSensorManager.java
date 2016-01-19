package org.smcnus.irace_gaittester.Sensor;

import android.content.Context;
import android.util.Log;

import org.smcnus.irace_gaittester.FileLogger.FileLogger;
import org.smcnus.irace_gaittester.Models.SensorInstance;
import org.smcnus.irace_gaittester.SensorStore.SensorStore;

import java.util.ArrayList;

public class GaitSensorManager {

    private static String TAG   = GaitSensorManager.class.getSimpleName();

    protected Context context;
    protected SensorStore sensorStore;
    protected FileLogger fileLogger;

    public GaitSensorManager(Context context) {
        this.context = context;
        sensorStore = new SensorStore();
        fileLogger = FileLogger.getInstance(context);
    }

    public void writeSensorLogsToFile(String prefixFileName) {
        writeAccelLogsToFile();
        writeGyroLogsToFile();
        writeCompassLogsToFile();

        fileLogger.writeLogsToFile(prefixFileName);
    }

    /*
    * Overridden methods
    * */

    public void initializeSensorThreads() {

    }

    public void startSensors() {

    }

    public void pauseSensors() {

    }

    public void destroySensorThreads() {

    }

    /*
    * Storage
    * */

    private void writeAccelLogsToFile() {
        ArrayList<SensorInstance> accels = sensorStore.getSensorList(SensorInstance.ACCELEROMETER);

        for(SensorInstance accel : accels) {
            fileLogger.addAccelLogToFile(accel);
        }
    }

    private void writeGyroLogsToFile() {
        ArrayList<SensorInstance> gyros = sensorStore.getSensorList(SensorInstance.GYROSCOPE);

        for(SensorInstance gyro : gyros) {
            fileLogger.addGyroLogToFile(gyro);
        }
    }

    private void writeCompassLogsToFile() {
        ArrayList<SensorInstance> compasses = sensorStore.getSensorList(SensorInstance.MAGNOMETER);

        for(SensorInstance compass : compasses) {
            fileLogger.addCompassLogToFile(compass);
        }
    }

}
