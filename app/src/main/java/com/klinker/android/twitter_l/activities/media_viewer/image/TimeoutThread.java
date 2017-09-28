package com.klinker.android.twitter_l.activities.media_viewer.image;

public class TimeoutThread extends Thread {

    private static final long SECOND = 1000;
    private static final long TIMEOUT = SECOND * 50; // 50 seconds

    public TimeoutThread(Runnable runnable) {
        super(runnable);
    }

    @Override
    public synchronized void start() {
        final Thread threadToTime = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    long time = 0;
                    while (time < TIMEOUT && !threadToTime.isAlive()) {
                        Thread.sleep(SECOND * 2);
                        time += SECOND * 2;
                    }

                    if (!threadToTime.isAlive()) {
                        threadToTime.interrupt();
                    }
                } catch (InterruptedException e) {

                }
            }
        }).start();

        super.start();
    }
}
