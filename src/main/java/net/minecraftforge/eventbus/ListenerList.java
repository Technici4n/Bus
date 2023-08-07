package net.minecraftforge.eventbus;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

class ListenerList {
    private static final Consumer<Event>[] PHASE_SETTERS;
    static {
        EventPriority[] priorities = EventPriority.values();
        //noinspection unchecked
        PHASE_SETTERS = new Consumer[priorities.length];
        for (int i = 0; i < priorities.length; i++) {
            var priority = priorities[i];
            PHASE_SETTERS[i] = event -> event.setPhase(priority);
        }
    }

    // Null if the list needs to be rebuilt
    @Nullable
    private volatile Consumer<Event>[] listeners = null;
    private final ArrayList<ArrayList<Consumer<Event>>> priorities;
    private final Object lock = new Object();

    ListenerList()
    {
        int count = EventPriority.values().length;
        priorities = new ArrayList<>(count);

        for (int x = 0; x < count; x++)
        {
            priorities.add(new ArrayList<>());
        }
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
                ret.add(PHASE_SETTERS[value.ordinal()]); //Add the priority to notify the event of its current phase.
                ret.addAll(listeners);
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
