package kol2.run;

/** 可替换的时钟，便于测试不等待真实时间。 */
public interface Clock {
    /** 当前毫秒时间。 */
    long nowMs();

    /** 等待。中断时视为停止。 */
    void sleep(long ms);
}
