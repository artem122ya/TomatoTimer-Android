package artem122ya.tomatotimer.timer;



public class Timer {

    private Object timerThreadLock = new Object();

    public enum TimerState {STARTED, PAUSED, STOPPED}
    private volatile TimerState timerState = TimerState.STOPPED;

    private int millisLeft;

    public void startTimer(int millis){
        synchronized (timerThreadLock){
            if (timerState == TimerState.STOPPED){

            }
        }
    }

    public void stopTimer(){
        synchronized (timerThreadLock){

        }
    }

    public void pauseTimer(){
        synchronized (timerThreadLock){

        }
    }


    private class TimerRunnable implements Runnable {
        @Override
        public void run() {
            synchronized (timerThreadLock) {
                while (timerState == TimerState.STARTED) {

                    timeMillisLeft = (int)(stopTimeMillis - System.currentTimeMillis());

                    sendTime(totalMillis, timeMillisLeft);
                    timerRunningNotification();
                    if(timeMillisLeft <= 0){
                        timerFinishedNotification();
                        stopTimer();
                    }

                    try {
                        timerThreadLock.wait(995);
                    } catch (InterruptedException e) {

                    }
                    while (timerState == TimerState.PAUSED) {
                        timerPausedNotification();
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



}
