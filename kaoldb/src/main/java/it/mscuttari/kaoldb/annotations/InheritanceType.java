package it.mscuttari.kaoldb.annotations;

public enum InheritanceType {
    SINGLE_TABLE,
    TABLE_PER_CLASS,
    JOINED;

    private InheritanceType() {

    }

}