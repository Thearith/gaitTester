package org.smcnus.irace_gaittester.Models;

import android.hardware.Sensor;

public class SensorInstance extends SessionData {

    /*
    * Constants
    * */

    public static final int ACCELEROMETER   = Sensor.TYPE_LINEAR_ACCELERATION;
    public static final int GYROSCOPE       = Sensor.TYPE_GYROSCOPE;
    public static final int MAGNOMETER      = Sensor.TYPE_MAGNETIC_FIELD;

    private double dataX;
    private double dataY;
    private double dataZ;
    private int sensorType;

    public SensorInstance(int dataID, int sessionID, String timestamp, int cueTime,
                          double dataX, double dataY, double dataZ, int sensorType) {
        super(dataID, sessionID, timestamp, cueTime);
        setData(dataX, dataY, dataZ, sensorType);
    }

    /* public setter and getter methods */

    @Override
    public void setData(double dataX, double dataY, double dataZ, int type) {
        this.dataX = dataX;
        this.dataY = dataY;
        this.dataZ = dataZ;
        this.sensorType = type;
    }

    @Override
    public double getDataX() {
        return this.dataX;
    }

    @Override
    public double getDataY() {
        return this.dataY;
    }

    @Override
    public double getDataZ() {
        return this.dataZ;
    }

    @Override
    public int getSensorType() {
        return this.sensorType;
    }
}
