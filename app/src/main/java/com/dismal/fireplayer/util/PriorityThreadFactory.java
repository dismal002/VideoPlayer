package com.dismal.fireplayer.util;

import android.os.Process;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class PriorityThreadFactory implements ThreadFactory {
    private final String mName;
    private final AtomicInteger mNumber = new AtomicInteger();
    /* access modifiers changed from: private */
    public final int mPriority;

    public PriorityThreadFactory(String name, int priority) {
        this.mName = name;
        this.mPriority = priority;
    }

    public Thread newThread(Runnable r) {
        return new Thread(r, String.valueOf(this.mName) + "-" + this.mNumber.getAndIncrement()) {
            public void run() {
                Process.setThreadPriority(PriorityThreadFactory.this.mPriority);
                super.run();
            }
        };
    }
}
