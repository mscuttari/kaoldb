package it.mscuttari.kaoldb.core;

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
     * @param   onUpdate    on update action
     * @param   onDelete    on delete action
     */
    public Propagation(Action onUpdate, Action onDelete) {
        this.onUpdate = onUpdate;
        this.onDelete = onDelete;
    }


    @Override
    public String toString() {
        return "ON UPDATE " + onUpdate + " on DELETE " + onDelete;
    }

}
