package com.tomfran.lsm.tree;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BackgroundExecutor {

    int repeatIntervalMills;
    ScheduledExecutorService executor;
    Runnable runnable;

    public BackgroundExecutor(int repeatIntervalMills) {
        this.repeatIntervalMills = repeatIntervalMills;
        executor = Executors.newSingleThreadScheduledExecutor();
    }

    public void setRunnable(Runnable runnable) {
        this.runnable = runnable;
    }

    public void start() {
        executor.scheduleAtFixedRate(
                runnable,
                repeatIntervalMills,
                repeatIntervalMills,
                TimeUnit.MILLISECONDS
        );
    }

    public void stop() {
        executor.shutdown();
    }

}
