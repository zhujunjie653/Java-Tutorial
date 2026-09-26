package kol2.ui;

import kol2.Kol2HostApp;
import kol2.RunMode;
import kol2.flow.HostRuntime;
import kol2.flow.HostServices;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** 在后台跑一局流程，并让界面暂停或停止。 */
public final class HostRun {
    private final Kol2HostApp app = new Kol2HostApp();
    private final AtomicReference<HostServices> services = new AtomicReference<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicBoolean stopRequested = new AtomicBoolean();

    /** 已有任务在跑时返回 false。 */
    public boolean start(RunMode mode, Path config, HostRuntime runtime, Consumer<String> lines, Runnable onDone) {
        if (!busy.compareAndSet(false, true)) {
            return false;
        }
        stopRequested.set(false);
        Thread worker = new Thread(() -> {
            try {
                app.run(mode, config, runtime, lines, services, stopRequested::get);
            } finally {
                busy.set(false);
                if (onDone != null) {
                    onDone.run();
                }
            }
        }, "kol2-run");
        worker.setDaemon(false);
        worker.start();
        return true;
    }

    /** 是否正在跑。 */
    public boolean isBusy() {
        return busy.get();
    }

    /** 暂停并立刻松开按键。 */
    public void pause() {
        HostServices current = services.get();
        if (current == null) {
            return;
        }
        current.actions.releaseAll();
        current.control.pause();
    }

    /** 从暂停继续。 */
    public void resume() {
        HostServices current = services.get();
        if (current != null) {
            current.control.resume();
        }
    }

    /** 停止并松开按键。 */
    public void stop() {
        stopRequested.set(true);
        HostServices current = services.get();
        if (current == null) {
            return;
        }
        current.control.stop();
        current.actions.releaseAll();
    }
}
