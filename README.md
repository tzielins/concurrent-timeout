# concurrent-timeout

A project with TimeoutExecutor an executor serivce which interrupts running tasks if they are not completed within the given timeout.

It extends ThreadPoolExecutor so the execution can be done on multiple threads.

### Usage

        import ed.biodare.concurrent.timeout.TimeoutFixPoolExecutor
        // extecutor = new TimeoutFixPoolExecutor(threads, timeOut, timeUnit);
        TimeoutFixPoolExecutor executor = new TimeoutFixPoolExecutor(4, 1, TimeUnit.SECONDS);        
        
        Future<?> future = executor.submit(() -> longRunningJob());
        
        try {
            future.get();
        } catch (TimeoutCancellationException e) {
            //job run for too long
        }

Creates an executor which by default will time out tasks after one second on their execution.
If the submitted job takes longer than one seconded its corresponding future will be cancelled and thread interrupted.

As with regular futures, the cancellation only works if the run code monitors for Thread.interrupted()
otherwise while the future will be marked as done and cancelled the processor can still be ocuppied with the code execution.
The thread stop() is deprecated so cancellation cannot be forced any more.

        TimeoutFixPoolExecutor executor = new TimeoutFixPoolExecutor(4);        
        executor.setGlobalDeadline(new Date(System.currentTimeMillis()+5000));
        
        for (int i = 0; i< 100; i++) {
            Future<?> future = executor.submit(() -> longRunningJob());
        }

This exectuor does not have timeout set so each task could run till it is completed. 
However it has a global deadline set to 5 seconds into the future. 
As the results any task that has not started withing the first 5 seconds will not be even started, and any
task that started but which runs behind the now+5s will be cancelled.

        executor.submit(task,timeOut, timeUnit);

Executes task with the given execution timeout.

        TimeoutFixPoolExecutor executor = new TimeoutFixPoolExecutor(threads, timeOut, timeUnit, completionQueue);        

Creates executor which will put the finised tasks into completonQueue. It can be used to create logic similar to CompletionService but only with task that managed to complete within timeout and deadline.

