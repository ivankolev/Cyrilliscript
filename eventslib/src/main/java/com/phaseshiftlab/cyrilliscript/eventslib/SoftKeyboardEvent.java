package com.phaseshiftlab.cyrilliscript.eventslib;

public class SoftKeyboardEvent extends BaseEnumerationEvent {

    public static final int CLEAR;
    public static final int UNDO;
    public static final int REDO;

    static {
        CLEAR = 0;
        UNDO = 1;
        REDO = 2;
    }

    public SoftKeyboardEvent(int eventType) {
        super(eventType);
    }
}
