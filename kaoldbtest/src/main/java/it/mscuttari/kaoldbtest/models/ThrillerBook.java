package it.mscuttari.kaoldbtest.models;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.Table;

@Table(name = "thriller_books")
public class ThrillerBook extends Book {

    @Column(name = "age", unique = true)
    private int age;

}
