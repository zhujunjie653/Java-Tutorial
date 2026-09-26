package kol2.run;

import java.util.concurrent.atomic.AtomicReference;

/** 控制台写入、对局循环读出的一次性标记。 */
public final class SignalBox {
    private final AtomicReference<UserMark> mark = new AtomicReference<>();

    /** 记下用户标记。后一次覆盖前一次。 */
    public void mark(UserMark value) {
        mark.set(value);
    }

    /** 取走标记。没有则返回 null。 */
    public UserMark poll() {
        return mark.getAndSet(null);
    }
}
