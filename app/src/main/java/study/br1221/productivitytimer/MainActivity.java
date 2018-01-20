package study.br1221.productivitytimer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import study.br1221.productivitytimer.TimerService.TimerState;
import study.br1221.productivitytimer.views.TimerView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private FloatingActionButton startButton;
    private ImageButton stopButton, skipButton;

    private TimerService timerService;

    private TimerView timerView;

    private TimerState currentTimerState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = findViewById(R.id.startPauseButton);
        stopButton = findViewById(R.id.stopButton);
        skipButton = findViewById(R.id.skipButton);
        timerView = findViewById(R.id.timerView);

        startButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);
        skipButton.setOnClickListener(this);

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
        updateButtons(timerService.getCurrentTimerState());
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
        if (view == startButton){
            timerService.onStartPauseButtonClick();
        } else if (view == stopButton) {
            timerService.onStopButtonClick();
        } else if (view == skipButton) {
            timerService.onSkipButtonClick();
        }
    }

    private BroadcastReceiver onEvent = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            int timeMillisLeft = intent.getIntExtra(TimerService.INT_TIME_MILLIS_LEFT, 0);
            int timeMillisTotal = intent.getIntExtra(TimerService.INT_TIME_MILLIS_TOTAL, 0);
            TimerState timerState = (TimerState) intent.getSerializableExtra(TimerService.ENUM_TIMER_STATE);

            updateButtons(timerState);

            updateTimerView(timerState, timeMillisTotal, timeMillisLeft);

        }
    };


    private void updateTimerView(TimerState currentTimerState, int millisTotal, int millisLeft){
        switch (currentTimerState){
            case STARTED:
                timerView.timerStarted(millisTotal, millisLeft);
                break;
            case PAUSED:
                timerView.timerPaused(millisTotal, millisLeft);
                break;
            case STOPPED:
                timerView.timerStopped(millisTotal, millisLeft);
                break;
            default:
                timerView.updateTimer(millisTotal, millisLeft);
        }
    }


    public void initializeTimerView(){
        updateTimerView(timerService.getCurrentTimerState(), timerService.getMillisTotal(), timerService.getMillisLeft());
    }

    public void updateButtons(TimerState timerState){
        if (currentTimerState != timerState){
            currentTimerState = timerState;
            onTimerStateChanged();
        }
    }

    private void onTimerStateChanged(){
        switch (currentTimerState){
            case STARTED:
                startButton.setImageResource(R.drawable.ic_pause_white_85_opacity);
                stopButton.setVisibility(View.VISIBLE);
                skipButton.setVisibility(View.VISIBLE);
                break;
            case STOPPED:
                stopButton.setVisibility(View.INVISIBLE);
            case PAUSED:
                startButton.setImageResource(R.drawable.ic_play_white_85_opacity);
                skipButton.setVisibility(View.INVISIBLE);
                break;
        }
    }


    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            timerService = ((TimerService.LocalBinder) iBinder).getService();
            updateButtons(timerService.getCurrentTimerState());
            initializeTimerView();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

}
