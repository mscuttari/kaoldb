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

import it.mscuttari.kaoldb.exceptions.KaolDBException;

class ConcurrencyUtils {

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
