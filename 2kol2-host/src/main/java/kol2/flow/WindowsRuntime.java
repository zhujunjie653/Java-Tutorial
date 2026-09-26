package kol2.flow;

import kol2.config.AppConfig;
import kol2.run.ConsoleControl;
import kol2.run.Control;
import kol2.run.HostLog;
import kol2.run.SignalBox;
import kol2.run.SingleInstance;
import kol2.run.SystemClock;
import kol2.win.Actions;
import kol2.win.Desktop;
import kol2.win.JnaDesktop;
import kol2.win.ProcessStarter;
import kol2.win.RobotActions;
import kol2.win.WindowsProcessStarter;

import java.nio.file.Files;
import java.util.Locale;

/** 只在 Windows 上连接真实窗口和键鼠。 */
public final class WindowsRuntime implements HostRuntime {
    @Override
    public boolean windows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    @Override
    public HostLog openLog(AppConfig config) {
        return HostLog.open(config.logDir, config.account, config.password);
    }

    @Override
    public SingleInstance lock(AppConfig config) {
        return SingleInstance.acquire(config.lockFile);
    }

    @Override
    public HostServices services(AppConfig config, HostLog log) {
        Control control = new Control();
        SignalBox signals = new SignalBox();
        ConsoleControl console = new ConsoleControl(control, signals);
        Desktop desktop = new JnaDesktop();
        Actions actions = new RobotActions();
        ProcessStarter processes = new WindowsProcessStarter();
        SystemClock clock = new SystemClock();
        MatchEndProbe probe = probe(config, desktop, actions, clock, log);
        return new HostServices(config, control, log, desktop, actions, processes, clock, signals, probe, console);
    }

    private static MatchEndProbe probe(
            AppConfig config, Desktop desktop, Actions actions, SystemClock clock, HostLog log) {
        if (config.templateMatchEnd != null && Files.isRegularFile(config.templateMatchEnd)) {
            log.info("template", "ok", "已加载终场模板");
            return new TemplateProbe(config.templateMatchEnd, desktop, actions, config.gameTitle, clock);
        }
        log.info("template", "skip", "没有终场模板。结束一场请在控制台输入 done、win 或 loss");
        return () -> false;
    }
}
