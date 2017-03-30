package com.phaseshiftlab.cyrilliscript.events;

public class WritingViewEvent {
    private final String message;
    public WritingViewEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }
}
