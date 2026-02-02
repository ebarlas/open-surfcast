package org.opensurfcast.timer;

import java.util.concurrent.TimeUnit;

public class Timer {

    private final long t = System.nanoTime();

    public long elapsed() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t);
    }

}
