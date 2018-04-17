package artem122ya.tomatotimer;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import artem122ya.tomatotimer.timer.Timer;
import artem122ya.tomatotimer.timer.TimerContract.TimerListener;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


@RunWith(MockitoJUnitRunner.class)
public class TimerTest {

    @Mock
    TimerListener timerListener;

    Timer timer;

    @Before
    public void initializeTimer(){
        timer = new Timer();
    }

    @Test
    public void testTimerFinishCallback() throws InterruptedException {
        timer.registerListener(timerListener);
        timer.startTimer(1000);
        Thread.sleep(1300);
        verify(timerListener).onTimerFinish();
    }

    @Test
    public void testTimerTickCallback() throws InterruptedException {
        timer.registerListener(timerListener);
        timer.startTimer(1000);
        Thread.sleep(1300);
        verify(timerListener).onTimerTick(0);
    }

    @Test
    public void testStartedStateWhenTimerRunning(){
        timer.startTimer(1000);
        assertEquals(Timer.TimerState.STARTED, timer.getTimerState());
    }

    @Test
    public void testPausedStateWhenTimerPaused() throws InterruptedException {
        timer.startTimer(1000);
        timer.pauseTimer();
        assertEquals(Timer.TimerState.PAUSED, timer.getTimerState());
    }

    @Test
    public void testStoppedStateWhenTimerPaused(){
        timer.startTimer(1000);
        timer.stopTimer();
        assertEquals(Timer.TimerState.STOPPED, timer.getTimerState());
    }

    @Test
    public void testStartedStateWhenTimerResumed() throws InterruptedException {
        timer.startTimer(1000);
        timer.pauseTimer();
        timer.resumeTimer();
        assertEquals(Timer.TimerState.STARTED, timer.getTimerState());
    }

    @Test
    public void timerShouldNotCallbackWhenListenerUnregistered() throws InterruptedException{
        timer.registerListener(timerListener);
        timer.startTimer(1000);
        timer.unregisterListener();
        Thread.sleep(1300);
        verify(timerListener, never()).onTimerFinish();
        verify(timerListener, never()).onTimerTick(0);
    }

}
