package it.mscuttari.kaoldbtest.models;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.Id;
import it.mscuttari.kaoldb.annotations.Table;

@Table(name = "libraries")
public class Library {

    @Id
    @Column(name = "name")
    private String name;

    @Id
    @Column(name = "place")
    private String place;

}
