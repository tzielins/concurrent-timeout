/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ed.biodare.concurrent.timeout;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;

/**
 *
 * @author tzielins
 */
public class BusySleeper implements FutureStorable<Integer> {

        int delayMili;
        int iter;
        boolean finished = false;
        public BusySleeper(int delayMili) {
            
            if (delayMili < 10) throw new IllegalArgumentException("I am not so fast to finish in "+delayMili);
            this.delayMili = delayMili;
        }
        
        @Override
        public Integer call() {
            
            boolean work = true;
            long deadline = System.currentTimeMillis()+delayMili;
            
            Random rnd = new Random();
            Map<Integer, Double> vals = new HashMap<>();
            
            for (int i = 0; i< 500; i++) {
                vals.put(i, 100*rnd.nextDouble());
            }
            
            if (System.currentTimeMillis() >= deadline) work = false;
            
            iter = 0;
            double res = 0;
            while(work) {
                vals.keySet().forEach( ix -> {
                    double val = vals.get(ix);
                    val = Math.cos(val)*Math.cos(rnd.nextDouble());
                    vals.put(ix, val);
                });
                res += Math.cos(vals.values().stream().mapToDouble( v -> v).sum());
                iter++;
                if (System.currentTimeMillis() >= deadline) work = false;
                if (Thread.interrupted()) throw new RuntimeException("Interrupted");
            }
            //System.out.println("For "+delayMili+" iter: "+iter);
            finished = true;
            return (int)res;
        }
        
        
    
}
