/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ed.biodare.concurrent.timeout;

/**
 *
 * @author tzielins
 */
public class Sleeper implements FutureStorable<Integer> {

        int delayMili;
        int value;
        public Sleeper(int delayMili, int value) {
            this.delayMili = delayMili;
            this.value = value;
        }
        
        @Override
        public Integer call() throws Exception {
            
            Thread.sleep(delayMili);
            return value;
        }
        
        
    
}
