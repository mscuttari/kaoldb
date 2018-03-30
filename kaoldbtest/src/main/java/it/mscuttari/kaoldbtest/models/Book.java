package it.mscuttari.kaoldbtest.models;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.Id;
import it.mscuttari.kaoldb.annotations.Inheritance;
import it.mscuttari.kaoldb.annotations.InheritanceType;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.annotations.ManyToOne;
import it.mscuttari.kaoldb.annotations.Table;
import it.mscuttari.kaoldb.annotations.UniqueConstraint;

@Table(name = "books", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name", "year"}),
        @UniqueConstraint(columnNames = {"name", "year", "cost"})
})
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Book {

    @Id
    @Column(name = "id")
    private int id;

    @Column(name = "name")
    private String name;

    @Column(name = "year")
    private int year;

    @Column(name = "cost", unique = true)
    private float cost;

    @ManyToOne
    @JoinColumns(value = {
            @JoinColumn(name = "library_name", referencedColumnName = "name", type = String.class),
            @JoinColumn(name = "library_place", referencedColumnName = "place", type = String.class)
    })
    private Library library;


    @JoinTable(joinColumns = {
            @JoinColumn(name = "prova", referencedColumnName = "prova_referenced", type = String.class),
            @JoinColumn(name = "prova2", referencedColumnName = "referenced2", type = Integer.class)
    })
    private Library test;

}
