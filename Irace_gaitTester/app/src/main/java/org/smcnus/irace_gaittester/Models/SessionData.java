package org.smcnus.irace_gaittester.Models;

/**
 * Created by thearith on 30/5/15.
 */
public class SessionData {

    private int dataID;
    private int sessionID;
    private int cueTime;
    private String timestamp;

    public SessionData(int dataID, int sessionID, String timestamp) {
        setDataID(dataID);
        setSessionID(sessionID);
        setTimestamp(timestamp);
        setCueTime(0);
    }

    public SessionData(int dataID, int sessionID, String timestamp, int cueTime) {
        setDataID(dataID);
        setSessionID(sessionID);
        setTimestamp(timestamp);
        setCueTime(cueTime);
    }

    /* overriding methods */


    /*
    * To be overridden by SensorData
    * */

    public void setData(double dataX, double dataY, double dataZ, int sensorType) {}

    public double getDataZ() {
        return 0.0f;
    }

    public int getSensorType() {
        return 0;
    }


    /*
    * To be overridden by TapData
    * */

    public void setData(double tapX, double tapY, int isLeft, int tapEvent, int tapIsSync) { }

    public boolean isLeft() {
        return true;
    }

    public int getTapIsSync() {
        return 0;
    }


    /*
    * To be overridden by SensorData, and TapData
    * */

    public double getDataX() {
        return 0.0f;
    }

    public double getDataY() {
        return 0.0f;
    }

    public int getTapEvent() {
        return 0;
    }


    /* Getters and setters*/

    public int getDataID() {
        return dataID;
    }

    public void setDataID(int dataID) {
        this.dataID = dataID;
    }

    public void setSessionID(int sessionID) {
        this.sessionID = sessionID;
    }

    public int getSessionID() {
        return this.sessionID;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getCueTime() {
        return this.cueTime;
    }

    public void setCueTime(int time) {
        this.cueTime = time;
    }

}
