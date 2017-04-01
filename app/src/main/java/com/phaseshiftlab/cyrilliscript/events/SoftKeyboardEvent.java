package com.phaseshiftlab.cyrilliscript.events;

public class SoftKeyboardEvent {

    public static final int CLEAR;
    public static final int UNDO;
    public static final int REDO;

    static {
        CLEAR = 0;
        UNDO = 1;
        REDO = 2;
    }


    private final int eventType;

    public SoftKeyboardEvent(int eventType) {
        this.eventType = eventType;
    }

    public int getEventType() {
        return this.eventType;
    }
}
