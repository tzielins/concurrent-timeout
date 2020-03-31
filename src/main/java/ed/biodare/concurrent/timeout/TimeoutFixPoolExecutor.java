/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ed.biodare.concurrent.timeout;

import java.util.Date;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * Implementation of TimeoutExecutor which is based on ThreadPoolExecutor and a delay queue that provides the timeout functionality.
 * <p>This is a neat approach to TimeoutExecutor that uses the java.concurrent classes. 
 * <br>The executor itself is the extension of ThreadPoolExecutor,
 * constructed with fixed thread pool and open-end queue of tasks in the form of LinkedBlockingQueue, yielding the typical FixPoolExecutor. And all the 'usual'
 * ExecutorService methods are provided by the parent class.
 * <br>To cater for timeout functionality, this implementations overwrites the newTaskFor method so it returns TimeoutFutureTask instead of FutureTask.
 * The TimeoutFuture is configured with timeout and deadline using either the executor default values or the explicit parameters from the submit methods.
 * The TimeoutFutureTask inserts itself into the delay queue of task to be timed out, and its delay method will reflect the desired timeout values. 
 * <br>Executor creates one TimeKeeper thread which 'listens' on the delay queue of the tasks to be timed-out, and calls the task timeout() method
 * once it receives it. Inside the timeout method of TimeoutFutureTask the usual Future.cancel method is called and the timeout flag is set.
 * <br>As the results all the thread governing and synchronisation is done in Java way using the provided implementations of ThreadPoolExecutor, FutureTask and BlockingQueue, with
 * only difference that the future will insert itself into a queue prior to invoking run, and the consumer of this queue (TimeKeeper) will call the future
 * cancel method (indirectly by invoking the timeout method).
 * <p>To reduce number of encapsulating entities this class also supports completion listeners, using queue to which futures insert themselves
 * once they are done (either cause it run successfully or has been cancelled). That way it can be used in the completion service implementation.
 * <p><b>WARNING</b> This implementation achieves timeout by calling the FutureTask.cancel method, which itself calls interrupt on the thread which runs the future task code.
 * It means that although the future will report itself as cancelled (or timed-out) the thread which was executing it may still continue execution of the future code (
 * if the code does not check for interrupted flag and does not break its execution). 
 * <br>As the result, in case of 'non-interruptible' tasks the worker thread from the thread pool will remained occupied (unless the ThreadPoolExecutor
 * has some mechanism to detect so which at this moment it doesn't) and no new task will be able to replace the timed-out one in the working thread.
 * <br>For example if the task is an infinite loop it will never stop
 * and will not release the working thread blocking execution of other tasks.
 * <p>Despite the limitation for non-interruptible tasks, this implementation of TimeoutExecutor should be chosen for most of the cases.
 * <br>The interruptibility of the tasks can be easily tested and fixed before they are used with TimeoutExecutor, so the blocking limitation will not affect the system.
 * <br>At the same time, it is a very neat implementation, using the java provided classes, which will handle all the synchronisation issues thus
 * is inherently solid.
 * @param <V>
 * @see TimeoutForcibleExecutor
 * @see TimeoutExecutor
 * @see TimeoutFutureTask
 * @author tzielins
 */
public class TimeoutFixPoolExecutor<V> extends ThreadPoolExecutor implements TimeoutExecutor {
    
    /**
     * If true it will printout some status messages
     */
    protected final boolean DEBUG = false;//true;
    
    /**
     * If not null a 'listener' queue into which task will be inserted once they are completed (with success or without).
     * Typically it will be a blocking queue.
     */
    private final Queue<TimeoutFuture<?>> completionQueue;
    /**
     * Delay queue of task which should be cancelled due to their timeout. The task delay is based on the task timeout, so the tasks which
     * can be pulled from the queue will be the ones after their timeout.
     */
    private final DelayQueue<TimeoutFuture<?>> timedOut;
    
    /**
     * Listener of the timedOut queue, which runs on separate thread and timeouts (cancel) the tasks which are pulled from the timedOut queue.
     */
    private final TimeKeeper timeKeeper;
    
    /**
     * Point in time in milliseconds, after reaching which all the tasks will become timedout. 
     */
    private volatile long globalDeadline = Long.MAX_VALUE;
    /**
     * The default timeout use for task without explicit time out.
     */
    private volatile long defaultTimeOut;// = Long.MAX_VALUE;
    /**
     * Unit of the defaultTimeOut.
     */
    private volatile TimeUnit defaultTimeOutUnit;// = TimeUnit.MILLISECONDS;
    
    /**
     * Creates new TimeoutExectuor that uses given number of threads. 
     * The default timeout and global deadline are set to infinity, meaning that tasks without explicit timeout parameters will
     * run without any interruption
     * @param nThreads number of threads this executor uses for running the tasks
     */
    public TimeoutFixPoolExecutor(int nThreads) {
        this(nThreads,Long.MAX_VALUE,TimeUnit.MILLISECONDS);
    }
    
    /**
     * Creates new TimeoutExectuor that uses given number of threads, and which will use the provided value for the default timeout.
     * The value of defaultTimeOut is used for all the task with explicit timeout (inserted by the superclass submit methods)
     * @param nThreads number of threads this executor uses for running the tasks
     * @param defaultTimeOut value of timeout for all the tasks without explicit timeout set in submit mehtod
     * @param defaultTimeOutUnit unit for the timeout
     */
    public TimeoutFixPoolExecutor(int nThreads,long defaultTimeOut,TimeUnit defaultTimeOutUnit) {
        this(nThreads, defaultTimeOut, defaultTimeOutUnit, null);
    }
    
    /**
     * Creates new TimeoutExectuor that uses given number of threads, and which will use the provided value for the default timeout.
     * Additionally it will support the task completion listener by inserting the completed tasks into the provided completionQueue.
     * The value of defaultTimeOut is used for all the task with explicit timeout (inserted by the superclass submit methods)
     * @param nThreads number of threads this executor uses for running the tasks
     * @param defaultTimeOut value of timeout for all the tasks without explicit timeout set in submit method
     * @param defaultTimeOutUnit unit for the timeout
     * @param completionQueue if not null the completed tasks will be inserted into this queue (regardless if they finished successfully or not)
     */
    public TimeoutFixPoolExecutor(int nThreads,long defaultTimeOut,TimeUnit defaultTimeOutUnit,Queue<TimeoutFuture<?>> completionQueue) {        
        this(nThreads, defaultTimeOut,defaultTimeOutUnit,
                new LinkedBlockingQueue<Runnable>(), completionQueue);
        
    }    

    
    /**
     * Creates new TimeoutExectuor that uses given number of threads, and which will use the provided value for the default timeout.Additionally it will support the task completion listener by inserting the completed tasks into the provided completionQueue.
     * The value of defaultTimeOut is used for all the task with explicit timeout (inserted by the superclass submit methods)
     * @param nThreads number of threads this executor uses for running the tasks
     * @param defaultTimeOut value of timeout for all the tasks without explicit timeout set in submit method
     * @param defaultTimeOutUnit unit for the timeout
     * @param workQueue the queue to use for holding tasks before they are executed, inherited from superclass
     * @param completionQueue if not null the completed tasks will be inserted into this queue (regardless if they finished successfully or not)
     */
    public TimeoutFixPoolExecutor(int nThreads,long defaultTimeOut,TimeUnit defaultTimeOutUnit,
            BlockingQueue<Runnable> workQueue, Queue<TimeoutFuture<?>> completionQueue) {        
        super(nThreads, nThreads,0L, TimeUnit.MILLISECONDS,workQueue);
        
        this.defaultTimeOut = defaultTimeOut;
        this.defaultTimeOutUnit = defaultTimeOutUnit;
        this.completionQueue = completionQueue;
        this.timedOut = new DelayQueue<>();
        
        //ThreadFactory threadFactory = Executors.defaultThreadFactory();
        this.timeKeeper = new TimeKeeper(timedOut,DEBUG);
        //this.timeKeeper.setThread(threadFactory.newThread(timeKeeper));
        this.timeKeeper.start();
        
        
    }

    @Override
    protected void terminated() {
        super.terminated();
        timeKeeper.stop();
    }

    @Override
    protected void finalize() {
        timeKeeper.stop();
        super.finalize();
    }
    
    
    
    
    @Override
    protected <T> TimeoutFuture<T> newTaskFor(Runnable runnable, T value) {
        return newTaskFor(Executors.callable(runnable, value));
    }

    @Override
    protected <T> TimeoutFuture<T> newTaskFor(Callable<T> callable) {
        return newTaskFor(callable,defaultTimeOut,defaultTimeOutUnit);
    }
    
    protected <T> TimeoutFuture<T> newTaskFor(Callable<T> callable,long timeout,TimeUnit timeUnit) {
        return new TimeoutFutureTask(callable, timedOut, completionQueue,timeout, timeUnit,globalDeadline);
    }

    /*@Override
    public <T> TimeoutFuture<T> submit(Callable<T> task) {
        return submit(task, defaultTimeOut, defaultTimeOutUnit);
    }

    @Override
    public TimeoutFuture<?> submit(Runnable task) {
        return submit(task, defaultTimeOut, defaultTimeOutUnit);
    }*/
    
    
    
    @Override
    public <T> TimeoutFuture<T> submit(Callable<T> task,long timeout,TimeUnit timeOutUnit) {
        TimeoutFuture<T> future = newTaskFor(task, timeout,timeOutUnit);
        execute(future);
        return future;
    }
    
    @Override
    public TimeoutFuture<?> submit(Runnable task,long timeout,TimeUnit timeOutUnit) {
        TimeoutFuture<Object> future = newTaskFor(Executors.callable(task),timeout,timeOutUnit);
        execute(future);
        return future;
    }

    @Override
    public void setDefaultTimeOut(long timeout,TimeUnit timeoutUnit) {
        defaultTimeOut = timeout;
        defaultTimeOutUnit = timeoutUnit;
    }
    
    @Override
    public long getDefaultTimeOut(TimeUnit unit) {
        return unit.convert(defaultTimeOut, defaultTimeOutUnit);
    }
    
    @Override
    public void setGlobalDeadline(Date deadline) {
        globalDeadline = deadline.getTime();
    }
    
    @Override
    public void resetGlobalDeadline() {
        globalDeadline = Long.MAX_VALUE;
    }
    
    @Override
    public boolean hasGlobalDeadline() {
        return globalDeadline < Long.MAX_VALUE;
    }
    
    @Override
    public Date getGlobalDeadline() {        
        return new Date(globalDeadline);
    }
    
}
