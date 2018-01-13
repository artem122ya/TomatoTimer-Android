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
    public static String BOOLEAN_TIMER_PAUSED = "timer_paused";
    public static String BOOLEAN_TIMER_STOPPED = "timer_stopped";
    public static String BOOLEAN_TIMER_STARTED = "timer_started";


    private enum TimerState {STARTED, PAUSED, STOPPED}
    private volatile TimerState timerState = TimerState.STOPPED;


    private Thread timerThread;
    private final Object timerThreadLock = new Object();

    int timerNotificationId = 135001;

    private TimerActionReceiver actionReceiver = new TimerActionReceiver();

    private final IBinder iBinder = new LocalBinder();

    public static TimerService thisTimerService;



    private enum PeriodState {FOCUS, BREAK, BIG_BREAK, NOT_INITIALIZED}
    private PeriodState currentPeriod = PeriodState.NOT_INITIALIZED;
    private int consecutiveFocusPeriod = 0;
    private int periodsUntilBreak = 3;

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

        if (timerState != TimerState.PAUSED){
            moveToNextPeriod();
        }

        thisTimerService = this;

        synchronized (timerThreadLock) {
            if (timerState == TimerState.STOPPED) {
                timeMillisStarted = System.currentTimeMillis();
                totalMillis = getTimeLeftMillis(currentPeriod);
                timeMillisLeft = totalMillis;
                stopTimeMillis = timeMillisStarted + totalMillis;

                sendStartIntent();

                timerThread = new Thread(new TimerRunnable());
                timerState = TimerState.STARTED;
                timerThread.start();
            } else if (timerState == TimerState.PAUSED) {
                sendStartIntent();
                timerState = TimerState.STARTED;
                timerThreadLock.notifyAll();
            }
        }
    }


    private void moveToNextPeriod(){
        currentPeriod = getNextPeriod();
        if (currentPeriod == PeriodState.FOCUS){
            consecutiveFocusPeriod++;
        } else if (currentPeriod == PeriodState.BIG_BREAK){
            consecutiveFocusPeriod = 0;
        }
    }




    public void pauseTimer() {
        synchronized (timerThreadLock) {
            sendPauseIntent();
            if (timerState == TimerState.STARTED) {
                timerState = TimerState.PAUSED;
                timerThreadLock.notifyAll();
            }
        }
    }

    public void stopTimer() {

        if (timerState != TimerState.STOPPED) {
            synchronized (timerThreadLock) {
                timerState = TimerState.STOPPED;
                timerThreadLock.notifyAll();
            }

            startForeground(0, null);
            sendStopIntent();
        }

    }
    public PeriodState getNextPeriod(){
        switch (currentPeriod){
            case FOCUS:
                if (consecutiveFocusPeriod >= periodsUntilBreak) return PeriodState.BIG_BREAK;
                else return PeriodState.BREAK;
            case NOT_INITIALIZED:
            case BREAK:
            case BIG_BREAK:
                return PeriodState.FOCUS;
        }
        return PeriodState.NOT_INITIALIZED;
    }

    public int getTimeLeftMillis(PeriodState period){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        int timeLeft = 0;
        switch (period){
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




    private void sendTime(int totalMillis, int millisLeft){
        /* Method sends total time and time left in milliseconds */
        Intent intent = new Intent(ACTION_SEND_TIME);
        intent.putExtra(INT_TIME_MILLIS_LEFT, millisLeft);
        intent.putExtra(INT_TIME_MILLIS_TOTAL, totalMillis);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendStartIntent() {
        Intent intent = new Intent(ACTION_SEND_TIME);
        intent.putExtra(BOOLEAN_TIMER_STARTED, true);
        intent.putExtra(INT_TIME_MILLIS_LEFT, timeMillisLeft);
        intent.putExtra(INT_TIME_MILLIS_TOTAL, totalMillis);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    private void sendPauseIntent(){

        Intent intent = new Intent(ACTION_SEND_TIME);
        intent.putExtra(BOOLEAN_TIMER_PAUSED, true);
        intent.putExtra(INT_TIME_MILLIS_LEFT, timeMillisLeft);
        intent.putExtra(INT_TIME_MILLIS_TOTAL, totalMillis);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendStopIntent(){
        int millis = getTimeLeftMillis(getNextPeriod());
        Intent intent = new Intent(ACTION_SEND_TIME);
        intent.putExtra(BOOLEAN_TIMER_STOPPED, true);
        intent.putExtra(INT_TIME_MILLIS_LEFT, millis);
        intent.putExtra(INT_TIME_MILLIS_TOTAL, millis);
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
                    if(timeMillisLeft <= 0){
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