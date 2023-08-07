package net.minecraftforge.eventbus.api;

/**
 * Base class for events that the event bus should fire in parallel.
 * <p>
 * This event will <b>always</b> fire in parallel across listeners with the same {@link EventPriority}.
 * The listeners will receive the <b>same instance</b> of the event,
 * so make sure that any state modification function is thread-safe.
 */
public class ParallelEvent extends Event {
}
