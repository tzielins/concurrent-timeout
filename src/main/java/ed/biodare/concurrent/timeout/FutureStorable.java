/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ed.biodare.concurrent.timeout;

import java.util.concurrent.Callable;

/**
 * Marking interface to mark tasks (Callable) that should be stored
 * with the feature upon its completion.
 * Normally the callable is null after running the feature. Using classes that
 * implement this interface lets them be stored with the feature so that can be accessed
 * after their execution for example to retrieve the task id.
 * 
 * @param <V> the result type of method {@code call}
 * @author Tomasz Zielinski <tomasz.zielinski@ed.ac.uk>
 */
public interface FutureStorable<V> extends Callable<V> {
    
}
