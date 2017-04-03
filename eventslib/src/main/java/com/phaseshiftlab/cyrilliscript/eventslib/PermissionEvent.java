package com.phaseshiftlab.cyrilliscript.eventslib;

public class PermissionEvent extends BaseEnumerationEvent {

    public static final int DENIED;
    public static final int GRANTED;

    static {
        DENIED = 0;
        GRANTED = 1;
    }

    public PermissionEvent(int eventType) {
        super(eventType);
    }

}
