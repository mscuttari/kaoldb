package it.mscuttari.examples.films;

import it.mscuttari.kaoldb.annotations.DiscriminatorValue;
import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.Table;

@Entity
@Table(name = "fantasy_films")
@DiscriminatorValue(value = "Fantasy")
public final class FantasyFilm extends Film {

    /**
     * Default constructor
     */
    public FantasyFilm() {
        this(null, null, null, null, FilmRestriction.NONE);
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
    public FantasyFilm(String title, Integer year, Person director, Integer length, FilmRestriction restriction) {
        super(title, year, new Genre("Fantasy"), director, length, restriction);
    }

}
