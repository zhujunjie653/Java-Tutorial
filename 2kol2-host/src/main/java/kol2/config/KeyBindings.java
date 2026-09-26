package kol2.config;

/** 对局用键。数值是 java.awt.event.KeyEvent 的虚拟键。takeControl 为 0 表示不按。 */
public final class KeyBindings {
    public final int moveForward;
    public final int sprint;
    public final int callCut;
    public final int pass;
    public final int shoot;
    public final int takeControl;

    public KeyBindings(int moveForward, int sprint, int callCut, int pass, int shoot, int takeControl) {
        this.moveForward = moveForward;
        this.sprint = sprint;
        this.callCut = callCut;
        this.pass = pass;
        this.shoot = shoot;
        this.takeControl = takeControl;
    }
}
