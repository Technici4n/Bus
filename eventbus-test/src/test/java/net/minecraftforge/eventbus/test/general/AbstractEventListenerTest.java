package net.minecraftforge.eventbus.test.general;

import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.test.ITestHandler;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractEventListenerTest implements ITestHandler {
    public void test(Consumer<Class<?>> validator, Supplier<BusBuilder> builder) {
        validator.accept(AbstractEvent.class);

        IEventBus bus = builder.get().build();
        AtomicBoolean rootEventHandled = new AtomicBoolean(false);
        AtomicBoolean abstractEventHandled = new AtomicBoolean(false);
        AtomicBoolean concreteEventHandled = new AtomicBoolean(false);
        // Check that we cannot listen to the root Event class
        assertThrows(IllegalArgumentException.class, () -> bus.addListener(EventPriority.NORMAL, false, Event.class, (event) -> rootEventHandled.set(true)));
        // Check that we cannot listen to the
        assertThrows(IllegalArgumentException.class, () -> bus.addListener(EventPriority.NORMAL, false, AbstractEvent.class, (event) -> abstractEventHandled.set(true)));
        bus.addListener(EventPriority.NORMAL, false, ConcreteEvent.class, (event) -> concreteEventHandled.set(true));

        bus.post(new ConcreteEvent());

        assertFalse(rootEventHandled.get(), "handled root event");
        assertFalse(abstractEventHandled.get(), "handled abstract event");
        assertTrue(concreteEventHandled.get(), "handled concrete event");
    }

    public static abstract class AbstractEvent extends Event {}

    public static class ConcreteEvent extends AbstractEvent {
        public ConcreteEvent() {}
    }

}
