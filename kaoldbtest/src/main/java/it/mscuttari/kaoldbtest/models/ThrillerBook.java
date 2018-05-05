package it.mscuttari.kaoldbtest.models;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.DiscriminatorValue;
import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.Table;

@Entity
@Table(name = "thriller_books")
@DiscriminatorValue(value = "thriller")
public class ThrillerBook extends Book {

    @Column(name = "prova")
    public String prova;

    @Override
    public String toString() {
        return super.toString() + " -" + prova + "-";
    }
}
