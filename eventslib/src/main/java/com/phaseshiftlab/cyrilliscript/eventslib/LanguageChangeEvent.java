package com.phaseshiftlab.cyrilliscript.eventslib;

public class LanguageChangeEvent extends BaseEnumerationEvent {
    public static final int BG;
    public static final int EN;

    static {
        BG = 0;
        EN = 1;
    }
    public LanguageChangeEvent(int eventType) {
        super(eventType);
    }
}
