package it.mscuttari.kaoldb.core;

import androidx.annotation.NonNull;

class Propagation {

    public enum Action {
        NO_ACTION("NO ACTION"),
        RESTRICT("RESTRICT"),
        SET_NULL("SET NULL"),
        SET_DEFAULT("SET DEFAULT"),
        CASCADE("CASCADE");

        private final String action;

        Action(String action) {
            this.action = action;
        }

        @NonNull
        @Override
        public String toString() {
            return action;
        }
    }


    private final Action onUpdate;
    private final Action onDelete;


    /**
     * Constructor
     *
     * @param onUpdate      on update action
     * @param onDelete      on delete action
     */
    public Propagation(Action onUpdate, Action onDelete) {
        this.onUpdate = onUpdate;
        this.onDelete = onDelete;
    }


    @NonNull
    @Override
    public String toString() {
        return "ON UPDATE " + onUpdate + " ON DELETE " + onDelete + " DEFERRABLE INITIALLY DEFERRED";
    }

}
