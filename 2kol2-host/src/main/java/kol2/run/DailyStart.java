package kol2.run;

import kol2.Halt;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** 每日模式等到本地时间的整点。已过点则马上返回。 */
public final class DailyStart {
    private DailyStart() {
    }

    /** 阻塞到当天该小时。停止请求会立刻退出。 */
    public static void waitUntil(int hour, Clock clock, Control control) {
        ZoneId zone = ZoneId.systemDefault();
        while (true) {
            if (!control.awaitRunning()) {
                throw new Halt(2, "已停止。");
            }
            ZonedDateTime now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(clock.nowMs()), zone);
            ZonedDateTime start = now.withHour(hour).withMinute(0).withSecond(0).withNano(0);
            if (!now.isBefore(start)) {
                return;
            }
            long remain = Duration.between(now, start).toMillis();
            clock.sleep(Math.min(remain, 1000));
        }
    }
}
