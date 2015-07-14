package com.aplos.common.application;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class PriorityFutureTask<T> extends FutureTask<T> {
	private int priority;
	
    public PriorityFutureTask(Callable<T> callable, int priority) 
    {
        super(callable);
        this.priority = priority;
    }
    
    @Override
    public void run()
    {
        int originalPriority = Thread.currentThread().getPriority();
        Thread.currentThread().setPriority(priority);
        super.run();
        Thread.currentThread().setPriority(originalPriority);
    }
}
