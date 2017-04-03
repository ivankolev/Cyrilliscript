package com.phaseshiftlab.cyrilliscript.eventslib;

public class InputSelectChangedEvent extends BaseEnumerationEvent {
    public static final int LETTERS;
    public static final int DIGITS;
    public static final int SYMBOLS;

    static {
        LETTERS = 0;
        DIGITS = 1;
        SYMBOLS = 2;
    }
    public InputSelectChangedEvent(int eventType) {
        super(eventType);
    }
}
