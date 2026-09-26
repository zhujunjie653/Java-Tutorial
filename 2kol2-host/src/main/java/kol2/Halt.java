package kol2;

/** 主动停下。1 是配置问题，2 是安全停止。 */
public final class Halt extends RuntimeException {
    public final int exitCode;

    public Halt(int exitCode, String message) {
        super(message);
        this.exitCode = exitCode;
    }
}
