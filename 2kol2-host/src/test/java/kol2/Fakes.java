package kol2;

import kol2.config.AppConfig;
import kol2.flow.HostRuntime;
import kol2.flow.HostServices;
import kol2.flow.MatchEndProbe;
import kol2.run.Clock;
import kol2.run.Control;
import kol2.run.HostLog;
import kol2.run.SignalBox;
import kol2.run.SingleInstance;
import kol2.win.Actions;
import kol2.win.Desktop;
import kol2.win.ProcessStarter;
import kol2.win.WindowRef;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;

final class Fakes {
    private Fakes() {
    }

    static final class FakeClock implements Clock {
        long now;

        FakeClock(long now) {
            this.now = now;
        }

        @Override
        public long nowMs() {
            return now;
        }

        @Override
        public void sleep(long ms) {
            if (ms > 0) {
                now += ms;
            }
        }
    }

    static final class FakeDesktop implements Desktop {
        final List<WindowRef> windows = new ArrayList<>();
        final Map<Object, ClientArea> areas = new IdentityHashMap<>();

        WindowRef add(String title, ClientArea area) {
            WindowRef window = new WindowRef(title, new Object());
            windows.add(window);
            areas.put(window.handle, area);
            return window;
        }

        @Override
        public List<WindowRef> findVisible(String titleContains) {
            List<WindowRef> found = new ArrayList<>();
            for (WindowRef window : windows) {
                if (window.title != null && window.title.contains(titleContains)) {
                    found.add(window);
                }
            }
            return found;
        }

        @Override
        public boolean foreground(WindowRef window) {
            return true;
        }

        @Override
        public void restore(WindowRef window) {
        }

        @Override
        public ClientArea client(WindowRef window) {
            ClientArea area = areas.get(window.handle);
            if (area == null) {
                throw new Halt(2, "读不到窗口客户区，已停止。");
            }
            return area;
        }
    }

    static final class RecordingActions implements Actions {
        final List<String> events = new ArrayList<>();
        final Set<Integer> held = new HashSet<>();
        int typeCalls;
        IntConsumer onTap = vk -> {
        };

        @Override
        public void click(int screenX, int screenY) {
            events.add("click:" + screenX + "," + screenY);
        }

        @Override
        public void keyDown(int vk) {
            held.add(vk);
            events.add("down:" + vk);
        }

        @Override
        public void keyUp(int vk) {
            held.remove(vk);
            events.add("up:" + vk);
        }

        @Override
        public boolean isDown(int vk) {
            return held.contains(vk);
        }

        @Override
        public void releaseAll() {
            held.clear();
            events.add("release");
        }

        @Override
        public void tap(int vk) {
            events.add("tap:" + vk);
            onTap.accept(vk);
        }

        @Override
        public void typeAscii(String text) {
            typeCalls++;
            events.add("type");
        }

        @Override
        public BufferedImage capture(int screenX, int screenY, int width, int height) {
            return null;
        }

        long taps(int vk) {
            return events.stream().filter(event -> event.equals("tap:" + vk)).count();
        }
    }

    static final class FakeProcess implements ProcessStarter {
        boolean running;
        int starts;
        boolean failAlways;
        int failTimes;
        Runnable onStart = () -> {
        };

        @Override
        public boolean running(String exeName) {
            return running;
        }

        @Override
        public void start(Path exe, Path workDir) {
            starts++;
            if (failAlways || starts <= failTimes) {
                throw new Halt(2, "启动 WeGame 失败。");
            }
            running = true;
            onStart.run();
        }
    }

    static final class EndAfterNewShoot implements MatchEndProbe {
        private final RecordingActions actions;
        private final int shoot;
        private long baseline;

        EndAfterNewShoot(RecordingActions actions, int shoot) {
            this.actions = actions;
            this.shoot = shoot;
        }

        void note(long shoots) {
            baseline = shoots;
        }

        @Override
        public boolean ended() {
            long count = actions.taps(shoot);
            if (count > baseline) {
                baseline = count;
                return true;
            }
            return false;
        }
    }

    static final class FakeRuntime implements HostRuntime {
        boolean windows = true;
        final FakeDesktop desktop = new FakeDesktop();
        final RecordingActions actions = new RecordingActions();
        final FakeProcess processes = new FakeProcess();
        final FakeClock clock = new FakeClock(1_700_000_000_000L);
        final SignalBox signals = new SignalBox();
        MatchEndProbe probe = () -> false;

        @Override
        public boolean windows() {
            return windows;
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
            return new HostServices(
                    config,
                    new Control(),
                    log,
                    desktop,
                    actions,
                    processes,
                    clock,
                    signals,
                    probe,
                    null);
        }
    }
}
