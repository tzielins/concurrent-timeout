/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ed.biodare.concurrent.timeout;

import java.util.concurrent.CancellationException;

/**
 * Exception indicating that the result of a value-producing task,
 * such as a {@link FutureTask}, cannot be retrieved because the task
 * was cancelled due to its timeout.
 * @author Zielu
 */
public class TimeoutCancellationException extends CancellationException {
    
    TimeoutCancellationException() {
        super();
    }
    
    TimeoutCancellationException(String msg) {
        super(msg);
    }
}
