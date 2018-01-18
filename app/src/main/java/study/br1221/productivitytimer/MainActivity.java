package study.br1221.productivitytimer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.sql.Time;
import java.util.concurrent.TimeUnit;

import study.br1221.productivitytimer.views.TimerView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView remainingTimeTv;
    private Button start, stop, pause;

    private TimerService timerService;

    private TimerView timerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        remainingTimeTv = (TextView) findViewById(R.id.remainingTimeTv);
        start = (Button) findViewById(R.id.startBtn);
        stop = (Button) findViewById(R.id.stopBtn);
        pause = (Button) findViewById(R.id.pauseBtn);
        start.setOnClickListener(this);
        stop.setOnClickListener(this);
        pause.setOnClickListener(this);

        timerView = (TimerView) findViewById(R.id.timerView);

        //--------------------------------------------------------

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        remainingTimeTv.setText(String.valueOf(sharedPrefs.getInt("sessions_until_big_break", -1)));


    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind TimerService.
        LocalBroadcastManager.getInstance(this).registerReceiver(onEvent , new IntentFilter(TimerService.ACTION_SEND_TIME));
        Intent intent = new Intent(this, TimerService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);


    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onEvent);
    }

    @Override
    protected void onRestart() {
        // Redraw timerView in case any settings changed
        super.onRestart();
        initializeTimerView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.settings_menu:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.about_menu:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        if (view == start){
            timerService.startTimer();
        } else if (view == stop) {
            timerService.stopServiceAndTimer();
        } else if (view == pause) {
            timerService.pauseTimer();
        }
    }

    private BroadcastReceiver onEvent = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            int timeMillisLeft = intent.getIntExtra(TimerService.INT_TIME_MILLIS_LEFT, 0);
            int timeMillisTotal = intent.getIntExtra(TimerService.INT_TIME_MILLIS_TOTAL, 0);
            boolean timerStopped = intent.getBooleanExtra(TimerService.BOOLEAN_TIMER_STOPPED, false);
            boolean timerPaused = intent.getBooleanExtra(TimerService.BOOLEAN_TIMER_PAUSED, false);
            boolean timerStarted = intent.getBooleanExtra(TimerService.BOOLEAN_TIMER_STARTED, false);

            updateTimerView(timerStarted ,timerPaused, timerStopped, timeMillisTotal, timeMillisLeft);


            remainingTimeTv.setText(getTimeString(timeMillisLeft));

        }
    };


    private void updateTimerView(boolean timerStarted, boolean timerPaused, boolean timerStopped, int millisTotal, int millisLeft){
        if (timerStarted){
            timerView.timerStarted(millisTotal, millisLeft);
        }else if(timerPaused){
            timerView.timerPaused(millisTotal, millisLeft);
        } else if(timerStopped){
            timerView.timerStopped(millisTotal, millisLeft);
        } else timerView.updateTimer(millisTotal, millisLeft);

    }

    private String getTimeString(int millis){
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        return String.format("%02d:%02d",
                minutes,TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes));
    }

    public void initializeTimerView(){
        updateTimerView(timerService.isStarted(),timerService.isPaused(), timerService.isStopped(),
                timerService.getMillisTotal(), timerService.getMillisLeft());
    }


    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            timerService = ((TimerService.LocalBinder) iBinder).getService();
            initializeTimerView();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

}
