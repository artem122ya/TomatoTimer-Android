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




    private enum TimerState {STARTED, PAUSED, STOPPED}
    private volatile TimerState timerState = TimerState.STOPPED;


    private Thread timerThread;
    private final Object timerThreadLock = new Object();

    int timerNotificationId = 135001;

    private TimerActionReceiver actionReceiver = new TimerActionReceiver();

    private final IBinder iBinder = new LocalBinder();

    public static TimerService thisTimerService;



    private enum PeriodStates {FOCUS, BREAK, BIG_BREAK, NOT_INITIALIZED}
    private PeriodStates currentPeriod = PeriodStates.NOT_INITIALIZED;

    private int timeMillisLeft = 0;
    private long timeMillisStarted = 0;
    private int totalMillis = 0;
    private long stopTimeMillis = 0;

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

        createIntentFilter();

        return START_STICKY;
    }

    private void createIntentFilter(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        intentFilter.addAction("com.example.app.ACTION_TIMER_START");
        intentFilter.addAction("com.example.app.ACTION_TIMER_PAUSE");
        intentFilter.addAction("com.example.app.ACTION_TIMER_STOP");
        registerReceiver(actionReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        stopTimer();
        unregisterReceiver(actionReceiver);
    }

    public void startTimer(){




        thisTimerService = this;

        synchronized (timerThreadLock) {
            if (timerState == TimerState.STOPPED) {
                timeMillisStarted = System.currentTimeMillis();
                totalMillis = getTimeLeftMillis();
                timeMillisLeft = totalMillis;
                stopTimeMillis = timeMillisStarted + totalMillis;

                timerThread = new Thread(new TimerRunnable());
                timerState = TimerState.STARTED;
                timerThread.start();
            } else if (timerState == TimerState.PAUSED) {
                timerState = TimerState.STARTED;
                timerThreadLock.notifyAll();
            }
        }
    }


    public void pauseTimer() {
        synchronized (timerThreadLock) {
            if (timerState != TimerState.PAUSED) {
                timerState = TimerState.PAUSED;
                timerThreadLock.notifyAll();
            }
        }
    }

    public void stopTimer() {
        synchronized (timerThreadLock) {
            if (timerState != TimerState.STOPPED) {
                timerState = TimerState.STOPPED;
                timerThreadLock.notifyAll();
            }
        }

        try {
            timerThread.join();
        } catch (InterruptedException e) {

        }
        startForeground(0, null);


    }

    private int getTimeLeftMillis(){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        int timeLeft = 0;
        switch (currentPeriod){
            case NOT_INITIALIZED:

            case FOCUS:
                timeLeft = sharedPrefs.getInt("focus_time_minutes", 25);
                break;
            case BREAK:
                timeLeft = sharedPrefs.getInt("small_break_time_minutes", 5);
                break;
            case BIG_BREAK:
                timeLeft = sharedPrefs.getInt("big_break_period_minutes", 15);
                break;
        }
        return timeLeft*60000;

    }




    private void initTimer(){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        int focusPeriodMillis = sharedPrefs.getInt("focus_time_minutes", 25) * 60000;
        int smallBreakPeriodMillis = sharedPrefs.getInt("small_break_time_minutes", 5) * 60000;
        int bigBreakPeriodMillis = sharedPrefs.getInt("big_break_period_minutes", 15) * 60000;
        int sessionsUntilBreak = sharedPrefs.getInt("sessions_until_big_break", 0);


        switch (currentPeriod){
            case NOT_INITIALIZED:

            case FOCUS:

            case BREAK:

            case BIG_BREAK:
        }

    }



    private void sendTime(int totalMillis, int millisLeft){
        /* Method sends total time and time left in milliseconds */
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
            synchronized (timerThreadLock) {
                while (timerState == TimerState.STARTED) {

                    timeMillisLeft = (int)(stopTimeMillis - System.currentTimeMillis());

                    sendTime(totalMillis, timeMillisLeft);
                    outputNotification(String.valueOf(timeMillisLeft));
                    if(timeMillisLeft <= 500){
                        stopTimer();
                    }

                    try {
                        timerThreadLock.wait(1000);
                    } catch (InterruptedException e) {

                    }
                    while (timerState == TimerState.PAUSED) {
                        try {
                            timerThreadLock.wait();
                        } catch (InterruptedException e) {

                        }
                        stopTimeMillis = System.currentTimeMillis() + timeMillisLeft;
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