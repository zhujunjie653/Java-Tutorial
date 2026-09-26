package kol2.run;

import kol2.Halt;

/** 使用系统时间。 */
public final class SystemClock implements Clock {
    @Override
    public long nowMs() {
        return System.currentTimeMillis();
    }

    @Override
    public void sleep(long ms) {
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new Halt(2, "等待被中断，已停止。");
        }
    }
}
