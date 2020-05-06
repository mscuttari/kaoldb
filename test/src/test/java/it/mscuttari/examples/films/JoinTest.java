/*
 * Copyright 2018 Scuttari Michele
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.mscuttari.examples.films;

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
        Person person1 = new Person("Quentin", "Tarantino", getCalendar(1963, Calendar.MARCH, 27), usa);
        em.persist(person1);

        Person person2 = new Person("Martin", "Scorsese", getCalendar(1942, Calendar.NOVEMBER, 17), italy);
        em.persist(person2);

        Person person3 = new Person("Steven", "Spielberg", getCalendar(1946, Calendar.DECEMBER, 18), usa);
        em.persist(person3);

        // Get the people from a specific country
        QueryBuilder<Person> qb = em.getQueryBuilder(Person.class);

        Root<Person> personRoot = qb.getRoot(Person.class);
        Root<Country> countryRoot = qb.getRoot(Country.class);

        Root<Person> joinRoot = personRoot.join(countryRoot, Person_.country);
        qb.from(joinRoot);

        // USA
        qb.where(countryRoot.eq(Country_.name, usa.name));
        Collection<Person> peopleFromUSA = qb.build(personRoot).getResults();
        assertEquals(peopleFromUSA, Arrays.asList(person1, person3));

        // Italy
        qb.where(countryRoot.eq(Country_.name, italy.name));
        Collection<Person> peopleFromItaly = qb.build(personRoot).getResults();
        assertEquals(peopleFromItaly, Collections.singletonList(person2));
    }

    @Test
    public void getFilmsWithDirector() {
        Person director = new Person("Quentin", "Tarantino", getCalendar(1963, Calendar.MARCH, 27), new Country("USA"));
        em.persist(director.country);
        em.persist(director);

        ThrillerFilm film1 = new ThrillerFilm("Kill Bill: Volume 1", 2003, director, 106, null);
        ThrillerFilm film2 = new ThrillerFilm("Kill Bill: Volume 2", 2004, director, 137, null);
        ActionFilm film3 = new ActionFilm("Inglourious Bastards", 2009, director, 153, null);

        em.persist(film1.genre);
        em.persist(film3.genre);

        em.persist(film1);
        em.persist(film2);
        em.persist(film3);

        // Get the films
        QueryBuilder<Film> qb = em.getQueryBuilder(Film.class);

        Root<Film> filmRoot = qb.getRoot(Film.class);
        Root<Person> personRoot = qb.getRoot(Person.class);

        Root<Film> joinRoot = filmRoot.join(personRoot, Film_.director);

        qb.from(joinRoot).where(
                personRoot.eq(Person_.firstName, director.firstName)
                        .and(personRoot.eq(Person_.lastName, director.lastName))
        );

        Collection<Film> films = qb.build(filmRoot).getResults();
        assertEquals(films, Arrays.asList(film1, film2, film3));
    }

    @Test
    public void getFilmsWithGenre() {
        Person director = new Person("Quentin", "Tarantino", getCalendar(1963, Calendar.MARCH, 27), new Country("USA"));
        em.persist(director.country);
        em.persist(director);

        ThrillerFilm film1 = new ThrillerFilm("Kill Bill: Volume 1", 2003, director, 106, null);
        ThrillerFilm film2 = new ThrillerFilm("Kill Bill: Volume 2", 2004, director, 137, null);
        ActionFilm film3 = new ActionFilm("Inglourious Bastards", 2009, director, 153, null);

        em.persist(film1.genre);
        em.persist(film3.genre);

        em.persist(film1);
        em.persist(film2);
        em.persist(film3);

        // Get the films
        QueryBuilder<Film> qb = em.getQueryBuilder(Film.class);

        Root<Film> filmRoot = qb.getRoot(Film.class);
        Root<Genre> genreRoot = qb.getRoot(Genre.class);

        // Without join
        qb.from(filmRoot).where(filmRoot.eq(Film_.genre, new Genre("Thriller")));
        assertEquals(qb.build(filmRoot).getResults(), Arrays.asList(film1, film2));

        // With join
        Root<Film> joinRoot = filmRoot.join(genreRoot, Film_.genre);
        qb.from(joinRoot).where(genreRoot.eq(Genre_.name, "Thriller"));
        assertEquals(qb.build(filmRoot).getResults(), Arrays.asList(film1, film2));
    }


    @Test
    public void getFilmsWithGenreAndDirector() {
        Person director = new Person("Quentin", "Tarantino", getCalendar(1963, Calendar.MARCH, 27), new Country("USA"));
        em.persist(director.country);
        em.persist(director);

        ThrillerFilm film1 = new ThrillerFilm("Kill Bill: Volume 1", 2003, director, 106, null);
        ThrillerFilm film2 = new ThrillerFilm("Kill Bill: Volume 2", 2004, director, 137, null);
        ActionFilm film3 = new ActionFilm("Inglourious Bastards", 2009, director, 153, null);

        em.persist(film1.genre);
        em.persist(film3.genre);

        em.persist(film1);
        em.persist(film2);
        em.persist(film3);

        // Get the films
        QueryBuilder<Film> qb = em.getQueryBuilder(Film.class);

        Root<Film> filmRoot = qb.getRoot(Film.class);
        Root<Genre> genreRoot = qb.getRoot(Genre.class);
        Root<Person> personRoot = qb.getRoot(Person.class);

        // Without join
        qb.from(filmRoot).where(
                filmRoot.eq(Film_.genre, film1.genre)
                .and(filmRoot.eq(Film_.director, director))
        );

        assertEquals(qb.build(filmRoot).getResults(), Arrays.asList(film1, film2));

        // With join
        Root<Film> joinRoot = filmRoot.join(genreRoot, Film_.genre).join(personRoot, Film_.director);

        qb.from(joinRoot).where(
                genreRoot.eq(Genre_.name, film1.genre.name)
                .and(personRoot.eq(Person_.firstName, director.firstName))
                .and(personRoot.eq(Person_.lastName, director.lastName))
        );

        assertEquals(qb.build(filmRoot).getResults(), Arrays.asList(film1, film2));
    }


    @Test
    public void getFilmsWithDirectorFromCountry() {
        // Countries
        Country usa = new Country("USA");
        Country italy = new Country("Italy");

        em.persist(usa);
        em.persist(italy);

        // People
        Person person1 = new Person("Quentin", "Tarantino", getCalendar(1963, Calendar.MARCH, 27), usa);
        Person person2 = new Person("Martin", "Scorsese", getCalendar(1942, Calendar.NOVEMBER, 17), italy);
        Person person3 = new Person("Steven", "Spielberg", getCalendar(1946, Calendar.DECEMBER, 18), usa);

        em.persist(person1);
        em.persist(person2);
        em.persist(person3);

        // Films
        ThrillerFilm film1 = new ThrillerFilm("Kill Bill: Volume 1", 2003, person1, 106, null);
        ThrillerFilm film2 = new ThrillerFilm("Kill Bill: Volume 2", 2004, person1, 137, null);
        ActionFilm film3 = new ActionFilm("Inglourious Bastards", 2009, person1, 153, null);
        ThrillerFilm film4 = new ThrillerFilm("Shutter Island", 2010, person2, 138, null);
        FantasyFilm film5 = new FantasyFilm("Jurassic Park", 1993, person3, 127, null);

        em.persist(film1.genre);
        em.persist(film3.genre);
        em.persist(film5.genre);

        em.persist(film1);
        em.persist(film2);
        em.persist(film3);
        em.persist(film4);
        em.persist(film5);

        // Get the films
        QueryBuilder<Film> qb = em.getQueryBuilder(Film.class);

        Root<Film> filmRoot = qb.getRoot(Film.class);
        Root<Person> personRoot = qb.getRoot(Person.class);
        Root<Country> countryRoot = qb.getRoot(Country.class);

        qb.from(filmRoot.join(personRoot.join(countryRoot, Person_.country), Film_.director))
                .where(countryRoot.eq(Country_.name, usa.name));
        assertEquals(qb.build(filmRoot).getResults(), Arrays.asList(film1, film2, film3, film5));
    }

}
