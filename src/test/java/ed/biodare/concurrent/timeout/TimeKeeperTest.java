/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ed.biodare.concurrent.timeout;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


/**
 *
 * @author tzielins
 */
public class TimeKeeperTest {
    
    public TimeKeeperTest() {
    }
    
    BlockingQueue<TimeoutFuture<?>> timedOut;
    TimeKeeper instance;
    
    @BeforeEach
    public void setUp() {
        timedOut = new ArrayBlockingQueue<>(10);
        instance = new TimeKeeper(timedOut);
        
    }

    @Test
    public void testLifeCycle() throws Exception {
        
        assertFalse(instance.isRunning());
        instance.start();
        assertTrue(instance.isRunning());
        instance.stop();
        Thread.sleep(10);
        assertFalse(instance.isRunning());
        
    }
    
    @Test
    public void takesTasksFromTheQueueAndCancelsThem() throws Exception {
        
        TimeoutFuture task = mock(TimeoutFuture.class);
        instance.start();
        
        timedOut.offer(task);
        
        Thread.sleep(10);
        assertTrue(timedOut.isEmpty());
        verify(task).timeOut();
        
        instance.stop();
    }
    
}
