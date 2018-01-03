package study.br1221.productivitytimer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.sql.Time;

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
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(onEvent , new IntentFilter(TimerService.ACTION_SEND_TIME));
        Intent intent = new Intent(this, TimerService.class);
        startService(intent);
        bindService(intent,serviceConnection, Context.BIND_AUTO_CREATE);


    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onEvent);
    }

    @Override
    public void onClick(View view) {
        if (view == start){
            timerService.startTimer();
        } else if (view == stop) {
            timerService.stopTimer();
        } else if (view == pause) {
            timerService.pauseTimer();
        }
    }

    private BroadcastReceiver onEvent = new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {
            long timeMillis = intent.getLongExtra(TimerService.LONG_TIME_MILLIS, 0);
            remainingTimeTv.setText(String.valueOf(timeMillis));
            timerView.setTime((double)timeMillis);

        }
    };


    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            timerService = ((TimerService.LocalBinder) iBinder).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

}
