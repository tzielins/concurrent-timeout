/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ed.biodare.concurrent.timeout;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author tzielins
 */
public class TimeoutFixPoolExecutorTest {
    
    public TimeoutFixPoolExecutorTest() {
    }
    
    long timeOut;
    TimeUnit timeUnit;
    TimeoutFixPoolExecutor instance;
    int threads;
         
    @BeforeEach
    public void setUp() {
        timeOut = 100;
        timeUnit = TimeUnit.MILLISECONDS;
        threads = 1;
        instance = new TimeoutFixPoolExecutor(threads, timeOut, timeUnit);
    }
    
    

    @Test
    public void noParamsActsLikeExecutor() throws Exception {
        
        instance = new TimeoutFixPoolExecutor(threads);
        
        List<TimeoutFuture<Integer>> futures = new ArrayList<>();
        for (int i = 0; i< 4; i ++) {
            futures.add((TimeoutFuture<Integer>)instance.submit(new Sleeper(50,i)));
        }
        
        instance.shutdown();
        instance.awaitTermination(500, TimeUnit.MILLISECONDS);
        
        for (int i =0; i< 4; i++) {
            TimeoutFuture<Integer> future = futures.get(i);
            assertTrue(future.isDone());
            assertFalse(future.isCancelled());
            assertFalse(future.isTimedOut());
            assertEquals(i, (int)future.get(1, TimeUnit.MILLISECONDS));
        }
    }
    
    @Test
    public void timeOutsCorrectly() throws Exception {
        
        timeOut = 10;
        instance = new TimeoutFixPoolExecutor(threads, timeOut, timeUnit);
        
        List<TimeoutFuture<Integer>> futures = new ArrayList<>();
        for (int i = 0; i< 4; i ++) {
            futures.add((TimeoutFuture<Integer>)instance.submit(new Sleeper(50,i)));
        }
        
        instance.shutdown();
        instance.awaitTermination(500, TimeUnit.MILLISECONDS);
        
        for (int i =0; i< 4; i++) {
            TimeoutFuture<Integer> future = futures.get(i);
            assertTrue(future.isDone());
            assertTrue(future.isCancelled());
            assertTrue(future.isTimedOut());
            try {
                assertEquals(i, (int)future.get(1, TimeUnit.MILLISECONDS));
            } catch (TimeoutCancellationException e) {}
        }
    }  
    
    @Test
    public void timeOutsLongerTasks() throws Exception {
        
        timeOut = 20;
        instance = new TimeoutFixPoolExecutor(threads, timeOut, timeUnit);
        
        List<TimeoutFuture<Integer>> futures = new ArrayList<>();
        futures.add((TimeoutFuture<Integer>)instance.submit(new Sleeper(50,1)));
        futures.add((TimeoutFuture<Integer>)instance.submit(new Sleeper(40,2)));
        futures.add((TimeoutFuture<Integer>)instance.submit(new Sleeper(10,3)));
        
        instance.shutdown();
        instance.awaitTermination(500, TimeUnit.MILLISECONDS);

        TimeoutFuture<Integer> future = futures.get(0);
        
        assertTrue(future.isDone());
        assertTrue(future.isCancelled());
        assertTrue(future.isTimedOut());
        try {
            assertEquals(3, (int)future.get(1, TimeUnit.MILLISECONDS));
        } catch (TimeoutCancellationException e) {}
        
        future = futures.get(1);
        
        assertTrue(future.isDone());
        assertTrue(future.isCancelled());
        assertTrue(future.isTimedOut());
        try {
            assertEquals(3, (int)future.get(1, TimeUnit.MILLISECONDS));
        } catch (TimeoutCancellationException e) {}
        
        future = futures.get(2);
        
        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
        assertFalse(future.isTimedOut());
        assertEquals(3, (int)future.get(1, TimeUnit.MILLISECONDS));
        
    }    
    
    @Test
    public void globalDeadlineCancels() throws Exception {
        
        timeOut = 100;
        instance = new TimeoutFixPoolExecutor(threads, timeOut, timeUnit);
        instance.setGlobalDeadline(new Date(System.currentTimeMillis()+60));
        
        List<TimeoutFuture<Integer>> futures = new ArrayList<>();
        futures.add((TimeoutFuture<Integer>)instance.submit(new Sleeper(50,1)));
        futures.add((TimeoutFuture<Integer>)instance.submit(new Sleeper(50,2)));
        futures.add((TimeoutFuture<Integer>)instance.submit(new Sleeper(50,3)));
        
        instance.shutdown();
        instance.awaitTermination(500, TimeUnit.MILLISECONDS);

        TimeoutFuture<Integer> future = futures.get(0);
        
        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
        assertFalse(future.isTimedOut());
        assertEquals(1, (int)future.get(1, TimeUnit.MILLISECONDS));
        
        future = futures.get(1);
        
        assertTrue(future.isDone());
        assertTrue(future.isCancelled());
        assertTrue(future.isTimedOut());

        future = futures.get(2);
        
        assertTrue(future.isDone());
        assertTrue(future.isCancelled());
        assertTrue(future.isTimedOut());
        
    }  
    
    @Test
    public void timeOutsLongerRunningTasks() throws Exception {
        
        timeOut = 50;
        instance = new TimeoutFixPoolExecutor(threads, timeOut, timeUnit);
        
        List<TimeoutFuture<Integer>> futures = new ArrayList<>();
        futures.add((TimeoutFuture<Integer>)instance.submit(new BusySleeper(100)));
        futures.add((TimeoutFuture<Integer>)instance.submit(new BusySleeper(100)));
        futures.add((TimeoutFuture<Integer>)instance.submit(new BusySleeper(20)));
        
        instance.shutdown();
        instance.awaitTermination(500, TimeUnit.MILLISECONDS);

        TimeoutFuture<Integer> future = futures.get(0);
        
        assertTrue(future.isDone());
        assertTrue(future.isCancelled());
        assertTrue(future.isTimedOut());
        assertFalse(((BusySleeper)future.task().get()).finished);
        try {
            assertEquals(3, (int)future.get(1, TimeUnit.MILLISECONDS));
        } catch (TimeoutCancellationException e) {}
        
        future = futures.get(1);
        
        assertTrue(future.isDone());
        assertTrue(future.isCancelled());
        assertTrue(future.isTimedOut());
        assertFalse(((BusySleeper)future.task().get()).finished);
        try {
            assertEquals(3, (int)future.get(1, TimeUnit.MILLISECONDS));
        } catch (TimeoutCancellationException e) {}
        
        future = futures.get(2);
        
        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
        assertFalse(future.isTimedOut());
        assertTrue(((BusySleeper)future.task().get()).finished);
        
    }    
    
    
    
}
