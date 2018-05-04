package it.mscuttari.kaoldbtest.models;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.DiscriminatorColumn;
import it.mscuttari.kaoldb.annotations.DiscriminatorType;
import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.Id;
import it.mscuttari.kaoldb.annotations.Inheritance;
import it.mscuttari.kaoldb.annotations.InheritanceType;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.Table;

@Entity
@Table(name = "books")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "genre", discriminatorType = DiscriminatorType.STRING)
public abstract class Book {

    @Id
    @JoinColumns(value = {
            @JoinColumn(name = "author_first_name", referencedColumnName = "first_name"),
            @JoinColumn(name = "author_last_name", referencedColumnName = "last_name")
    })
    private Person author;

    @Id
    @Column(name = "title")
    public String title;

    @Column(name = "genre")
    public String genre;

    @Column(name = "year")
    public int year;

    @Column(name = "cost")
    public Float cost;

    @Override
    public String toString() {
        return title;
    }

}
