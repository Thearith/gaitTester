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

import org.smcnus.irace_gaittester.Helpers.DateTime;
import org.smcnus.irace_gaittester.Sensor.AndroidSensorManager;
import org.smcnus.irace_gaittester.Sensor.EMSensorManager;
import org.smcnus.irace_gaittester.Sensor.GaitSensorManager;

import java.lang.ref.WeakReference;

public class GaitAnalyzer extends Service {

    private static final String TAG                 = GaitAnalyzer.class.getSimpleName();

    // Handler Message Types
    public static final int MSG_START_SESSION       = 0;
    public static final int MSG_STOP_SESSION        = 3;


    // Handler Broadcast Receiver Message Types
    public static final String PEDOMETER_BROADCAST  = "pedometer_broadcast";
    public static final String MSG_TIME_LAPSED      = PEDOMETER_BROADCAST + "time_lapsed";

    public static final int COUNTDOWN_TIMER         = 1000;

    private static final String TIMER_THREAD        = "timer_thread";

    // state variables
    private int timeLapsed;

    // Countdown timer
    private HandlerThread timerThread;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private boolean isTimerRunning = true;

    // Sensor
    private GaitSensorManager emSensorManager;
    private GaitSensorManager aSensorManager;

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


    /*
    * Overridden Service methods
    * */

    @Override
    public void onCreate() {
        Log.d(TAG, "Pedometer Service is started in background thread");

        initializeSensorManager();
        initializeTimerThread();
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
        emSensorManager.writeSensorLogsToFile(EMSensorManager.PREFIX_FILENAME);
        aSensorManager.writeSensorLogsToFile(AndroidSensorManager.PREFIX_FILENAME);
    }

    /*
    * Sensor
    * */

    private void initializeSensorManager() {
        aSensorManager = new AndroidSensorManager(this);
        emSensorManager = new EMSensorManager(this);
    }

    private void initializeSensorThreads() {
        aSensorManager.initializeSensorThreads();
        emSensorManager.initializeSensorThreads();
    }

    private void startSensors() {
        aSensorManager.startSensors();
        emSensorManager.startSensors();
    }

    private void pauseSensors() {
        aSensorManager.pauseSensors();
        emSensorManager.pauseSensors();
    }

    private void destroySensorThreads() {
        aSensorManager.destroySensorThreads();
        emSensorManager.destroySensorThreads();
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
    * State variables
    * */

    private void initializeTimeLapsed() {
        timeLapsed = 0;
    }

    private void incrementTimeLapsed() {
        timeLapsed += DateTime.MILLISECOND_RATE;
    }
}
