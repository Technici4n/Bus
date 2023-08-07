package net.minecraftforge.eventbus;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.ParallelEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

class ListenerList {
    // Null if the list needs to be rebuilt
    @Nullable
    private volatile Consumer<Event>[] listeners = null;
    private final ArrayList<ArrayList<Consumer<Event>>> priorities;
    private final Object lock = new Object();
    private final boolean isParallel;

    ListenerList(Class<? extends Event> eventType)
    {
        int count = EventPriority.values().length;
        priorities = new ArrayList<>(count);

        for (int x = 0; x < count; x++)
        {
            priorities.add(new ArrayList<>());
        }

        isParallel = ParallelEvent.class.isAssignableFrom(eventType);
    }

    /**
     * Returns a full list of all listeners for all priority levels.
     * Including all parent listeners.
     *
     * List is returned in proper priority order.
     *
     * Automatically rebuilds the internal Array cache if its information is out of date.
     *
     * @return Array containing listeners
     */
    public Consumer<Event>[] getListeners()
    {
        Consumer<Event>[] ret = listeners;
        if (listeners == null) {
            synchronized (lock) {
                listeners = ret = buildCache();
            }
        }
        return ret;
    }

    /**
     * Rebuild the local Array of listeners, returns early if there is no work to do.
     */
    @SuppressWarnings("unchecked")
    private Consumer<Event>[] buildCache()
    {
        ArrayList<Consumer<Event>> ret = new ArrayList<>();
        Arrays.stream(EventPriority.values()).forEach(value -> {
            List<Consumer<Event>> listeners = priorities.get(value.ordinal());
            if (listeners.size() > 0) {
                ret.add(value); //Add the priority to notify the event of its current phase.
                if (isParallel) {
                    ret.add(new ParallelListenerGroup(listeners.toArray(Consumer[]::new)));
                } else {
                    ret.addAll(listeners);
                }
            }
        });
        return ret.toArray(Consumer[]::new);
    }

    public void register(EventPriority priority, Consumer<Event> listener)
    {
        synchronized (lock) {
            priorities.get(priority.ordinal()).add(listener);
            listeners = null;
        }
    }
}
