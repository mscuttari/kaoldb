package it.mscuttari.kaoldbtest.films;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Calendar;

import it.mscuttari.kaoldb.core.KaolDB;
import it.mscuttari.kaoldb.interfaces.EntityManager;
import it.mscuttari.kaoldb.interfaces.Expression;
import it.mscuttari.kaoldb.interfaces.QueryBuilder;
import it.mscuttari.kaoldb.interfaces.Root;
import it.mscuttari.kaoldbtest.R;

import static org.junit.Assert.*;

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

        Expression where = personRoot
                .eq(Person_.firstName, "Robert")
                .and(personRoot.eq(Person_.lastName, "Downey Jr."));

        qb.from(personRoot).where(where);

        Person result = qb.build("p").getSingleResult();
        assertEquals(person, result);
    }


    @Test
    public void persistFantasyFilm() {
        Person director = new Person(
                "David",
                "Yates",
                null,
                null
        );

        FantasyFilm film = new FantasyFilm(
                "Fantastic Beasts and Where to Find Them",
                2016,
                director,
                133,
                null
        );

        em.persist(director);
        em.persist(film.genre);
        em.persist(film);

        QueryBuilder<Film> qb = em.getQueryBuilder(Film.class);
        Root<Film> filmRoot = qb.getRoot(Film.class, "f");

        Expression where = filmRoot
                .eq(Film_.title, "Fantastic Beasts and Where to Find Them")
                .and(filmRoot.eq(Film_.year, 2016));

        qb.from(filmRoot).where(where);

        Film result = qb.build("f").getSingleResult();
        assertEquals(film, result);
    }

}