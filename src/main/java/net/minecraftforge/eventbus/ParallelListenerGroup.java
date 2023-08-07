package net.minecraftforge.eventbus;

import net.minecraftforge.eventbus.api.Event;

import java.util.function.Consumer;
import java.util.stream.Stream;

public class ParallelListenerGroup implements Consumer<Event> {
    private final Consumer<Event>[] eventListeners;

    public ParallelListenerGroup(Consumer<Event>[] eventListeners) {
        this.eventListeners = eventListeners;
    }

    @Override
    public void accept(Event event) {
        // TODO: check that this is ok for classloading
        Stream.of(this.eventListeners)
                .parallel()
                .forEach(listener -> listener.accept(event));
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("ParallelListenerGroup[\n");
        for (var listener : eventListeners) {
            builder.append("\t\t\t");
            builder.append(listener);
            builder.append("\n");
        }
        builder.append("\t\t]");

        return builder.toString();
    }
}
