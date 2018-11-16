package it.mscuttari.kaoldbtest.films;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;

import it.mscuttari.kaoldb.interfaces.QueryBuilder;
import it.mscuttari.kaoldb.interfaces.Root;

import static org.junit.Assert.assertEquals;

public class JoinTest extends AbstractFilmTest {

    @Test
    public void getPeopleWithCountry() {
        // Countries
        Country usa = new Country("USA");
        Country italy = new Country("Italy");

        em.persist(usa);
        em.persist(italy);

        // People
        Person person1 = new Person(
                "Quentin",
                "Tarantino",
                getCalendar(1963, Calendar.MARCH, 27),
                usa
        );

        em.persist(person1);

        Person person2 = new Person(
                "Martin",
                "Scorsese",
                getCalendar(1942, Calendar.NOVEMBER, 17),
                italy
        );

        em.persist(person2);

        Person person3 = new Person(
                "Steven",
                "Spielberg",
                getCalendar(1946, Calendar.DECEMBER, 18),
                usa
        );

        em.persist(person3);

        // Get the people from a specific country
        QueryBuilder<Person> qb = em.getQueryBuilder(Person.class);
        Root<Person> personRoot = qb.getRoot(Person.class, "p");
        Root<Country> countryRoot = personRoot.leftJoin(Country.class, "c", Person_.country);
        qb.from(countryRoot);

        // USA
        qb.where(countryRoot.eq(Country_.name, usa.name));
        Collection<Person> peopleFromUSA = qb.build("p").getResultList();
        assertEquals(peopleFromUSA, Arrays.asList(person1, person3));

        // Italy
        qb.where(countryRoot.eq(Country_.name, italy.name));
        Collection<Person> peopleFromItaly = qb.build("p").getResultList();
        assertEquals(peopleFromItaly, Collections.singletonList(person2));
    }


    @Test
    public void getFilmsWithDirector() {
        Person director = new Person(
                "Quentin",
                "Tarantino",
                getCalendar(1963, Calendar.MARCH, 27),
                new Country("USA")
        );

        em.persist(director.country);
        em.persist(director);

        ThrillerFilm film1 = new ThrillerFilm(
                "Kill Bill: Volume 1",
                2003,
                director,
                106,
                null
        );

        ThrillerFilm film2 = new ThrillerFilm(
                "Kill Bill: Volume 2",
                2004,
                director,
                137,
                null
        );

        ActionFilm film3 = new ActionFilm(
                "Inglourious Bastards",
                2009,
                director,
                153,
                null
        );

        em.persist(film1.genre);
        em.persist(film3.genre);

        em.persist(film1);
        em.persist(film2);
        em.persist(film3);

        // Get the films
        QueryBuilder<Film> qb = em.getQueryBuilder(Film.class);
        Root<Film> filmRoot = qb.getRoot(Film.class, "f");
        Root<Person> personRoot = filmRoot.leftJoin(Person.class, "p", Film_.director);

        qb.from(personRoot).where(
                personRoot.eq(Person_.firstName, director.firstName)
                        .and(personRoot.eq(Person_.lastName, director.lastName))
        );

        Collection<Film> films = qb.build("f").getResultList();
        assertEquals(films, Arrays.asList(film1, film2, film3));
    }

}
