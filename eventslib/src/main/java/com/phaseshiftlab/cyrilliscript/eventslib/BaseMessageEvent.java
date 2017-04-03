package com.phaseshiftlab.cyrilliscript.eventslib;

public abstract class BaseMessageEvent {
    private final String message;

    BaseMessageEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }

}
