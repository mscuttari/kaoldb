package it.mscuttari.kaoldbtest.films;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Calendar;

import it.mscuttari.kaoldb.core.KaolDB;
import it.mscuttari.kaoldb.interfaces.EntityManager;
import it.mscuttari.kaoldb.interfaces.Expression;
import it.mscuttari.kaoldb.interfaces.QueryBuilder;
import it.mscuttari.kaoldb.interfaces.Root;
import it.mscuttari.kaoldbtest.R;

import static org.junit.Assert.*;

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class PersistTest {

    private KaolDB kdb;
    private static final String databaseName = "films";
    private EntityManager em;


    @Before
    public void setUp() {
        // KaolDB instance
        kdb = KaolDB.getInstance();
        kdb.setConfig(RuntimeEnvironment.application, R.xml.persistence);
        kdb.setDebugMode(true);

        // Entity manager
        em = kdb.getEntityManager(RuntimeEnvironment.application, databaseName);
        em.deleteDatabase();
    }


    @After
    public void tearDown() {
        EntityManager em = kdb.getEntityManager(RuntimeEnvironment.application, databaseName);
        assertTrue(em.deleteDatabase());
    }


    @Test
    public void persistCountry() {
        Country country = new Country("Italy");

        em.persist(country);

        QueryBuilder<Country> qb = em.getQueryBuilder(Country.class);
        Root<Country> countryRoot = qb.getRoot(Country.class, "c");
        Expression where = countryRoot.eq(Country_.name, country.name);

        qb.from(countryRoot).where(where);

        assertEquals(country, qb.build("c").getSingleResult());
    }


    @Test
    public void persistPerson() {
        Person person = new Person(
                "Robert",
                "Downey Jr.",
                Calendar.getInstance(),
                new Country("USA")
        );

        person.birthDate.set(Calendar.YEAR, 1965);
        person.birthDate.set(Calendar.MONTH, Calendar.APRIL);
        person.birthDate.set(Calendar.DAY_OF_MONTH, 4);

        em.persist(person.country);
        em.persist(person);

        QueryBuilder<Person> qb = em.getQueryBuilder(Person.class);
        Root<Person> personRoot = qb.getRoot(Person.class, "p");

        qb.from(personRoot).where(
                personRoot.eq(Person_.firstName, person.firstName)
                        .and(personRoot.eq(Person_.lastName, person.lastName))
        );

        assertEquals(person, qb.build("p").getSingleResult());
    }


    @Test
    public void persistGenre() {
        Genre genre = new Genre("Adventure");

        em.persist(genre);

        QueryBuilder<Genre> qb = em.getQueryBuilder(Genre.class);
        Root<Genre> genreRoot = qb.getRoot(Genre.class, "g");
        Expression where = genreRoot.eq(Genre_.name, genre.name);

        qb.from(genreRoot).where(where);

        Genre result = qb.build("g").getSingleResult();
        assertEquals(genre, result);
    }


    @Test
    public void persistFantasyFilm() {
        Person director = new Person(
                "David",
                "Yates",
                Calendar.getInstance(),
                new Country("UK")
        );

        director.birthDate.set(Calendar.YEAR, 1963);
        director.birthDate.set(Calendar.MONTH, Calendar.OCTOBER);
        director.birthDate.set(Calendar.DAY_OF_MONTH, 8);

        FantasyFilm film = new FantasyFilm(
                "Fantastic Beasts and Where to Find Them",
                2016,
                director,
                133,
                null
        );

        em.persist(director.country);
        em.persist(director);

        em.persist(film.genre);
        em.persist(film);

        // Polymorphic query
        QueryBuilder<Film> filmQb = em.getQueryBuilder(Film.class);
        Root<Film> filmRoot = filmQb.getRoot(Film.class, "f");

        filmQb.from(filmRoot).where(
                filmRoot.eq(Film_.title, film.title)
                        .and(filmRoot.eq(Film_.year, film.year))
        );

        assertEquals(film, filmQb.build("f").getSingleResult());

        // Specific query
        QueryBuilder<FantasyFilm> fantasyFilmQb = em.getQueryBuilder(FantasyFilm.class);
        Root<FantasyFilm> fantasyFilmRoot = fantasyFilmQb.getRoot(FantasyFilm.class, "f");

        fantasyFilmQb.from(fantasyFilmRoot).where(
                fantasyFilmRoot.eq(FantasyFilm_.title, film.title)
                        .and(fantasyFilmRoot.eq(FantasyFilm_.year, film.year))
        );

        assertEquals(film, fantasyFilmQb.build("f").getSingleResult());
    }


    @Test
    public void persistThrillerFilm() {
        Person director = new Person(
                "Christopher",
                "Nolan",
                Calendar.getInstance(),
                new Country("UK")
        );

        director.birthDate.set(Calendar.YEAR, 1970);
        director.birthDate.set(Calendar.MONTH, Calendar.JULY);
        director.birthDate.set(Calendar.DAY_OF_MONTH, 30);

        ThrillerFilm film = new ThrillerFilm(
                "Memento",
                2000,
                director,
                113,
                null
        );

        em.persist(director.country);
        em.persist(director);

        em.persist(film.genre);
        em.persist(film);

        // Polymorphic query
        QueryBuilder<Film> filmQb = em.getQueryBuilder(Film.class);
        Root<Film> filmRoot = filmQb.getRoot(Film.class, "f");

        filmQb.from(filmRoot).where(
                filmRoot.eq(Film_.title, film.title)
                        .and(filmRoot.eq(Film_.year, film.year))
        );

        assertEquals(film, filmQb.build("f").getSingleResult());

        // Specific query
        QueryBuilder<ThrillerFilm> thrillerFilmQb = em.getQueryBuilder(ThrillerFilm.class);
        Root<ThrillerFilm> thrillerFilmRoot = filmQb.getRoot(ThrillerFilm.class, "f");

        thrillerFilmQb.from(thrillerFilmRoot).where(
                thrillerFilmRoot.eq(ThrillerFilm_.title, film.title)
                        .and(thrillerFilmRoot.eq(ThrillerFilm_.year, film.year))
        );

        assertEquals(film, thrillerFilmQb.build("f").getSingleResult());
    }

}