package com.phaseshiftlab.cyrilliscript.eventslib;

public class UserDefinedDictionaryEvent extends BaseEnumerationEvent {
    public static final int HIDE;
    public static final int SHOW;

    static {
        HIDE = 0;
        SHOW = 1;
    }

    public UserDefinedDictionaryEvent(int eventType) {
        super(eventType);
    }
}
