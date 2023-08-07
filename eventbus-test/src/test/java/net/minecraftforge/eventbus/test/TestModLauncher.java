package net.minecraftforge.eventbus.test;

import net.minecraftforge.eventbus.test.general.EventReuseTest;
import net.minecraftforge.eventbus.test.general.ParallelPostEventTest;
import org.junit.jupiter.api.Test;

import net.minecraftforge.eventbus.test.general.AbstractEventListenerTest;
import net.minecraftforge.eventbus.test.general.DeadlockingEventTest;
import net.minecraftforge.eventbus.test.general.EventBusSubtypeFilterTest;
import net.minecraftforge.eventbus.test.general.EventFiringEventTest;
import net.minecraftforge.eventbus.test.general.EventHandlerExceptionTest;
import net.minecraftforge.eventbus.test.general.LambdaHandlerTest;
import net.minecraftforge.eventbus.test.general.NonPublicEventHandler;
import net.minecraftforge.eventbus.test.general.ParallelEventTest;
import net.minecraftforge.eventbus.test.general.ThreadedListenerExceptionTest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;

public class TestModLauncher extends TestModLauncherBase {
    @Test
    public void eventHandlersCannotSubscribeToAbstractEvents() {
        doTest(new AbstractEventListenerTest() {});
    }

    @RepeatedTest(10)
    public void testMultipleThreadsMultipleBus() {
        doTest(new ParallelEventTest.Multiple() {});
    }

    @RepeatedTest(100)
    public void testMultipleThreadsOneBus() {
        doTest(new ParallelEventTest.Single() {});
    }

    @Test
    public void testEventHandlerException() {
        doTest(new EventHandlerExceptionTest() {});
    }

    @Test
    public void testValidType() {
        doTest(new EventBusSubtypeFilterTest.Valid() {});
    }

    @Test
    public void testInvalidType() {
        doTest(new EventBusSubtypeFilterTest.Invalid() {});
    }

    @Test
    public void eventHandlersCanFireEvents() {
        doTest(new EventFiringEventTest() {});
    }

    @Test
    public void lambdaBasic() {
        doTest(new LambdaHandlerTest.Basic() {});
    }

    @Test
    public void lambdaGenerics() {
        doTest(new LambdaHandlerTest.Generics() {});
    }

    @Disabled
    @RepeatedTest(500)
    public void deadlockTest() {
        doTest(new DeadlockingEventTest() {});
    }

    @RepeatedTest(100)
    public void testThreadedEventFiring() {
        doTest(new ThreadedListenerExceptionTest() {});
    }

    @Test
    public void testNonPublicEventHandler() {
        doTest(new NonPublicEventHandler(true) {});
    }

    @Test
    public void testParallelPost() {
        doTest(new ParallelPostEventTest() {});
    }

    @Test
    public void testEventReuse() {
        doTest(new EventReuseTest() {});
    }
}
