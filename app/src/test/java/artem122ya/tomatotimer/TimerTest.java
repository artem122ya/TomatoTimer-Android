package artem122ya.tomatotimer;


import org.junit.Test;

import artem122ya.tomatotimer.timer.Timer;

public class TimerTest {

    @Test
    public void testTimer(){
        Timer timer = new Timer(10);
        timer.startTimer();
    }
}
