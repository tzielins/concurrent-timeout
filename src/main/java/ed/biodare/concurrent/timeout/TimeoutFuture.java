/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ed.biodare.concurrent.timeout;

import java.util.Optional;
import java.util.concurrent.Delayed;
import java.util.concurrent.RunnableFuture;

/**
 * Future that represents task that may be timedout.The timedout task is cancelled task with its timedout flag set.Also the implementation may change get methods symantics and throw TimeoutCancellationException if
 get is called on timed out future. 
 * @author Tomasz Zielinski <tomasz.zielinski@ed.ac.uk>
 * @param <V>
 */
public interface TimeoutFuture<V> extends RunnableFuture<V>, Delayed {
    
    /**
     * Checks if the underlying task has been timed out. It can be true only if task has been cancelled due to timeout,
     * which means that the isDone and isCancelled will also return true.
     * <p>Same like with cancel, there is no guarantee that the thread running this future actually stopped, only that it has been signal to do so.
     * @return true if future has been timed out.
     */
    public boolean isTimedOut();
    
    /**
     * Attempts to timeout a future. Future can be timed out only if it is still running or have not started yet, otherwise
     * the timeout is ignored.
     * @return true if timing out of the future was successful, similar like with cancel, returning true does not guarantee that that
     * the thread running the future actually stop executing.
     */
    public boolean timeOut();
    
    public Optional<FutureStorable> task();
}
