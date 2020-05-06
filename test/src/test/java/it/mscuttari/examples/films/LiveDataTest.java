package it.mscuttari.examples.films;

import androidx.lifecycle.LiveData;

import org.junit.Test;

import java.util.Calendar;
import java.util.List;

import edu.emory.mathcs.backport.java.util.Collections;
import it.mscuttari.kaoldb.interfaces.Query;
import it.mscuttari.kaoldb.interfaces.QueryBuilder;
import it.mscuttari.kaoldb.interfaces.Root;

import static org.junit.Assert.assertEquals;

public class LiveDataTest extends AbstractFilmTest {

    @Test
    public void updateFilmDirectorCountry() {
        Person director = new Person("David", "Yates", getCalendar(1963, Calendar.OCTOBER, 8), new Country("IT"));
        FantasyFilm film = new FantasyFilm("Fantastic Beasts and Where to Find Them", 2016, director, 133, null);

        em.persist(director.country);
        em.persist(director);

        em.persist(film.genre);
        em.persist(film);

        QueryBuilder<Film> qb = em.getQueryBuilder(Film.class);
        Root<Film> root = qb.getRoot(Film.class);
        qb.from(root);
        Query<Film> query = qb.build(root);
        LiveData<List<Film>> liveData = query.getLiveResults();

        director.country = new Country("UK");
        em.persist(director.country);
        em.update(director);

        assertEquals(Collections.singletonList(film), liveData.getValue());
    }

}
