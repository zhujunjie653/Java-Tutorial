package kol2.win;

import kol2.Halt;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** 用系统进程列表确认 WeGame 是否已启动，并启动官方程序。 */
public final class WindowsProcessStarter implements ProcessStarter {
    @Override
    public boolean running(String exeName) {
        try {
            Process process = new ProcessBuilder("tasklist", "/FI", "IMAGENAME eq " + exeName, "/NH")
                    .redirectErrorStream(true)
                    .start();
            String text = new String(process.getInputStream().readAllBytes(), Charset.defaultCharset());
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroy();
                throw new Halt(2, "检查 WeGame 进程超时，已停止。");
            }
            return text.toLowerCase(Locale.ROOT).contains(exeName.toLowerCase(Locale.ROOT));
        } catch (Halt halt) {
            throw halt;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new Halt(2, "检查 WeGame 进程被中断，已停止。");
        } catch (IOException ex) {
            throw new Halt(2, "无法检查 WeGame 是否已在运行。");
        }
    }

    @Override
    public void start(Path exe, Path workDir) {
        try {
            new ProcessBuilder(exe.toString())
                    .directory(workDir.toFile())
                    .start();
        } catch (IOException ex) {
            throw new Halt(2, "启动 WeGame 失败。");
        }
    }
}
