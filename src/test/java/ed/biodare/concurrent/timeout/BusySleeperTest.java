/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ed.biodare.concurrent.timeout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author tzielins
 */
public class BusySleeperTest {
    
    
    @Test
    public void busyAsAsked() throws Exception {
        
        int sleep = 10;
        BusySleeper instance = new BusySleeper(sleep);
        
        long sT = System.currentTimeMillis();
        instance.call();
        long dur = System.currentTimeMillis()-sT;
        
        assertTrue(dur > sleep);
        assertTrue(dur < sleep+10);
        
        sleep = 200;
        instance = new BusySleeper(sleep);
        
        sT = System.currentTimeMillis();
        instance.call();
        dur = System.currentTimeMillis()-sT;
        
        assertTrue(instance.finished);
        assertTrue(dur > sleep);
        assertTrue(dur < sleep+10);        
    }
    
    @Test
    public void canBeInterrupted() throws Exception {
        
        BusySleeper instance = new BusySleeper(250);
        Thread th = new Thread(() -> instance.call());
        
        th.start();
        Thread.sleep(50);
        th.interrupt();
        //th.stop();
        
        th.join(100);
        assertFalse(th.isAlive());
        assertFalse(instance.finished);
        
        
    }
}
