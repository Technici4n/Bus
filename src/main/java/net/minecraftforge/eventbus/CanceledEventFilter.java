package net.minecraftforge.eventbus;

import net.minecraftforge.eventbus.api.Event;

import java.util.function.Consumer;

public class CanceledEventFilter implements Consumer<Event> {
    private final Consumer<Event> delegate;

    public CanceledEventFilter(Consumer<Event> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void accept(Event event) {
        if (!event.isCanceled()) {
            delegate.accept(event);
        }
    }

    @Override
    public String toString() {
        return delegate.toString() + " (filtered)";
    }
}
