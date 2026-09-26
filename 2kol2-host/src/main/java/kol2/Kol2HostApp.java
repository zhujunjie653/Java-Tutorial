package kol2;

import kol2.config.AppConfig;
import kol2.config.ConfigLoader;
import kol2.flow.HostRuntime;
import kol2.flow.HostServices;
import kol2.flow.Session;
import kol2.flow.WindowsRuntime;
import kol2.run.HostLog;
import kol2.run.SingleInstance;

/** 程序入口。默认试跑 1 场；daily 才是到点后的多场。 */
public final class Kol2HostApp {
    /** 启动。退出码由 {@link #run(String[], HostRuntime)} 决定。 */
    public static void main(String[] args) {
        int code = new Kol2HostApp().run(args, new WindowsRuntime());
        System.exit(code);
    }

    /**
     * 跑一次。
     * 返回 0 表示按模式打完，1 表示配置问题，2 表示安全停止。
     */
    public int run(String[] args, HostRuntime runtime) {
        HostLog log = null;
        SingleInstance instance = null;
        HostServices services = null;
        Thread hook = null;
        try {
            Args parsed = Args.parse(args);
            if (!runtime.windows()) {
                throw new Halt(2, "仅支持 Windows。当前系统不会操作 WeGame 或游戏。");
            }
            AppConfig config = ConfigLoader.load(parsed.configPath);
            log = runtime.openLog(config);
            log.info("startup", "ok", "模式=" + parsed.mode.name().toLowerCase(java.util.Locale.ROOT));
            instance = runtime.lock(config);
            services = runtime.services(config, log);
            HostServices bound = services;
            hook = new Thread(() -> {
                bound.control.stop();
                bound.actions.releaseAll();
            }, "kol2-shutdown");
            Runtime.getRuntime().addShutdownHook(hook);
            new Session().run(parsed.mode, services);
            log.info("startup", "finished", "按模式完成");
            return 0;
        } catch (Halt halt) {
            report(log, halt.getMessage());
            return halt.exitCode;
        } catch (RuntimeException ex) {
            report(log, "未预期停止: " + ex.getClass().getSimpleName());
            return 2;
        } finally {
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

    private static void report(HostLog log, String message) {
        if (log != null) {
            log.error("startup", "stop", message);
        } else {
            System.err.println(message);
        }
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
