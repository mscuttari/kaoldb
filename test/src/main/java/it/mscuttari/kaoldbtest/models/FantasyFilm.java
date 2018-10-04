package it.mscuttari.kaoldbtest.models;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.DiscriminatorColumn;
import it.mscuttari.kaoldb.annotations.DiscriminatorValue;
import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.Inheritance;
import it.mscuttari.kaoldb.annotations.InheritanceType;
import it.mscuttari.kaoldb.annotations.Table;

@Entity
@Table(name = "fantasy_films")
@DiscriminatorValue(value = "Fantasy")
public final class FantasyFilm extends Film {

    /**
     * Default constructor
     */
    public FantasyFilm() {
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
    public FantasyFilm(String title, Integer year, Genre genre, Person director, Integer length) {
        super(title, year, genre, director, length);
    }

}
