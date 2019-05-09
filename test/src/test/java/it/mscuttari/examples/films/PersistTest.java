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

import java.util.Calendar;

import it.mscuttari.kaoldb.interfaces.Expression;
import it.mscuttari.kaoldb.interfaces.QueryBuilder;
import it.mscuttari.kaoldb.interfaces.Root;

import static org.junit.Assert.*;

public class PersistTest extends AbstractFilmTest {

    @Test
    public void persistCountry() {
        Country country = new Country("Italy");

        em.persist(country);

        QueryBuilder<Country> qb = em.getQueryBuilder(Country.class);
        Root<Country> countryRoot = qb.getRoot(Country.class);
        Expression where = countryRoot.eq(Country_.name, country.name);

        qb.from(countryRoot).where(where);

        assertEquals(country, qb.build(countryRoot).getSingleResult());
    }


    @Test
    public void persistPerson() {
        Person person = new Person(
                "Robert",
                "Downey Jr.",
                getCalendar(1965, Calendar.APRIL, 4),
                new Country("USA")
        );

        em.persist(person.country);
        em.persist(person);

        QueryBuilder<Person> qb = em.getQueryBuilder(Person.class);
        Root<Person> personRoot = qb.getRoot(Person.class);

        qb.from(personRoot).where(
                personRoot.eq(Person_.firstName, person.firstName)
                        .and(personRoot.eq(Person_.lastName, person.lastName))
        );

        assertEquals(person, qb.build(personRoot).getSingleResult());
    }


    @Test
    public void persistGenre() {
        Genre genre = new Genre("Adventure");

        em.persist(genre);

        QueryBuilder<Genre> qb = em.getQueryBuilder(Genre.class);
        Root<Genre> genreRoot = qb.getRoot(Genre.class);
        Expression where = genreRoot.eq(Genre_.name, genre.name);

        qb.from(genreRoot).where(where);

        Genre result = qb.build(genreRoot).getSingleResult();
        assertEquals(genre, result);
    }


    @Test
    public void persistFantasyFilm() {
        Person director = new Person(
                "David",
                "Yates",
                getCalendar(1963, Calendar.OCTOBER, 8),
                new Country("UK")
        );

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
        Root<Film> filmRoot = filmQb.getRoot(Film.class);

        filmQb.from(filmRoot).where(
                filmRoot.eq(Film_.title, film.title)
                        .and(filmRoot.eq(Film_.year, film.year))
        );

        assertEquals(film, filmQb.build(filmRoot).getSingleResult());

        // Specific query
        QueryBuilder<FantasyFilm> fantasyFilmQb = em.getQueryBuilder(FantasyFilm.class);
        Root<FantasyFilm> fantasyFilmRoot = fantasyFilmQb.getRoot(FantasyFilm.class);

        fantasyFilmQb.from(fantasyFilmRoot).where(
                fantasyFilmRoot.eq(FantasyFilm_.title, film.title)
                        .and(fantasyFilmRoot.eq(FantasyFilm_.year, film.year))
        );

        assertEquals(film, fantasyFilmQb.build(fantasyFilmRoot).getSingleResult());
    }


    @Test
    public void persistThrillerFilm() {
        Person director = new Person(
                "Christopher",
                "Nolan",
                getCalendar(1970, Calendar.JULY, 30),
                new Country("UK")
        );

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
        Root<Film> filmRoot = filmQb.getRoot(Film.class);

        filmQb.from(filmRoot).where(
                filmRoot.eq(Film_.title, film.title)
                        .and(filmRoot.eq(Film_.year, film.year))
        );

        assertEquals(film, filmQb.build(filmRoot).getSingleResult());

        // Specific query
        QueryBuilder<ThrillerFilm> thrillerFilmQb = em.getQueryBuilder(ThrillerFilm.class);
        Root<ThrillerFilm> thrillerFilmRoot = filmQb.getRoot(ThrillerFilm.class);

        thrillerFilmQb.from(thrillerFilmRoot).where(
                thrillerFilmRoot.eq(ThrillerFilm_.title, film.title)
                        .and(thrillerFilmRoot.eq(ThrillerFilm_.year, film.year))
        );

        assertEquals(film, thrillerFilmQb.build(thrillerFilmRoot).getSingleResult());
    }

}