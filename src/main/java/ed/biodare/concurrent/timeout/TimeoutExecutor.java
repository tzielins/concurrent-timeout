/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ed.biodare.concurrent.timeout;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Interface for executors that supports timeout of executed task.
 * <p>Each job/task can have its own timeout set, which starts counting from the moment when task starts to be actually executed,
 * not from the time when it was submited! Executor can also have a global deadline set which after reaching will results in timing out all
 * the submitted jobs.
 * <p>The inherited submit methods without timeout parameters, do not guarantee undisrupted execution, as their will be submitted under
 * the default timeout control which is set for executor. However, as a convention if not default timeout was explicit set for the executor,
 * the timeout is assumed to be infinity (Long.MAX_VALUE).
 * <p>The tasks which execution time exceeds the limit will be timed-out, ie. they will be cancelled. Its mean that their future
 * isDone() isCancelled() and isTimeOut will return true. The get methods will throw TimeoutCancellationException which is sublass of CancellationException
 * <p>Without setting global deadline or default timeout the executor behaviour is same as for ordinary executors.
 * <p>Implementations must be of course thread save.
 * <p>Concepts:
 * <ul>
 * <li>TimeoutFuture - future which can be timed out. Same bahaviour as cancelled but the flag for timeout is additionally set.</li>
 * <li>default timeout - timeout which is assigned to all the tasks for which not explicit timeout was provided (the superclass submit methods).
 * If not set it will equal inifnity.</li>
 * <li>global deadline - point in time after reaching which all the tasks submitted to the executor will be timedout.</li>
 * </ul>
 * @author tzielins
 */
public interface TimeoutExecutor extends ExecutorService {
    
    /**
     * Submits a value-returning task for execution and returns a Future representing the pending results of the task. 
     * The Future's get method will return the task's result upon successful completion only if the task could be completed
     * during assigned time. 
     * <p>The time which can be spent to completed the task is limited by the timeout parameter. Once computation exceeds this limit
     * the task will be timed out (cancelled). The time is counted from the moment when the task is actually executed not submitted.
     * <p>The task can be also timed out if executor reaches its global timeout.
     * <p>There is no guarantee that the task will be timed out exactly after reaching timeout as it would be impossible to implement,
     * however implementation will do its best to try to achieve time out as close as possible.
     * @param <T> type of the return value 
     * @param task the task to submit
     * @param timeout how long the task can be run before being timedout
     * @param timeOutUnit unit of the timeout parameters
     * @return a Future representing pending completion of the task, the timeout future posses extra method for providing timeout status of the task
     */
    public <T> TimeoutFuture<T> submit(Callable<T> task,long timeout,TimeUnit timeOutUnit); 
    
    /**
     * Submits a Runnable task for execution and returns a Future representing that task.
     * The Future's get method will return the given result upon successful completion only if the task could be completed
     * during assigned time..
     * <p>The time which can be spent to completed the task is limited by the timeout parameter. Once computation exceeds this limit
     * the task will be timed out (cancelled). The time is counted from the moment when the task is actually executed not submitted.
     * <p>The task can be also timed out if executor reaches its global timeout.
     * <p>There is no guarantee that the task will be timed out exactly after reaching timeout as it would be impossible to implement,
     * however implementation will do its best to try to achieve time out as close as possible.
     * @param task the task to submit
     * @param timeout how long the task can be run before being timedout
     * @param timeOutUnit unit of the timeout parameters
     * @return a Future representing pending completion of the task, the timeout future posses extra method for providing timeout status of the task
     */
    public TimeoutFuture<?> submit(Runnable task,long timeout,TimeUnit timeOutUnit); 

    /**
     * Sets the global computation deadline for the executor. 
     * When the system time reaches the provided deadline, currently running jobs as well as all the pending will be marked as timedout (cancelled).
     * <p>The deadline should be set before submitting tasks as the behaviour is undefined if the deadline is changed once some tasks have been already submitted. 
     * Depending on implementation the existing tasks may comply to old or new deadline.
     * <p>There is no guarantee that the tasks will be timed out exactly at reaching the deadline, but implementation should do its best
     * <p>The task submitted after the deadline will be timed out as well.
     * @param deadline point in time after reaching which all the tasks will be timed out. 
     */
    public void setGlobalDeadline(Date deadline);

    /**
     * Resets the global computation deadline for the executor to the infinity. Actually to the time matching Long.MAX_VALUE.
     * <p>The behaviour is undefined if the tasks which have been already submitted. 
     * Depending on implementation the existing tasks may comply to the old deadline or not.
     */
    public void resetGlobalDeadline();

    /**
     * Checks if executor has the global deadline set.
     * @return True if there is global deadline
     */
    public boolean hasGlobalDeadline();
    
    /**
     * Gives value of the global deadline set for the executor.
     * @return point in time when executor will start cancelling all its jobs.
     */
    public Date getGlobalDeadline();
    
    /**
     * Sets value of the default timeout for the executor, ie timeout for the task submitted without explicit timeout.
     * Changing defaultTimeOut does not affect the already submitted tasks.
     * @param timeout value for the default timeout
     * @param timeoutUnit unit of the timeout parameter
     */
    public void setDefaultTimeOut(long timeout,TimeUnit timeoutUnit); 
    
    /**
     * Gives value of the default timeout for this exectuor using provided time unit.
     * @param unit unit in whihc the value should be returrned
     * @return the deafault timeoout value in the provided unit.
     */
    public long getDefaultTimeOut(TimeUnit unit); 
    
}
