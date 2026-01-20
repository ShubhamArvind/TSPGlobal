package com.price.publiser.service;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class CustomThreadPool {
    // The queue of tasks to be executed by the worker threads
    private final BlockingQueue<Runnable> taskQueue;

    // A list of all the worker threads created
    private final Thread[] workers;
    private volatile boolean isStopped = false;
    /**
     * Initializes the custom thread pool.
     * @param noOfThreads The number of threads in the pool.

     */
    public CustomThreadPool(int noOfThreads){
        this.taskQueue = new ArrayBlockingQueue<>(10);
        this.workers = new Thread[noOfThreads];
        // Create and start the worker threads
        for (int i = 0; i < noOfThreads; i++) {
            workers[i] = new WorkerThread(taskQueue, this);
            workers[i].start();
            System.out.println("Worker thread " + i + " started.");
        }
    }

    // Helper method for worker threads to check status
    protected synchronized boolean isStopped() {
        return this.isStopped;
    }

    /**
     * Submits a new task to the thread pool queue.
     * @param task The Runnable task to execute.
     * @throws InterruptedException
     */
    public void submit(Runnable task) throws InterruptedException {
        if (this.isStopped) {
            throw new IllegalStateException("ThreadPool is stopped");
        }
        this.taskQueue.put(task); // Adds the task to the queue, blocks if full
    }

    /**
     * Stops the thread pool gracefully.
     */
    public void stop() {
        this.isStopped = true;
        // Interrupt all worker threads to make them exit their loop
        for (Thread worker : workers) {
            worker.interrupt();
        }
    }
}

/**
 * The Runnable that continuously pulls tasks from the queue.
 */
class WorkerThread extends Thread{
    private final BlockingQueue<Runnable> taskQueue;
    private final CustomThreadPool pool;

    public WorkerThread(BlockingQueue<Runnable> taskQueue, CustomThreadPool pool) {
        this.taskQueue = taskQueue;
        this.pool = pool;
    }
    @Override
    public void run() {
        while (!pool.isStopped()) {
            try {
                // Takes a task from the queue; blocks if empty
                Runnable task = taskQueue.take();
                System.out.println(Thread.currentThread().getName() + " executing task...");
                task.run(); // Execute the task
                System.out.println(Thread.currentThread().getName() + " finished task.");
            } catch (InterruptedException e) {
                // Handle interruption during "take()", likely from pool.stop()
                if (!pool.isStopped()) {
                    // Log or handle unexpected interruption if not shutting down
                    System.err.println(Thread.currentThread().getName() + " was interrupted unexpectedly.");
                }
            }
        }
        System.out.println(Thread.currentThread().getName() + " stopped.");
    }
}