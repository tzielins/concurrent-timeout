/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ed.biodare.concurrent.timeout;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author tzielins
 */
public class TimeoutFutureTaskTest {
    
    public TimeoutFutureTaskTest() {
    }
    
    Callable<Integer> callable;
    DelayQueue<TimeoutFuture<?>> timedOutQueue;
    Queue<TimeoutFuture<?>> completionQueue;
    long timeout;
    TimeUnit timeoutUnit;
    long deadline;
    
    TimeoutFutureTask<Integer> instance;
    

    
    @BeforeEach
    public void setUp() {
        callable = mock(Callable.class);
        timedOutQueue = mock(DelayQueue.class);
        completionQueue = mock(Queue.class);
        timeout = 1000;
        timeoutUnit = TimeUnit.MILLISECONDS;
        deadline = System.currentTimeMillis()+60*1000;
        instance = new TimeoutFutureTask<>(callable,timedOutQueue, completionQueue, timeout, timeoutUnit, deadline);
    }

    @Test
    public void testSetup() {
        assertNotNull(instance);
    }
    
    @Test
    public void delayGivesRemainingTime() {
        
        assertEquals(Long.MAX_VALUE-System.currentTimeMillis(), instance.getDelay(TimeUnit.MILLISECONDS));
        instance.run();
        long d = instance.getDelay(TimeUnit.MILLISECONDS);
        assertTrue(d <= timeout);
        assertTrue(d > timeout - 100);
        
    }
    
    @Test
    public void comparesWithDelayedCorrectly() {
        Delayed other = null;
        
        instance.run();
        
        assertEquals(-1, instance.compareTo(other));
        
        other = instance;
        assertEquals(0, instance.compareTo(other));
        
        
        
        other = mock(Delayed.class);
        when(other.getDelay(TimeUnit.MILLISECONDS)).thenReturn(10L);
        
        assertEquals(1, instance.compareTo(other));
        
        when(other.getDelay(TimeUnit.MILLISECONDS)).thenReturn(2000L);
        assertEquals(-1, instance.compareTo(other));
        
        when(other.getDelay(TimeUnit.MILLISECONDS)).thenReturn(instance.getDelay(TimeUnit.MILLISECONDS));
        assertEquals(0, instance.compareTo(other));
    }
    
    @Test
    public void taskGivesStoredIfStorable() {
        
        assertFalse(instance.task().isPresent());
        
        callable = mock(FutureStorable.class);
        instance = new TimeoutFutureTask<>(callable,timedOutQueue, completionQueue, timeout, timeoutUnit, deadline);
        assertTrue(instance.task().isPresent());
        assertSame(callable, instance.task().get());
        
    }
    
    @Test
    public void getGivesComputedValue() throws Exception {
        when(callable.call()).thenReturn(2);
        
        instance.run();
        
        assertTrue(instance.isDone());
        assertFalse(instance.isCancelled());
        assertFalse(instance.isTimedOut());
        assertEquals(2, (int)instance.get());
        
        callable = new Sleeper(10, 3);
        
        instance = new TimeoutFutureTask<>(callable,timedOutQueue, completionQueue, timeout, timeoutUnit, deadline);
        instance.run();
        
        assertTrue(instance.isDone());
        assertFalse(instance.isCancelled());
        assertFalse(instance.isTimedOut());
        assertEquals(3, (int)instance.get());
    }
    
    @Test
    public void getRaisesTimeoutIfTimeouted() throws Exception {
        when(callable.call()).thenReturn(2);
        
        instance.timeOut();
        instance.run();
        
        assertTrue(instance.isDone());
        assertTrue(instance.isCancelled());
        assertTrue(instance.isTimedOut());
        
        try {
            assertEquals(2, (int)instance.get());
        } catch (TimeoutCancellationException e) {}
        
    }   
    
    @Test
    public void doneRemovesFromTimedoutQueue() {
        
        instance.done();
        verify(timedOutQueue).remove(instance);
    }

    @Test
    public void donePutsIntoCompletedQueue() {
        
        instance.done();
        verify(completionQueue).offer(instance);
    } 
    
    @Test
    public void runDoesNotRunAfterDeadline() throws Exception {
        deadline = System.currentTimeMillis()-1;

        instance = new TimeoutFutureTask<>(callable,timedOutQueue, completionQueue, timeout, timeoutUnit, deadline);
        instance.run();

        verify(callable, never()).call();
    }
    
    @Test
    public void runQuesForTimeout() throws Exception {

        instance.run();

        verify(timedOutQueue).put(instance);
        verify(callable).call();
    }    
    
}
