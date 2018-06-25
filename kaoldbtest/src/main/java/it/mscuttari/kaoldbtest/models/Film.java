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
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.annotations.ManyToMany;
import it.mscuttari.kaoldb.annotations.ManyToOne;
import it.mscuttari.kaoldb.annotations.OneToMany;
import it.mscuttari.kaoldb.annotations.Table;

@Entity
@Table(name = "films")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "genre", discriminatorType = DiscriminatorType.STRING)
public abstract class Film {

    @Id
    @Column(name = "title")
    public String title;

    @Id
    @Column(name = "year")
    public Integer year;

    @JoinColumn(name = "genre", referencedColumnName = "name")
    @ManyToOne
    public Genre genre;

    @ManyToMany
    @JoinTable(
            name = "films_directors",
            joinColumns = {
                    @JoinColumn(name = "director_first_name", referencedColumnName = "first_name"),
                    @JoinColumn(name = "director_last_name", referencedColumnName = "last_name")
            })
    public Person director;
    
    @Column(name = "length")
    public Integer length;


    /**
     * Default constructor
     */
    public Film() {
        this(null, null, null, null, null);
    }


    /**
     * Constructor
     *
     * @param   title       title
     * @param   year        year
     * @param   genre       genre
     * @param   director    director
     * @param   length      length
     */
    public Film(String title, Integer year, Genre genre, Person director, Integer length) {
        this.title = title;
        this.year = year;
        this.genre = genre;
        this.director = director;
        this.length = length;
    }

}
