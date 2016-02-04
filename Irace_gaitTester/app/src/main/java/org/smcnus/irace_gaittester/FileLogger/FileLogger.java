package org.smcnus.irace_gaittester.FileLogger;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.smcnus.irace_gaittester.Helpers.DateTime;
import org.smcnus.irace_gaittester.Models.SensorInstance;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class FileLogger {

    // constants
    private static final String TAG                 = FileLogger.class.getSimpleName();

    private static final String IRACE_GAITANALYZER_FOLDER   = "GaitAnalyzer";
    private static final String LOG_FILE_EXTENSION          = ".txt";

    private static final String SEPERATOR                   = "\t";

    private static final String SENSOR_LOG_FORMAT   = // sensor_id sensor_type data_x data_y data_z timestamp cue_time
            "%d" + SEPERATOR + "%s" + SEPERATOR + "%f" + SEPERATOR + "%f" +
            SEPERATOR + "%f" + SEPERATOR + "%s" + SEPERATOR +"%d";


    private static final String ACCEL_TYPE          = "Accel";
    private static final String GYRO_TYPE           = "Gyro";
    private static final String COMPASS_TYPE        = "Compass";

    private static FileLogger fileLogger;

    private Context mContext;
    private ArrayList<String> logs;

    /*
    * Constructor
    * */

    public static FileLogger getInstance(Context context) {
        if(fileLogger == null) {
            fileLogger = new FileLogger(context);
        }

        return fileLogger;
    }

    private FileLogger(Context context) {
        mContext = context;
        logs = new ArrayList<>();
    }


    /*
    * public methods
    * */

    public void writeLogsToFile(String prefixFileName) {

        String recordDirPath = createGaitAnalyzerFolderIfNotExists();

        String fileName = DateTime.getCurrentTimestamp() + "---"  + prefixFileName
                + LOG_FILE_EXTENSION;
        String filePath = recordDirPath + "/" + fileName;
        File logFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), filePath);

        try {
            if (!logFile.exists())
                logFile.createNewFile();

            BufferedWriter outputStreamWriter = new BufferedWriter(new FileWriter(logFile));

            for(int index=0; index<logs.size(); index++) {
                String log = index + SEPERATOR + logs.get(index) + "\n";
                outputStreamWriter.write(log);
            }

            outputStreamWriter.close();
            logs.clear();

        } catch (IOException e) {
            Log.e(TAG, "File write failed: " + e.toString());
        }
    }

    public void addAccelLogToFile (SensorInstance accel) {
        addSensorLogToFile(ACCEL_TYPE, accel);
    }

    public void addGyroLogToFile (SensorInstance gyro) {
        addSensorLogToFile(GYRO_TYPE, gyro);
    }

    public void addCompassLogToFile (SensorInstance compass) {
        addSensorLogToFile(COMPASS_TYPE, compass);
    }

    public void addLogToFile (String log) {
        logs.add(log);
    }

    /*
    * Helper method
    * */

    private String createGaitAnalyzerFolderIfNotExists() {
        String recordDirPath = IRACE_GAITANALYZER_FOLDER;
        File recordDirectory =
                new File(Environment.getExternalStorageDirectory().getAbsolutePath(), recordDirPath);

        if(!recordDirectory.exists())
            recordDirectory.mkdir();

        return recordDirPath;
    }

    private void addSensorLogToFile(String sensorType, SensorInstance data) {
        String log = String.format(SENSOR_LOG_FORMAT, data.getDataID(),
                sensorType, data.getDataX(), data.getDataY(), data.getDataZ(),
                data.getTimestamp(), data.getCueTime());

        logs.add(log);
    }
}
