package study.br1221.productivitytimer;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import static java.lang.Thread.sleep;

public class TimerService extends Service {

    public static String ACTION_SEND_TIME = "time_send";
    public static String INT_TIME_MILLIS_LEFT = "time_extra_millis_left";
    public static String INT_TIME_MILLIS_TOTAL = "time_extra_millis_total";

    private long stopTime;


    private enum TimerState {STARTED, PAUSED, STOPPED}
    private volatile TimerState timerState = TimerState.STOPPED;


    private Thread timerThread = new Thread(new TimerRunnable());

    int timerNotificationId = 135001;

    private TimerActionReceiver actionReceiver = new TimerActionReceiver();

    private final IBinder iBinder = new LocalBinder();

    public static TimerService thisTimerService;

    private int totalMillis = -1;


    public class LocalBinder extends Binder {
        TimerService getService() {
            return TimerService.this;
        }
    }



    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        thisTimerService = this;

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        intentFilter.addAction("com.example.app.ACTION_TIMER_START");
        intentFilter.addAction("com.example.app.ACTION_TIMER_PAUSE");
        intentFilter.addAction("com.example.app.ACTION_TIMER_STOP");
        registerReceiver(actionReceiver, intentFilter);


        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopTimer();
        unregisterReceiver(actionReceiver);
    }

    public void startTimer(){

        if (totalMillis == -1) totalMillis = getTotalMillis();

        thisTimerService = this;
        stopTime = System.currentTimeMillis() + totalMillis;

        synchronized (timerThread) {
            if (timerState == TimerState.STOPPED) {
                timerState = TimerState.STARTED;
                timerThread.start();
            } else if (timerState == TimerState.PAUSED) {
                timerState = TimerState.STARTED;
                timerThread.notifyAll();
            }
        }
    }


    public void pauseTimer() {
        synchronized (timerThread) {
            if (timerState != TimerState.PAUSED) {
                timerState = TimerState.PAUSED;
                timerThread.notify();
            }
        }
    }

    public void stopTimer() {

        if (timerState != TimerState.STOPPED) {
            synchronized (timerThread) {
                timerState = TimerState.STOPPED;
                timerThread.notifyAll();
            }
            try {
                timerThread.join();
            } catch (InterruptedException e) {

            }
            timerThread = new Thread(new TimerRunnable());
        }

    }


    public int getTotalMillis(){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        return sharedPrefs.getInt("timer_minutes", 0) * 60000;
    }



    private void sendTime(int totalMillis, int millisLeft){
        Intent intent = new Intent(ACTION_SEND_TIME);
        intent.putExtra(INT_TIME_MILLIS_LEFT, millisLeft);
        intent.putExtra(INT_TIME_MILLIS_TOTAL, totalMillis);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void outputNotification(String time){
        NotificationManager NotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Intent intentMainActivity = new Intent(this, MainActivity.class);
        PendingIntent pendingIntentMainActivity = PendingIntent.getActivity(this, 1, intentMainActivity, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent startIntent = new Intent("com.example.app.ACTION_TIMER_START");
        PendingIntent startPendingIntent = PendingIntent.getBroadcast(this, 100, startIntent, 0);

        Intent pauseIntent = new Intent("com.example.app.ACTION_TIMER_PAUSE");
        PendingIntent pausePendingIntent = PendingIntent.getBroadcast(this, 100, pauseIntent, 0);


        Intent stopIntent = new Intent("com.example.app.ACTION_TIMER_STOP");
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 100, stopIntent, 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher_round)
                        .setContentTitle("My notification")
                        .setContentText(time)
                        .setContentIntent(pendingIntentMainActivity)
                        .addAction(new NotificationCompat.Action(R.mipmap.ic_launcher_round, "Start", startPendingIntent))
                        .addAction(new NotificationCompat.Action(R.mipmap.ic_launcher_round, "Pause", pausePendingIntent))
                        .addAction(new NotificationCompat.Action(R.mipmap.ic_launcher_round, "Stop", stopPendingIntent));
        startForeground(timerNotificationId, mBuilder.build());

    }



    private class TimerRunnable implements Runnable {
        @Override
        public void run() {
            synchronized (timerThread) {
                while (timerState == TimerState.STARTED) {

                    long timeMillisLeft = stopTime - System.currentTimeMillis();

                    sendTime(totalMillis,(int) timeMillisLeft);
                    outputNotification(String.valueOf(timeMillisLeft));
                    try {
                        timerThread.wait(1000);
                    } catch (InterruptedException e) {

                    }
                    while (timerState == TimerState.PAUSED) {
                        try {
                            timerThread.wait();
                        } catch (InterruptedException e) {

                        }

                    }
                }
            }
        }
    }


    public static class TimerActionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(action.equalsIgnoreCase("com.example.app.ACTION_TIMER_START")){
                if(thisTimerService != null) thisTimerService.startTimer();
            } else if(action.equalsIgnoreCase("com.example.app.ACTION_TIMER_PAUSE")){
                if(thisTimerService != null) thisTimerService.pauseTimer();
            } else if(action.equalsIgnoreCase("com.example.app.ACTION_TIMER_STOP")){
                if(thisTimerService != null) thisTimerService.stopTimer();
            }
        }
    }
}