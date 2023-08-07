/*
 * Minecraft Forge
 * Copyright (c) 2016.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.eventbus;

import net.jodah.typetools.TypeResolver;
import net.minecraftforge.eventbus.api.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static net.minecraftforge.eventbus.LogMarkers.EVENTBUS;

public class EventBus implements IEventExceptionHandler, IEventBus {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final boolean checkTypesOnDispatchProperty = Boolean.parseBoolean(System.getProperty("eventbus.checkTypesOnDispatch", "false"));
    private final boolean trackPhases;


    private final LockHelper<Class<?>, ListenerList> listenerLists = new LockHelper<>(IdentityHashMap::new);
    private final Set<Object> registeredListeners = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final IEventExceptionHandler exceptionHandler;
    private volatile boolean shutdown = false;

    private final Class<?> baseType;
    private final boolean checkTypesOnDispatch;

    @SuppressWarnings("unused")
    private EventBus() {
        exceptionHandler = this;
        this.trackPhases = true;
        this.baseType = Event.class;
        this.checkTypesOnDispatch = checkTypesOnDispatchProperty;
    }

    private EventBus(final IEventExceptionHandler handler, boolean trackPhase, boolean startShutdown, Class<?> baseType, boolean checkTypesOnDispatch) {
        if (handler == null) exceptionHandler = this;
        else exceptionHandler = handler;
        this.trackPhases = trackPhase;
        this.shutdown = startShutdown;
        this.baseType = baseType;
        this.checkTypesOnDispatch = checkTypesOnDispatch || checkTypesOnDispatchProperty;
    }

    public EventBus(final BusBuilderImpl busBuilder) {
        this(busBuilder.exceptionHandler, busBuilder.trackPhases, busBuilder.startShutdown,
             busBuilder.markerType, busBuilder.checkTypesOnDispatch);
    }

    private void registerClass(final Class<?> clazz) {
        Arrays.stream(clazz.getMethods()).
                filter(m->Modifier.isStatic(m.getModifiers())).
                filter(m->m.isAnnotationPresent(SubscribeEvent.class)).
                forEach(m->registerListener(clazz, m, m));
    }

    private Optional<Method> getDeclMethod(final Class<?> clz, final Method in) {
        try {
            return Optional.of(clz.getDeclaredMethod(in.getName(), in.getParameterTypes()));
        } catch (NoSuchMethodException nse) {
            return Optional.empty();
        }

    }
    private void registerObject(final Object obj) {
        final HashSet<Class<?>> classes = new HashSet<>();
        typesFor(obj.getClass(), classes);
        Arrays.stream(obj.getClass().getMethods()).
                filter(m->!Modifier.isStatic(m.getModifiers())).
                forEach(m -> classes.stream().
                        map(c->getDeclMethod(c, m)).
                        filter(rm -> rm.isPresent() && rm.get().isAnnotationPresent(SubscribeEvent.class)).
                        findFirst().
                        ifPresent(rm->registerListener(obj, m, rm.get())));
    }


    private void typesFor(final Class<?> clz, final Set<Class<?>> visited) {
        if (clz.getSuperclass() == null) return;
        typesFor(clz.getSuperclass(),visited);
        Arrays.stream(clz.getInterfaces()).forEach(i->typesFor(i, visited));
        visited.add(clz);
    }

    @Override
    public void register(final Object target)
    {
        // TODO: shouldn't we remove this check?
        if (registeredListeners.contains(target))
        {
            return;
        }
        registeredListeners.add(target);

        if (target.getClass() == Class.class) {
            registerClass((Class<?>) target);
        } else {
            registerObject(target);
        }
    }

    private void registerListener(final Object target, final Method method, final Method real) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != 1)
        {
            throw new IllegalArgumentException(
                    "Method " + method + " has @SubscribeEvent annotation. " +
                    "It has " + parameterTypes.length + " arguments, " +
                    "but event handler methods require a single argument only."
            );
        }

        Class<?> eventType = parameterTypes[0];

        if (!Event.class.isAssignableFrom(eventType))
        {
            throw new IllegalArgumentException(
                    "Method " + method + " has @SubscribeEvent annotation, " +
                            "but takes an argument that is not an Event subtype : " + eventType);
        }
        if (baseType != Event.class && !baseType.isAssignableFrom(eventType))
        {
            throw new IllegalArgumentException(
                    "Method " + method + " has @SubscribeEvent annotation, " +
                            "but takes an argument that is not a subtype of the base type " + baseType + ": " + eventType);
        }

        if (!Modifier.isPublic(method.getModifiers()))
        {
            throw new IllegalArgumentException("Failed to create ASMEventHandler for " + target.getClass().getName() + "." + method.getName() + Type.getMethodDescriptor(method) + " it is not public and our transformer is disabled");
        }

        try {
            Consumer<Event> unreflectedListener = EventListenerUnreflection.unreflectMethod(method, target);

            var subscribeInfo = method.getAnnotation(SubscribeEvent.class);
            addListener(subscribeInfo.priority(), subscribeInfo.receiveCanceled(), (Class<? extends Event>) eventType, unreflectedListener);
        } catch (Throwable e) {
            LOGGER.error(EVENTBUS, "Error registering event handler: {} {}", eventType, method, e);
        }
    }

    @Override
    public <T extends Event> void addListener(final EventPriority priority, final boolean receiveCancelled, final Consumer<T> consumer) {
        addListener(priority, receiveCancelled, getEventClass(consumer), consumer);
    }

    @SuppressWarnings("unchecked")
    private <T extends Event> Class<T> getEventClass(Consumer<T> consumer) {
        final Class<T> eventClass = (Class<T>) TypeResolver.resolveRawArgument(Consumer.class, consumer.getClass());
        if ((Class<?>)eventClass == TypeResolver.Unknown.class) {
            LOGGER.error(EVENTBUS, "Failed to resolve handler for \"{}\"", consumer.toString());
            throw new IllegalStateException("Failed to resolve consumer event type: " + consumer.toString());
        }
        return eventClass;
    }

    @Override
    public <T extends Event> void addListener(final EventPriority priority, final boolean receiveCancelled, final Class<T> eventType, final Consumer<? super T> consumer) {
        if (baseType != Event.class && !baseType.isAssignableFrom(eventType)) {
            throw new IllegalArgumentException(
                    "Listener for event " + eventType + " takes an argument that is not a subtype of the base type " + baseType);
        }

        if (Modifier.isAbstract(eventType.getModifiers())) {
            throw new IllegalArgumentException("Cannot register a listener for abstract event class " + eventType);
        }

        if (EventAnnotationHelper.isCancelable(eventType) && ParallelEvent.class.isAssignableFrom(eventType)) {
            throw new IllegalArgumentException("Event class " + eventType + " is parallel, it cannot be cancelable");
        }

        @SuppressWarnings("unchecked")
        var castConsumer = (Consumer<Event>) consumer;
        Consumer<Event> listener = !EventAnnotationHelper.isCancelable(eventType) || receiveCancelled ? castConsumer : new CanceledEventFilter(castConsumer);
        listenerLists.computeIfAbsent(eventType, () -> new ListenerList(eventType)).register(priority, listener);
    }

    @Override
    public boolean post(Event event) {
        return post(event, Consumer::accept);
    }

    @Override
    public boolean post(Event event, IEventBusInvokeDispatcher wrapper)
    {
        if (shutdown) return false;
        if (checkTypesOnDispatch && !baseType.isInstance(event))
        {
            throw new IllegalArgumentException("Cannot post event of type " + event.getClass().getSimpleName() + " to this event. Must match type: " + baseType.getSimpleName());
        }

        ListenerList listenerList = listenerLists.get(event.getClass());
        if (listenerList == null) {
            return false; // No listener
        }

        Consumer<Event>[] listeners = listenerList.getListeners();
        int index = 0;
        try
        {
            for (; index < listeners.length; index++)
            {
                if (!trackPhases && Objects.equals(listeners[index].getClass(), EventPriority.class)) continue;
                wrapper.invoke(listeners[index], event);
            }
        }
        catch (Throwable throwable)
        {
            exceptionHandler.handleException(this, event, listeners, index, throwable);
            throw throwable;
        }
        return event.isCanceled();
    }

    @Override
    public void handleException(IEventBus bus, Event event, Consumer<Event>[] listeners, int index, Throwable throwable)
    {
        LOGGER.error(EVENTBUS, ()->new EventBusErrorMessage(event, index, listeners, throwable));
    }

    @Override
    public void shutdown()
    {
        LOGGER.fatal(EVENTBUS, "EventBus shutting down - future events will not be posted.", new Exception("stacktrace"));
        this.shutdown = true;
    }

    @Override
    public void start() {
        this.shutdown = false;
    }
}
