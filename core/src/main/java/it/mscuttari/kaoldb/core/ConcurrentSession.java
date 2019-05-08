/*
 * Copyright 2018 Scuttari Michele
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.mscuttari.kaoldb.core;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import it.mscuttari.kaoldb.exceptions.KaolDBException;

/**
 * A concurrent session must be intended as a set of concurrently executing tasks that must all
 * terminate successfully. If any of them fails, all the others are cancelled.
 *
 * @param <T>   type of the tasks results
 */
class ConcurrentSession<T> implements Iterable<T> {

    private static ExecutorService executorService;
    private final List<Future<T>> tasks = new ArrayList<>();
    private final List<T> results = new ArrayList<>();
    private final AtomicBoolean finished = new AtomicBoolean(true);


    /**
     * Constructor
     */
    public ConcurrentSession() {
        if (executorService == null) {
            int cpuCores = Runtime.getRuntime().availableProcessors();

            ConcurrentSession.executorService = new ThreadPoolExecutor(
                    cpuCores,
                    Integer.MAX_VALUE,
                    60L,
                    TimeUnit.SECONDS,
                    new SynchronousQueue<>()
            );
        }
    }


    /**
     * Submits a {@link Runnable} task for execution and returns a {@link Future}
     * representing that task
     *
     * @param task  task to be executed
     */
    @SuppressWarnings("unchecked")
    public synchronized void submit(Runnable task) {
        finished.set(false);
        Future<T> future = (Future<T>) executorService.submit(task);
        tasks.add(future);
    }


    /**
     * Submits a value-returning task for execution and returns a {@link Future}
     * representing the pending results of the task
     *
     * @param task  task to be executed
     */
    public synchronized void submit(Callable<T> task) {
        finished.set(false);
        Future<T> future = executorService.submit(task);
        tasks.add(future);
    }


    /**
     * Wait for all the task to be completed
     */
    public synchronized void waitForAll() throws ExecutionException, InterruptedException {
        for (Future<T> task : tasks) {
            try {
                results.add(task.get());

            } catch (ExecutionException | InterruptedException e) {
                for (Future<?> t : tasks)
                    t.cancel(true);

                throw e;
            }
        }

        doAndNotifyAll(finished, () -> finished.set(true));
    }


    @NonNull
    @Override
    public Iterator<T> iterator() {
        waitWhile(finished, () -> !finished.get());
        return results.iterator();
    }


    /**
     * Wait until a condition is satisfied
     *
     * @param lock          object to be locked
     * @param condition     condition to be checked
     *
     * @throws KaolDBException in case of thread interruption
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void waitWhile(Object lock, SynchCondition condition) throws KaolDBException {
        synchronized (lock) {
            while (condition.check()) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    throw new KaolDBException(e);
                }
            }
        }
    }


    /**
     * Execute an action and notify all the threads waiting on the lock
     *
     * @param lock          object to be locked
     * @param action        action to be executed
     *
     * @throws KaolDBException in case of thread interruption
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void doAndNotifyAll(Object lock, Runnable action) {
        synchronized (lock) {
            try {
                action.run();
            } finally {
                lock.notifyAll();
            }
        }
    }


    public interface SynchCondition {
        boolean check();
    }

}
