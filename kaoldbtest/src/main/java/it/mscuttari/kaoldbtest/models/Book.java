package it.mscuttari.kaoldbtest.models;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.DiscriminatorColumn;
import it.mscuttari.kaoldb.annotations.DiscriminatorType;
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
        @UniqueConstraint(columnNames = {"author_first_name", "author_last_name", "year"})
})
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "genre", discriminatorType = DiscriminatorType.STRING)
public class Book {

    @Id
    @JoinColumns(value = {
            @JoinColumn(name = "author_first_name", referencedColumnName = "first_name"),
            @JoinColumn(name = "author_last_name", referencedColumnName = "last_name")
    })
    private Person author;

    @Id
    @Column(name = "title")
    private String title;

    @JoinColumn(name = "genre", referencedColumnName = "name")
    private Genre genre;

    @Column(name = "year")
    private int year;

    @Column(name = "cost")
    private float cost;

}
