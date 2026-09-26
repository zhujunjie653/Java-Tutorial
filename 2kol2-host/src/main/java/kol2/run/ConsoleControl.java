package kol2.run;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** 后台读控制台。守护线程，关闭时不再处理新行。 */
public final class ConsoleControl implements AutoCloseable {
    private final Thread thread;
    private volatile boolean closed;

    public ConsoleControl(Control control, SignalBox signals) {
        thread = new Thread(() -> read(control, signals), "kol2-console");
        thread.setDaemon(true);
        thread.start();
    }

    private void read(Control control, SignalBox signals) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        while (!closed) {
            String line;
            try {
                line = reader.readLine();
            } catch (IOException ex) {
                return;
            }
            if (line == null) {
                return;
            }
            ConsoleCommands.parse(line).ifPresent(command -> apply(command, control, signals));
        }
    }

    private static void apply(ConsoleCommands.Command command, Control control, SignalBox signals) {
        switch (command.kind()) {
            case PAUSE -> control.pause();
            case RESUME -> control.resume();
            case STOP -> control.stop();
            case MARK -> signals.mark(command.mark());
            default -> {
            }
        }
    }

    /** 停止读取。不关闭 System.in。 */
    @Override
    public void close() {
        closed = true;
        thread.interrupt();
    }
}
