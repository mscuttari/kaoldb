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
@Table(name = "films")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "genre", discriminatorType = DiscriminatorType.STRING)
public abstract class Film {

    @Id
    @Column(name = "title")
    public String title;

    @Column(name = "year")
    public Integer year;

    @JoinColumn(name = "genre", referencedColumnName = "name")
    public Genre genre;

    @JoinColumns(value = {
            @JoinColumn(name = "director_first_name", referencedColumnName = "first_name"),
            @JoinColumn(name = "director_last_name", referencedColumnName = "last_name")
    })
    public Person director;

}
