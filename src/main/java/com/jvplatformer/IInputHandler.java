package com.jvplatformer;

public interface IInputHandler {
    boolean isHeld(int keyCode);
    boolean isJustPressed(int keyCode);
    void clearJustPressed();
    int drainKeyEventCount();
    String heldSummary();
}
