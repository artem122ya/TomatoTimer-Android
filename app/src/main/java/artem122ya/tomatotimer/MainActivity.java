package artem122ya.tomatotimer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import artem122ya.tomatotimer.TimerService.PeriodState;
import artem122ya.tomatotimer.TimerService.TimerState;
import artem122ya.tomatotimer.settings.SettingsActivity;
import artem122ya.tomatotimer.views.TimerView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private FloatingActionButton startButton;
    private ImageButton stopButton, skipButton;

    private TimerService timerService;

    private TimerView timerView;

    private TimerState currentTimerState;
    private PeriodState currentTimerPeriod;

    private TextView currentPeriodTextView, periodsUntilBigBreakTextView;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setDarkTheme(sharedPreferences.getBoolean(getString(R.string.dark_mode_preference_key),
                false));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = findViewById(R.id.startPauseButton);
        stopButton = findViewById(R.id.stopButton);
        skipButton = findViewById(R.id.skipButton);
        timerView = findViewById(R.id.timerView);
        currentPeriodTextView = findViewById(R.id.currentPeriodTextView);
        periodsUntilBigBreakTextView = findViewById(R.id.periodsUntilBigBreakTextView);


        startButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);
        skipButton.setOnClickListener(this);


        sharedPreferences.registerOnSharedPreferenceChangeListener(this);


        this.setTitle("");

    }


    @Override
    protected void onStart() {
        super.onStart();
        // Bind TimerService.
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(TimerService.ACTION_SEND_TIME));
        Intent intent = new Intent(this, TimerService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);


    }


    @Override
    protected void onRestart() {
        // Redraw timerView in case any settings changed
        super.onRestart();
        timerView.stopAnimation();
        updateButtons(timerService.getCurrentTimerState());
        initializeTimerView();
        showCurrentStateText();
    }


    @Override
    protected void onStop() {
        super.onStop();
        unbindService(serviceConnection);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
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
        showCurrentStateText();
        if (view == startButton){
            timerService.onStartPauseButtonClick();
        } else if (view == stopButton) {
            timerService.onStopButtonClick();
        } else if (view == skipButton) {
            timerService.onSkipButtonClickInActivity();
        }
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.dark_mode_preference_key))) {
            setDarkTheme(sharedPreferences.getBoolean(getString(R.string.dark_mode_preference_key)
                    , false));
            recreate();
        }
    }

    private void showCurrentStateText(){
        PeriodState newPeriod = timerService.getCurrentPeriod();
        if (currentTimerPeriod != newPeriod) {
            currentTimerPeriod = newPeriod;
            String currentSession = "";
            switch (currentTimerPeriod) {
                case FOCUS:
                    currentSession = getString(R.string.work_time_text);
                    break;
                case BREAK:
                    currentSession = getString(R.string.break_time_text);
                    break;
                case BIG_BREAK:
                    currentSession = getString(R.string.big_break_text);
                    break;
            }
            currentPeriodTextView.setText(currentSession);
        }
    }

    private void setDarkTheme(boolean darkThemeEnabled){
        setTheme(darkThemeEnabled ? R.style.TimerActivityDark : R.style.TimerActivityLight);
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            int timeMillisLeft = intent.getIntExtra(TimerService.INT_TIME_MILLIS_LEFT, 0);
            int timeMillisTotal = intent.getIntExtra(TimerService.INT_TIME_MILLIS_TOTAL, 0);
            TimerState timerState = (TimerState) intent.getSerializableExtra(TimerService.ENUM_TIMER_STATE);

            showCurrentStateText();

            updateButtons(timerState);

            updateTimerView(timerState, timeMillisTotal, timeMillisLeft);

        }
    };


    private void updateTimerView(TimerState currentTimerState, int millisTotal, int millisLeft){
        switch (currentTimerState){
            case STARTED:
                timerView.onTimerStarted(millisTotal, millisLeft);
                break;
            case PAUSED:
                timerView.onTimerPaused(millisTotal, millisLeft);
                break;
            case STOPPED:
                timerView.onTimerStopped(millisTotal, millisLeft);
                break;
            default:
                timerView.onTimerUpdate(millisTotal, millisLeft);
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
                startButton.setImageResource(R.drawable.ic_pause_85_opacity);
                stopButton.setVisibility(View.VISIBLE);
                break;
            case STOPPED:
                stopButton.setVisibility(View.INVISIBLE);
            case PAUSED:
                startButton.setImageResource(R.drawable.ic_play_85_opacity);
                break;
        }
    }


    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            timerService = ((TimerService.LocalBinder) iBinder).getService();
            updateButtons(timerService.getCurrentTimerState());
            initializeTimerView();
            showCurrentStateText();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

}
