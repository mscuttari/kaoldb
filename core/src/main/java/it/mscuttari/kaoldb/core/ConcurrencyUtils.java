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
