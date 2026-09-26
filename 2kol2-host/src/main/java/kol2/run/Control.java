package kol2.run;

/** 暂停、继续和停止。暂停时调用方不得再发键鼠。 */
public final class Control {
    private final Object lock = new Object();
    private volatile boolean stopped;
    private boolean paused;

    /** 暂停。已停止时不再进入暂停。 */
    public void pause() {
        synchronized (lock) {
            if (!stopped) {
                paused = true;
            }
        }
    }

    /** 继续。 */
    public void resume() {
        synchronized (lock) {
            paused = false;
            lock.notifyAll();
        }
    }

    /** 停止并唤醒正在暂停的等待。 */
    public void stop() {
        synchronized (lock) {
            stopped = true;
            paused = false;
            lock.notifyAll();
        }
    }

    /** 是否处于暂停。 */
    public boolean isPaused() {
        synchronized (lock) {
            return paused && !stopped;
        }
    }

    /** 是否已停止。 */
    public boolean isStopped() {
        return stopped;
    }

    /** 暂停时阻塞。返回 false 表示应当停止。 */
    public boolean awaitRunning() {
        synchronized (lock) {
            while (paused && !stopped) {
                try {
                    lock.wait(200);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    stopped = true;
                    paused = false;
                    return false;
                }
            }
            return !stopped;
        }
    }
}
