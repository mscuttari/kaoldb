package it.mscuttari.kaoldbtest.models;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.DiscriminatorValue;
import it.mscuttari.kaoldb.annotations.Table;

@Table(name = "thriller_books")
@DiscriminatorValue(value = "thriller")
public class ThrillerBook extends Book {

    @Column(name = "prova")
    public String prova;

}
