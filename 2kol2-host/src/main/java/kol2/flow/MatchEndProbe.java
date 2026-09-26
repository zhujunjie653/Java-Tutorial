package kol2.flow;

/** 判断这一场是否已经结束。无法判断时返回 false。 */
public interface MatchEndProbe {
    /** 终场画面是否出现。 */
    boolean ended();
}
