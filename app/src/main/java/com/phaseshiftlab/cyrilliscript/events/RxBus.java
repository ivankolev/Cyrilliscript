package com.phaseshiftlab.cyrilliscript.events;

import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

@SuppressWarnings({"unused", "FieldCanBeLocal", "WeakerAccess"})
public final class RxBus {

    private static volatile RxBus ourInstance = null;

    public static RxBus getInstance() {
        if (ourInstance == null) {
            synchronized (RxBus.class) {
                if (ourInstance == null) {
                    ourInstance = new RxBus();
                }
            }
        }
        return ourInstance;
    }

    private final Relay<Object> relay;

    private RxBus() {
        relay = PublishRelay.create().toSerialized();
    }

    public void post(Object event) {
        relay.accept(event);
    }

    public <T> Disposable receive(final Class<T> clazz, Consumer<T> consumer) {
        return receive(clazz).subscribe(consumer);
    }

    public <T> Observable<T> receive(final Class<T> clazz) {
        return receive().ofType(clazz);
    }

    public Observable<Object> receive() {
        return relay;
    }
}