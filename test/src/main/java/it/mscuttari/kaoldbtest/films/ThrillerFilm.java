package it.mscuttari.kaoldbtest.films;

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
        this(null, null, null, null, FilmRestriction.MIN14);
    }


    /**
     * Constructor
     *
     * @param   title           title
     * @param   year            year
     * @param   director        director
     * @param   length          length
     * @param   restriction     restriction
     */
    public ThrillerFilm(String title, Integer year, Person director, Integer length, FilmRestriction restriction) {
        super(title, year, new Genre("Thriller"), director, length, restriction);
    }

}
