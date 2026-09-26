package kol2;

import java.nio.file.Path;

/** 命令行。无参数时是试跑 1 场。 */
public final class Args {
    public final RunMode mode;
    public final Path configPath;

    private Args(RunMode mode, Path configPath) {
        this.mode = mode;
        this.configPath = configPath;
    }

    /** 解析启动参数。无法识别时拒绝启动。 */
    public static Args parse(String[] args) {
        RunMode mode = RunMode.TRIAL;
        Path config = Path.of("config", "application.properties");
        for (int i = 0; i < args.length; i++) {
            String token = args[i];
            if ("--config".equals(token)) {
                if (i + 1 >= args.length) {
                    throw new Halt(1, "缺少 --config 后面的配置文件路径。");
                }
                config = Path.of(args[++i]);
            } else if ("trial".equalsIgnoreCase(token)) {
                mode = RunMode.TRIAL;
            } else if ("daily".equalsIgnoreCase(token)) {
                mode = RunMode.DAILY;
            } else {
                throw new Halt(1, "无法识别的参数。可用 trial（默认）、daily，以及 --config 路径。");
            }
        }
        return new Args(mode, config);
    }
}
