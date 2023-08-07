package net.minecraftforge.eventbus.test.general;

import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.ParallelEvent;
import net.minecraftforge.eventbus.test.ITestHandler;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParallelPostEventTest implements ITestHandler {
    private static final int LISTENER_COUNT = 200;

    private static final AtomicLong COUNTER = new AtomicLong();
    private static int waitingThreads = 0;
    private static int maxWaitingThreads = 0;

    private static class TestEvent extends ParallelEvent {
    }

    // Also test classloading...
    private static class Handler {
        private static void handle() {
            OtherHandler.handle();
        }
    }

    private static class OtherHandler {
        private static void handle() {
            COUNTER.incrementAndGet();
        }
    }

    @Override
    public void before(Consumer<Class<?>> validator, Supplier<BusBuilder> builder) {
        COUNTER.set(0);
        waitingThreads = 0;
        maxWaitingThreads = 0;
        validator.accept(TestEvent.class);
    }

    protected void handle(TestEvent event) {
        synchronized (ParallelPostEventTest.class) {
            waitingThreads++;
            maxWaitingThreads = Math.max(maxWaitingThreads, waitingThreads);
        }
        Handler.handle();
        try {
            Thread.sleep(50);
        } catch (Exception ignored) { }
        synchronized (ParallelPostEventTest.class) {
            waitingThreads--;
        }
    }

    @Override
    public void test(Consumer<Class<?>> validator, Supplier<BusBuilder> builder) {
        IEventBus bus = builder.get().build();
        for (int i = 0; i < LISTENER_COUNT; i++) {
            bus.addListener(EventPriority.HIGH, this::handle);
            bus.addListener(EventPriority.LOW, this::handle);
        }

        // Test that phases are respected
        bus.addListener((TestEvent event) -> assertEquals(COUNTER.get(), LISTENER_COUNT));
        bus.addListener(EventPriority.LOWEST, (TestEvent event) -> assertEquals(COUNTER.get(), 2 * LISTENER_COUNT));

        bus.post(new TestEvent());

        assertEquals(COUNTER.get(), 2 * LISTENER_COUNT);
        assertTrue(maxWaitingThreads > 1, "expected some level of parallelism");
    }
}
