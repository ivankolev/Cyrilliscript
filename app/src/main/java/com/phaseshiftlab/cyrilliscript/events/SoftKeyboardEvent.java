package com.phaseshiftlab.cyrilliscript.events;

public class SoftKeyboardEvent {
    private final String message;

    public SoftKeyboardEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }
}
