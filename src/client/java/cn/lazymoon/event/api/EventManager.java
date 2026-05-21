package cn.lazymoon.event.api;

import cn.lazymoon.event.api.event.Cancellable;
import cn.lazymoon.event.api.event.Event;
import cn.lazymoon.event.api.handler.Handler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Manages registering, unregistering and firing of events
 *
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-26
 */
public class EventManager  {
    private final Map<Class<? extends Event>, Set<Handler>> eventHandlers;

    public EventManager() {
        this.eventHandlers = new HashMap<>();
    }

    /**
     * Registers one or more objects to associate their methods with event annotations and stores them in the event handler.
     *
     * @param listener One or more objects to register.
     */
    public void register(Object... listener) {
        for(Object object : listener){
            register(object);
        }
    }

    /**
     * Registers an object to associate its methods with event annotations and stores them in the event handler.
     *
     * @param listener The object to register.
     */
    public void register(Object listener) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            if (isEventHandler(method)) {
                registerEventHandler(method, listener);
            }
        }
    }

    /**
     * Unregisters an object, removing its associated methods from the event handler.
     *
     * @param listener The object to unregister.
     */
    public void unregister(Object listener) {
        for (Iterator<Set<Handler>> iterator = eventHandlers.values().iterator(); iterator.hasNext(); ) {
            Set<Handler> handlers = iterator.next();
            handlers.removeIf(handler -> handler.getListener().equals(listener));

            if (handlers.isEmpty()) {
                iterator.remove();
            }
        }
    }

    /**
     * Unregisters every event handler
     */
    public void unregisterAllListeners() {
        eventHandlers.clear();
    }

    /**
     * Fires an event to all event handlers that are registered
     * If the event handler is ignoring cancelled and the
     * event is cancelled then the handler will not be called
     *
     * @param event the event to fire
     * @param <T> the type of event
     * @return the event that has been modified by all of the handlers
     * @see Cancellable
     */
    public <T extends Event> T call(T event) {
        List<Handler> sortedHandlers = new ArrayList<>();

        Class<?> clazz = event.getClass();

        // Traverse the inheritance hierarchy adding all handlers until we reach Object.class
        while (clazz != Object.class) {
            // Check if class has handlers
            Set<Handler> classHandlers = eventHandlers.get(clazz);
            if (classHandlers != null) {
                sortedHandlers.addAll(classHandlers);
            }

            // Check if any implemented interfaces have handlers
            for (Class<?> i : clazz.getInterfaces()) {
                Set<Handler> interfaceHandlers = eventHandlers.get(i);
                if (interfaceHandlers != null) {
                    sortedHandlers.addAll(interfaceHandlers);
                }
            }

            clazz = clazz.getSuperclass();
        }

        // If this event actually has handlers
        if (!sortedHandlers.isEmpty()) {
            // Sort based on priorities
            sortedHandlers.sort(Comparator.comparing(Handler::getPriority));

            Cancellable cancellable = event instanceof Cancellable ? (Cancellable) event : null;

            for (Handler handler : sortedHandlers) {
                // If the event is cancellable, the handler is ignoringCancelled and the event is cancelled
                if (cancellable != null && handler.isIgnoringCancelled() && cancellable.isCancelled()) {
                    continue;
                }

                // Call the event handler
                invoke(handler, event);
            }
        }

        return event;
    }

    /**
     * Registers the method as an event handler
     *
     * @param method the method that is being registered
     * @param listener the listener of which the method belongs to
     */
    private void registerEventHandler(Method method, Object listener) {
        Class<? extends Event> clazz = method.getParameterTypes()[0].asSubclass(Event.class);

        if (!method.isAccessible()) {
            method.setAccessible(true);
        }

        Handler handler = new Handler(listener, method, method.getAnnotation(EventTarget.class));
        eventHandlers.computeIfAbsent(clazz, k -> new HashSet<>()).add(handler);
    }

    /**
     * Checks if the given method is a valid event handler
     * The method must have the EventHandler annotation
     * and have a singular parameter that is an INSTANCE of event
     *
     * @param method the method to check
     * @return true if the method is a valid event handler
     */
    private static boolean isEventHandler(Method method) {
        Class<?>[] parameters = method.getParameterTypes();
        return parameters.length == 1 && Event.class.isAssignableFrom(parameters[0]) && method.isAnnotationPresent(EventTarget.class);
    }

    /**
     * Invokes the handlers method with the event as a parameter
     *
     * @param handler the handler whose method should be invoked
     * @param event the event that is being passed in
     */
    private static void invoke(Handler handler, Event event) {
        try {
            // 垃圾代码 - 混淆视听
            int[] dummyArray = {1, 2, 3, 4, 5};
            int sum = 0;
            for (int num : dummyArray) {
                sum += num;
                if (sum % 2 == 0) {
                    sum *= 2;
                }
            }
            
            String dummyText = "Event";
            StringBuilder sb = new StringBuilder();
            for (char c : dummyText.toCharArray()) {
                sb.append(c);
            }
            
            boolean dummyFlag = false;
            while (!dummyFlag) {
                dummyFlag = true;
            }
            
            handler.getMethod().invoke(handler.getListener(), event);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}