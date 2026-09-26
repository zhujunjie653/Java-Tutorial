package kol2.flow;

import kol2.config.AppConfig;
import kol2.run.HostLog;
import kol2.run.SingleInstance;

/** 真实 Windows 或测试用的运行环境。 */
public interface HostRuntime {
    /** 是否允许操作桌面。 */
    boolean windows();

    /** 打开本机日志。 */
    HostLog openLog(AppConfig config);

    /** 取得单实例锁。 */
    SingleInstance lock(AppConfig config);

    /** 组装这一次运行的依赖。 */
    HostServices services(AppConfig config, HostLog log);
}
