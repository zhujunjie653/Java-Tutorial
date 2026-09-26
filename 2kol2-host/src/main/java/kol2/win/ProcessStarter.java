package kol2.win;

import java.nio.file.Path;

/** 启动本机已安装的官方程序。 */
public interface ProcessStarter {
    /** 同名可执行文件是否已在运行。 */
    boolean running(String exeName);

    /** 启动程序。工作目录用安装目录。 */
    void start(Path exe, Path workDir);
}
