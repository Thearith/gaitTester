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
        writeAccelLogsToFile(prefixFileName);
        writeGyroLogsToFile(prefixFileName);
        writeCompassLogsToFile(prefixFileName);
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

    public double[] getMinMaxAccelY() {
        return sensorStore.getMinMaxAccelY();
    }

    /*
    * Storage
    * */

    private void writeAccelLogsToFile(String prefixFileName) {
        ArrayList<SensorInstance> accels = sensorStore.getSensorList(SensorInstance.ACCELEROMETER);

        for(SensorInstance accel : accels) {
            fileLogger.addAccelLogToFile(accel);
        }

        fileLogger.writeLogsToFile(prefixFileName + " Accel ");
    }

    private void writeGyroLogsToFile(String prefixFileName) {
        ArrayList<SensorInstance> gyros = sensorStore.getSensorList(SensorInstance.GYROSCOPE);

        for(SensorInstance gyro : gyros) {
            fileLogger.addGyroLogToFile(gyro);
        }

        fileLogger.writeLogsToFile(prefixFileName + " Gyro ");
    }

    private void writeCompassLogsToFile(String prefixFileName) {
        ArrayList<SensorInstance> compasses = sensorStore.getSensorList(SensorInstance.MAGNOMETER);

        for(SensorInstance compass : compasses) {
            fileLogger.addCompassLogToFile(compass);
        }

        fileLogger.writeLogsToFile(prefixFileName + " Compass ");
    }

}
