package net.minecraftforge.eventbus;

import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventListener;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

class ListenerList {
    // Null if the list needs to be rebuilt
    @Nullable
    private volatile IEventListener[] listeners = null;
    private final ArrayList<ArrayList<IEventListener>> priorities;
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
    public IEventListener[] getListeners()
    {
        IEventListener[] ret = listeners;
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
    private IEventListener[] buildCache()
    {
        ArrayList<IEventListener> ret = new ArrayList<>();
        Arrays.stream(EventPriority.values()).forEach(value -> {
            List<IEventListener> listeners = priorities.get(value.ordinal());
            if (listeners.size() > 0) {
                ret.add(value); //Add the priority to notify the event of its current phase.
                ret.addAll(listeners);
            }
        });
        return ret.toArray(IEventListener[]::new);
    }

    public void register(EventPriority priority, IEventListener listener)
    {
        synchronized (lock) {
            priorities.get(priority.ordinal()).add(listener);
            listeners = null;
        }
    }
}
