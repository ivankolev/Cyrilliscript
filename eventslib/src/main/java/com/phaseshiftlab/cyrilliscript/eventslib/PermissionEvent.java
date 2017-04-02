package com.phaseshiftlab.cyrilliscript.eventslib;

public class PermissionEvent {

    public static final int DENIED;
    public static final int GRANTED;

    static {
        DENIED = 0;
        GRANTED = 1;
    }


    private final int eventType;

    public PermissionEvent(int eventType) {
        this.eventType = eventType;
    }

    public int getEventType() {
        return this.eventType;
    }
}
