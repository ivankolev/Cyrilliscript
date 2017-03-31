package com.phaseshiftlab.cyrilliscript.events;

public class InputSelectChangedEvent {
    private final String message;

    public InputSelectChangedEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }
}
