package com.aplos.common.push;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.MetaBroadcaster;

public class EventBus  {
    private final MetaBroadcaster metaBroadcaster; 
    
    public EventBus(MetaBroadcaster metaBroadcaster) {
        this.metaBroadcaster = metaBroadcaster;
    }

    public EventBus publish(Object o) {
        metaBroadcaster.broadcastTo("/*", o);
        return this;
    }

    public EventBus publish(String path, Object o) {
        if (!path.startsWith("/")) path = "/" + path;

        metaBroadcaster.broadcastTo(path, o);
        return this;
    }

    /*public EventBus publish(String path, Object o, final Reply r) {
        metaBroadcaster.addBroadcasterListener(new BroadcasterListenerAdapter() {
            public void onComplete(Broadcaster b) {
                try {
                    r.completed(b.getID());
                } finally {
                    metaBroadcaster.removeBroadcasterListener(this);
                }
            }
        });
        metaBroadcaster.broadcastTo(path, o);
        return this;
    }*/

    public <T> Future<T> schedule(final String path, final T t, int time, TimeUnit unit) {
        final Future<List<Broadcaster>> f = metaBroadcaster.scheduleTo(path, t, time, unit);
        return new WrappedFuture<T>(f, t);
    }

    private final static class WrappedFuture<T> implements Future<T> {

        private final Future<?> f;
        private final T t;

        private WrappedFuture(Future<?> f, T t) {
            this.f = f;
            this.t = t;
        }

        public boolean cancel(boolean b) {
            return f.cancel(b);
        }

        public boolean isCancelled() {
            return f.isCancelled();
        }

        public boolean isDone() {
            return f.isDone();
        }

        public T get() throws InterruptedException, ExecutionException {
            f.get();
            return t;
        }

        public T get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
            f.get(l, timeUnit);
            return t;
        }
    }

}
