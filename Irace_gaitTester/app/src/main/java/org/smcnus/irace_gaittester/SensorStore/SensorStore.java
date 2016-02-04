package org.smcnus.irace_gaittester.SensorStore;

import org.smcnus.irace_gaittester.Models.SensorInstance;

import java.util.ArrayList;

public class SensorStore {

    private ArrayList<SensorInstance> accels;
    private ArrayList<SensorInstance> gyros;
    private ArrayList<SensorInstance> compasses;

    private double[] currentAccel;
    private double[] currentGyro;
    private double[] currentCompass;

    public SensorStore() {
        accels = new ArrayList<>();
        gyros = new ArrayList<>();
        compasses = new ArrayList<>();
    }

    public void addSensorData(SensorInstance data) {
        switch(data.getSensorType()) {
            case SensorInstance.ACCELEROMETER:
                accels.add(data);
                break;
            case SensorInstance.GYROSCOPE:
                gyros.add(data);
                break;
            case SensorInstance.MAGNOMETER:
                compasses.add(data);
                break;
        }
    }

    public void updateCurrentSensorData(double[] data, int type) {
        switch(type) {
            case SensorInstance.ACCELEROMETER:
                updateCurrentAccelData(data);
                break;
            case SensorInstance.GYROSCOPE:
                updateCurrentGyroData(data);
                break;
            case SensorInstance.MAGNOMETER:
                updateCurrentCompassData(data);
                break;
        }
    }

    public ArrayList<SensorInstance> getSensorList(int type) {
        switch(type) {
            case SensorInstance.ACCELEROMETER:
                return accels;
            case SensorInstance.GYROSCOPE:
                return gyros;
            case SensorInstance.MAGNOMETER:
                return compasses;
        }

        return accels;
    }

    public double[] getCurrentSensorData(int type) {
        switch(type) {
            case SensorInstance.ACCELEROMETER:
                return currentAccel;
            case SensorInstance.GYROSCOPE:
                return currentGyro;
            case SensorInstance.MAGNOMETER:
                return currentCompass;
        }

        return currentAccel;
    }

    private void updateCurrentAccelData(double[] accel) {
        currentAccel = accel;
    }

    private void updateCurrentGyroData(double[] gyro) {
        currentGyro = gyro;
    }

    private void updateCurrentCompassData(double[] compass) {
        currentCompass = compass;
    }

    public double[] getMinMaxAccelY() {
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;

        for(SensorInstance accel : accels) {
            if(accel.getDataY() < minY && accel.getDataY() > 6)
                minY = accel.getDataY();

            if(accel.getDataY() > maxY)
                maxY = accel.getDataY();
        }

        return new double[] {minY, maxY};
    }
}
