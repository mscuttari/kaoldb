package it.mscuttari.kaoldbtest.films;

import it.mscuttari.kaoldb.annotations.DiscriminatorValue;
import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.Table;

@Entity
@Table(name = "fantasy_films")
@DiscriminatorValue(value = "Action")
public final class ActionFilm extends Film {

    /**
     * Default constructor
     */
    public ActionFilm() {
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
    public ActionFilm(String title, Integer year, Person director, Integer length, FilmRestriction restriction) {
        super(title, year, new Genre("Action"), director, length, restriction);
    }

}
