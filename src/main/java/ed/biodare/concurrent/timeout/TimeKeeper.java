/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ed.biodare.concurrent.timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a task which monitores future queue awaiting futures that should be timedout. 
 * In principle this implementation should work with delay queue and TimeoutFuture as both can simple provide
 * the timeout behaviour (the timeout is set inside the future, the delay queue assures its execution at right time and timekeeper will invoke 
 * the timeout method. However it can use and other blocking queue and timeout tasks as soon as they become available.
 * <p>This implementation awaits tasks for the timedOut queue and call their timeout method.
 * @author tzielins
 */
class TimeKeeper implements Runnable {

        final Logger logger = LoggerFactory.getLogger(this.getClass());
    
        /**
         * If true will print out some status messages
         */
        protected boolean DEBUG = false;
        
        /**
         * Thread which executes this time keeper.
         */
        private final Thread myThread = Executors.defaultThreadFactory().newThread(this);
        /**
         * Queue which will provide futures to be cancelled. Most likely it will be DelayQueue at it will solve the
         * problem of invoking timeout at right moment.
         */
        private final BlockingQueue<TimeoutFuture<?>> timedOut;
        /**
         * Signals runing thread to stop
         */
        volatile boolean stop;
        
        TimeKeeper(BlockingQueue<TimeoutFuture<?>> timedOut,boolean debug) {
            this(timedOut);  
            this.DEBUG = debug;
        }
        
        TimeKeeper(BlockingQueue<TimeoutFuture<?>> timedOut) {
            if (timedOut == null) throw new IllegalArgumentException("TimedOut queue cannot be null");
            
            this.timedOut = timedOut;            
            stop = false;
            //this.myThread = Executors.defaultThreadFactory().newThread(this);
        }
        
        void start() {
            myThread.start();
        }
        
        /**
         * Stomps the timekeeper by signalling its running thread
         */
        void stop() {
            stop = true;
            if (myThread != null) {
                myThread.interrupt();
                //myThread = null;
            }
        }
        
        @Override
        public void run() {
            if (DEBUG) System.out.println(hashCode()+" TimeKeeper starts on "+Thread.currentThread().hashCode());
            for(;;) {
                if (stop) break;
                try {
                    TimeoutFuture<?> task = timedOut.take();

                    task.timeOut();
                    
                } catch (InterruptedException e) {
                    //continue;
                }
                
            }
            
            List<TimeoutFuture<?>> toStop = new ArrayList<>();            
            timedOut.drainTo(toStop);
            
            if (DEBUG) logger.info(hashCode()+" TimeKeeper will cancel upon shutdown: "+toStop.size());
            for(TimeoutFuture<?> task : toStop) task.timeOut();
            if (DEBUG) logger.info(hashCode()+" TimeKeeper stopped on "+Thread.currentThread().hashCode());
        }
        
}

