package com.phaseshiftlab.cyrilliscript.eventslib;

public class InputSelectChangedEvent {
    private final int eventType;
    public static final int LETTERS;
    public static final int DIGITS;
    public static final int SYMBOLS;

    static {
        LETTERS = 0;
        DIGITS = 1;
        SYMBOLS = 2;
    }
    public InputSelectChangedEvent(int eventType) {
        this.eventType = eventType;
    }

    public int getEventType() {
        return this.eventType;
    }
}
