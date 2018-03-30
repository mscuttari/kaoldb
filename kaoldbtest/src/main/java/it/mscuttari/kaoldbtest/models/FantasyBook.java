package it.mscuttari.kaoldbtest.models;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.Table;
import it.mscuttari.kaoldb.annotations.UniqueConstraint;

@Table(name = "fantasy_books", uniqueConstraints = @UniqueConstraint(columnNames = "setting"))
public class FantasyBook extends Book {

    @Column(name = "setting")
    private String setting;

}
