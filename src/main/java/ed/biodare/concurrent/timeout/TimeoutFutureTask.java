/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ed.biodare.concurrent.timeout;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of TimeoutFuture that can be used in services implementing timeout using delay queue. 
 * <p>
 * Upon its starts (run) the future can insert itself in the delayed queue, with delay determined by this feature timeout
 * parameters. The managing service can then use entries for delay queue to cancel them (timeout) them once they reach their deadline.
 * <p>To reduce number of encapsulating entities this class also supports completion listeners, using queue to which it inserts itself once
 * the future is done (either cause it run successfully or has been cancelled). That way it can be used in the completion service implementation.
 * <p>The timeout method is implemented by calling cancel on self and setting the timeout flag.
 * <p>The run method starts only if it is before the deadline, and it inserts this future in the delay queue of tasks to be timed out.
 * <p>The done method, removes itself from the timedOut queue and places itself in the completed queue if such exists.
 * @author tzielins
 */
class TimeoutFutureTask<T> extends FutureTask<T> implements TimeoutFuture<T> {

    /**
     * Deadline for this task execution, used to compute the getDelay method for delay queue.
     */
    private long waitTill = Long.MAX_VALUE;
    
    /**
     * Requested timeout parameter for this task in miliseconds.
     */
    private final long timeout;
    /**
     * Delay queue storing futures to be timed out using they timeout parameters to decide on the order.
     */
    private final DelayQueue<TimeoutFuture<?>> timedOut;
    /**
     * If not null the completion queue into which future will be inserted once it is done. Used to impelement completion service.
     */
    private final Queue<TimeoutFuture<?>> completionQueue;
    
    /**
     * Imposed deadline after reaching which task should not start running nor continue execution. 
     */
    private final long deadline;
    
    /**
     * Flag for the timeout status.
     */
    private final AtomicBoolean isTimedOut = new AtomicBoolean(false);
    
    private final Optional<FutureStorable> task;
    
    /**
     * Creates new future which will never be timed out
     * @param callable task for this future
     * @param timedOutQueue delay queue into which this future should be inserted to implement its timeing out
     */
    public TimeoutFutureTask(Callable<T> callable, DelayQueue<TimeoutFuture<?>> timedOutQueue) {
        this(callable,timedOutQueue,Long.MAX_VALUE,TimeUnit.MILLISECONDS);
    }

    /**
     * Creates new future which will never be timed out
     * @param runnable task for this future
     * @param result returned value upon completion
     * @param timedOutQueue delay queue into which this future should be inserted to implement its timing out
     */
    public TimeoutFutureTask(Runnable runnable, T result,DelayQueue<TimeoutFuture<?>> timedOutQueue) {
        this(Executors.callable(runnable, result),timedOutQueue);
    }
    
    /**
     * Creates new future which represent a task which execution should be terminated after given timeout
     * @param runnable task for this future
     * @param result returned value upon completion
     * @param timedOutQueue delay queue into which this future should be inserted once started to implement its timing out
     * @param timeout value of timeout (counted from beginning of the future execution
     * @param timeoutUnit unit of the timeout
     */
    public TimeoutFutureTask(Runnable runnable, T result, DelayQueue<TimeoutFuture<?>> timedOutQueue,long timeout,TimeUnit timeoutUnit) {
        this(Executors.callable(runnable, result),timedOutQueue,timeout,timeoutUnit);
    }
    
    /**
     * Creates new future which represent a task which execution should be terminated after given timeout
     * @param callable task for this future
     * @param timedOutQueue delay queue into which this future should be inserted once started to implement its timing out
     * @param timeout value of timeout (counted from beginning of the future execution
     * @param timeoutUnit unit of the timeout
     */
    public TimeoutFutureTask(Callable<T> callable, DelayQueue<TimeoutFuture<?>> timedOutQueue,long timeout,TimeUnit timeoutUnit) {
        this(callable, timedOutQueue, timeout, timeoutUnit, Long.MAX_VALUE);
    }
    
    /**
     * Creates new future which represent a task which execution should be terminated after given timeout or before the given deadline is reached whichever
     * is faster
     * @param callable task for this future
     * @param timedOutQueue delay queue into which this future should be inserted once started to implement its timing out
     * @param timeout value of timeout (counted from beginning of the future execution
     * @param timeoutUnit unit of the timeout
     * @param deadline time in miliseconds after which the task should not start execution nor continue its execution
     */
    public TimeoutFutureTask(Callable<T> callable, DelayQueue<TimeoutFuture<?>> timedOutQueue,long timeout,TimeUnit timeoutUnit,long deadline) {
        this(callable,timedOutQueue,null,timeout,timeoutUnit,deadline);
    }
    
    /**
     * Creates new future which represent a task which execution should be terminated after given timeout or before the given deadline is reached whichever
     * is faster
     * @param callable task for this future
     * @param timedOutQueue delay queue into which this future should be inserted once started to implement its timing out
     * @param completionQueue if not null, a queue into which this task will be inserted once done (both successfully or cancelled)
     * @param timeout value of timeout (counted from beginning of the future execution
     * @param timeoutUnit unit of the timeout
     * @param deadline time in miliseconds after which the task should not start execution nor continue its execution
     */
    public TimeoutFutureTask(Callable<T> callable, DelayQueue<TimeoutFuture<?>> timedOutQueue,Queue<TimeoutFuture<?>> completionQueue,long timeout,TimeUnit timeoutUnit,long deadline) {
        super(callable);
        if (timedOutQueue == null) throw new IllegalArgumentException("TimedOutQueue cannot be null");
        this.timedOut = timedOutQueue;
        this.completionQueue = completionQueue;
        this.timeout = TimeUnit.MILLISECONDS.convert(timeout, timeoutUnit);
        this.deadline = deadline;
        
        this.task = (callable instanceof FutureStorable) ? Optional.of((FutureStorable)callable) : Optional.empty();
            
    }

    @Override
    public boolean timeOut() {
        //System.out.println("Req for Timeout: "+this.hashCode()+" "+isTimedOut.get());
        if (isDone() || isTimedOut.get()) {
            //System.out.println("Ignoring timeout of "+this.hashCode());
            return isTimedOut.get();
        }
    
        //System.out.println("Timeout: "+this.hashCode());
        isTimedOut.compareAndSet(false, cancel(true));
        //System.out.println("Timeout: "+this.hashCode()+" "+isTimedOut.get());
        return isTimedOut.get();
    }
    
    @Override
    public boolean isTimedOut() {
        return isTimedOut.get();
    }

    @Override
    public void run() {
        //we only run if have not reached the deadlin
        if (deadline < System.currentTimeMillis()) {
            //we are already too late to do something
            //System.out.println(hashCode()+" Task not running as it is already after its deadline");
            timeOut();
            return;
        }
        
        long max = Long.MAX_VALUE - System.currentTimeMillis()-1;
        if (timeout >= max) waitTill = Long.MAX_VALUE;
        else waitTill = System.currentTimeMillis()+timeout;
        
        if (waitTill > deadline) waitTill = deadline;
        
        //System.out.println(hashCode()+" will wait till: "+waitTill);
        
        timedOut.put(this);
        super.run();
    }

    @Override
    protected void done() {
        super.done();
        //System.out.println(hashCode()+" done");
        timedOut.remove(this);
        if (completionQueue!=null) completionQueue.offer(this);
        
    }

    @Override
    public T get() throws InterruptedException, ExecutionException, CancellationException {
        try {
            return super.get();
        } catch (CancellationException e) {
            if (isTimedOut()) throw new TimeoutCancellationException("Future was timed out");
            else throw e;
        }
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException, CancellationException {
        try {
            return super.get(timeout, unit);
        } catch (CancellationException e) {
            if (isTimedOut()) throw new TimeoutCancellationException("Future was timed out");
            else throw e;
        }
    }
    
    
    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(waitTill-System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        long diff;
        
        if (o == null) return -1;
        
        if (o instanceof TimeoutFutureTask) {
            diff = waitTill - ((TimeoutFutureTask)o).waitTill;
        } else {
            diff = getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS);
        }
        
        if (diff == 0) return 0;
        return (diff < 0 ? -1 : 1);
    }

    @Override
    public Optional<FutureStorable> task() {
        return task;
    }

    
 
    
    
}
