package it.mscuttari.kaoldbtest.models;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.Id;
import it.mscuttari.kaoldb.annotations.Table;

@Table(name = "genres")
public class Genre {

    @Id
    @Column(name = "name")
    private String name;

}
