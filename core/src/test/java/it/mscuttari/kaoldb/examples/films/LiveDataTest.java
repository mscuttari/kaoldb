package it.mscuttari.kaoldb.examples.films;

import static org.hamcrest.junit.MatcherAssert.assertThat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.test.core.app.ActivityScenario;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.robolectric.Robolectric;

import java.util.Calendar;
import java.util.List;

import it.mscuttari.kaoldb.examples.films.models.Country;
import it.mscuttari.kaoldb.examples.films.models.FantasyFilm;
import it.mscuttari.kaoldb.examples.films.models.Film;
import it.mscuttari.kaoldb.examples.films.models.Person;
import it.mscuttari.kaoldb.interfaces.Query;
import it.mscuttari.kaoldb.interfaces.QueryBuilder;
import it.mscuttari.kaoldb.interfaces.Root;

/*
public class LiveDataTest extends AbstractFilmTest {

    static class FilmsListActivity extends AppCompatActivity {
        public List<Film> data;
    }

    @Test
    public void updateFilmDirectorCountry() {
        FilmsListActivity activity = Robolectric.setupActivity(FilmsListActivity.class);

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
        liveData.observe(activity, films -> activity.data = films);

        director.country = new Country("UK");
        em.persist(director.country);
        em.update(director);

        assertThat(liveData.getValue(), Matchers.containsInAnyOrder(film));
    }
}
*/
