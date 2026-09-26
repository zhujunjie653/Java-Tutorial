package kol2;

import kol2.config.AppConfig;
import kol2.config.ConfigLoader;
import kol2.flow.HostRuntime;
import kol2.flow.HostServices;
import kol2.flow.Session;
import kol2.flow.WindowsRuntime;
import kol2.run.HostLog;
import kol2.run.SingleInstance;
import kol2.ui.HostFrame;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** 程序入口。没有参数时打开窗口；trial / daily 仍走命令行。 */
public final class Kol2HostApp {
    /** 无参数打开图形界面。带参数则按命令行跑完后退出。 */
    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            HostFrame.launch(new WindowsRuntime());
            return;
        }
        int code = new Kol2HostApp().run(args, new WindowsRuntime());
        System.exit(code);
    }

    /**
     * 按命令行跑一次。
     * 返回 0 表示按模式打完，1 表示配置问题，2 表示安全停止。
     */
    public int run(String[] args, HostRuntime runtime) {
        Args parsed = Args.parse(args);
        return run(parsed.mode, parsed.configPath, runtime, null, null, null);
    }

    /**
     * 按指定模式跑一次。lines 收到的是已经去掉密码的日志行。
     * slot 用来让界面在运行中暂停或停止。
     */
    public int run(
            RunMode mode,
            Path configPath,
            HostRuntime runtime,
            Consumer<String> lines,
            AtomicReference<HostServices> slot,
            BooleanSupplier stopRequested) {
        HostLog log = null;
        SingleInstance instance = null;
        HostServices services = null;
        Thread hook = null;
        try {
            if (stopRequested != null && stopRequested.getAsBoolean()) {
                throw new Halt(2, "已停止。");
            }
            if (!runtime.windows()) {
                throw new Halt(2, "仅支持 Windows。当前系统不会操作 WeGame 或游戏。");
            }
            AppConfig config = ConfigLoader.load(configPath);
            log = runtime.openLog(config);
            log.setListener(lines);
            log.info("startup", "ok", "模式=" + mode.name().toLowerCase(java.util.Locale.ROOT));
            instance = runtime.lock(config);
            services = runtime.services(config, log);
            if (slot != null) {
                slot.set(services);
            }
            if (stopRequested != null && stopRequested.getAsBoolean()) {
                services.control.stop();
                services.actions.releaseAll();
                throw new Halt(2, "已停止。");
            }
            HostServices bound = services;
            hook = new Thread(() -> {
                bound.control.stop();
                bound.actions.releaseAll();
            }, "kol2-shutdown");
            Runtime.getRuntime().addShutdownHook(hook);
            new Session().run(mode, services);
            log.info("startup", "finished", "按模式完成");
            return 0;
        } catch (Halt halt) {
            report(log, lines, halt.getMessage());
            return halt.exitCode;
        } catch (RuntimeException ex) {
            report(log, lines, "未预期停止: " + ex.getClass().getSimpleName());
            return 2;
        } finally {
            if (slot != null) {
                slot.set(null);
            }
            if (services != null) {
                services.close();
            }
            if (instance != null) {
                instance.close();
            }
            if (log != null) {
                log.close();
            }
            removeHook(hook);
        }
    }

    private static void report(HostLog log, Consumer<String> lines, String message) {
        if (log != null) {
            log.error("startup", "stop", message);
            return;
        }
        if (lines != null) {
            lines.accept(message);
            return;
        }
        System.err.println(message);
    }

    private static void removeHook(Thread hook) {
        if (hook == null) {
            return;
        }
        try {
            Runtime.getRuntime().removeShutdownHook(hook);
        } catch (IllegalStateException ignored) {
            // 正在退出时不能再摘钩子。
        }
    }
}
