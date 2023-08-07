package net.minecraftforge.eventbus.api;

import java.util.function.Consumer;

public interface IEventBusInvokeDispatcher {
    void invoke(Consumer<Event> listener, Event event);
}
