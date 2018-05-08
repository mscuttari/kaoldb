package it.mscuttari.kaoldbtest;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import it.mscuttari.kaoldb.core.EntityManagerFactory;
import it.mscuttari.kaoldb.core.KaolDB;
import it.mscuttari.kaoldb.interfaces.EntityManager;
import it.mscuttari.kaoldb.interfaces.Expression;
import it.mscuttari.kaoldb.interfaces.Query;
import it.mscuttari.kaoldb.interfaces.QueryBuilder;
import it.mscuttari.kaoldb.interfaces.Root;
import it.mscuttari.kaoldbtest.models.Film;
import it.mscuttari.kaoldbtest.models.Film_;
import it.mscuttari.kaoldbtest.models.Genre;
import it.mscuttari.kaoldbtest.models.Genre_;
import it.mscuttari.kaoldbtest.models.Person;
import it.mscuttari.kaoldbtest.models.Person_;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PersistTest {

    @Test
    public void setupKaolDB() {
        Context context = InstrumentationRegistry.getTargetContext();

        KaolDB instance = KaolDB.getInstance();
        instance.setConfig(context, R.xml.persistence);
    }


    @Test
    public void createPerson() {
        setupKaolDB();

        EntityManagerFactory emf = EntityManagerFactory.getInstance();
        EntityManager em = emf.getEntityManager(InstrumentationRegistry.getTargetContext(), "films");
        em.deleteDatabase();

        Person person = new Person();
        person.firstName = "Robert";
        person.lastName = "Downey Jr";

        em.persist(person);

        QueryBuilder<Person> qb = em.getQueryBuilder(Person.class);
        Root<Person> personRoot = qb.getRoot(Person.class, "p");
        Expression where = personRoot.eq(Person_.firstName, "Robert").and(personRoot.eq(Person_.lastName, "Downey Jr"));
        qb.from(personRoot).where(where);

        Person result = qb.build("p").getSingleResult();
        assertNotNull(result);
        assertEquals(result.firstName, "Robert");
        assertEquals(result.lastName, "Downey Jr");
    }


    @Test
    public void createFilm() {
        setupKaolDB();

        EntityManagerFactory emf = EntityManagerFactory.getInstance();
        EntityManager em = emf.getEntityManager(InstrumentationRegistry.getTargetContext(), "films");
        em.deleteDatabase();

        // Create fantasy genre
        Genre genre = new Genre();
        genre.name = "Fantasy";
        em.persist(genre);

        // Create film
        Film film = new Film();
        film.title = "Avengers - Infinity War";
        film.year = 2018;
        film.genre = genre;
        em.persist(film);

        // Get film
        QueryBuilder<Film> qb = em.getQueryBuilder(Film.class);
        Root<Film> root = qb.getRoot(Film.class, "f");
        Expression where = root.eq(Film_.genre, genre);
        Film queryResult = qb.from(root).where(where).build("f").getSingleResult();

        assertNotNull(queryResult);
        assertNotNull(queryResult.genre);
        assertEquals(queryResult.title, "Avengers - Infinity War");
        assertEquals(queryResult.year, Integer.valueOf(2018));
        assertEquals(queryResult.genre.name, "Fantasy");
    }

}
