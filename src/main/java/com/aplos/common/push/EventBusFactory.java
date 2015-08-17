package com.aplos.common.push;

import org.atmosphere.cpr.MetaBroadcaster;

public class EventBusFactory {

    private final EventBus eventBus;
    private static EventBusFactory f = null;

    public EventBusFactory(MetaBroadcaster metaBroadcaster) {
        eventBus = new EventBus(metaBroadcaster);
        f = this;
    }

    /**
     * Return the default factory
     * @return the default factory
     */
    public final static EventBusFactory getDefault() {
        return f;
    }

    /**
     * Return a {@link EventBus}
     * @return a {@link EventBus}
     */
    public EventBus eventBus(){
        return eventBus;
    }

}
