package org.smcnus.irace_gaittester;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import org.smcnus.irace_gaittester.Helpers.DateTime;
import org.smcnus.irace_gaittester.Service.GaitAnalyzer;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private TextView timerTextView;
    private TextView sensorStatTextView;
    private TextView numStepsTextView;

    // Service
    private Messenger pedometerMessenger;
    private ServiceConnection mConnection;
    private BroadcastReceiver pedometerReceiver;
    private boolean mBound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        timerTextView = (TextView) findViewById(R.id.timerTextView);
        sensorStatTextView = (TextView) findViewById(R.id.sensorStatTextView);
        numStepsTextView = (TextView) findViewById(R.id.numStepsTextView);

        Button stopButton = (Button) findViewById(R.id.stopButton);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTimer();
            }
        });

        startService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterBroadcastReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerBroadcastReceiver();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterBroadcastReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService();
    }


    /*
    * Timer
    * */

    private void startTimer() {
        sendMessageToService(GaitAnalyzer.MSG_START_SESSION, 0, 0);
    }

    private void stopTimer() {
        sendMessageToService(GaitAnalyzer.MSG_STOP_SESSION, 0, 0);
    }

    private void sendMessageToService(int message, int value1, double value2) {
        if(!mBound)
            return;

        Message msg = Message.obtain(null, message, value1, 0, value2);
        try {
            pedometerMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to send message to service: " + e.getMessage());
        }
    }



    /*
    * Service
    * */

    private void startService() {
        startServiceConnection();
        bindConnection();
    }

    private void stopService() {
        if(mBound) {
            unbindConnection();
        }
    }

    private void bindConnection() {
        Intent intent = new Intent(this, GaitAnalyzer.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindConnection() {
        unbindService(mConnection);
        mBound = false;
    }

    private void startServiceConnection() {
        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                pedometerMessenger = new Messenger(service);
                mBound = true;
                startTimer();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                pedometerMessenger = null;
                mBound = false;
                Log.d(TAG, "Service stopped in ServiceConnection");
            }
        };
    }


    /*
    * Broadcast Receiver
    * */

    private void registerBroadcastReceiver() {
        IntentFilter intentFilter = setupIntentFilter();
        if(pedometerReceiver == null)
            setupPedometerBroadCasterReceiver();

        LocalBroadcastManager.getInstance(this).registerReceiver(pedometerReceiver, intentFilter);
    }

    private void unregisterBroadcastReceiver() {
        LocalBroadcastManager.getInstance(this).
                unregisterReceiver(pedometerReceiver);
    }

    private void setupPedometerBroadCasterReceiver() {
        pedometerReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String message = intent.getAction();
                String text;

                if(message.contains(GaitAnalyzer.PEDOMETER_BROADCAST)) {
                    switch (message) {
                        case GaitAnalyzer.MSG_TIME_LAPSED:
                            int value = intent.getIntExtra(GaitAnalyzer.MSG_TIME_LAPSED, 0);
                            setTimeLapsed(value);
                            break;

                        case GaitAnalyzer.MSG_MIN_MAX:
                            double min = intent.getDoubleExtra(GaitAnalyzer.MIN, 0);
                            double max = intent.getDoubleExtra(GaitAnalyzer.MAX, 0);
                            text = min + " " + max;
                            Log.d(TAG, text);
                            sensorStatTextView.setText(text);
                            break;

                        case GaitAnalyzer.MSG_NUM_STEPS:
                            int numSteps = intent.getIntExtra(GaitAnalyzer.MSG_NUM_STEPS, 0);
                            playSound();
                            text = "Steps: " + numSteps;
                            numStepsTextView.setText(text);
                            break;

                    }
                }
            }
        };
    }

    private IntentFilter setupIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(GaitAnalyzer.MSG_TIME_LAPSED);
        filter.addAction(GaitAnalyzer.MSG_MIN_MAX);
        filter.addAction(GaitAnalyzer.MSG_NUM_STEPS);

        return filter;
    }

    private void playSound() {
        Uri defaultRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        MediaPlayer mediaPlayer = new MediaPlayer();

        try {
            mediaPlayer.setDataSource(this, defaultRingtoneUri);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
            mediaPlayer.prepare();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp)
                {
                    mp.release();
                }
            });
            mediaPlayer.start();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    * Widgets
    * */

    private void setTimeLapsed(int value) {
        int seconds = value / DateTime.MILLISECOND_RATE;
        timerTextView.setText(String.valueOf(seconds));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
