package com.phaseshiftlab.cyrilliscript.eventslib;

public abstract class BaseEnumerationEvent {
    private final int eventType;

    BaseEnumerationEvent(int eventType) {
        this.eventType = eventType;
    }

    public int getEventType() {
        return this.eventType;
    }

}
