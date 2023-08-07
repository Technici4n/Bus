package net.minecraftforge.eventbus.test.general;

import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.test.ITestHandler;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EventReuseTest implements ITestHandler {
    private static final int ITERATIONS = 1000;

    private static class ResettableEvent extends Event {
        private int data = 0;

        @Override
        public void reset() {
            data = 0;
            super.reset();
        }
    }

    @Override
    public void test(Consumer<Class<?>> validator, Supplier<BusBuilder> builder) {
        IEventBus bus = builder.get().build();

        bus.addListener((ResettableEvent event) -> {
            event.data++;
            assertEquals(1, event.data);
        });
        bus.addListener((ResettableEvent event) -> {
            event.data++;
            assertEquals(2, event.data);
        });

        ResettableEvent event = new ResettableEvent();

        for (int i = 0; i < ITERATIONS; i++) {
            event.reset();
            bus.post(event);
        }

        assertEquals(2, event.data);
    }
}
