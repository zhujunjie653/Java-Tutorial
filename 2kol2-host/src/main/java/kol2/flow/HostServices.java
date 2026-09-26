package kol2.flow;

import kol2.Halt;
import kol2.config.AppConfig;
import kol2.run.Clock;
import kol2.run.ConsoleControl;
import kol2.run.Control;
import kol2.run.HostLog;
import kol2.run.SignalBox;
import kol2.win.Actions;
import kol2.win.Desktop;
import kol2.win.ProcessStarter;

/** 一次运行要用到的依赖。测试可以换成假的键鼠和窗口。 */
public final class HostServices implements AutoCloseable {
    public final AppConfig config;
    public final Control control;
    public final HostLog log;
    public final Desktop desktop;
    public final Actions actions;
    public final ProcessStarter processes;
    public final Clock clock;
    public final SignalBox signals;
    public final MatchEndProbe matchEnd;
    public final ConsoleControl console;

    public HostServices(
            AppConfig config,
            Control control,
            HostLog log,
            Desktop desktop,
            Actions actions,
            ProcessStarter processes,
            Clock clock,
            SignalBox signals,
            MatchEndProbe matchEnd,
            ConsoleControl console) {
        this.config = config;
        this.control = control;
        this.log = log;
        this.desktop = desktop;
        this.actions = actions;
        this.processes = processes;
        this.clock = clock;
        this.signals = signals;
        this.matchEnd = matchEnd;
        this.console = console;
    }

    /** 暂停时先松键再等。已停止则抛出。 */
    public void gate() {
        if (control.isPaused() || control.isStopped()) {
            actions.releaseAll();
        }
        if (!control.awaitRunning()) {
            actions.releaseAll();
            throw new Halt(2, "已停止。");
        }
    }

    /** 分段等待，以便暂停和停止能插进来。 */
    public void sleep(long ms) {
        long left = ms;
        while (left > 0) {
            gate();
            long slice = Math.min(100, left);
            clock.sleep(slice);
            left -= slice;
        }
    }

    /** 松开按键并结束控制台线程。 */
    @Override
    public void close() {
        actions.releaseAll();
        config.clearPassword();
        if (console != null) {
            console.close();
        }
    }
}
