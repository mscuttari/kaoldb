package it.mscuttari.kaoldbtest.models;

import it.mscuttari.kaoldb.annotations.DiscriminatorValue;
import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.Table;

@Entity
@Table(name = "thriller_films")
@DiscriminatorValue(value = "Thriller")
public final class ThrillerFilm extends Film {

    /**
     * Default constructor
     */
    public ThrillerFilm() {
        this(null, null, null, null, null, FilmRestriction.MIN14);
    }


    /**
     * Constructor
     *
     * @param   title           title
     * @param   year            year
     * @param   genre           genre
     * @param   director        director
     * @param   length          length
     * @param   restriction     restriction
     */
    public ThrillerFilm(String title, Integer year, Genre genre, Person director, Integer length, FilmRestriction restriction) {
        super(title, year, genre, director, length, restriction);
    }

}
