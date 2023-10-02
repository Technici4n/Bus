package net.neoforged.bus.benchmarks;

import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
public class CastBenchmark {
    public static class BaseEvent extends Event {
        protected int value = 0;
    }

    public static class TestEvent1 extends BaseEvent implements SomeInterface {
    }

    public static class TestEvent2 extends BaseEvent implements SomeInterface {
    }

    public static class TestEvent3 extends BaseEvent {
        public int getValue() {
            return value;
        }

        public void incrementValue() {
            value++;
        }
    }

    public interface SomeInterface {
        default int getValue() {
            return ((BaseEvent) this).value;
        }
        default void incrementValue() {
            ((BaseEvent) this).value++;
        }
    }

    private final IEventBus bus = BusBuilder.builder().build();

    @Setup
    public void setup() {
        for (int i = 0; i < 20; ++i) {
            int finalI = i;

            bus.addListener(TestEvent1.class, event -> {
                if (event.getValue() < finalI) {
                    event.incrementValue();
                }
            });
            bus.addListener(TestEvent2.class, event -> {
                if (event.getValue() < finalI) {
                    event.incrementValue();
                }
            });
            bus.addListener(TestEvent3.class, event -> {
                if (event.getValue() < finalI) {
                    event.incrementValue();
                }
            });
        }
    }

    @Benchmark
    public void testInterface(Blackhole blackhole) {
        Event event = new TestEvent1();
        bus.post(event);
        blackhole.consume(event);
        event = new TestEvent2();
        bus.post(event);
        blackhole.consume(event);
    }

    @Benchmark
    public void testDirect(Blackhole blackhole) {
        Event event = new TestEvent3();
        bus.post(event);
        blackhole.consume(event);
        event = new TestEvent3();
        bus.post(event);
        blackhole.consume(event);
    }
}
