package artem122ya.tomatotimer.timer;


import artem122ya.tomatotimer.timer.TimerContract.TimerListener;

public class Timer implements TimerContract.Timer {

    private final Object timerThreadLock = new Object();
    private final TimerRunnable timerRunnable = new TimerRunnable();


    public enum TimerState {STARTED, PAUSED, STOPPED}
    private volatile TimerState timerState = TimerState.STOPPED;

    private int millisecondsLeft;

    private TimerListener timerListener;

    @Override
    public void registerListener(TimerListener timerListener) {
        this.timerListener = timerListener;
    }

    @Override
    public void unregisterListener() {
        timerListener = null;
        stopTimer();
    }

    @Override
    public void startTimer(int milliseconds){
        synchronized (timerThreadLock){
            if (timerState == TimerState.STOPPED){
                timerState = TimerState.STARTED;
                millisecondsLeft = milliseconds;
                runThread();
            }
        }
    }

    @Override
    public void stopTimer(){
        synchronized (timerThreadLock){
            if(timerState != TimerState.STOPPED){
                timerState = TimerState.STOPPED;
                if(timerListener != null) timerListener.onTimerFinish();
                timerThreadLock.notifyAll();
            }
        }
    }

    @Override
    public void pauseTimer(){
        synchronized (timerThreadLock){
            if(timerState == TimerState.STARTED){
                timerState = TimerState.PAUSED;
                timerThreadLock.notifyAll();
            }
        }
    }

    @Override
    public void resumeTimer(){
        synchronized (timerThreadLock){
            if(timerState == TimerState.PAUSED){
                timerState = TimerState.STARTED;
                timerThreadLock.notifyAll();
            }
        }
    }

    @Override
    public TimerState getTimerState() {
        return timerState;
    }

    private void runThread(){
        synchronized (timerThreadLock){
            Thread timerThread = new Thread(timerRunnable);
            timerThread.start();
        }
    }

    private class TimerRunnable implements Runnable {
        @Override
        public void run() {
            synchronized (timerThreadLock) {
                while (timerState == TimerState.STARTED) {

                    try {
                        timerThreadLock.wait(998);
                        millisecondsLeft -= 1000;
                    } catch (InterruptedException e){}

                    if(millisecondsLeft <= 0){
                        stopTimer();
                    }

                    if(timerListener != null) timerListener.onTimerTick(millisecondsLeft);


                    while (timerState == TimerState.PAUSED) {
                        try {
                            timerThreadLock.wait();
                        } catch (InterruptedException e) {}
                    }

                }
            }
        }
    }



}
